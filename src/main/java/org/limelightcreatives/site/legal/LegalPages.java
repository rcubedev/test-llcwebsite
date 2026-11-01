package org.limelightcreatives.site.legal;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.TextContentRenderer;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Loads and renders legal documents from {@code content/legal/*.md} at startup.
 *
 * <p>File format:
 * <pre>
 * ---
 * title: "Privacy policy."
 * intro: "Intro text, **Markdown** allowed"
 * lastUpdated: "31 August 2026"
 * ---
 *
 * ## Section title
 * Section body as normal Markdown (paragraphs, lists, links, tables, etc.).
 * </pre>
 * Each {@code ##} heading becomes its own section with a unique, linkable id.
 *
 * <p>{@code title} and {@code lastUpdated} are required; {@code intro} is optional and must be a
 * single Markdown paragraph.
 *
 * <p>Content before the first {@code ##} heading is not allowed.
 */
@Service
public class LegalPages {

    private static final Pattern FRONT_MATTER = Pattern.compile("\\A---\\R(.*?)\\R---(?:\\R|\\z)(.*)\\z", Pattern.DOTALL);
    private static final Pattern SLUG = Pattern.compile("[a-z0-9-]+");

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final TextContentRenderer textRenderer = TextContentRenderer.builder().build();
    private final Map<String, LegalPage> pages;

    /**
     * Supplies consistent styling for elements rendered from Markdown
     */
    private static final class LegalStyles implements AttributeProvider {
        private static final String TABLE = "w-full border-collapse text-left"
                + " [&_thead_tr]:border-b-2 [&_thead_tr]:border-black"
                + " [&_tbody_tr]:border-b [&_tbody_tr]:border-black/20"
                + " [&_th]:py-4 [&_th]:pr-6 [&_th]:font-display [&_th]:text-xl"
                + " [&_td]:py-5 [&_td]:pr-6";


        @Override
        public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
            String classes = switch (node) {
                case Heading h when h.getLevel() == 3 -> "mt-10 font-display text-2xl tracking-[-0.03em] first:mt-0";
                case Paragraph ignored -> "[&:not(:first-child)]:mt-6";
                case BulletList ignored -> "mt-6 list-disc space-y-3 pl-6 first:mt-0";
                case OrderedList ignored -> "mt-6 list-decimal space-y-3 pl-6 first:mt-0";
                case Link ignored -> "underline underline-offset-4 hover:opacity-50";
                default -> null;
            };
            if (classes != null) attributes.put("class", classes);
        }
    }

    public LegalPages() {
        List<Extension> extensions = List.of(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .escapeHtml(true) // content is Markdown only; raw HTML is shown as text
                .attributeProviderFactory(content -> new LegalStyles())
                .build();
        this.pages = this.loadAll();
    }

    /**
     * Finds a legal document by its URL identifier
     *
     * @param slug the document URL identifier
     * @return the matching document, or an empty optional if it does not exist
     */
    public Optional<LegalPage> find(String slug) {
        return Optional.ofNullable(this.pages.get(slug));
    }

    /**
     * Returns all loaded legal documents, sorted by their URL identifiers.
     *
     * @return an immutable list of legal documents
     */
    public List<LegalPage> all() {
        return this.pages.values().stream().sorted(Comparator.comparing(LegalPage::slug)).toList();
    }

    private Map<String, LegalPage> loadAll() {
        try {
            Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath:content/legal/*.md");
            return Arrays.stream(files)
                    .map(this::parse)
                    .collect(Collectors.toUnmodifiableMap(LegalPage::slug, Function.identity()));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load content/legal/*.md", e);
        }
    }

    private LegalPage parse(Resource file) {
        String filename = file.getFilename();
        if (filename == null || !filename.endsWith(".md"))
            throw new IllegalStateException("Legal document has an invalid filename: " + file.getDescription());

        String slug = filename.substring(0, filename.length() - ".md".length());
        if (!SLUG.matcher(slug).matches()) {
            throw new IllegalStateException("Invalid legal page filename: " + filename
                    + ". Filenames must contain only lowercase letters, digits, and hyphens.");
        }

        String raw;
        try {
            raw = file.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't read " + filename, e);
        }

        Matcher matcher = FRONT_MATTER.matcher(raw);
        if (!matcher.matches()) throw new IllegalStateException("Malformed legal page '" + filename + "'.\n" +
                "Legal pages need front matter between --- lines.");

        Object loaded = new Yaml().load(matcher.group(1));
        if (!(loaded instanceof Map<?, ?> meta)) {
            throw new IllegalStateException(
                    "Invalid front matter in '" + filename + "'. Expected a YAML mapping.");
        }

        String intro = string(meta, "intro");

        return new LegalPage(
                slug,
                requiredString(meta, "title", filename),
                this.renderInline(intro, filename),
                this.plainText(intro),
                requiredString(meta, "lastUpdated", filename),
                List.copyOf(this.parseSections(matcher.group(2), filename)));
    }

    /**
     * Splits a document body into sections.
     *
     * <p>Each level-two heading starts a new section. Section ids are derived from the heading text
     * and made unique by appending {@code -2}, {@code -3} and so on.
     *
     * @param body the Markdown body, with the front matter already removed
     * @param filename the source filename, used in error messages
     * @return the sections in document order
     * @throws IllegalStateException if there is content before the first section heading, or a
     *                               heading produces an empty id
     */
    private List<LegalPage.Section> parseSections(String body, String filename) {
        List<LegalPage.Section> sections = new ArrayList<>();
        Set<String> usedIds = new HashSet<>();

        Heading current = null;
        List<Node> blocks = new ArrayList<>();

        for (Node node = this.parser.parse(body).getFirstChild(); node != null; node = node.getNext()) {
            if (node instanceof Heading h && h.getLevel() == 2) {
                if (current != null) sections.add(this.buildSection(current, blocks, usedIds, filename));
                current = h;
                blocks = new ArrayList<>();
            } else if (current == null) {
                throw new IllegalStateException("Legal page '" + filename + "' has content before its first "
                        + "'##' heading. Put introductory text in the 'intro' front matter field.");
            } else {
                blocks.add(node);
            }
        }
        if (current != null) sections.add(this.buildSection(current, blocks, usedIds, filename));

        return sections;
    }

    private LegalPage.Section buildSection(Heading heading, List<Node> blocks, Set<String> usedIds, String filename) {
        String title = this.textRenderer.render(heading).strip();
        String baseId = slugify(title);
        if (baseId.isEmpty()) {
            throw new IllegalStateException("Section heading '" + title + "' in legal page '" + filename
                    + "' does not produce a valid id. Use at least one letter or digit.");
        }

        String id = baseId;
        for (int n = 2; !usedIds.add(id); n++) {
            id = baseId + "-" + n;
        }

        String html = blocks.stream().map(this.renderer::render).collect(Collectors.joining());
        return new LegalPage.Section(id, title, wrapTables(html).strip());
    }


    private static String wrapTables(String html) {
        // tables scroll horizontally on smaller screens
        return html
                .replace("<table>", "<div class=\"mt-10 overflow-x-auto first:mt-0\"><table class=\"" + LegalStyles.TABLE + "\">")
                .replace("</table>", "</table></div>");
    }

    // renders a single Markdown paragraph without its surrounding <p> element
    private @Nullable String renderInline(@Nullable String markdown, String filename) {
        if (markdown == null || markdown.isBlank()) return null;

        Node first = this.parser.parse(markdown).getFirstChild();
        if (!(first instanceof Paragraph paragraph) || first.getNext() != null) {
            throw new IllegalStateException(
                    "The 'intro' in legal page '" + filename + "' must be a single Markdown paragraph.");
        }

        StringBuilder html = new StringBuilder();
        for (Node child = paragraph.getFirstChild(); child != null; child = child.getNext()) {
            html.append(this.renderer.render(child));
        }
        return html.toString().strip();
    }

    private @Nullable String plainText(@Nullable String markdown) {
        return markdown == null || markdown.isBlank() ? null : this.textRenderer.render(this.parser.parse(markdown)).strip();
    }

    private static @Nullable String string(Map<?, ?> meta, String key) {
        Object value = meta.get(key);
        if (value == null) return null;
        if (!(value instanceof String string)) {
            throw new IllegalStateException("Expected '" + key + "' in legal page front matter to be a string.");
        }
        return string;
    }

    private static String requiredString(Map<?, ?> meta, String key, String filename) {
        String value = string(meta, key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required '" + key + "' in legal page '" + filename + "'.");
        }
        return value;
    }

    /**
     * Converts a heading into a URL-friendly identifier.
     *
     * <p>e.g. "Children's privacy" -> "childrens-privacy"
     *
     * @param text the heading text
     * @return the heading's URL-friendly identifier, which is empty if the text has no letters or digits
     */
    static String slugify(String text) {
        return text.toLowerCase().strip()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");
    }
}

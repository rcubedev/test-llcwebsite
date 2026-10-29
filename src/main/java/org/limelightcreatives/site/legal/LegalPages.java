package org.limelightcreatives.site.legal;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.BulletList;
import org.commonmark.node.Heading;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Loads every {@code content/legal/*.md} file once at startup.
 *
 * <p>File format:
 * <pre>
 * ---
 * title: "Privacy policy."
 * intro: "Intro text, **markdown** allowed."
 * lastUpdated: "31 August 2026"
 * ---
 *
 * ## Section title
 * Section body as normal markdown (paragraphs, lists, links, tables).
 * </pre>
 * Each {@code ## heading} becomes its own section with a linkable id.
 */
@Service
public class LegalPages {

    private static final Pattern FRONT_MATTER = Pattern.compile("\\A---\\R(.*?)\\R---\\R?(.*)\\z", Pattern.DOTALL);
    private static final Pattern SECTION_SPLIT = Pattern.compile("\\R(?=##\\s)");

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final Map<String, LegalPage> pages;

    public LegalPages() {
        List<Extension> extensions = List.of(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .escapeHtml(true) // content is Markdown only; raw HTML is shown as text
                .attributeProviderFactory(context -> new LegalStyles())
                .build();
        this.pages = loadAll();
    }

    public Optional<LegalPage> find(String slug) {
        return Optional.ofNullable(pages.get(slug));
    }

    public List<LegalPage> all() {
        return pages.values().stream().sorted(Comparator.comparing(LegalPage::slug)).toList();
    }

    private Map<String, LegalPage> loadAll() {
        try {
            Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath:content/legal/*.md");
            return Arrays.stream(files)
                    .map(this::parse)
                    .collect(Collectors.toUnmodifiableMap(LegalPage::slug, Function.identity()));
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't read content/legal", e);
        }
    }

    private LegalPage parse(Resource file) {
        String filename = file.getFilename();
        String slug = filename.substring(0, filename.length() - ".md".length());

        String raw;
        try {
            raw = file.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't read " + filename, e);
        }

        var matcher = FRONT_MATTER.matcher(raw);
        if (!matcher.matches()) {
            throw new IllegalStateException(filename + " needs front matter between --- lines");
        }
        Map<String, Object> meta = new Yaml().load(matcher.group(1));
        String body = matcher.group(2);

        List<LegalPage.Section> sections = new ArrayList<>();
        for (String block : SECTION_SPLIT.split(body)) {
            block = block.strip();
            if (block.isEmpty()) continue;

            String[] lines = block.split("\\R", 2);
            String title = lines[0].replaceFirst("^##\\s*", "").strip();
            String markdown = lines.length > 1 ? lines[1].strip() : "";
            sections.add(new LegalPage.Section(slugify(title), title, render(markdown)));
        }

        return new LegalPage(
                slug,
                string(meta, "title"),
                renderInline(string(meta, "intro")),
                plainText(string(meta, "intro")),
                string(meta, "lastUpdated"),
                List.copyOf(sections));
    }

    private String render(String markdown) {
        String html = renderer.render(parser.parse(markdown));
        // Tables scroll sideways on small screens.
        return html
                .replace("<table>", "<div class=\"mt-10 overflow-x-auto first:mt-0\"><table class=\"" + LegalStyles.TABLE + "\">")
                .replace("</table>", "</table></div>");
    }

    /** Renders a single line of Markdown without the surrounding paragraph. */
    private String renderInline(String markdown) {
        if (markdown == null) return null;
        String html = renderer.render(parser.parse(markdown)).strip();
        return html.replaceFirst("^<p[^>]*>", "").replaceFirst("</p>$", "");
    }

    private String plainText(String markdown) {
        return markdown == null ? null : TextContentRenderer.builder().build().render(parser.parse(markdown)).strip();
    }

    private static String string(Map<String, Object> meta, String key) {
        Object value = meta == null ? null : meta.get(key);
        return value == null ? null : value.toString();
    }

    /** "Children's privacy" -> "childrens-privacy", so sections can link to each other with #ids. */
    static String slugify(String text) {
        return text.toLowerCase().strip()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");
    }

    /** Adds the Tailwind classes each Markdown element gets, so content files never need styling. */
    private static final class LegalStyles implements AttributeProvider {
        static final String TABLE = "w-full border-collapse text-left"
                + " [&_thead_tr]:border-b-2 [&_thead_tr]:border-black"
                + " [&_tbody_tr]:border-b [&_tbody_tr]:border-black/20"
                + " [&_th]:py-4 [&_th]:pr-6 [&_th]:font-display [&_th]:text-xl"
                + " [&_td]:py-5 [&_td]:pr-6";

        @Override
        public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
            String classes = switch (node) {
                case Heading h when h.getLevel() == 3 -> "mt-10 font-display text-2xl tracking-[-0.03em] first:mt-0";
                case Paragraph p -> "[&:not(:first-child)]:mt-6";
                case BulletList l -> "mt-6 list-disc space-y-3 pl-6 first:mt-0";
                case OrderedList l -> "mt-6 list-decimal space-y-3 pl-6 first:mt-0";
                case Link l -> "underline underline-offset-4 hover:opacity-50";
                default -> null;
            };
            if (classes != null) attributes.put("class", classes);
        }
    }
}

package org.limelightcreatives.site.web;

import org.limelightcreatives.site.content.SiteContent;
import org.limelightcreatives.site.legal.LegalPage;
import org.limelightcreatives.site.legal.LegalPages;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every page on the site: its URL, template and the data the template needs.
 *
 * <p>Both ways of serving the site go through here, so they can't drift apart:
 * {@link SiteController} renders these on request, and the exporter renders
 * {@link #all()} to static files.
 */
@Component
public class SitePages {

    /** A page's model always includes {@code site}, {@code home} and {@code meta} (its {@link PageMeta}). */
    public record Page(String path, String template, PageMeta meta, Map<String, Object> model) {}

    private static final String NAME = "Limelight Creatives";

    private final SiteContent site;
    private final LegalPages legal;

    public SitePages(SiteContent site, LegalPages legal) {
        this.site = site;
        this.legal = legal;
    }

    public Page home() {
        return page("/", "index", true,
                NAME + " - Teen Filmmaking Event in Sydney",
                "Limelight Creatives runs free film-a-thons for high schoolers; no experience needed!",
                Map.of());
    }

    public Optional<Page> legal(String slug) {
        return legal.find(slug).map(this::legalPage);
    }

    public Page notFound() {
        return page("/404", "error/404", false, "Page not found | " + NAME, "This page doesn't exist.", Map.of());
    }

    /** All pages that exist, for the static export. */
    public List<Page> all() {
        List<Page> pages = new ArrayList<>();
        pages.add(home());
        legal.all().stream().map(this::legalPage).forEach(pages::add);
        return pages;
    }

    private Page legalPage(LegalPage page) {
        // "Privacy policy." -> "Privacy policy | Limelight Creatives"
        String title = page.title().replaceFirst("\\.$", "") + " | " + NAME;
        return page("/" + page.slug(), "legal", true, title, page.summary(), Map.of("legal", page));
    }

    private Page page(String path, String template, boolean indexed, String title, String description,
                      Map<String, Object> extra) {
        var meta = new PageMeta(title, description, site.url() + path, indexed);
        Map<String, Object> model = new HashMap<>(extra);
        model.put("site", site);
        model.put("home", path.equals("/"));
        model.put("meta", meta);
        return new Page(path, template, meta, Map.copyOf(model));
    }
}

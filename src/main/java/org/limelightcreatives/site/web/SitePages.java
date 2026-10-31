package org.limelightcreatives.site.web;

import org.limelightcreatives.site.content.SiteContent;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the pages available on the site and constructs their view models.
 *
 * <p>Centralises page paths, templates and metadata so they are consistent
 * across the site's controllers and other consumers.
 */
@Component
public class SitePages {

    private static final String NAME = "Limelight Creatives"; // todo(fut): site.yml (?)

    private final SiteContent site;

    public SitePages(SiteContent site) {
        this.site = site;
    }

    /**
     * Returns all page definitions, excluding {@link #notFound()}.
     *
     * @return all pages available on the site
     */
    public List<Page> all() {
        return List.of(home());
    }

    /**
     * Returns the homepage.
     *
     * @return the homepage definition
     */
    public Page home() {
        return page("/", "index", true,
                NAME + " - Teen Filmmaking Event in Sydney",
                "Limelight Creatives runs free film-a-thons for high schoolers; no experience needed!");
    }

    /**
     * Returns the not found page.
     *
     * @return the not found page definition
     */
    public Page notFound() {
        return page("/404", "error/404", false,
                "Page not found | " + NAME,
                "This page doesn't exist.");
    }

    private Page page(String path, String template, boolean indexed, String title, String description, Map<String, Object> extra) {
        URI pathUri = URI.create(path);
        PageMeta meta = new PageMeta(title, description, this.site.url().resolve(pathUri), indexed);

        Map<String, Object> model = new HashMap<>(extra);
        model.put("site", this.site);
        model.put("home", path.equals("/"));
        model.put("meta", meta);

        return new Page(pathUri, template, meta, model);
    }

    private Page page(String path, String template, boolean indexed, String title, String description) {
        return page(path, template, indexed, title, description, Map.of());
    }
}

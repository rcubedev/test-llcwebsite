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
        return page("/", true,
                NAME + " - Teen Filmmaking Event in Sydney",
                "Limelight Creatives runs free film-a-thons for high schoolers; no experience needed!");
    }

    /**
     * Returns the not found page.
     *
     * @return the not found page definition
     */
    public Page notFound() {
        return page("/404", false,
                "Page not found | " + NAME,
                "This page doesn't exist.");
    }

    /**
     * Returns the internal server error page.
     *
     * @return the internal server error page definition
     */
    public Page internalServerError() {
        return page("/500", false,
                "Something went wrong | " + NAME,
                "Something went wrong on our end. Please try again later.");
    }

    private Page page(String path, boolean indexed, String title, String description) {
        URI pathUri = URI.create(path);
        PageMeta meta = new PageMeta(title, description, this.site.url().resolve(pathUri), indexed);

        return new Page(pathUri, meta);
    }
}

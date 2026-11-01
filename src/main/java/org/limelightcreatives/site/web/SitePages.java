package org.limelightcreatives.site.web;

import org.limelightcreatives.site.content.SiteContent;
import org.limelightcreatives.site.legal.LegalPage;
import org.limelightcreatives.site.legal.LegalPages;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
    private final LegalPages legal;

    public SitePages(SiteContent site, LegalPages legal) {
        this.site = site;
        this.legal = legal;
    }

    /**
     * Returns all page definitions.
     *
     * @return all pages available on the site
     */
    public List<Page> all() {
        List<Page> pages = new ArrayList<>();
        pages.add(home());
        pages.add(notFound());
        pages.add(internalServerError());
        this.legal.all().stream().map(this::legalPage).forEach(pages::add);
        return Collections.unmodifiableList(pages);
    }

    /**
     * Returns a legal page by its slug.
     *
     * @param slug the legal page slug
     * @return the legal content and its site page definition, if found
     */
    public Optional<LegalPageDefinition> legal(String slug) {
        return this.legal.find(slug).map(content -> new LegalPageDefinition(content, this.legalPage(content)));
    }

    /**
     * Returns the homepage.
     *
     * @return the homepage definition
     */
    public Page home() {
        return this.page("/", true,
                NAME + " - Teen Filmmaking Event in Sydney",
                "Limelight Creatives runs free film-a-thons for high schoolers; no experience needed!");
    }

    /**
     * Returns the not found page.
     *
     * @return the not found page definition
     */
    public Page notFound() {
        return this.page("/404", false,
                "Page not found | " + NAME,
                "This page doesn't exist.");
    }

    /**
     * Returns the internal server error page.
     *
     * @return the internal server error page definition
     */
    public Page internalServerError() {
        return this.page("/500", false,
                "Something went wrong | " + NAME,
                "Something went wrong on our end. Please try again later.");
    }

    private Page legalPage(LegalPage page) {
        String title = page.title().replaceFirst("\\.$", "") + " | " + NAME;
        String description = page.summary() != null
                ? page.summary()
                : page.title();

        return this.page("/" + page.slug(), true, title, description);
    }

    private Page page(String path, boolean indexed, String title, String description) {
        URI pathUri = URI.create(path);
        URI url = URI.create(this.site.url()).resolve(pathUri);
        PageMeta meta = new PageMeta(title, description, url, indexed);

        return new Page(pathUri, meta);
    }
}

package org.limelightcreatives.site.web;

import java.net.URI;

/**
 * Metadata describing how a page is presented to search engines and link previews.
 *
 * <p>Pages that are not indexed are excluded from search engine indexing and the sitemap.
 *
 * @param title the page title
 * @param description the page description
 * @param url the canonical page URL
 * @param indexed whether the page should be indexed by search engines
 */
public record PageMeta(String title, String description, URI url, boolean indexed) {}

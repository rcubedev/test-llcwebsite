package org.limelightcreatives.site.web;

/**
 * What search engines and link previews see for a page: the {@code <title>}, the description,
 * and its canonical address. Pages that aren't {@code indexed} (the 404) are kept out of search
 * results and the sitemap.
 */
public record PageMeta(String title, String description, String url, boolean indexed) {}

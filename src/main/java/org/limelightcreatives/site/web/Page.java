package org.limelightcreatives.site.web;

import java.net.URI;

/**
 * Represents a website page.
 *
 * @param path the local absolute URL path
 * @param meta the page's metadata
 */
public record Page(URI path, PageMeta meta) {

    public Page {
        if (path.isAbsolute() || path.getRawAuthority() != null || path.getRawPath() == null || !path.getRawPath().startsWith("/"))
            throw new IllegalArgumentException("Page path must be a local absolute path");
    }
}

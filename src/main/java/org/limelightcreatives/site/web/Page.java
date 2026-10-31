package org.limelightcreatives.site.web;

import java.net.URI;
import java.util.Map;

/**
 * Represents a page, including its path, template, metadata and model.
 *
 * @param path the page URL's path
 * @param template the Thymeleaf template used to render the page
 * @param meta the page's metadata
 * @param model the attributes required by the template
 */
public record Page(URI path, String template, PageMeta meta, Map<String, Object> model) {

    public Page {
        if (path().isAbsolute() || path().getRawAuthority() != null || path().getRawPath() == null || !path().getRawPath().startsWith("/"))
            throw new IllegalArgumentException("Page path must be a local absolute path");

        model = Map.copyOf(model);
    }
}

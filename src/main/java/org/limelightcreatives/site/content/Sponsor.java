package org.limelightcreatives.site.content;

import java.net.URI;

/**
 * An event sponsor.
 *
 * @param name the sponsor's name
 * @param logo the sponsor's logo URI
 * @param link the sponsor's website URI
 */
public record Sponsor(String name, URI logo, URI link) {}

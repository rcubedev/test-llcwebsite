package org.limelightcreatives.site.content;

/**
 * An event sponsor.
 *
 * @param name the sponsor's name
 * @param logo the sponsor's logo path or URL
 * @param link the sponsor's website URL
 */
public record Sponsor(String name, String logo, String link) {}

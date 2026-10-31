package org.limelightcreatives.site.content;

/**
 * A navigation link on the homepage.
 *
 * @param label the displayed link text
 * @param target the target section identifier
 */
public record NavLink(String label, String target) {}

package org.limelightcreatives.site.content;

import java.net.URI;

/**
 * A step subsection.
 *
 * @param number the displayed step number
 * @param rotation the image rotation in degrees
 * @param title the step description, containing HTML
 * @param image the image URI
 * @param imageAlt the image alternative text
 */
public record Step(String number, double rotation, String title, URI image, String imageAlt) {}

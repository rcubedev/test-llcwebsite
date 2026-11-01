package org.limelightcreatives.site.content;

/**
 * A step subsection.
 *
 * @param number the displayed step number
 * @param rotation the image rotation in degrees
 * @param title the step description, containing HTML
 * @param image the image path or URL
 * @param imageAlt the image alternative text
 */
public record Step(String number, double rotation, String title, String image, String imageAlt) {}

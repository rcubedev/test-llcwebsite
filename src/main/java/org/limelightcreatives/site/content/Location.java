package org.limelightcreatives.site.content;

/**
 * A venue location for this event.
 *
 * @param label the location's purpose
 * @param venue the venue name
 * @param address the venue's street address
 */
public record Location(String label, String venue, String address) {}

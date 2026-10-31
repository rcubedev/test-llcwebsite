package org.limelightcreatives.site.content;

import org.jspecify.annotations.Nullable;

/**
 * A scheduled activity within an event day.
 *
 * @param time the displayed start time
 * @param name the activity name
 * @param kind the activity classification
 * @param tag the optional displayed label
 */
// todo(fut): String kind -> [enum Kind] kind (?)
public record Slot(String time, String name, String kind, @Nullable String tag) {

    /**
     * Determines whether this activity is a milestone.
     *
     * @return {@code true} if this activity is a milestone
     */
    public boolean milestone() {
        return "milestone".equals(this.kind);
    }
}

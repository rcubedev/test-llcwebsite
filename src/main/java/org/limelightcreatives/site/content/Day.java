package org.limelightcreatives.site.content;

import java.util.List;

/**
 * A day in the event schedule.
 *
 * @param label the displayed day label
 * @param date the displayed date
 * @param slots the scheduled activities for the day
 */
public record Day(String label, String date, List<Slot> slots) {}

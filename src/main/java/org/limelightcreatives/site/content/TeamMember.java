package org.limelightcreatives.site.content;

/**
 * A member of the team.
 *
 * @param name the team member's name
 * @param position the team member's role
 * @param image the team member's portrait path or URL
 */
public record TeamMember(String name, String position, String image) {}

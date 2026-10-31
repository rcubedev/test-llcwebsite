package org.limelightcreatives.site.content;

import java.net.URI;

/**
 * A member of the team.
 *
 * @param name the team member's name
 * @param position the team member's role
 * @param image the team member's portrait URI
 */
public record TeamMember(String name, String position, URI image) {}

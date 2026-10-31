package org.limelightcreatives.site.content;

import java.net.URI;

/**
 * A social media profile.
 *
 * @param name the social media platform's name
 * @param icon the platform's icon identifier
 * @param url the social media profile URI
 */
// todo(fut): String icon -> [enum SocialIcon] icon (?)
public record Social(String name, String icon, URI url) {}

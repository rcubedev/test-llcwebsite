package org.limelightcreatives.site.content;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.net.URI;
import java.util.List;

/**
 * Website content.
 *
 * @param url the canonical website URI
 * @param event the event details
 * @param nav the homepage navigation links
 * @param keyDates the important event dates
 * @param locations the event venues
 * @param steps the steps describing how the event works
 * @param schedule the event schedule
 * @param sponsors the event sponsors
 * @param faqs the frequently asked questions
 * @param team the team members
 * @param socials the social media profiles
 */
@ConfigurationProperties("site")
public record SiteContent(
        URI url,
        @NestedConfigurationProperty Event event,
        List<NavLink> nav,
        List<KeyDate> keyDates,
        List<Location> locations,
        List<Step> steps,
        List<Day> schedule,
        List<Sponsor> sponsors,
        List<Faq> faqs,
        List<TeamMember> team,
        List<Social> socials
) {}

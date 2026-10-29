package org.limelightcreatives.site.content;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * The home page content, bound from {@code content/site.yml}.
 * Spring fills these records in at startup; templates read them as {@code ${site...}}.
 */
@ConfigurationProperties("site")
public record SiteContent(
        String url,
        Event event,
        List<NavLink> nav,
        List<KeyDate> keyDates,
        List<Location> locations,
        List<Step> steps,
        List<Day> schedule,
        List<Sponsor> sponsors,
        List<Faq> faqs,
        List<TeamMember> team,
        List<Social> socials
) {

    public record Event(String banner, String youtubeId, String filloutId, String contactEmail, String parentsGuide) {
        public String videoThumbnail() {
            return "https://i.ytimg.com/vi/" + youtubeId + "/hqdefault.jpg";
        }

        public String videoEmbed() {
            return "https://www.youtube-nocookie.com/embed/" + youtubeId + "?autoplay=1&rel=0";
        }

        /** Fillout's embed URL: the version of the form made to be shown inside another site. */
        public String filloutUrl() {
            return "https://embed.fillout.com/t/" + filloutId;
        }
    }

    public record NavLink(String label, String target) {}

    public record KeyDate(String date, String label) {}

    public record Location(String label, String venue, String address) {}

    /** {@code title} is HTML. */
    public record Step(String n, double rotate, String title, String image, String imageAlt) {}

    public record Day(String label, String date, List<Slot> slots) {}

    public record Slot(String time, String name, String kind, String tag) {
        public boolean milestone() {
            return "milestone".equals(kind);
        }
    }

    public record Sponsor(String name, String logo, String link) {}

    public record Faq(String question, String answer) {}

    public record TeamMember(String name, String position, String image) {}

    public record Social(String name, String icon, String url) {}
}

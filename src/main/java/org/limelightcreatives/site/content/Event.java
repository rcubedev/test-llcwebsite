package org.limelightcreatives.site.content;

import java.net.URI;

/**
 * Details of an event.
 *
 * @param banner the announcement banner text
 * @param youtubeId the YouTube video identifier
 * @param filloutId the Fillout registration form identifier
 * @param contactEmail the event contact email address
 * @param parentsGuide the URL of the parents' and guardians' guide
 */
public record Event(String banner, String youtubeId, String filloutId, String contactEmail, String parentsGuide) {

    /**
     * Returns the thumbnail URI for the event introduction video.
     *
     * @return the video thumbnail URI
     */
    public URI videoThumbnail() {
        return URI.create("https://i.ytimg.com/vi/" + this.youtubeId + "/hqdefault.jpg");
    }

    /**
     * Returns the embed URI for the event introduction video.
     *
     * @return the video embed URI
     */
    public URI videoEmbed() {
        return URI.create("https://www.youtube-nocookie.com/embed/" + this.youtubeId + "?autoplay=1&rel=0");
    }

    /**
     * Returns the embed URI for the event registration form.
     *
     * @return the event registration form URI
     */
    public URI filloutUrl() {
        return URI.create("https://embed.fillout.com/t/" + this.filloutId);
    }
}

package org.limelightcreatives.site.legal;

import java.util.List;

/**
 * One legal page (privacy, code of conduct, ...), parsed from {@code content/legal/<slug>.md}.
 * {@code introHtml} and each section's {@code html} are already rendered from Markdown;
 * {@code summary} is the intro as plain text, for search results and link previews.
 */
public record LegalPage(
        String slug,
        String title,
        String introHtml,
        String summary,
        String lastUpdated,
        List<Section> sections
) {
    public record Section(String id, String title, String html) {}
}

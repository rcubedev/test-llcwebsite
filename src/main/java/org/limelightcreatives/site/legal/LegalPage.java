package org.limelightcreatives.site.legal;

import java.util.List;

/**
 * A legal document parsed from a Markdown file in {@code content/legal/}.
 *
 * @param slug the document's URL identifier
 * @param title the document's title
 * @param introHtml the introduction rendered as HTML from Markdown
 * @param summary the introduction rendered as plain text for metadata and previews
 * @param lastUpdated the date the document was last updated
 * @param sections the document's sections, rendered as HTML from Markdown
 */
public record LegalPage(String slug, String title, String introHtml, String summary, String lastUpdated, List<Section> sections) {

    /**
     * A section within a legal document
     *
     * @param id the section's URL fragment identifier
     * @param title the section's heading
     * @param html the section's content rendered as HTML from Markdown
     */
    public record Section(String id, String title, String html) {}
}

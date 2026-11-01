package org.limelightcreatives.site.web;

import org.limelightcreatives.site.legal.LegalPage;

/**
 * Combines a legal document with its site page definition.
 *
 * @param content the legal document content
 * @param page the page's metadata and path
 */
public record LegalPageDefinition(LegalPage content, Page page) {
}

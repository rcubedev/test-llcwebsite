package org.limelightcreatives.site.web;

import org.limelightcreatives.site.content.SiteContent;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * {@code robots.txt} and {@code sitemap.xml}, built from {@link SitePages} so they always list
 * exactly the pages that exist. Served by {@link SiteController} and written by the exporter.
 */
@Component
public class CrawlerFiles {

    private final SiteContent site;
    private final SitePages pages;

    public CrawlerFiles(SiteContent site, SitePages pages) {
        this.site = site;
        this.pages = pages;
    }

    public String robotsTxt() {
        return """
                User-agent: *
                Allow: /

                Sitemap: %s/sitemap.xml
                """.formatted(site.url());
    }

    public String sitemapXml() {
        String urls = pages.all().stream()
                .map(SitePages.Page::meta)
                .filter(PageMeta::indexed)
                .map(meta -> "  <url><loc>" + escapeXml(meta.url()) + "</loc></url>")
                .collect(Collectors.joining("\n"));
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                %s
                </urlset>
                """.formatted(urls);
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

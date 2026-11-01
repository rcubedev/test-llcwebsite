package org.limelightcreatives.site.web;

import org.limelightcreatives.site.content.SiteContent;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Generates {@code robots.txt} and {@code sitemap.xml} from the site's configuration
 * and page definitions.
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
               
               Sitemap: %s
               """.formatted(URI.create(this.site.url()).resolve("/sitemap.xml"));
    }

    public String sitemapXml() {
        String urls = this.pages.all().stream()
                .map(Page::meta)
                .filter(PageMeta::indexed)
                .map(meta -> "  <url><loc>" + escapeXml(meta.url().toString()) + "</loc></url>")
                .collect(Collectors.joining("\n"));
        return """
               <?xml version="1.0" encoding="UTF-8"?>
               <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
               %s
               </urlset>
               """.formatted(urls);
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}

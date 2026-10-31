package org.limelightcreatives.site.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.limelightcreatives.site.content.SiteContent;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlerFilesTest {

    @Mock
    SiteContent site;

    @Mock
    SitePages pages;

    @Test
    void robotsTxt_ValidSiteUrl_IncludesSitemapUrl() {
        when(this.site.url()).thenReturn(URI.create("https://limelightcreatives.org/"));

        CrawlerFiles files = new CrawlerFiles(this.site, this.pages);

        assertEquals("""
                User-agent: *
                Allow: /
                
                Sitemap: https://limelightcreatives.org/sitemap.xml
                """, files.robotsTxt());
    }

    @Test
    void sitemapXml_indexedPages_IncludesOnlyIndexedPages() {
        Page indexedPage = new Page(
                URI.create("/"),
                "index",
                new PageMeta(
                        "Homepage",
                        "Homepage description",
                        URI.create("https://limelightcreatives.org/"),
                        true
                ),
                Map.of()
        );

        Page nonIndexedPage = new Page(
                URI.create("/notIndexed"),
                "notIndexed",
                new PageMeta(
                        "Not indexed",
                        "Not indexed description",
                        URI.create("https://limelightcreatives.org/notIndexed"),
                        false
                ),
                Map.of()
        );

        when(this.pages.all()).thenReturn(List.of(indexedPage, nonIndexedPage));

        CrawlerFiles files = new CrawlerFiles(this.site, this.pages);
        String sitemap = files.sitemapXml();

        assertTrue(sitemap.contains("<loc>https://limelightcreatives.org/</loc>"));
        assertFalse(sitemap.contains("/notIndexed"));
    }

    @Test
    void sitemapXml_UrlWithXmlCharacters_EscapesSpecialCharacters() {
        Page page = new Page(
                URI.create("/"),
                "index",
                new PageMeta(
                        "Homepage",
                        "Homepage description",
                        URI.create("https://limelightcreatives.org/?a=1&b=2"),
                        true
                ),
                Map.of()
        );

        when(this.pages.all()).thenReturn(List.of(page));

        CrawlerFiles files = new CrawlerFiles(this.site, this.pages);

        assertTrue(files.sitemapXml().contains("<loc>https://limelightcreatives.org/?a=1&amp;b=2</loc>"));
    }
}

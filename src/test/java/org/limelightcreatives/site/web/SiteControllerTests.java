package org.limelightcreatives.site.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SiteController.class)
class SiteControllerTests {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SitePages pages;

    @MockitoBean
    CrawlerFiles crawlerFiles;

    @MockitoBean
    Assets assets;

    /*@Test
    void home_PageRequested_ReturnsHomePage() throws Exception {
        Page home = new Page(
                URI.create("/"),
                "index",
                new PageMeta(
                        "Limelight Creatives",
                        "A filmmaking event for high schoolers.",
                        URI.create("https://limelightcreatives.org/"),
                        true
                ),
                Map.of(
                        "site", "Limelight Creatives",
                        "home", true
                )
        );

        when(this.pages.home()).thenReturn(home);

        this.mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("site", "Limelight Creatives"))
                .andExpect(model().attribute("home", true));
    }*/

    @Test
    void robotsTxt_Requested_ReturnsPlainText() throws Exception {
        String robotsTxt = """
                User-agent: *
                Allow: /
                
                Sitemap: https://limelightcreatives.org/sitemap.xml
                """;

        when(this.crawlerFiles.robotsTxt()).thenReturn(robotsTxt);

        this.mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(robotsTxt));
    }

    @Test
    void sitemapXml_Requested_ReturnsXml() throws Exception {
        String sitemapXml = """
               <?xml version="1.0" encoding="UTF-8"?>
               <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                 <url><loc>https://limelightcreatives.org/</loc></url>
               </urlset>
               """;

        when(this.crawlerFiles.sitemapXml()).thenReturn(sitemapXml);

        this.mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(sitemapXml));
    }
}

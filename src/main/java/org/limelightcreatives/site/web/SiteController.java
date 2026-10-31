package org.limelightcreatives.site.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SiteController {

    private final SitePages pages;
    private final CrawlerFiles crawlerFiles;

    public SiteController(SitePages pages, CrawlerFiles crawlerFiles) {
        this.pages = pages;
        this.crawlerFiles = crawlerFiles;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String robotsTxt() {
        return this.crawlerFiles.robotsTxt();
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemapXml() {
        return this.crawlerFiles.sitemapXml();
    }

    @GetMapping("/")
    public String home(Model model) {
        return render(this.pages.home(), model);
    }

    private static String render(Page page, Model model) {
        model.addAllAttributes(page.model());
        return page.template();
    }
}

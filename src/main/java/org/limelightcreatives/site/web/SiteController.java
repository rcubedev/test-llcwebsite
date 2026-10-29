package org.limelightcreatives.site.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/** Serves the site dynamically. Used by {@code bootRun}, and in production if the site ever needs a server. */
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
        return crawlerFiles.robotsTxt();
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemapXml() {
        return crawlerFiles.sitemapXml();
    }

    @GetMapping("/")
    public String home(Model model) {
        return render(pages.home(), model);
    }

    // [a-z-]+ so this never swallows static files like /favicon.ico
    @GetMapping("/{slug:[a-z-]+}")
    public String legal(@PathVariable String slug, Model model) {
        var page = pages.legal(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return render(page, model);
    }

    private static String render(SitePages.Page page, Model model) {
        model.addAllAttributes(page.model());
        return page.template();
    }
}

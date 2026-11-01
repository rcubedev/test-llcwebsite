package org.limelightcreatives.site.web;

import org.limelightcreatives.site.content.SiteContent;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URI;

@Controller
public class SiteController {

    private final SitePages pages;
    private final CrawlerFiles crawlerFiles;
    private final SiteContent site;
    private final Assets assets;

    public SiteController(SitePages pages, CrawlerFiles crawlerFiles, SiteContent site, Assets assets) {
        this.pages = pages;
        this.crawlerFiles = crawlerFiles;
        this.site = site;
        this.assets = assets;
    }

    @ModelAttribute("site")
    public SiteContent site() {
        return this.site;
    }

    @ModelAttribute("assets")
    public Assets assets() {
        return this.assets;
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
        Page home = this.pages.home();
        populateModel(home, model);
        return "index";
    }

    @GetMapping("/404")
    public String notFound(Model model) {
        Page notFound = this.pages.notFound();
        populateModel(notFound, model);
        return "error/404";
    }

    @GetMapping("/500")
    public String internalServerError(Model model) {
        Page notFound = this.pages.internalServerError();
        populateModel(notFound, model);
        return "error/500";
    }

    private static void populateModel(Page page, Model model) {
        model.addAttribute("meta", page.meta());
        model.addAttribute("home", URI.create("/").equals(page.path()));
    }
}

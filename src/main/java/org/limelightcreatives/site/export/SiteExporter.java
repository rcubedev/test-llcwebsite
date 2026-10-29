package org.limelightcreatives.site.export;

import org.limelightcreatives.site.web.CrawlerFiles;
import org.limelightcreatives.site.web.SitePages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Renders every page in {@link SitePages} to plain HTML and copies the static assets,
 * producing a folder any static host can serve.
 *
 * <p>Only active with the "export" profile; run it with {@code ./gradlew exportSite}.
 * Pages are written as {@code <path>/index.html} so URLs stay as they are ({@code /privacy}).
 */
@Component
@Profile("export")
public class SiteExporter implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SiteExporter.class);
    private static final String STATIC_ROOT = "/static/";

    private final SpringTemplateEngine templates;
    private final SitePages pages;
    private final CrawlerFiles crawlerFiles;
    private final Path out;

    public SiteExporter(SpringTemplateEngine templates, SitePages pages, CrawlerFiles crawlerFiles,
                        @Value("${export.out}") Path out) {
        this.templates = templates;
        this.pages = pages;
        this.crawlerFiles = crawlerFiles;
        this.out = out;
    }

    @Override
    public void run(String... args) throws IOException {
        // This program owns the output folder, so it also clears it: no stale pages survive.
        clean(out);

        for (SitePages.Page page : pages.all()) {
            String file = page.path().equals("/") ? "index.html" : page.path().substring(1) + "/index.html";
            write(file, page.template(), page.model());
        }
        SitePages.Page notFound = pages.notFound();
        write("404.html", notFound.template(), notFound.model());
        Files.writeString(out.resolve("robots.txt"), crawlerFiles.robotsTxt());
        Files.writeString(out.resolve("sitemap.xml"), crawlerFiles.sitemapXml());

        int assets = copyStaticAssets();
        log.info("Exported {} pages and {} assets to {}", pages.all().size() + 1, assets, out);
    }

    private void write(String file, String template, Map<String, Object> model) throws IOException {
        Path target = out.resolve(file);
        Files.createDirectories(target.getParent());
        Files.writeString(target, templates.process(template, new Context(Locale.ENGLISH, model)));
    }

    /** Copies everything under classpath:/static/ (CSS, JS, fonts, images, PDFs) into the output. */
    private int copyStaticAssets() throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        // There can be several static/ roots on the classpath (src/main/resources and the generated CSS).
        List<String> roots = new ArrayList<>();
        for (Resource root : resolver.getResources("classpath*:" + STATIC_ROOT)) {
            roots.add(root.getURL().toString());
        }

        int copied = 0;
        for (Resource resource : resolver.getResources("classpath*:" + STATIC_ROOT + "**")) {
            if (!resource.isReadable()) continue; // directories
            String url = resource.getURL().toString();
            String root = roots.stream().filter(url::startsWith).findFirst().orElse(null);
            if (root == null) continue;

            // URLs encode spaces etc. ("Olivia%20Li.webp"); decode back to the real file name.
            String relative = URLDecoder.decode(
                    url.substring(root.length()).replace("+", "%2B"), StandardCharsets.UTF_8);
            Path target = out.resolve(relative).normalize();
            if (!target.startsWith(out)) continue;

            Files.createDirectories(target.getParent());
            try (InputStream in = resource.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            copied++;
        }
        return copied;
    }

    private static void clean(Path dir) throws IOException {
        if (Files.notExists(dir)) return;
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}

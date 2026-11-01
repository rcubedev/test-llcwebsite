package org.limelightcreatives.site.export;

import org.limelightcreatives.site.web.Page;
import org.limelightcreatives.site.web.SitePages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

@Component
@Profile("export")
public class SiteExporter implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SiteExporter.class);
    private static final String STATIC_ROOT = "/static/";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final SitePages pages;
    private final WebServerApplicationContext context;
    private final Path out;

    public SiteExporter(SitePages pages, WebServerApplicationContext context, @Value("${export.out}") Path out) {
        this.pages = pages;
        this.context = context;
        this.out = out.toAbsolutePath().normalize();
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int port = this.context.getWebServer().getPort();
        if (port <= 0) throw new IllegalStateException("Export needs a listening web server, but port is " + port);
        this.export(URI.create("http://localhost:" + port));
    }

    private void export(URI base) throws IOException, InterruptedException {
        clean(this.out);

        List<String> paths = new ArrayList<>();
        for (Page page : this.pages.all())
            paths.add(page.path().getPath());
        paths.add("/robots.txt");
        paths.add("/sitemap.xml");

        HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        for (String path : paths) {
            HttpResponse<byte[]> response = fetch(client, base.resolve(path));
            this.write(fileFor(path, response), response.body());
        }

        int assets = this.copyStaticAssets();
        LOGGER.info("Exported {} pages and {} assets to {}", paths.size(), assets, this.out);
    }

    private static HttpResponse<byte[]> fetch(HttpClient client, URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) throw new IllegalStateException("GET " + uri.getPath() + " returned " + response.statusCode());
        return response;
    }

    private static String fileFor(String path, HttpResponse<?> response) {
        if ("/".equals(path)) return "index.html";
        boolean html = response.headers().firstValue("Content-Type")
                .map(type -> type.startsWith("text/html"))
                .orElse(false);
        return html ? path.substring(1) + ".html" : path.substring(1);
    }

    private void write(String file, byte[] content) throws IOException {
        Path target = this.out.resolve(file);
        Files.createDirectories(target.getParent());
        Files.write(target, content);
    }

    private int copyStaticAssets() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        List<String> roots = new ArrayList<>();
        for (Resource root : resolver.getResources("classpath*:" + STATIC_ROOT))
            roots.add(root.getURL().toString());

        int copied = 0;
        for (Resource resource : resolver.getResources("classpath*:" + STATIC_ROOT + "**")) {
            if (!resource.isReadable()) continue; // directories
            String url = resource.getURL().toString();
            String root = roots.stream().filter(url::startsWith).findFirst().orElse(null);
            if (root == null) continue;

            String relative = URLDecoder.decode(
                    url.substring(root.length()).replace("+", "%2B"), StandardCharsets.UTF_8);
            Path target = this.out.resolve(relative).normalize();
            if (!target.startsWith(this.out)) continue;

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

package org.limelightcreatives.site;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.site.web.SitePages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Every page renders, and unknown URLs get the 404 page. Run with ./gradlew test. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SitePagesTest {

    @Autowired MockMvc mvc;
    @Autowired SitePages pages;
    @LocalServerPort int port;

    @Test
    void everyPageRenders() throws Exception {
        assertThat(pages.all()).hasSize(4); // home + 3 legal pages
        for (SitePages.Page page : pages.all()) {
            mvc.perform(get(page.path()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Limelight Creatives acknowledges")));
        }
    }

    @Test
    void homePageHasAllContent() throws Exception {
        mvc.perform(get("/"))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("Storyboarding workshop"),
                        org.hamcrest.Matchers.containsString("Golden Age Cinema"),
                        org.hamcrest.Matchers.containsString("Ontik Reza"),
                        org.hamcrest.Matchers.containsString("What does it cost?"))));
    }

    @Test
    void everyPageHasItsOwnTitleDescriptionAndCanonicalLink() throws Exception {
        var titles = new java.util.HashSet<String>();
        for (SitePages.Page page : pages.all()) {
            String html = fetch(page.path()).body();
            assertThat(html).contains("<title>" + page.meta().title() + "</title>");
            assertThat(html).contains("<meta name=\"description\" content=\"" + page.meta().description() + "\">");
            assertThat(html).contains("<link rel=\"canonical\" href=\"https://limelightcreatives.org" + page.path() + "\">");
            assertThat(html).contains("<meta property=\"og:image\" content=\"https://limelightcreatives.org/assets/og.jpg\">");
            assertThat(html).doesNotContain("noindex");
            assertThat(titles.add(page.meta().title())).as("unique title for %s", page.path()).isTrue();
        }
        assertThat(fetch("/privacy").body()).contains("<title>Privacy policy | Limelight Creatives</title>");
    }

    @Test
    void everyPageHasOneMainHeading() throws Exception {
        for (SitePages.Page page : pages.all()) {
            assertThat(fetch(page.path()).body().split("<h1", -1)).as(page.path()).hasSize(2);
        }
    }

    @Test
    void notFoundPageIsKeptOutOfSearchResults() throws Exception {
        String html = fetch("/does-not-exist").body();
        assertThat(html).contains("<meta name=\"robots\" content=\"noindex\">");
        assertThat(html).doesNotContain("rel=\"canonical\"");
    }

    @Test
    void robotsTxtPointsToTheSitemap() throws Exception {
        var response = fetch("/robots.txt", "*/*");
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(type -> assertThat(type).startsWith("text/plain"));
        assertThat(response.body()).contains("Sitemap: https://limelightcreatives.org/sitemap.xml");
    }

    @Test
    void sitemapListsEveryPageAndNothingElse() throws Exception {
        String xml = fetch("/sitemap.xml", "*/*").body();
        for (SitePages.Page page : pages.all()) {
            assertThat(xml).contains("<loc>https://limelightcreatives.org" + page.path() + "</loc>");
        }
        assertThat(xml.split("<loc>", -1)).hasSize(pages.all().size() + 1);
        assertThat(xml).doesNotContain("/404");
    }

    @Test
    void imagesAreSmall() throws Exception {
        // Big photos are converted to WebP at display size; a stray multi-megabyte original slows every visit.
        try (var files = java.nio.file.Files.walk(java.nio.file.Path.of("src/main/resources/static"))) {
            var tooBig = files.filter(java.nio.file.Files::isRegularFile)
                    .filter(path -> path.toString().matches("(?i).*\\.(png|jpe?g|webp|gif)$"))
                    .filter(path -> path.toFile().length() > 400_000)
                    .toList();
            assertThat(tooBig).isEmpty();
        }
    }

    private HttpResponse<String> fetch(String path) throws Exception {
        return fetch(path, "text/html");
    }

    private HttpResponse<String> fetch(String path, String accept) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", accept).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void unknownPageIs404() throws Exception {
        // Through the real server: MockMvc skips the error page handling that picks the 404 template.
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/does-not-exist"))
                .header("Accept", "text/html").build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("Page not");
    }
}

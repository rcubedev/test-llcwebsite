package org.limelightcreatives.site.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.limelightcreatives.site.content.SiteContent;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SitePagesTest {

    @Mock
    SiteContent site;

    private SitePages pages;

    @BeforeEach
    void setUp() {
        when(this.site.url()).thenReturn(URI.create("https://limelightcreatives.org/"));
        this.pages = new SitePages(this.site);
    }

    @Test
    void all_PagesExist_ReturnsAllPages() {
        assertEquals(1, this.pages.all().size());
        assertEquals(this.pages.home(), this.pages.all().getFirst());
    }

    @Test
    void home_PageRequested_ReturnsCorrectPage() {
        Page home = this.pages.home();

        assertEquals(URI.create("/"), home.path());
        assertEquals("index", home.template());
        assertEquals(URI.create("https://limelightcreatives.org/"), home.meta().url());
        assertTrue(home.meta().indexed());
    }

    @Test
    void notFound_PageRequested_IsNotIndexed() {
        Page notFound = this.pages.notFound();

        assertEquals(URI.create("/404"), notFound.path());
        assertEquals("error/404", notFound.template());
        assertFalse(notFound.meta().indexed());
    }
}

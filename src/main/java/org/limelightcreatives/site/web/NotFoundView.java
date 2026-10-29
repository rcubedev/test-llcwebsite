package org.limelightcreatives.site.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/** Shows the site's own 404 page (the same one the export writes to 404.html). */
@Component
public class NotFoundView implements ErrorViewResolver {

    private final SitePages pages;

    public NotFoundView(SitePages pages) {
        this.pages = pages;
    }

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        if (status != HttpStatus.NOT_FOUND) return null; // fall back to Spring's default error page
        var page = pages.notFound();
        return new ModelAndView(page.template(), page.model(), status);
    }
}

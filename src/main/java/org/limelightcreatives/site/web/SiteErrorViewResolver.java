package org.limelightcreatives.site.web;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.limelightcreatives.site.content.SiteContent;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Component
public class SiteErrorViewResolver implements ErrorViewResolver {

    private record ErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {}

    private final SitePages pages;
    private final SiteContent site;
    private final Assets assets;

    public SiteErrorViewResolver(SitePages pages, SiteContent site, Assets assets) {
        this.pages = pages;
        this.site = site;
        this.assets = assets;
    }

    @Override
    public @Nullable ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        ErrorView error = new ErrorView(request, status, model);

        return switch (status) {
            case NOT_FOUND -> createModel("error/404", this.pages.notFound(), error);
            case INTERNAL_SERVER_ERROR -> createModel("error/500", this.pages.internalServerError(), error);
            default -> null;
        };
    }

    private ModelAndView createModel(String template, Page page, ErrorView errorView) {
        return new ModelAndView(template, populateModel(page, errorView.model()), errorView.status());
    }

    private Map<String, Object> populateModel(Page page, Map<String, Object> model) {
        Map<String, Object> modelMap = new HashMap<>(model);
        modelMap.put("site", this.site);
        modelMap.put("assets", this.assets);
        modelMap.put("meta", page.meta());
        modelMap.put("home", URI.create("/").equals(page.path()));
        return modelMap;
    }
}

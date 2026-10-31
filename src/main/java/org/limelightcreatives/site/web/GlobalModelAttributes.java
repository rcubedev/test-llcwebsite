package org.limelightcreatives.site.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Provides shared model attributes to all MVC controllers.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final Assets assets;

    public GlobalModelAttributes(Assets assets) {
        this.assets = assets;
    }

    @ModelAttribute("assets")
    public Assets assets() {
        return this.assets;
    }
}

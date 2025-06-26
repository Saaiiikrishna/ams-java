package com.example.attendancesystem.subscriber.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller to handle React Router fallback for Single Page Applications
 * Forwards all non-API routes to the appropriate React app index.html
 */
@Controller
public class ReactRouterController {

    /**
     * Handle all admin panel routes and forward to admin index.html
     * This ensures React Router can handle client-side routing
     */
    @RequestMapping(value = {
        "/admin",
        "/admin/",
        "/admin/login",
        "/admin/dashboard",
        "/admin/dashboard/**"
    })
    public String adminFallback() {
        // Forward to admin index.html for React Router to handle
        return "forward:/admin/index.html";
    }

    /**
     * Handle all entity dashboard routes and forward to entity index.html
     * This ensures React Router can handle client-side routing
     */
    @RequestMapping(value = {
        "/entity",
        "/entity/",
        "/entity/login",
        "/entity/dashboard",
        "/entity/dashboard/**"
    })
    public String entityFallback() {
        // Forward to entity index.html for React Router to handle
        return "forward:/entity/index.html";
    }
}

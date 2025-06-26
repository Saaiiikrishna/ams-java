package com.example.attendancesystem.interceptor;

import com.example.attendancesystem.config.ObservabilityConfig.CustomMetrics;
import com.example.attendancesystem.service.MonitoringService;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * Monitoring Interceptor for automatic request tracking
 * Captures performance metrics, traces requests, and monitors system health
 */
@Component
public class MonitoringInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringInterceptor.class);
    private static final String REQUEST_START_TIME = "request_start_time";
    private static final String REQUEST_ID = "request_id";
    private static final String TIMER_SAMPLE = "timer_sample";

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private CustomMetrics customMetrics;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Generate unique request ID for tracing
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        request.setAttribute(REQUEST_ID, requestId);
        
        // Record request start time
        long startTime = System.currentTimeMillis();
        request.setAttribute(REQUEST_START_TIME, startTime);
        
        // Start metrics timer
        Timer.Sample timerSample = customMetrics.startTimer();
        request.setAttribute(TIMER_SAMPLE, timerSample);
        
        // Increment active connections
        customMetrics.incrementActiveConnections();
        
        // Log request start
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        
        logger.debug("🚀 Request Started: [{}] {} {} {} - IP: {}, UA: {}", 
                    requestId, method, uri, 
                    queryString != null ? "?" + queryString : "",
                    clientIp, userAgent);
        
        // Check for suspicious activity
        checkSuspiciousActivity(request, clientIp, userAgent);
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        try {
            // Get request metadata
            String requestId = (String) request.getAttribute(REQUEST_ID);
            Long startTime = (Long) request.getAttribute(REQUEST_START_TIME);
            Timer.Sample timerSample = (Timer.Sample) request.getAttribute(TIMER_SAMPLE);
            
            if (startTime == null) {
                logger.warn("⚠️ Request start time not found for request: {}", requestId);
                return;
            }
            
            // Calculate response time
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Stop metrics timer
            if (timerSample != null) {
                customMetrics.recordTimer(timerSample);
            }
            
            // Decrement active connections
            customMetrics.decrementActiveConnections();
            
            // Get request details
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int statusCode = response.getStatus();
            String clientIp = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            // Record the request in monitoring service
            monitoringService.recordApiRequest(uri, method, responseTime, statusCode, userAgent);
            
            // Log request completion
            if (statusCode >= 400) {
                logger.warn("❌ Request Failed: [{}] {} {} - Status: {}, Time: {}ms, IP: {}", 
                           requestId, method, uri, statusCode, responseTime, clientIp);
                
                // Record security event for certain error codes
                if (statusCode == 401 || statusCode == 403) {
                    monitoringService.recordSecurityEvent("UNAUTHORIZED_ACCESS", 
                                                         String.format("%s %s", method, uri), 
                                                         clientIp, userAgent);
                } else if (statusCode == 404) {
                    monitoringService.recordSecurityEvent("RESOURCE_NOT_FOUND", 
                                                         String.format("%s %s", method, uri), 
                                                         clientIp, userAgent);
                }
            } else if (responseTime > 5000) {
                logger.warn("🐌 Slow Request: [{}] {} {} - Time: {}ms, IP: {}", 
                           requestId, method, uri, responseTime, clientIp);
            } else {
                logger.debug("✅ Request Completed: [{}] {} {} - Status: {}, Time: {}ms", 
                            requestId, method, uri, statusCode, responseTime);
            }
            
            // Record exception if present
            if (ex != null) {
                logger.error("💥 Request Exception: [{}] {} {} - Exception: {}", 
                            requestId, method, uri, ex.getMessage(), ex);
                
                monitoringService.recordSecurityEvent("REQUEST_EXCEPTION", 
                                                     String.format("%s %s: %s", method, uri, ex.getMessage()), 
                                                     clientIp, userAgent);
            }
            
        } catch (Exception e) {
            logger.error("❌ Error in monitoring interceptor", e);
        }
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Check for suspicious activity patterns
     */
    private void checkSuspiciousActivity(HttpServletRequest request, String clientIp, String userAgent) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        
        // Check for common attack patterns
        if (uri.contains("../") || uri.contains("..\\")) {
            monitoringService.recordSecurityEvent("PATH_TRAVERSAL_ATTEMPT", 
                                                 String.format("%s %s", method, uri), 
                                                 clientIp, userAgent);
        }
        
        if (uri.toLowerCase().contains("script") || uri.toLowerCase().contains("javascript")) {
            monitoringService.recordSecurityEvent("SCRIPT_INJECTION_ATTEMPT", 
                                                 String.format("%s %s", method, uri), 
                                                 clientIp, userAgent);
        }
        
        if (uri.toLowerCase().contains("union") || uri.toLowerCase().contains("select") || 
            uri.toLowerCase().contains("drop") || uri.toLowerCase().contains("insert")) {
            monitoringService.recordSecurityEvent("SQL_INJECTION_ATTEMPT", 
                                                 String.format("%s %s", method, uri), 
                                                 clientIp, userAgent);
        }
        
        // Check for suspicious user agents
        if (userAgent == null || userAgent.isEmpty()) {
            monitoringService.recordSecurityEvent("MISSING_USER_AGENT", 
                                                 String.format("%s %s", method, uri), 
                                                 clientIp, "NONE");
        } else if (userAgent.toLowerCase().contains("bot") || 
                   userAgent.toLowerCase().contains("crawler") ||
                   userAgent.toLowerCase().contains("spider")) {
            monitoringService.recordSecurityEvent("BOT_ACCESS", 
                                                 String.format("%s %s", method, uri), 
                                                 clientIp, userAgent);
        }
        
        // Check for excessive request patterns (basic rate limiting check)
        // This is a simplified check - in production you'd use a proper rate limiter
        String requestKey = clientIp + ":" + uri;
        // Implementation would track request counts per IP/endpoint
    }
}

/**
 * Configuration to register the monitoring interceptor
 */
@Component
class MonitoringInterceptorConfig implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer {
    
    @Autowired
    private MonitoringInterceptor monitoringInterceptor;
    
    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(monitoringInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/static/**", "/css/**", "/js/**", "/images/**");
    }
}

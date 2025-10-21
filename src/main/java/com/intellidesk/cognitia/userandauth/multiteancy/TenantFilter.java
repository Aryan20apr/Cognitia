package com.intellidesk.cognitia.userandauth.multiteancy;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.intellidesk.cognitia.userandauth.services.TenantService;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class TenantFilter implements Filter {

    private static final List<String> PUBLIC_PATHS = List.of(
        "/public", "/auth"
    );

    private boolean isPublicPath(ServletRequest request) {
        if (request instanceof HttpServletRequest httpRequest) {
            String path = httpRequest.getRequestURI();
            log.info("[TenantFilter] : [isPublicPath] : Checking path '{}' against public paths: {}", path, PUBLIC_PATHS);
            boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith);
            log.info("[TenantFilter] : [isPublicPath] : Result: {}", isPublic);
            return isPublic;
        }
        return false;
    }

    private final TenantService tenantService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String tenantId = httpRequest.getHeader("x-tenant-id");
        
        log.info("[TenantFilter] : [doFilter] : Received request with tenantId header '{}'", tenantId);
        
        if (isPublicPath(request)) {
            // Skip tenant context for public paths
            chain.doFilter(request, response);
            return;
        }  

        // Only handle tenant context if x-tenant-id header is present
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            Boolean exists = tenantService.checkIfExists(tenantId);

            if(!exists){
                log.info("[TenantFilter] : [doFilter] : Invalid company id passed in header");
                throw new RuntimeException("Company does not exists with company id: "+tenantId);
            }

            TenantContext.setTenantId(UUID.fromString(tenantId));
            try {
                chain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
        } else {
            // No x-tenant-id header, let other mechanisms (like JWT) handle tenant context
            log.info("[TenantFilter] : [doFilter] : No x-tenant-id header found, letting other mechanisms handle tenant context");
            chain.doFilter(request, response);
        }
    }
}

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
        "/public"
    );

    private boolean isPublicPath(ServletRequest request) {
        if (request instanceof HttpServletRequest httpRequest) {
            String path = httpRequest.getRequestURI(); // Gets the path part of the URL
            return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
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
            // Skip tenant context
            chain.doFilter(request, response);
            return;
        }  

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
    }
}

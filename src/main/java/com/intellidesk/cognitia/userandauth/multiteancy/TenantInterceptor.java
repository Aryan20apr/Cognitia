package com.intellidesk.cognitia.userandauth.multiteancy;

import java.util.UUID;

import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {
    
    
    private final EntityManager entityManager;

    public TenantInterceptor(EntityManager entityManager){
        this.entityManager = entityManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        
                
        UUID tenantId = TenantContext.getTenantId();
        log.info("[TenantInterceptor] [preHandle] Inside preHandle, obtained tenant id from tenant context: "+tenantId);
        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);
            if (session != null) {
                log.info("[TenantInterceptor] [preHandle] Inside preHandle, enabling tenant filter for tenant id: "+tenantId);
                session.enableFilter("tenantFilter")
                        .setParameter("tenantId", tenantId);
            }
        }

        return true; // Continue with request
    }
}

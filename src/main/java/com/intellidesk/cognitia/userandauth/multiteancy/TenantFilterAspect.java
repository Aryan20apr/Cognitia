package com.intellidesk.cognitia.userandauth.multiteancy;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

/**
 * the tenantFilter not applying inside @Transactional service methods, it's because the Hibernate Session used there may be created after the interceptor runs.

➡ To fix that, we can also add a @TransactionalEventListener or a small AOP advice around @Transactional methods to enable the filter again within the active session
 */

@Aspect
@Component
@Slf4j
public class TenantFilterAspect {
    
    @PersistenceContext
    private EntityManager entityManager;

    // @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    // public Object applyTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
    //     log.info("[TenantFilterAspect] [applyTenantFilter] applying tenant filter to transaction");
        
    //     try {
    //         Session session = entityManager.unwrap(Session.class);
    //         if (TenantContext.getTenantId() != null) {
    //             log.info("[TenantFilterAspect] [applyTenantFilter] enabling tenant filter for tenant id: {}", TenantContext.getTenantId());
    //             session.enableFilter("tenantFilter")
    //                     .setParameter("tenantId", TenantContext.getTenantId().toString());
    //         } else {
    //             log.warn("[TenantFilterAspect] [applyTenantFilter] No tenant ID found in context");
    //         }
            
    //         // Proceed with the actual method execution
    //         return joinPoint.proceed();
            
    //     } catch (Exception e) {
    //         log.error("[TenantFilterAspect] [applyTenantFilter] Error applying tenant filter", e);
    //         throw e;
    //     }
    // }

    @Around("@within(org.springframework.transaction.annotation.Transactional) || @annotation(org.springframework.transaction.annotation.Transactional)")
    public Object applyTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("[TenantFilterAspect] [applyTenantFilter] Intercepted transactional method: {}.{}", 
            joinPoint.getSignature().getDeclaringTypeName(), 
            joinPoint.getSignature().getName());

        if (TenantContext.getTenantId() != null) {
            log.info("[TenantFilterAspect] [applyTenantFilter] Tenant ID in context: {}", TenantContext.getTenantId());
        } else {
            log.warn("[TenantFilterAspect] [applyTenantFilter] No tenant ID found in TenantContext at interception");
        }


        enableTenantFilter();
        try {
            return joinPoint.proceed(); // Hibernate session & transaction are active here
        } finally {
            disableTenantFilter(); // optional cleanup if desired
        }
    }

    private void disableTenantFilter() {
        try {
            Session session = entityManager.unwrap(Session.class);
            if (session.isOpen()) {
                session.disableFilter("tenantFilter");
                log.info("[TenantFilterAspect] tenant filter disabled after method execution");
            }
        } catch (Exception e) {
            log.error("[TenantFilterAspect] failed to disable tenant filter", e);
        }
    }
    private void enableTenantFilter() {
        log.info("[TenantFilterAspect] [enableTenantFilter] Attempting to enable tenant filter");
        if (TenantContext.getTenantId() == null) {
            log.warn("[TenantFilterAspect] [enableTenantFilter] No tenant ID found in TenantContext, skipping filter");
        }
        try {
            Session session = entityManager.unwrap(Session.class);
            if (session.isOpen() && TenantContext.getTenantId() != null) {
                session.enableFilter("tenantFilter")
                       .setParameter("tenantId", TenantContext.getTenantId());
                log.info("[TenantFilterAspect] tenant filter applied for {}", TenantContext.getTenantId());
            }
        } catch (Exception e) {
            log.error("[TenantFilterAspect] failed to enable tenant filter", e);
        }
    }
    }
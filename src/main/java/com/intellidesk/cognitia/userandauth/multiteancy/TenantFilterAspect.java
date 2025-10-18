package com.intellidesk.cognitia.userandauth.multiteancy;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

/**
 * the tenantFilter not applying inside @Transactional service methods, it’s because the Hibernate Session used there may be created after the interceptor runs.

➡ To fix that, we can also add a @TransactionalEventListener or a small AOP advice around @Transactional methods to enable the filter again within the active session
 */

@Aspect
@Component
@Slf4j
public class TenantFilterAspect {
    
    @PersistenceContext
    private EntityManager entityManager;

    @Before("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void applyTenantFilter() {
        log.info("[TenantFilterAspect] [applyTenantFilter] applying tenant filter to transaction");
        Session session = entityManager.unwrap(Session.class);
        if (TenantContext.getTenantId() != null) {
            log.info("[TenantFilterAspect] [applyTenantFilter] applying tenant filter to transaction");
            session.enableFilter("tenantFilter")
                    .setParameter("tenantId", TenantContext.getTenantId().toString());
        }
    }
}

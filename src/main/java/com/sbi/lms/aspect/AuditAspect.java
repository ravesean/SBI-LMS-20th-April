package com.sbi.lms.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
// sample comment 1
// sample comment 2
// sample comment 3

import java.time.Instant;

/**
 * DevSecOps: Audit logging for sensitive PII field access.
 *
 * RBI IT Framework Section 7.2 — banks must maintain audit logs of access
 * to sensitive customer data (CIBIL score, PAN, income).
 *
 * This aspect intercepts every call to getApplication() and logs:
 *   - Who accessed it (authenticated user)
 *   - Which application was accessed (ID)
 *   - When (timestamp)
 *
 * In production, AUDIT logger should ship to a SIEM (Splunk / ELK).
 */
@Aspect
@Component
public class AuditAspect {

    // Separate AUDIT logger — route to dedicated audit log file / SIEM in prod
    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    @AfterReturning(
        pointcut = "execution(* com.sbi.lms.service.LoanApplicationService.findById(..))",
        returning = "result"
    )
    public void logSensitiveFieldAccess(JoinPoint jp, Object result) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = (auth != null) ? auth.getName() : "anonymous";
        Object[] args = jp.getArgs();
        Object id = (args.length > 0) ? args[0] : "unknown";

        audit.info("SENSITIVE_FIELD_ACCESS user={} applicationId={} timestamp={}",
                user, id, Instant.now());
    }
}

package com.sbi.lms.controller;

import com.sbi.lms.dto.LoanApplicationDTO;
import com.sbi.lms.dto.LoanApplicationRequest;
import com.sbi.lms.dto.StatusUpdateRequest;
import com.sbi.lms.model.ApplicationStatus;
import com.sbi.lms.model.LoanApplication;
import com.sbi.lms.service.LoanApplicationService;
import com.sbi.lms.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Loan Application REST endpoints.
 *
 * DevSecOps teaching points in this controller:
 *
 *   GET  /{id}         — Lab 1: missing @PreAuthorize (SonarQube finds it); fix adds PII masking
 *   POST /             — @Valid on request + @PreAuthorize('MANAGER') — secure example
 *   PUT  /{id}/status  — state machine enforced at service layer (A04 Insecure Design)
 *   GET  /search       — CAPSTONE LAB: SQL injection vulnerability (string concatenation JPQL)
 *                        SonarQube flags it; ZAP confirms it; fix uses derived query
 */
@RestController
@RequestMapping("/api/v1/applications")
@Tag(name = "Loan Applications")
@SecurityRequirement(name = "bearerAuth")
public class ApplicationController {

    private final LoanApplicationService applicationService;
    private final SecurityService        securityService;

    @PersistenceContext
    private EntityManager em;

    public ApplicationController(LoanApplicationService applicationService,
                                  SecurityService securityService) {
        this.applicationService = applicationService;
        this.securityService    = securityService;
    }

    @GetMapping
    @Operation(summary = "List all loan applications")
    @PreAuthorize("hasAnyRole('MANAGER','OFFICER')")
    public ResponseEntity<List<LoanApplicationDTO>> listAll(Authentication auth) {
        return ResponseEntity.ok(
            applicationService.findAll().stream()
                .map(app -> maskPiiIfOfficer(applicationService.toDto(app), auth))
                .collect(Collectors.toList())
        );
    }

    /**
     * ╔══════════════════════════════════════════════════════════════╗
     * ║  LAB 1 — SAST EXERCISE (Fix Issue 2)                       ║
     * ║                                                              ║
     * ║  This endpoint is MISSING @PreAuthorize.                    ║
     * ║  SonarQube will flag it as a Critical vulnerability.        ║
     * ║                                                              ║
     * ║  Fix: add @PreAuthorize("hasAnyRole('MANAGER','OFFICER')")  ║
     * ║  and apply PII masking for OFFICER role (see maskPiiIfOfficer) ║
     * ╚══════════════════════════════════════════════════════════════╝
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get application by ID")
    // LAB 1: @PreAuthorize is intentionally missing here — SonarQube will catch it
    @PreAuthorize("hasAnyRole('MANAGER','OFFICER')")
    public ResponseEntity<LoanApplicationDTO> getById(@PathVariable Long id, Authentication auth) {
        LoanApplication app = applicationService.findById(id);
        LoanApplicationDTO dto = applicationService.toDto(app);
        // LAB 1 FIX: uncomment the line below after adding @PreAuthorize
        dto = maskPiiIfOfficer(dto, auth);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @Operation(summary = "Create a new loan application")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<LoanApplicationDTO> create(
            @Valid @RequestBody LoanApplicationRequest request) {
        LoanApplication saved = applicationService.create(request);
        return ResponseEntity.status(201).body(applicationService.toDto(saved));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update application status")
    @PreAuthorize("hasAnyRole('MANAGER','OFFICER')")
    public ResponseEntity<LoanApplicationDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest req) {
        ApplicationStatus newStatus = ApplicationStatus.valueOf(req.getStatus().toUpperCase());
        LoanApplication updated = applicationService.updateStatus(id, newStatus);
        return ResponseEntity.ok(applicationService.toDto(updated));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CAPSTONE LAB — SQL INJECTION VULNERABILITY
    //
    //  Step 1: this endpoint is INTENTIONALLY VULNERABLE.
    //          It uses string concatenation in a JPQL query.
    //          SonarQube will flag it as BLOCKER (taint analysis).
    //          ZAP Active Scan will confirm it is exploitable.
    //
    //  Step 4: FIX — replace the body with the safe version below (commented out).
    //          The fix uses a Spring Data derived query (no SQL string building at all).
    // ══════════════════════════════════════════════════════════════════════════
    @GetMapping("/search")
    @Operation(summary = "Search applications by branch name")
    @PreAuthorize("hasAnyRole('MANAGER','OFFICER')")
    public ResponseEntity<?> searchByBranch(@RequestParam String branch) {

        // VULNERABLE — string concatenation in JPQL (SonarQube: java:S2076 / taint)
        // Try: ?branch=' OR '1'='1  →  returns ALL applications
        List<LoanApplication> result = em.createQuery(
                "SELECT a FROM LoanApplication a WHERE a.branch.name = '" + branch + "'",
                LoanApplication.class
        ).getResultList();

        return ResponseEntity.ok(result.stream()
                .map(applicationService::toDto)
                .collect(Collectors.toList()));

        // ── CAPSTONE FIX (Step 4) — delete the block above and uncomment below ──
        //
        // @RequestParam @NotBlank @Size(max = 100) String branch  ← add validation too
        //
        // List<LoanApplication> result = applicationService.findByBranchName(branch);
        // return ResponseEntity.ok(result.stream()
        //         .map(applicationService::toDto)
        //         .collect(Collectors.toList()));
    }

    // ── PII masking helper — A01 Broken Access Control ────────────────────────
    private LoanApplicationDTO maskPiiIfOfficer(LoanApplicationDTO dto, Authentication auth) {
        if (!securityService.isManager(auth)) {
            dto.setCibilScore(null);
            dto.setMonthlyIncome(null);
            dto.setPanNumber(securityService.maskPan(dto.getPanNumber()));
        }
        return dto;
    }
}

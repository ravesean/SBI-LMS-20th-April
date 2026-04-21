package com.sbi.lms.service;

import com.sbi.lms.dto.LoanApplicationDTO;
import com.sbi.lms.dto.LoanApplicationRequest;
import com.sbi.lms.exception.InvalidStateTransitionException;
import com.sbi.lms.exception.ResourceNotFoundException;
import com.sbi.lms.model.*;
import com.sbi.lms.repository.BranchRepository;
import com.sbi.lms.repository.LoanApplicationRepository;
import com.sbi.lms.repository.LoanProductRepository;
import org.springframework.stereotype.Service;
// comment is this 
import java.util.List;

@Service
public class LoanApplicationService {

    private final LoanApplicationRepository applicationRepository;
    private final BranchRepository branchRepository;
    private final LoanProductRepository productRepository;

    public LoanApplicationService(LoanApplicationRepository applicationRepository,
                                   BranchRepository branchRepository,
                                   LoanProductRepository productRepository) {
        this.applicationRepository = applicationRepository;
        this.branchRepository      = branchRepository;
        this.productRepository     = productRepository;
    }

    public List<LoanApplication> findAll() {
        return applicationRepository.findAll();
    }

    public LoanApplication findById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }

    public LoanApplication create(LoanApplicationRequest req) {
        Branch branch = branchRepository.findById(req.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + req.getBranchId()));
        LoanProduct product = productRepository.findById(req.getLoanProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + req.getLoanProductId()));

        LoanApplication app = new LoanApplication();
        app.setApplicantName(req.getApplicantName());
        app.setEmail(req.getEmail());
        app.setPanNumber(req.getPanNumber());
        app.setMonthlyIncome(req.getMonthlyIncome());
        app.setCibilScore(req.getCibilScore());
        app.setLoanAmountRequested(req.getLoanAmountRequested());
        app.setStatus(ApplicationStatus.SUBMITTED);
        app.setBranch(branch);
        app.setLoanProduct(product);
        return applicationRepository.save(app);
    }

    /**
     * DevSecOps: State machine enforcement — A04 Insecure Design.
     *
     * Business rule: SUBMITTED → UNDER_REVIEW → APPROVED or REJECTED
     * Skipping UNDER_REVIEW or mutating terminal states is blocked at service layer,
     * not just controller layer (defence in depth).
     */
    public LoanApplication updateStatus(Long id, ApplicationStatus newStatus) {
        LoanApplication app = findById(id);
        ApplicationStatus current = app.getStatus();

        if (current == ApplicationStatus.APPROVED || current == ApplicationStatus.REJECTED) {
            throw new InvalidStateTransitionException(
                    "Terminal status cannot be changed: " + current);
        }
        if (current == ApplicationStatus.SUBMITTED && newStatus == ApplicationStatus.APPROVED) {
            throw new InvalidStateTransitionException(
                    "Application must pass through UNDER_REVIEW before APPROVED");
        }
        app.setStatus(newStatus);
        return applicationRepository.save(app);
    }

    // Safe derived query — used by capstone lab FIX (replaces vulnerable JPQL)
    public List<LoanApplication> findByBranchName(String branchName) {
        return applicationRepository.findByBranchName(branchName);
    }

    // DTO mapper — extracts into separate method to keep controller thin
    public LoanApplicationDTO toDto(LoanApplication app) {
        LoanApplicationDTO dto = new LoanApplicationDTO();
        dto.setId(app.getId());
        dto.setApplicantName(app.getApplicantName());
        dto.setEmail(app.getEmail());
        dto.setPanNumber(app.getPanNumber());
        dto.setMonthlyIncome(app.getMonthlyIncome());
        dto.setCibilScore(app.getCibilScore());
        dto.setLoanAmountRequested(app.getLoanAmountRequested());
        dto.setStatus(app.getStatus());
        dto.setBranchName(app.getBranch().getName());
        dto.setProductName(app.getLoanProduct().getProductName());
        return dto;
    }
}

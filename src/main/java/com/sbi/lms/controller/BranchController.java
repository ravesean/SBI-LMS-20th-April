package com.sbi.lms.controller;

import com.sbi.lms.model.Branch;
import com.sbi.lms.repository.BranchRepository;
import com.sbi.lms.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
@Tag(name = "Branches")
@SecurityRequirement(name = "bearerAuth")
public class BranchController {

    private final BranchRepository branchRepository;

    public BranchController(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','OFFICER')")
    @Operation(summary = "List all branches")
    public ResponseEntity<List<Branch>> listAll() {
        return ResponseEntity.ok(branchRepository.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','OFFICER')")
    @Operation(summary = "Get branch by ID")
    public ResponseEntity<Branch> getById(@PathVariable Long id) {
        return ResponseEntity.ok(branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + id)));
    }
}

package com.bql.feedback.controller;

import com.bql.feedback.dto.AccountApprovalResponseDto;
import com.bql.feedback.security.SecurityUtils;
import com.bql.feedback.service.AccountApprovalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@PreAuthorize("hasRole('CS_MANAGER')")
public class AccountApprovalController {

    private final AccountApprovalService accountApprovalService;

    public AccountApprovalController(AccountApprovalService accountApprovalService) {
        this.accountApprovalService = accountApprovalService;
    }

    // GET http://localhost:8080/api/accounts/pending (JWT của CS_MANAGER)
    @GetMapping("/pending")
    public ResponseEntity<List<AccountApprovalResponseDto>> getPending() {
        return ResponseEntity.ok(accountApprovalService.getPendingAccounts(SecurityUtils.currentStaffId()));
    }

    // POST http://localhost:8080/api/accounts/1/approve (JWT của CS_MANAGER)
    @PostMapping("/{accountId}/approve")
    public ResponseEntity<AccountApprovalResponseDto> approve(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountApprovalService.approve(accountId, SecurityUtils.currentStaffId()));
    }

    // POST http://localhost:8080/api/accounts/1/reject (JWT của CS_MANAGER)
    @PostMapping("/{accountId}/reject")
    public ResponseEntity<AccountApprovalResponseDto> reject(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountApprovalService.reject(accountId, SecurityUtils.currentStaffId()));
    }
}

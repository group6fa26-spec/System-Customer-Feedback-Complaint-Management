package com.bql.feedback.controller;

import com.bql.feedback.dto.AccountApprovalResponseDto;
import com.bql.feedback.service.AccountApprovalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountApprovalController {

    private final AccountApprovalService accountApprovalService;

    public AccountApprovalController(AccountApprovalService accountApprovalService) {
        this.accountApprovalService = accountApprovalService;
    }

    // GET http://localhost:8080/api/accounts/pending (Header: X-Staff-Id của CS_MANAGER)
    @GetMapping("/pending")
    public ResponseEntity<List<AccountApprovalResponseDto>> getPending(
            @RequestHeader("X-Staff-Id") Long staffId
    ) {
        return ResponseEntity.ok(accountApprovalService.getPendingAccounts(staffId));
    }

    // POST http://localhost:8080/api/accounts/1/approve (Header: X-Staff-Id của CS_MANAGER)
    @PostMapping("/{accountId}/approve")
    public ResponseEntity<AccountApprovalResponseDto> approve(
            @PathVariable Long accountId,
            @RequestHeader("X-Staff-Id") Long staffId
    ) {
        return ResponseEntity.ok(accountApprovalService.approve(accountId, staffId));
    }

    // POST http://localhost:8080/api/accounts/1/reject (Header: X-Staff-Id của CS_MANAGER)
    @PostMapping("/{accountId}/reject")
    public ResponseEntity<AccountApprovalResponseDto> reject(
            @PathVariable Long accountId,
            @RequestHeader("X-Staff-Id") Long staffId
    ) {
        return ResponseEntity.ok(accountApprovalService.reject(accountId, staffId));
    }
}

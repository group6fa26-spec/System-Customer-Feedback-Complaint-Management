package com.bql.feedback.controller;

import com.bql.feedback.dto.AccountManagementResponseDto;
import com.bql.feedback.dto.CreateStaffAccountRequestDto;
import com.bql.feedback.dto.ResetPasswordRequestDto;
import com.bql.feedback.dto.UpdateAccountRequestDto;
import com.bql.feedback.model.AccountStatus;
import com.bql.feedback.model.Role;
import com.bql.feedback.security.SecurityUtils;
import com.bql.feedback.service.AccountManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@PreAuthorize("hasRole('CS_MANAGER')")
public class AccountManagementController {

    private final AccountManagementService accountManagementService;

    public AccountManagementController(AccountManagementService accountManagementService) {
        this.accountManagementService = accountManagementService;
    }

    // GET http://localhost:8080/api/accounts?role=CS_STAFF&status=APPROVED&includeDeleted=false&keyword=an
    @GetMapping
    public ResponseEntity<List<AccountManagementResponseDto>> listAccounts(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(accountManagementService.listAccounts(
                SecurityUtils.currentStaffId(), role, status, includeDeleted, keyword));
    }

    // GET http://localhost:8080/api/accounts/1
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountManagementResponseDto> getAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountManagementService.getAccountById(
                SecurityUtils.currentStaffId(), accountId));
    }

    // POST http://localhost:8080/api/accounts/staff (CS_MANAGER tạo CS_STAFF hoặc DEPT_STAFF)
    @PostMapping("/staff")
    public ResponseEntity<AccountManagementResponseDto> createStaffAccount(
            @RequestBody CreateStaffAccountRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountManagementService.createStaffAccount(SecurityUtils.currentStaffId(), request));
    }

    // PUT http://localhost:8080/api/accounts/1 (Sửa thông tin nhân viên hoặc cư dân)
    @PutMapping("/{accountId}")
    public ResponseEntity<AccountManagementResponseDto> updateAccount(
            @PathVariable Long accountId,
            @RequestBody UpdateAccountRequestDto request
    ) {
        return ResponseEntity.ok(accountManagementService.updateAccount(
                SecurityUtils.currentStaffId(), accountId, request));
    }

    // POST http://localhost:8080/api/accounts/1/block (Khóa đăng nhập quản trị)
    @PostMapping("/{accountId}/block")
    public ResponseEntity<AccountManagementResponseDto> blockAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountManagementService.blockAccount(
                SecurityUtils.currentStaffId(), accountId));
    }

    // POST http://localhost:8080/api/accounts/1/unblock (Mở khóa đăng nhập quản trị)
    @PostMapping("/{accountId}/unblock")
    public ResponseEntity<AccountManagementResponseDto> unblockAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountManagementService.unblockAccount(
                SecurityUtils.currentStaffId(), accountId));
    }

    // POST http://localhost:8080/api/accounts/1/reset-password (Đặt lại mật khẩu tạm + bật mustChangePassword)
    @PostMapping("/{accountId}/reset-password")
    public ResponseEntity<AccountManagementResponseDto> resetPassword(
            @PathVariable Long accountId,
            @RequestBody(required = false) ResetPasswordRequestDto request
    ) {
        return ResponseEntity.ok(accountManagementService.resetPassword(
                SecurityUtils.currentStaffId(), accountId, request));
    }

    // DELETE http://localhost:8080/api/accounts/1 (Xóa mềm tài khoản)
    @DeleteMapping("/{accountId}")
    public ResponseEntity<AccountManagementResponseDto> softDeleteAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountManagementService.softDeleteAccount(
                SecurityUtils.currentStaffId(), accountId));
    }

    // POST http://localhost:8080/api/accounts/1/restore (Khôi phục tài khoản đã xóa mềm)
    @PostMapping("/{accountId}/restore")
    public ResponseEntity<AccountManagementResponseDto> restoreAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountManagementService.restoreAccount(
                SecurityUtils.currentStaffId(), accountId));
    }
}

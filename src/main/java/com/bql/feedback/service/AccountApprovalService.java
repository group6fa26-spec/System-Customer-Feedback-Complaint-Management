package com.bql.feedback.service;

import com.bql.feedback.dto.AccountApprovalResponseDto;
import com.bql.feedback.dto.RentalContractResponseDto;
import com.bql.feedback.exception.BusinessException;
import com.bql.feedback.exception.ForbiddenException;
import com.bql.feedback.exception.NotFoundException;
import com.bql.feedback.model.AccountStatus;
import com.bql.feedback.model.Customer;
import com.bql.feedback.model.RentalContract;
import com.bql.feedback.model.RentalContractStatus;
import com.bql.feedback.model.ResidentType;
import com.bql.feedback.model.Role;
import com.bql.feedback.model.Staff;
import com.bql.feedback.model.UserAccount;
import com.bql.feedback.repository.CustomerRepository;
import com.bql.feedback.repository.RentalContractRepository;
import com.bql.feedback.repository.StaffRepository;
import com.bql.feedback.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AccountApprovalService {

    private final UserAccountRepository userAccountRepository;
    private final CustomerRepository customerRepository;
    private final RentalContractRepository rentalContractRepository;
    private final StaffRepository staffRepository;

    public AccountApprovalService(
            UserAccountRepository userAccountRepository,
            CustomerRepository customerRepository,
            RentalContractRepository rentalContractRepository,
            StaffRepository staffRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.customerRepository = customerRepository;
        this.rentalContractRepository = rentalContractRepository;
        this.staffRepository = staffRepository;
    }

    private record PendingCustomerContext(UserAccount account, Customer customer) {
    }

    // 1. Lấy danh sách tài khoản cư dân đang PENDING (query theo lô, tránh N+1)
    public List<AccountApprovalResponseDto> getPendingAccounts(Long staffId) {
        verifyCsManager(staffId);

        List<UserAccount> pendingAccounts = userAccountRepository
                .findByRoleAndAccountStatusOrderByCreatedAtAsc(Role.CUSTOMER, AccountStatus.PENDING);
        if (pendingAccounts.isEmpty()) {
            return List.of();
        }

        List<Long> customerIds = pendingAccounts.stream()
                .map(UserAccount::getCustomerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Customer> customerMap = customerIds.isEmpty()
                ? Map.of()
                : customerRepository.findAllById(customerIds).stream()
                        .collect(Collectors.toMap(Customer::getId, Function.identity()));

        Map<Long, List<RentalContract>> contractsByCustomerId = customerIds.isEmpty()
                ? Map.of()
                : rentalContractRepository.findByTenantCustomerIdInOrderByStartDateDesc(customerIds).stream()
                        .collect(Collectors.groupingBy(RentalContract::getTenantCustomerId));

        return pendingAccounts.stream()
                .map(account -> {
                    Customer customer = account.getCustomerId() != null
                            ? customerMap.get(account.getCustomerId())
                            : null;
                    List<RentalContract> contracts = account.getCustomerId() != null
                            ? contractsByCustomerId.getOrDefault(account.getCustomerId(), List.of())
                            : List.of();
                    return toDto(account, customer, contracts);
                })
                .toList();
    }

    // 2. Duyệt tài khoản cư dân (PENDING -> APPROVED)
    @Transactional
    public AccountApprovalResponseDto approve(Long accountId, Long staffId) {
        verifyCsManager(staffId);
        PendingCustomerContext ctx = loadPendingCustomerContext(accountId);
        UserAccount account = ctx.account();
        Customer customer = ctx.customer();

        List<RentalContract> contracts = rentalContractRepository
                .findByTenantCustomerIdOrderByStartDateDesc(customer.getId());

        if (customer.getResidentType() == ResidentType.TENANT) {
            List<RentalContract> pendingContracts = contracts.stream()
                    .filter(c -> c.getStatus() == RentalContractStatus.PENDING)
                    .toList();
            if (pendingContracts.isEmpty()) {
                throw new BusinessException(
                        "Tài khoản người thuê không có hợp đồng chờ duyệt, không thể duyệt tài khoản");
            }
            LocalDate today = LocalDate.now();
            boolean hasExpiredContract = pendingContracts.stream()
                    .anyMatch(c -> c.getEndDate().isBefore(today));
            if (hasExpiredContract) {
                throw new BusinessException("Hợp đồng thuê đã hết hạn, không thể duyệt tài khoản");
            }
            for (RentalContract contract : pendingContracts) {
                contract.setStatus(RentalContractStatus.ACTIVE);
            }
            rentalContractRepository.saveAll(pendingContracts);
        }

        customer.setVerifiedResident(true);
        customer.setActive(true);
        Customer savedCustomer = customerRepository.save(customer);

        account.setAccountStatus(AccountStatus.APPROVED);
        account.setActive(true);
        UserAccount savedAccount = userAccountRepository.save(account);

        return toDto(savedAccount, savedCustomer, contracts);
    }

    // 3. Từ chối tài khoản cư dân (PENDING -> REJECTED)
    @Transactional
    public AccountApprovalResponseDto reject(Long accountId, Long staffId) {
        verifyCsManager(staffId);
        PendingCustomerContext ctx = loadPendingCustomerContext(accountId);
        UserAccount account = ctx.account();
        Customer customer = ctx.customer();

        List<RentalContract> contracts = rentalContractRepository
                .findByTenantCustomerIdOrderByStartDateDesc(customer.getId());

        if (customer.getResidentType() == ResidentType.TENANT) {
            List<RentalContract> pendingContracts = contracts.stream()
                    .filter(c -> c.getStatus() == RentalContractStatus.PENDING)
                    .toList();
            if (!pendingContracts.isEmpty()) {
                for (RentalContract contract : pendingContracts) {
                    contract.setStatus(RentalContractStatus.TERMINATED);
                }
                rentalContractRepository.saveAll(pendingContracts);
            }
        }

        customer.setVerifiedResident(false);
        customer.setActive(false);
        Customer savedCustomer = customerRepository.save(customer);

        account.setAccountStatus(AccountStatus.REJECTED);
        account.setActive(false);
        UserAccount savedAccount = userAccountRepository.save(account);

        return toDto(savedAccount, savedCustomer, contracts);
    }

    private void verifyCsManager(Long staffId) {
        if (staffId == null) {
            throw new ForbiddenException("Thiếu thông tin định danh quản lý (X-Staff-Id)");
        }
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ForbiddenException("Không có quyền truy cập chức năng duyệt tài khoản"));
        if (!staff.isActive() || staff.getRole() != Role.CS_MANAGER) {
            throw new ForbiddenException("Chỉ CS_MANAGER đang hoạt động mới có quyền duyệt tài khoản cư dân");
        }
    }

    private PendingCustomerContext loadPendingCustomerContext(Long accountId) {
        UserAccount account = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản với ID: " + accountId));

        if (account.getRole() != Role.CUSTOMER || account.getCustomerId() == null) {
            throw new BusinessException("Chỉ hỗ trợ duyệt tài khoản cư dân");
        }
        if (account.getAccountStatus() != AccountStatus.PENDING) {
            throw new BusinessException(
                    "Tài khoản không ở trạng thái chờ duyệt (hiện tại: " + account.getAccountStatus() + ")");
        }

        Customer customer = customerRepository.findById(account.getCustomerId())
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy hồ sơ cư dân với ID: " + account.getCustomerId()));

        return new PendingCustomerContext(account, customer);
    }

    private AccountApprovalResponseDto toDto(UserAccount account,
                                             Customer customer,
                                             List<RentalContract> contracts) {
        List<RentalContractResponseDto> contractDtos = contracts.stream()
                .map(contract -> new RentalContractResponseDto(
                        contract.getId(),
                        contract.getApartmentCode(),
                        contract.getStartDate(),
                        contract.getEndDate(),
                        contract.getStatus().name()))
                .toList();

        return new AccountApprovalResponseDto(
                account.getId(),
                account.getUsername(),
                account.getRole() != null ? account.getRole().name() : null,
                account.getAccountStatus().name(),
                account.isActive(),
                account.getCustomerId(),
                customer != null ? customer.getFullName() : null,
                customer != null ? customer.getPhone() : null,
                customer != null ? customer.getEmail() : null,
                customer != null ? customer.getApartmentCode() : null,
                customer != null ? customer.getResidentType().name() : null,
                customer != null && customer.isVerifiedResident(),
                customer != null && customer.isActive(),
                contractDtos,
                account.getCreatedAt()
        );
    }
}

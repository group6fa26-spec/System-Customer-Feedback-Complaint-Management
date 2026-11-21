package com.bql.feedback.service;

import com.bql.feedback.dto.CreateFeedbackRequestDto;
import com.bql.feedback.dto.FeedbackResponseDto;
import com.bql.feedback.dto.MergeFeedbackRequestDto;
import com.bql.feedback.model.ClassifiedBy;
import com.bql.feedback.model.Customer;
import com.bql.feedback.model.Feedback;
import com.bql.feedback.model.FeedbackCategory;
import com.bql.feedback.model.FeedbackStatus;
import com.bql.feedback.model.Priority;
import com.bql.feedback.model.Role;
import com.bql.feedback.model.StatusHistory;
import com.bql.feedback.repository.CustomerRepository;
import com.bql.feedback.repository.FeedbackCategoryRepository;
import com.bql.feedback.repository.FeedbackRepository;
import com.bql.feedback.repository.StatusHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class FeedbackService {

    private static final DateTimeFormatter CODE_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final List<FeedbackStatus> FINAL_STATUSES = List.of(
            FeedbackStatus.CLOSED,
            FeedbackStatus.REJECTED,
            FeedbackStatus.MERGED
    );

    private final FeedbackRepository feedbackRepository;
    private final CustomerRepository customerRepository;
    private final FeedbackCategoryRepository feedbackCategoryRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final long idempotencyWindowMinutes;
    private final long duplicateWindowHours;

    public FeedbackService(
            FeedbackRepository feedbackRepository,
            CustomerRepository customerRepository,
            FeedbackCategoryRepository feedbackCategoryRepository,
            StatusHistoryRepository statusHistoryRepository,
            @Value("${app.feedback.idempotency-window-minutes:2}") long idempotencyWindowMinutes,
            @Value("${app.feedback.duplicate-window-hours:24}") long duplicateWindowHours
    ) {
        this.feedbackRepository = feedbackRepository;
        this.customerRepository = customerRepository;
        this.feedbackCategoryRepository = feedbackCategoryRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.idempotencyWindowMinutes = idempotencyWindowMinutes;
        this.duplicateWindowHours = duplicateWindowHours;
    }

    public record CreateFeedbackResult(FeedbackResponseDto feedback, boolean idempotentHit) {
    }

    private record AutoClassifyResult(FeedbackCategory category, double confidence) {
    }

    // 1. Lấy toàn bộ danh sách phiếu (chưa xóa mềm)
    public List<FeedbackResponseDto> getAllFeedbacks() {
        return feedbackRepository.findByDeletedAtIsNull()
                .stream()
                .map(f -> toDto(f, List.of()))
                .toList();
    }

    // 2. Lấy chi tiết 1 phiếu theo ID (chưa xóa mềm)
    public FeedbackResponseDto getFeedbackById(Long id) {
        Feedback feedback = feedbackRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy phiếu với ID: " + id));
        return toDto(feedback, List.of());
    }

    // 3. Tra cứu phiếu theo mã (FB-yyyyMMdd-0001)
    public FeedbackResponseDto getFeedbackByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã phiếu không được để trống");
        }
        Feedback feedback = feedbackRepository.findByCodeAndDeletedAtIsNull(code.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy phiếu với mã: " + code));
        return toDto(feedback, List.of());
    }

    // 4. Cư dân xem danh sách phiếu của chính mình (CF13)
    public Page<FeedbackResponseDto> getMyFeedbacks(Long customerId, Pageable pageable) {
        if (customerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu header X-Customer-Id");
        }
        return feedbackRepository.findByCustomerIdAndDeletedAtIsNull(customerId, pageable)
                .map(f -> toDto(f, List.of()));
    }

    // 5. Cư dân xem chi tiết 1 phiếu của chính mình (CF13)
    public FeedbackResponseDto getMyFeedbackById(Long id, Long customerId) {
        if (customerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu header X-Customer-Id");
        }
        Feedback feedback = feedbackRepository.findByIdAndCustomerIdAndDeletedAtIsNull(id, customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy phiếu #" + id + " của cư dân #" + customerId));
        return toDto(feedback, List.of());
    }

    // 6. Tạo mới phiếu phản hồi/khiếu nại
    @Transactional
    public CreateFeedbackResult createFeedback(CreateFeedbackRequestDto request) {
        validateCreateRequest(request);

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy cư dân với ID: " + request.customerId()));

        if (!customer.canSubmit()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản cư dân đang bị khóa hoặc không hoạt động, không thể gửi phản hồi");
        }

        LocalDateTime now = LocalDateTime.now();
        String title = request.title().trim();
        String content = request.content().trim();

        // Mức 1 - Chống spam/bấm đúp (Idempotency): cùng customerId + title + content trong 1-2 phút
        LocalDateTime idempotencySince = now.minusMinutes(idempotencyWindowMinutes);
        List<Feedback> recentByCustomer = feedbackRepository
                .findByCustomerIdAndDeletedAtIsNullAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        customer.getId(), idempotencySince);
        for (Feedback recent : recentByCustomer) {
            if (Objects.equals(normalizeText(recent.getTitle()), title)
                    && Objects.equals(normalizeText(recent.getContent()), content)) {
                return new CreateFeedbackResult(toDto(recent, List.of()), true);
            }
        }

        // Sinh mã FB-yyyyMMdd-0001
        String code = generateDailyCode(now);

        Feedback feedback = new Feedback(code, customer.getId(), request.type(), title, content, now);
        feedback.setServiceName(trimToNull(request.serviceName()));
        feedback.setLocation(trimToNull(request.location()));
        String apartmentCode = trimToNull(request.apartmentCode());
        feedback.setApartmentCode(apartmentCode != null ? apartmentCode : customer.getApartmentCode());
        feedback.setAboutStaffId(request.aboutStaffId());

        // Phân loại tự động theo keyword của FeedbackCategory
        AutoClassifyResult autoResult = autoClassify(title, content);
        if (autoResult != null) {
            feedback.setAutoCategoryId(autoResult.category().getId());
            feedback.setAutoConfidence(autoResult.confidence());
        }

        FeedbackCategory chosenCategory = null;
        if (request.categoryId() != null) {
            chosenCategory = feedbackCategoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Danh mục không tồn tại: " + request.categoryId()));
            feedback.setCategoryId(chosenCategory.getId());
            feedback.setClassifiedBy(ClassifiedBy.MANUAL);
        } else if (autoResult != null) {
            chosenCategory = autoResult.category();
            feedback.setCategoryId(chosenCategory.getId());
            feedback.setClassifiedBy(ClassifiedBy.AUTO);
        } else {
            feedback.setMissingInfo(true);
        }

        // Tính mức ưu tiên & hạn SLA qua applyPriority()
        Priority priority = resolvePriority(request.type().isSimpleFlow(), chosenCategory);
        feedback.applyPriority(priority);

        // Mức 2 - Phát hiện nghi trùng nghiệp vụ (giữ nguyên trạng thái NEW để CS duyệt gộp)
        List<Long> suspectedDuplicateIds = findSuspectedDuplicateIds(feedback, now);

        Feedback saved = feedbackRepository.save(feedback);

        String historyNote = suspectedDuplicateIds.isEmpty()
                ? "Tạo mới phiếu phản hồi"
                : "Tạo mới phiếu phản hồi (nghi trùng với các phiếu ID: " + suspectedDuplicateIds + ")";
        statusHistoryRepository.save(new StatusHistory(
                null,
                saved.getId(),
                null,
                saved.getStatus(),
                customer.getId(),
                Role.CUSTOMER,
                historyNote,
                now
        ));

        return new CreateFeedbackResult(toDto(saved, suspectedDuplicateIds), false);
    }

    // 7. CS duyệt gộp phiếu trùng vào phiếu gốc (Mức 2)
    @Transactional
    public FeedbackResponseDto mergeDuplicateFeedback(Long duplicateId, MergeFeedbackRequestDto request) {
        if (request == null || request.originalFeedbackId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "originalFeedbackId là bắt buộc");
        }
        if (duplicateId.equals(request.originalFeedbackId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể gộp phiếu vào chính nó");
        }

        Feedback duplicate = feedbackRepository.findByIdAndDeletedAtIsNull(duplicateId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy phiếu cần gộp với ID: " + duplicateId));
        Feedback original = feedbackRepository.findByIdAndDeletedAtIsNull(request.originalFeedbackId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy phiếu gốc với ID: " + request.originalFeedbackId()));

        if (original.getStatus().isFinal()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Phiếu gốc đã ở trạng thái kết thúc (" + original.getStatus() + "), không thể gộp");
        }

        FeedbackStatus previousStatus = duplicate.getStatus();
        LocalDateTime now = LocalDateTime.now();
        try {
            duplicate.markDuplicateOf(original, now);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        feedbackRepository.save(original);
        Feedback savedDuplicate = feedbackRepository.save(duplicate);

        String note = trimToNull(request.note());
        if (note == null) {
            note = "Gộp phiếu trùng vào phiếu gốc #" + original.getId() + " (" + original.getCode() + ")";
        }
        statusHistoryRepository.save(new StatusHistory(
                null,
                savedDuplicate.getId(),
                previousStatus,
                savedDuplicate.getStatus(),
                request.staffId(),
                Role.CS_STAFF,
                note,
                now
        ));

        return toDto(savedDuplicate, List.of());
    }

    private void validateCreateRequest(CreateFeedbackRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu yêu cầu không được rỗng");
        }
        if (request.customerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "customerId là bắt buộc");
        }
        if (request.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type là bắt buộc");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title không được để trống");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content không được để trống");
        }
    }

    private String generateDailyCode(LocalDateTime now) {
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        long seq = feedbackRepository.countByCreatedAtBetween(startOfDay, endOfDay) + 1;
        String datePart = now.format(CODE_DATE_FORMAT);

        String code = String.format("FB-%s-%04d", datePart, seq);
        while (feedbackRepository.existsByCode(code)) {
            seq++;
            code = String.format("FB-%s-%04d", datePart, seq);
        }
        return code;
    }

    private AutoClassifyResult autoClassify(String title, String content) {
        List<FeedbackCategory> activeCategories = feedbackCategoryRepository.findByActiveTrue();
        if (activeCategories.isEmpty()) {
            return null;
        }

        String searchableText = (title + " " + content).toLowerCase(Locale.ROOT);
        FeedbackCategory bestCategory = null;
        int bestMatched = 0;
        double bestConfidence = 0.0;

        for (FeedbackCategory category : activeCategories) {
            List<String> keywords = category.getKeywords();
            if (keywords == null || keywords.isEmpty()) {
                continue;
            }

            int validKeywords = 0;
            int matched = 0;
            for (String kw : keywords) {
                if (kw == null || kw.isBlank()) {
                    continue;
                }
                validKeywords++;
                if (searchableText.contains(kw.trim().toLowerCase(Locale.ROOT))) {
                    matched++;
                }
            }

            if (matched > 0 && validKeywords > 0) {
                double confidence = (double) matched / validKeywords;
                if (matched > bestMatched || (matched == bestMatched && confidence > bestConfidence)) {
                    bestCategory = category;
                    bestMatched = matched;
                    bestConfidence = confidence;
                }
            }
        }

        if (bestCategory == null) {
            return null;
        }
        double roundedConfidence = Math.round(bestConfidence * 100.0) / 100.0;
        return new AutoClassifyResult(bestCategory, roundedConfidence);
    }

    private Priority resolvePriority(boolean simpleFlow, FeedbackCategory chosenCategory) {
        if (chosenCategory != null && chosenCategory.getDefaultPriority() != null) {
            return chosenCategory.getDefaultPriority();
        }
        return simpleFlow ? Priority.LOW : Priority.NORMAL;
    }

    private List<Long> findSuspectedDuplicateIds(Feedback feedback, LocalDateTime now) {
        if (feedback.getCategoryId() == null || feedback.getLocation() == null || feedback.getLocation().isBlank()) {
            return List.of();
        }
        Duration window = Duration.ofHours(duplicateWindowHours);
        List<Feedback> candidates = feedbackRepository.findDuplicateCandidates(
                feedback.getCategoryId(),
                feedback.getLocation(),
                now.minus(window),
                FINAL_STATUSES
        );
        return candidates.stream()
                .filter(candidate -> feedback.isPossibleDuplicateOf(candidate, window))
                .map(Feedback::getId)
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    // Hàm chuyển đổi Entity -> DTO
    private FeedbackResponseDto toDto(Feedback f, List<Long> suspectedDuplicateIds) {
        return new FeedbackResponseDto(
                f.getId(),
                f.getCode(),
                f.getTitle(),
                f.getContent(),
                f.getType() != null ? f.getType().name() : null,
                f.getPriority() != null ? f.getPriority().name() : null,
                f.getStatus() != null ? f.getStatus().name() : null,
                f.getApartmentCode(),
                f.getLocation(),
                f.getServiceName(),
                f.getCustomerId(),
                f.getCategoryId(),
                f.getAutoCategoryId(),
                f.getClassifiedBy() != null ? f.getClassifiedBy().name() : null,
                f.getAutoConfidence(),
                f.getAssignedStaffId(),
                f.isAfterHours(),
                f.isMissingInfo(),
                f.isRequiresManagerApproval(),
                f.getDuplicateOfId(),
                f.getDuplicateCount(),
                suspectedDuplicateIds != null ? List.copyOf(suspectedDuplicateIds) : List.of(),
                f.getResponseDueAt(),
                f.getResolveDueAt(),
                f.getCreatedAt()
        );
    }
}

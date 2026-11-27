package com.bql.feedback.service;

import com.bql.feedback.dto.CreateFeedbackRequestDto;
import com.bql.feedback.dto.FeedbackResponseDto;
import com.bql.feedback.exception.ForbiddenException;
import com.bql.feedback.model.Customer;
import com.bql.feedback.model.Feedback;
import com.bql.feedback.model.FeedbackType;
import com.bql.feedback.repository.CustomerRepository;
import com.bql.feedback.repository.FeedbackAttachmentRepository;
import com.bql.feedback.repository.FeedbackCategoryRepository;
import com.bql.feedback.repository.FeedbackRepository;
import com.bql.feedback.repository.StatusHistoryRepository;
import com.bql.feedback.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeedbackSecurityAccessTests {

    @Test
    void getFeedbackByCodeEnforcesOwnershipForCustomerAndAllowsStaff() {
        FeedbackRepository feedbackRepository = mock(FeedbackRepository.class);
        FeedbackService feedbackService = new FeedbackService(
                feedbackRepository,
                mock(CustomerRepository.class),
                mock(FeedbackCategoryRepository.class),
                mock(StatusHistoryRepository.class),
                2,
                24
        );

        Feedback ownedBy10 = new Feedback("FB-20261127-0001", 10L, FeedbackType.COMPLAINT,
                "Thang máy", "Hỏng thang máy", LocalDateTime.now());
        ownedBy10.setId(1L);
        Feedback orphanFeedback = new Feedback("FB-20261127-0002", null, FeedbackType.COMPLAINT,
                "Phiếu cũ", "Không có customerId", LocalDateTime.now());
        orphanFeedback.setId(2L);

        when(feedbackRepository.findByCodeAndDeletedAtIsNull("FB-20261127-0001"))
                .thenReturn(Optional.of(ownedBy10));
        when(feedbackRepository.findByCodeAndDeletedAtIsNull("FB-20261127-0002"))
                .thenReturn(Optional.of(orphanFeedback));

        AuthenticatedUser owner = new AuthenticatedUser(1L, "an@gmail.com", "CUSTOMER", 10L, null);
        AuthenticatedUser otherCustomer = new AuthenticatedUser(2L, "binh@gmail.com", "CUSTOMER", 20L, null);
        AuthenticatedUser csStaff = new AuthenticatedUser(3L, "cs01@bql.vn", "CS_STAFF", null, 5L);

        FeedbackResponseDto byOwner = feedbackService.getFeedbackByCode("FB-20261127-0001", owner);
        assertEquals(10L, byOwner.customerId());

        FeedbackResponseDto byStaff = feedbackService.getFeedbackByCode("FB-20261127-0001", csStaff);
        assertEquals(10L, byStaff.customerId());

        assertThrows(ForbiddenException.class,
                () -> feedbackService.getFeedbackByCode("FB-20261127-0001", otherCustomer));
        assertThrows(ForbiddenException.class,
                () -> feedbackService.getFeedbackByCode("FB-20261127-0002", owner));
    }

    @Test
    void createFeedbackUsesAuthenticatedCustomerIdAndIgnoresSpoofedBodyId() {
        FeedbackRepository feedbackRepository = mock(FeedbackRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        FeedbackCategoryRepository categoryRepository = mock(FeedbackCategoryRepository.class);
        StatusHistoryRepository statusHistoryRepository = mock(StatusHistoryRepository.class);
        FeedbackService feedbackService = new FeedbackService(
                feedbackRepository, customerRepository, categoryRepository, statusHistoryRepository, 2, 24);

        Customer realCustomer = new Customer(
                10L, "Nguyen Van An", "0901234567", "an@gmail.com", "A1-0501", true, LocalDateTime.now());
        when(customerRepository.findById(10L)).thenReturn(Optional.of(realCustomer));
        when(feedbackRepository.findByCustomerIdAndDeletedAtIsNullAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                eq(10L), any())).thenReturn(List.of());
        when(feedbackRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(feedbackRepository.existsByCode(any())).thenReturn(false);
        when(categoryRepository.findByActiveTrue()).thenReturn(List.of());
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> {
            Feedback fb = inv.getArgument(0);
            fb.setId(99L);
            return fb;
        });

        CreateFeedbackRequestDto spoofedRequest = new CreateFeedbackRequestDto(
                999L, FeedbackType.COMPLAINT, "Ồn ào", "Phòng bên mở nhạc lớn",
                null, null, "A1-05", null, null);

        FeedbackService.CreateFeedbackResult result = feedbackService.createFeedback(10L, spoofedRequest);
        assertEquals(10L, result.feedback().customerId());
    }

    @Test
    void attachmentServiceBlocksOtherCustomerAndAllowsStaff() {
        FeedbackAttachmentRepository attachmentRepository = mock(FeedbackAttachmentRepository.class);
        FeedbackRepository feedbackRepository = mock(FeedbackRepository.class);
        FeedbackAttachmentService attachmentService = new FeedbackAttachmentService(
                attachmentRepository, feedbackRepository, "uploads", 5, 10_485_760L);

        Feedback ownedBy10 = new Feedback("FB-20261127-0001", 10L, FeedbackType.COMPLAINT,
                "Test", "Content", LocalDateTime.now());
        ownedBy10.setId(1L);
        when(feedbackRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ownedBy10));
        when(attachmentRepository.findByFeedbackId(1L)).thenReturn(List.of());

        AuthenticatedUser otherCustomer = new AuthenticatedUser(2L, "binh@gmail.com", "CUSTOMER", 20L, null);
        AuthenticatedUser csStaff = new AuthenticatedUser(3L, "cs01@bql.vn", "CS_STAFF", null, 5L);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});

        assertThrows(ForbiddenException.class,
                () -> attachmentService.getAttachmentsByFeedbackId(1L, otherCustomer));
        assertThrows(ForbiddenException.class,
                () -> attachmentService.uploadAttachment(1L, file, otherCustomer));
        assertThrows(ForbiddenException.class,
                () -> attachmentService.loadAttachment(1L, 5L, otherCustomer));

        assertEquals(0, attachmentService.getAttachmentsByFeedbackId(1L, csStaff).size());
    }
}

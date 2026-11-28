package com.bql.feedback.service;

import com.bql.feedback.dto.AssignFeedbackRequestDto;
import com.bql.feedback.dto.InvestigationNoteRequestDto;
import com.bql.feedback.exception.ForbiddenException;
import com.bql.feedback.model.Department;
import com.bql.feedback.model.Feedback;
import com.bql.feedback.model.FeedbackAssignment;
import com.bql.feedback.model.FeedbackResponse;
import com.bql.feedback.model.FeedbackStatus;
import com.bql.feedback.model.FeedbackType;
import com.bql.feedback.model.Role;
import com.bql.feedback.model.Staff;
import com.bql.feedback.repository.EscalationHistoryRepository;
import com.bql.feedback.repository.FeedbackAssignmentRepository;
import com.bql.feedback.repository.FeedbackCategoryRepository;
import com.bql.feedback.repository.FeedbackRepository;
import com.bql.feedback.repository.FeedbackResponseRepository;
import com.bql.feedback.repository.StaffRepository;
import com.bql.feedback.repository.StatusHistoryRepository;
import com.bql.feedback.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StaffComplaintServiceTests {

    private FeedbackRepository feedbackRepository;
    private StaffRepository staffRepository;
    private FeedbackAssignmentRepository assignmentRepository;
    private FeedbackResponseRepository responseRepository;
    private StaffComplaintService service;

    @BeforeEach
    void setUp() {
        feedbackRepository = mock(FeedbackRepository.class);
        staffRepository = mock(StaffRepository.class);
        assignmentRepository = mock(FeedbackAssignmentRepository.class);
        responseRepository = mock(FeedbackResponseRepository.class);
        service = new StaffComplaintService(
                feedbackRepository,
                mock(FeedbackService.class),
                mock(FeedbackCategoryRepository.class),
                staffRepository,
                assignmentRepository,
                responseRepository,
                mock(EscalationHistoryRepository.class),
                mock(StatusHistoryRepository.class));
    }

    @Test
    void csStaffCanAssignComplaintToActiveDepartmentStaff() {
        Feedback feedback = feedback(1L);
        Staff assignee = new Staff(20L, "Department staff", Role.DEPT_STAFF, Department.TECHNICAL, true);
        when(feedbackRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(feedback));
        when(staffRepository.findById(20L)).thenReturn(Optional.of(assignee));
        when(assignmentRepository.findByFeedbackIdAndActiveTrue(1L)).thenReturn(Optional.empty());
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assignmentRepository.save(any(FeedbackAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.assign(1L, new AssignFeedbackRequestDto(20L, null, "Needs inspection"),
                new AuthenticatedUser(1L, "cs", "CS_STAFF", null, 10L));

        assertEquals(FeedbackStatus.ASSIGNED, feedback.getStatus());
        assertEquals(20L, feedback.getAssignedStaffId());
        verify(assignmentRepository).save(any(FeedbackAssignment.class));
    }

    @Test
    void departmentStaffCanOnlyAddInvestigationNotesToTheirOwnAssignments() {
        when(feedbackRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(feedback(1L)));
        when(assignmentRepository.findByFeedbackIdAndActiveTrue(1L)).thenReturn(Optional.of(
                new FeedbackAssignment(1L, 1L, 20L, Department.TECHNICAL, 10L,
                        LocalDateTime.now(), null, true, null)));
        when(responseRepository.save(any(FeedbackResponse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(ForbiddenException.class, () -> service.addInvestigationNote(
                1L, new InvestigationNoteRequestDto("Inspection complete"),
                new AuthenticatedUser(2L, "dept", "DEPT_STAFF", null, 21L)));
        verify(responseRepository, never()).save(any(FeedbackResponse.class));

        FeedbackResponse note = service.addInvestigationNote(
                1L, new InvestigationNoteRequestDto("Inspection complete"),
                new AuthenticatedUser(2L, "dept", "DEPT_STAFF", null, 20L));

        assertEquals(Role.DEPT_STAFF, note.getAuthorRole());
        assertTrue(note.isInternalNote());
    }

    private Feedback feedback(Long id) {
        Feedback feedback = new Feedback("FB-20261128-0001", 100L, FeedbackType.COMPLAINT,
                "Complaint", "Details", LocalDateTime.now());
        feedback.setId(id);
        return feedback;
    }
}

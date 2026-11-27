package com.bql.feedback.controller;

import com.bql.feedback.dto.FeedbackAttachmentResponseDto;
import com.bql.feedback.security.SecurityUtils;
import com.bql.feedback.service.FeedbackAttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/feedbacks/{feedbackId}/attachments")
@PreAuthorize("hasAnyRole('CUSTOMER', 'CS_STAFF', 'DEPT_STAFF', 'CS_MANAGER')")
public class FeedbackAttachmentController {

    private final FeedbackAttachmentService attachmentService;

    public FeedbackAttachmentController(FeedbackAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    // POST http://localhost:8080/api/feedbacks/1/attachments (multipart/form-data: file, định danh từ JWT)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FeedbackAttachmentResponseDto> upload(
            @PathVariable Long feedbackId,
            @RequestParam("file") MultipartFile file
    ) {
        FeedbackAttachmentResponseDto created =
                attachmentService.uploadAttachment(feedbackId, file, SecurityUtils.currentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET http://localhost:8080/api/feedbacks/1/attachments
    @GetMapping
    public ResponseEntity<List<FeedbackAttachmentResponseDto>> listByFeedback(@PathVariable Long feedbackId) {
        return ResponseEntity.ok(
                attachmentService.getAttachmentsByFeedbackId(feedbackId, SecurityUtils.currentUser()));
    }

    // GET http://localhost:8080/api/feedbacks/1/attachments/1/download
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long feedbackId,
            @PathVariable Long attachmentId
    ) {
        FeedbackAttachmentService.AttachmentDownload file =
                attachmentService.loadAttachment(feedbackId, attachmentId, SecurityUtils.currentUser());
        MediaType mediaType = file.contentType() != null
                ? MediaType.parseMediaType(file.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.resource());
    }
}

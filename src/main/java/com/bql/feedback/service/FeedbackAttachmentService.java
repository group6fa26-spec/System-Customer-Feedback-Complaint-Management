package com.bql.feedback.service;

import com.bql.feedback.dto.FeedbackAttachmentResponseDto;
import com.bql.feedback.exception.BusinessException;
import com.bql.feedback.exception.NotFoundException;
import com.bql.feedback.model.FeedbackAttachment;
import com.bql.feedback.repository.FeedbackAttachmentRepository;
import com.bql.feedback.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FeedbackAttachmentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "application/pdf"
    );

    private final FeedbackAttachmentRepository attachmentRepository;
    private final FeedbackRepository feedbackRepository;
    private final Path uploadRoot;
    private final int maxFilesPerFeedback;
    private final long maxFileSizeBytes;

    public FeedbackAttachmentService(
            FeedbackAttachmentRepository attachmentRepository,
            FeedbackRepository feedbackRepository,
            @Value("${app.upload.dir:uploads}") String uploadDir,
            @Value("${app.upload.max-files-per-feedback:5}") int maxFilesPerFeedback,
            @Value("${app.upload.max-file-size-bytes:10485760}") long maxFileSizeBytes
    ) {
        this.attachmentRepository = attachmentRepository;
        this.feedbackRepository = feedbackRepository;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxFilesPerFeedback = maxFilesPerFeedback;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public record AttachmentDownload(Resource resource, String fileName, String contentType) {
    }

    // 1. Upload tệp đính kèm cho phiếu (tối đa 5 file/phiếu, 10MB/file)
    @Transactional
    public FeedbackAttachmentResponseDto uploadAttachment(Long feedbackId, MultipartFile file, Long uploadedById) {
        ensureFeedbackExists(feedbackId);

        if (file == null || file.isEmpty()) {
            throw new BusinessException("Tệp đính kèm không được để trống");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BusinessException(
                    "Dung lượng tệp vượt quá giới hạn tối đa " + (maxFileSizeBytes / (1024 * 1024)) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(
                    "Định dạng tệp không được hỗ trợ. Chỉ chấp nhận: JPG, PNG, WEBP, GIF, PDF");
        }

        long currentCount = attachmentRepository.countByFeedbackId(feedbackId);
        if (currentCount >= maxFilesPerFeedback) {
            throw new BusinessException(
                    "Mỗi phiếu chỉ được đính kèm tối đa " + maxFilesPerFeedback + " tệp");
        }

        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        String extension = extractExtension(originalFileName);
        String storedFileName = UUID.randomUUID() + extension;
        String relativePath = "feedbacks/" + feedbackId + "/" + storedFileName;

        Path feedbackDir = uploadRoot.resolve("feedbacks").resolve(String.valueOf(feedbackId)).normalize();
        Path targetPath = feedbackDir.resolve(storedFileName).normalize();
        if (!targetPath.startsWith(uploadRoot)) {
            throw new BusinessException("Tên tệp không hợp lệ");
        }

        try {
            Files.createDirectories(feedbackDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Không thể lưu tệp đính kèm", ex);
        }

        try {
            FeedbackAttachment attachment = new FeedbackAttachment(
                    null,
                    feedbackId,
                    originalFileName,
                    relativePath,
                    contentType,
                    file.getSize(),
                    uploadedById,
                    LocalDateTime.now()
            );
            FeedbackAttachment saved = attachmentRepository.save(attachment);
            return toDto(saved);
        } catch (RuntimeException ex) {
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException ignored) {
            }
            throw ex;
        }
    }

    // 2. Lấy danh sách tệp đính kèm của 1 phiếu
    public List<FeedbackAttachmentResponseDto> getAttachmentsByFeedbackId(Long feedbackId) {
        ensureFeedbackExists(feedbackId);
        return attachmentRepository.findByFeedbackId(feedbackId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // 3. Tải/xem nội dung tệp đính kèm
    public AttachmentDownload loadAttachment(Long feedbackId, Long attachmentId) {
        ensureFeedbackExists(feedbackId);

        FeedbackAttachment attachment = attachmentRepository.findById(attachmentId)
                .filter(a -> feedbackId.equals(a.getFeedbackId()))
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy tệp đính kèm #" + attachmentId + " của phiếu #" + feedbackId));

        Path filePath = uploadRoot.resolve(attachment.getFileUrl()).normalize();
        if (!filePath.startsWith(uploadRoot) || !Files.exists(filePath)) {
            throw new NotFoundException("Tệp vật lý không tồn tại trên hệ thống");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("Không thể đọc tệp đính kèm");
            }
            return new AttachmentDownload(resource, attachment.getFileName(), attachment.getContentType());
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Đường dẫn tệp không hợp lệ", ex);
        }
    }

    private void ensureFeedbackExists(Long feedbackId) {
        feedbackRepository.findByIdAndDeletedAtIsNull(feedbackId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu với ID: " + feedbackId));
    }

    private String sanitizeFileName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "attachment";
        }
        String cleaned = StringUtils.cleanPath(rawName);
        int lastSlash = Math.max(cleaned.lastIndexOf('/'), cleaned.lastIndexOf('\\'));
        String baseName = lastSlash >= 0 ? cleaned.substring(lastSlash + 1) : cleaned;
        if (baseName.isBlank() || baseName.contains("..")) {
            return "attachment";
        }
        return baseName;
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String ext = fileName.substring(dot).toLowerCase(Locale.ROOT);
        return ext.matches("\\.[a-z0-9]{1,10}") ? ext : "";
    }

    private FeedbackAttachmentResponseDto toDto(FeedbackAttachment a) {
        String downloadUrl = "/api/feedbacks/" + a.getFeedbackId() + "/attachments/" + a.getId() + "/download";
        return new FeedbackAttachmentResponseDto(
                a.getId(),
                a.getFeedbackId(),
                a.getFileName(),
                downloadUrl,
                a.getContentType(),
                a.getSizeBytes(),
                a.getUploadedById(),
                a.getUploadedAt()
        );
    }
}

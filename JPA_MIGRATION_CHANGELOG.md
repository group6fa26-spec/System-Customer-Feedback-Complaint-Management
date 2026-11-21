# Nhật ký chỉnh sửa: Chuyển đổi Model sang JPA Entity

## 1. Tổng quan

- **Mục tiêu:** Khắc phục lỗi ứng dụng không thể khởi động (`Not a managed type`) do cả 10 model trong `com.bql.feedback.model` chưa được đánh dấu `@Entity`, trong đó 7 class đang được khai báo là Java `record` (không được JPA hỗ trợ làm entity).
- **Phạm vi:** Chỉnh sửa **10 file** trong `src/main/java/com/bql/feedback/model/`. Không thêm/xóa file Java nào, không sửa các enum hay repository.
- **Thống kê git diff:** `10 files changed, 490 insertions(+), 37 deletions(-)`

### Danh sách 10 entity & ánh xạ bảng

| # | Class | Trước | Sau | Bảng DB (`@Table`) |
|---|---|---|---|---|
| 1 | `Customer` | `class` (chưa có `@Entity`) | `@Entity class` | `customers` |
| 2 | `Staff` | `class` (chưa có `@Entity`) | `@Entity class` | `staff` |
| 3 | `Feedback` | `class` (chưa có `@Entity`) | `@Entity class` | `feedbacks` |
| 4 | `FeedbackAssignment` | `record` | `@Entity class` | `feedback_assignments` |
| 5 | `FeedbackAttachment` | `record` | `@Entity class` | `feedback_attachments` |
| 6 | `FeedbackCategory` | `record` | `@Entity class` | `feedback_categories` (+ `feedback_category_keywords`) |
| 7 | `FeedbackResponse` | `record` | `@Entity class` | `feedback_responses` |
| 8 | `EscalationHistory` | `record` | `@Entity class` | `escalation_histories` |
| 9 | `StatusHistory` | `record` | `@Entity class` | `status_histories` |
| 10 | `SatisfactionRating` | `record` | `@Entity class` | `satisfaction_ratings` |

---

## 2. Quy ước chung đã áp dụng cho cả 10 model

- **Thêm annotation JPA trên class:** `@Entity` và `@Table(name = "...")`.
- **Thêm khóa chính tự tăng trên field `id`:** `@Id` và `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- **Thêm constructor không tham số mức `protected` (`protected ClassName() {}`):** Đáp ứng yêu cầu của JPA/Hibernate để khởi tạo entity bằng reflection, đồng thời hạn chế code bên ngoài gọi trực tiếp constructor rỗng tạo entity thiếu dữ liệu.
- **Giữ nguyên constructor có tham số cũ** (đối với 7 `record`, chuyển canonical constructor thành all-args constructor có cùng thứ tự và kiểu tham số).
- **Ánh xạ tất cả field `enum` bằng `@Enumerated(EnumType.STRING)`:** Lưu tên enum dạng chuỗi thay vì số thứ tự (`ORDINAL`), tránh sai lệch dữ liệu khi thay đổi thứ tự enum.
- **Tránh từ khóa SQL dự phòng (reserved keywords):** Đổi tên cột DB bằng `@Column(name = "...")` cho các field trùng từ khóa SQL (`group`, `from`, `to`, `level`) trong khi vẫn giữ nguyên tên field Java để không phá vỡ repository/JPQL.

---

## 3. Chi tiết thay đổi theo từng file

### 3.1. Nhóm `class` bổ sung JPA Entity (3 file)

#### 1. `src/main/java/com/bql/feedback/model/Customer.java`
- **Thêm:**
  - Import `jakarta.persistence.*`.
  - Annotation `@Entity`, `@Table(name = "customers")`.
  - Annotation `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` trên field `id`.
  - Constructor rỗng `protected Customer() {}`.
- **Sửa:**
  - Thay wildcard import `import java.time.*;` bằng `import java.time.LocalDateTime;`.
  - Format lại các dòng gán trong constructor có tham số cho dễ đọc (giữ nguyên logic).
- **Bớt / Xóa:**
  - Bỏ `import java.time.*;`.
- **Giữ nguyên:** Toàn bộ field, getter/setter, method nghiệp vụ `canSubmit()` và `addSpamStrike()`.

#### 2. `src/main/java/com/bql/feedback/model/Staff.java`
- **Thêm:**
  - Import `jakarta.persistence.*`.
  - Annotation `@Entity`, `@Table(name = "staff")`.
  - Annotation `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` trên `id`.
  - Annotation `@Enumerated(EnumType.STRING)` trên `role` và `department`.
  - Constructor rỗng `protected Staff() {}`.
- **Sửa:**
  - Format lại thân constructor có tham số.
- **Bớt / Xóa:**
  - Xóa import thừa không dùng `import java.time.*;`.
- **Giữ nguyên:** Toàn bộ field và getter/setter.

#### 3. `src/main/java/com/bql/feedback/model/Feedback.java`
- **Thêm:**
  - Import `jakarta.persistence.*`.
  - Annotation `@Entity`, `@Table(name = "feedbacks")`.
  - Annotation `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` trên `id`.
  - Annotation `@Column(unique = true)` trên `code` (mã tra cứu duy nhất).
  - Annotation `@Enumerated(EnumType.STRING)` trên các field enum: `type`, `classifiedBy`, `priority`, `status`.
  - Annotation `@Column(length = 4000)` trên `content` và `resolutionSummary` (hỗ trợ nội dung dài hơn mặc định 255 ký tự).
  - Constructor rỗng `protected Feedback() {}`.
- **Sửa:**
  - Thay wildcard import `import java.time.*;` bằng import tường minh: `Duration`, `LocalDateTime`, `LocalTime`.
  - Format lại các câu lệnh nhiều biến trên một dòng trong constructor và các method nghiệp vụ.
- **Bớt / Xóa:**
  - Bỏ `import java.time.*;`.
- **Giữ nguyên:** Toàn bộ field, hằng số (`OFFICE_START`, `OFFICE_END`, `MAX_REOPEN`), getter/setter và toàn bộ logic nghiệp vụ (`isOutsideOfficeHours`, `applyPriority`, `isResponseOverdue`, `isResolveOverdue`, `isOverdue`, `markFirstResponse`, `approveByManager`, `changeStatus`, `canBeRated`, `isPossibleDuplicateOf`, `markDuplicateOf`, `softDelete`, `isDeleted`).

---

### 3.2. Nhóm chuyển từ `record` sang `@Entity class` (7 file)

> **Lưu ý chung khi chuyển từ `record` sang `class`:**
> - **Xóa:** Khai báo `public record ... (...) { }` và các accessor kiểu record không có tiền tố `get`/`is` (ví dụ `feedbackId()`), `equals`/`hashCode`/`toString` tự sinh của `record`.
> - **Thêm:** Khai báo `public class ...`, các field `private`, annotation JPA (`@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Enumerated`, `@Column`, ...), constructor rỗng `protected ClassName() {}`, all-args constructor giữ nguyên danh sách tham số, và bộ getter/setter chuẩn JavaBean (`getXxx()` / `isXxx()` / `setXxx(...)`).

#### 4. `src/main/java/com/bql/feedback/model/FeedbackAssignment.java`
- **Xóa:** Khai báo `public record FeedbackAssignment(...)`.
- **Thêm:**
  - Import `jakarta.persistence.*`.
  - `@Entity`, `@Table(name = "feedback_assignments")`.
  - `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` cho `id`.
  - `@Enumerated(EnumType.STRING)` cho `department`.
  - `@Column(length = 1000)` cho `note`.
  - 9 field `private` tương ứng, constructor rỗng, all-args constructor (9 tham số), đầy đủ getter/setter.

#### 5. `src/main/java/com/bql/feedback/model/FeedbackAttachment.java`
- **Xóa:** Khai báo `public record FeedbackAttachment(...)`.
- **Thêm:**
  - Import `jakarta.persistence.*`.
  - `@Entity`, `@Table(name = "feedback_attachments")`.
  - `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` cho `id`.
  - 8 field `private` tương ứng, constructor rỗng, all-args constructor (8 tham số), đầy đủ getter/setter.

#### 6. `src/main/java/com/bql/feedback/model/FeedbackCategory.java`
- **Xóa:** Khai báo `public record FeedbackCategory(...)`.
- **Thêm:**
  - Import `jakarta.persistence.*`, `java.util.ArrayList`.
  - `@Entity`, `@Table(name = "feedback_categories")`.
  - `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` cho `id`.
  - `@Column(unique = true)` cho `code`.
  - `@Enumerated(EnumType.STRING)` + `@Column(name = "issue_group")` cho field `group` *(tránh trùng từ khóa SQL `GROUP`)*.
  - `@Enumerated(EnumType.STRING)` cho `defaultPriority`.
  - Ánh xạ collection cho `List<String> keywords`:
    - `@ElementCollection(fetch = FetchType.EAGER)`
    - `@CollectionTable(name = "feedback_category_keywords", joinColumns = @JoinColumn(name = "category_id"))`
    - `@Column(name = "keyword")`
  - Khởi tạo mặc định `new ArrayList<>()` và sao chép sang `ArrayList` có thể thay đổi trong constructor và `setKeywords(...)` để tránh lỗi khi truyền immutable list (như `List.of(...)`) vào Hibernate.
  - Constructor rỗng, all-args constructor (7 tham số), đầy đủ getter/setter.

#### 7. `src/main/java/com/bql/feedback/model/FeedbackResponse.java`
- **Xóa:** Khai báo `public record FeedbackResponse(...)`.
- **Thêm:**
  - Import `jakarta.persistence.*`.
  - `@Entity`, `@Table(name = "feedback_responses")`.
  - `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` cho `id`.
  - `@Enumerated(EnumType.STRING)` cho `authorRole`.
  - `@Column(length = 4000)` cho `content`.
  - 8 field `private` tương ứng, constructor rỗng, all-args constructor (8 tham số), đầy đủ getter/setter.

#### 8. `src/main/java/com/bql/feedback/model/EscalationHistory.java`
- **Xóa:** Khai báo `public record EscalationHistory(...)`.
- **Thêm:**
  - Import `jakarta.persistence.*`.
  - `@Entity`, `@Table(name = "escalation_histories")`.
  - `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` cho `id`.
  - `@Enumerated(EnumType.STRING)` cho `reason`.
  - `@Column(name = "escalation_level")` cho field `level` *(tránh trùng từ khóa `LEVEL` trên một số hệ quản trị CSDL)*.
  - `@Column(length = 1000)` cho `note`.
  - 9 field `private` tương ứng, constructor rỗng, all-args constructor (9 tham số), đầy đủ getter/setter.

#### 9. `src/main/java/com/bql/feedback/model/StatusHistory.java`
- **Xóa:** Khai báo `public record StatusHistory(...)`.
- **Thêm:**
  - Import `jakarta.persistence.*`.
  - `@Entity`, `@Table(name = "status_histories")`.
  - `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` cho `id`.
  - `@Enumerated(EnumType.STRING)` + `@Column(name = "from_status")` cho field `from` *(tránh trùng từ khóa SQL `FROM`)*.
  - `@Enumerated(EnumType.STRING)` + `@Column(name = "to_status")` cho field `to` *(tránh trùng từ khóa SQL `TO`)*.
  - `@Enumerated(EnumType.STRING)` cho `changedByRole`.
  - `@Column(length = 1000)` cho `note`.
  - 8 field `private` tương ứng, constructor rỗng, all-args constructor (8 tham số), đầy đủ getter/setter.

#### 10. `src/main/java/com/bql/feedback/model/SatisfactionRating.java`
- **Xóa:** Khai báo `public record SatisfactionRating(...)` và compact constructor của record.
- **Thêm:**
  - Import `jakarta.persistence.*`.
  - `@Entity`, `@Table(name = "satisfaction_ratings")`.
  - `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` cho `id`.
  - `@Column(unique = true)` cho `feedbackId` *(phù hợp quy tắc nghiệp vụ: mỗi phiếu chỉ đánh giá một lần)*.
  - `@Column(length = 2000)` cho `comment`.
  - 8 field `private` tương ứng, constructor rỗng, all-args constructor (8 tham số), đầy đủ getter/setter.
  - **Bảo toàn & mở rộng validation:** Giữ nguyên kiểm tra `stars < 1 || stars > 5` (ném `IllegalArgumentException("stars phải từ 1 đến 5")`) ở cả all-args constructor và `setStars(int stars)`.

---

## 4. Kết quả kiểm thử sau chỉnh sửa

- **Lệnh chạy:** `mvn test` (Spring Boot 4.1.2, Hibernate ORM 7.4.13.Final, H2 2.4.240).
- **Kết quả:**
  - Quét và nạp thành công toàn bộ **10 JPA repository interfaces**.
  - Khởi tạo thành công `EntityManagerFactory` và sinh schema trên H2 in-memory mà không có lỗi `Not a managed type` hay lỗi cú pháp SQL.
  - `FeedbackApplicationTests.contextLoads()`: **PASSED** (`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`).

---

## 5. Nhật ký chỉnh sửa bổ sung: Sửa lỗi biên dịch trong `FeedbackService` & bổ sung truy vấn đọc cho `FeedbackRepository`

- **Ngày:** 2026-11-20
- **Mục tiêu:** Khắc phục 2 lỗi biên dịch (`cannot find symbol: method findAll()` và `cannot find symbol: method findById(Long)`) trong `FeedbackService` do `FeedbackRepository` cố ý kế thừa `Repository<Feedback, Long>` (không dùng `JpaRepository` để đảm bảo quy tắc xóa mềm và cấm xóa vật lý).
- **Phạm vi:** Chỉnh sửa **2 file** Java (`FeedbackRepository.java`, `FeedbackService.java`).

### Chi tiết thay đổi

1. **`src/main/java/com/bql/feedback/repository/FeedbackRepository.java`**
   - **Thêm:** Method `List<Feedback> findByDeletedAtIsNull();` trong nhóm `Read` để lấy toàn bộ phiếu chưa bị xóa mềm (`deletedAt is null`), giữ nguyên việc kế thừa `Repository<Feedback, Long>`.

2. **`src/main/java/com/bql/feedback/service/FeedbackService.java`**
   - **Sửa:** Trong `getAllFeedbacks()`, đổi `feedbackRepository.findAll()` thành `feedbackRepository.findByDeletedAtIsNull()`.
   - **Sửa:** Trong `getFeedbackById(Long id)`, đổi `feedbackRepository.findById(id)` thành `feedbackRepository.findByIdAndDeletedAtIsNull(id)`.

### Kết quả kiểm tra

- **Lệnh chạy:** `.\mvnw.cmd test-compile -o`
- **Kết quả:** Biên dịch thành công toàn bộ 32 file nguồn Java (`BUILD SUCCESS`), không còn lỗi `cannot find symbol`.

---

## 6. Nhật ký bổ sung API đọc (DTO, Service, Controller) cho `Customer`, `Staff`, `FeedbackCategory`

- **Ngày:** 2026-11-20
- **Mục tiêu:** Nhân bản đúng cấu trúc 3 tầng (`ResponseDto`, `Service`, `Controller`) tương tự `Feedback` cho 3 thực thể: `Customer`, `Staff` và `FeedbackCategory`.
- **Phạm vi:** Thêm mới **9 file** Java trong các package `dto`, `service`, `controller`.

### Danh sách 9 file thêm mới

| Nhóm | DTO (`dto/`) | Service (`service/`) | Controller (`controller/`) | Base URL |
|---|---|---|---|---|
| `Customer` | `CustomerResponseDto.java` | `CustomerService.java` | `CustomerController.java` | `/api/customers` |
| `Staff` | `StaffResponseDto.java` | `StaffService.java` | `StaffController.java` | `/api/staff` |
| `FeedbackCategory` | `FeedbackCategoryResponseDto.java` | `FeedbackCategoryService.java` | `FeedbackCategoryController.java` | `/api/categories` |

### Chi tiết triển khai

1. **`Customer` (`/api/customers`)**
   - `CustomerResponseDto`: record chứa `id`, `fullName`, `phone`, `email`, `apartmentCode`, `verifiedResident`, `active`, `blocked`, `spamStrikes`, `ratingTrustScore`, `createdAt`.
   - `CustomerService`: cung cấp `getAllCustomers()` (`findAll()`) và `getCustomerById(Long id)` (`findById(id)`), chuyển đổi entity sang DTO qua `toDto(Customer)`.
   - `CustomerController`: cung cấp `GET /api/customers` và `GET /api/customers/{id}`.

2. **`Staff` (`/api/staff`)**
   - `StaffResponseDto`: record chứa `id`, `fullName`, `role` (`String`), `department` (`String`), `active`, `onDuty`.
   - `StaffService`: cung cấp `getAllStaff()` (`findAll()`) và `getStaffById(Long id)` (`findById(id)`), ánh xạ enum `Role` và `Department` sang tên chuỗi (`name()`).
   - `StaffController`: cung cấp `GET /api/staff` và `GET /api/staff/{id}`.

3. **`FeedbackCategory` (`/api/categories`)**
   - `FeedbackCategoryResponseDto`: record chứa `id`, `code`, `name`, `group` (`String`), `defaultPriority` (`String`), `keywords` (`List<String>`), `active`.
   - `FeedbackCategoryService`: cung cấp `getAllCategories()` (`findAll()`) và `getCategoryById(Long id)` (`findById(id)`), ánh xạ enum `IssueGroup`, `Priority` sang tên chuỗi và sao chép `keywords` bằng `List.copyOf(...)`.
   - `FeedbackCategoryController`: cung cấp `GET /api/categories` và `GET /api/categories/{id}`.

### Kết quả kiểm tra

- **Lệnh chạy:** `.\mvnw.cmd test-compile -o`
- **Kết quả:** Biên dịch thành công toàn bộ 41 file nguồn chính và 1 file test (`BUILD SUCCESS`).

---

## 7. Nhật ký triển khai tạo phiếu phản hồi, tra cứu cư dân, gộp trùng 2 mức & upload tệp đính kèm

- **Ngày:** 2026-11-21
- **Mục tiêu:** Triển khai đầy đủ API tạo phiếu (`POST /api/feedbacks`), xem phiếu của cư dân (`GET /api/feedbacks/mine` dùng Header `X-Customer-Id`), tra cứu theo mã (`GET /api/feedbacks/code/{code}`), cơ chế chống trùng 2 mức (Idempotency + CS duyệt gộp), và upload/xem/tải tệp đính kèm (`FeedbackAttachment`).
- **Phạm vi:** Thêm mới **5 file** Java, chỉnh sửa **4 file** Java và **2 file** cấu hình (`application.properties`, `.gitignore`).

### Chi tiết các file thêm mới

1. **`src/main/java/com/bql/feedback/dto/CreateFeedbackRequestDto.java`**
   - Record nhận dữ liệu tạo mới phiếu phản hồi (`customerId`, `type`, `title`, `content`, `categoryId`, `serviceName`, `location`, `apartmentCode`, `aboutStaffId`).
2. **`src/main/java/com/bql/feedback/dto/MergeFeedbackRequestDto.java`**
   - Record nhận dữ liệu gộp phiếu trùng (`originalFeedbackId`, `staffId`, `note`) khi CS duyệt gộp.
3. **`src/main/java/com/bql/feedback/dto/FeedbackAttachmentResponseDto.java`**
   - Record trả về thông tin tệp đính kèm (`id`, `feedbackId`, `fileName`, `fileUrl`, `contentType`, `sizeBytes`, `uploadedById`, `uploadedAt`).
4. **`src/main/java/com/bql/feedback/service/FeedbackAttachmentService.java`**
   - Xử lý upload lưu file cục bộ vào thư mục `uploads/feedbacks/{feedbackId}/{uuid}.{ext}` (chống Path Traversal), kiểm tra tối đa **5 file/phiếu**, **10MB/file**, chỉ cho phép `JPG/PNG/WEBP/GIF/PDF`; cung cấp liệt kê và tải/xem tệp.
5. **`src/main/java/com/bql/feedback/controller/FeedbackAttachmentController.java`**
   - Cung cấp 3 endpoint dưới `/api/feedbacks/{feedbackId}/attachments`:
     - `POST /api/feedbacks/{feedbackId}/attachments` (`multipart/form-data`, nhận `file`, `uploadedById` hoặc header `X-Customer-Id`)
     - `GET /api/feedbacks/{feedbackId}/attachments`
     - `GET /api/feedbacks/{feedbackId}/attachments/{attachmentId}/download`

### Chi tiết các file chỉnh sửa

1. **`src/main/java/com/bql/feedback/repository/FeedbackRepository.java`**
   - **Thêm:** `existsByCode(String code)` để đảm bảo sinh mã ngày không bị trùng.
   - **Thêm:** `findByCustomerIdAndDeletedAtIsNullAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(...)` phục vụ chống bấm đúp (Idempotency).
2. **`src/main/java/com/bql/feedback/dto/FeedbackResponseDto.java`**
   - **Mở rộng:** Bổ sung `serviceName`, `autoCategoryId`, `classifiedBy`, `autoConfidence`, `afterHours`, `missingInfo`, `requiresManagerApproval`, `duplicateOfId`, `duplicateCount`, `suspectedDuplicateIds`, `responseDueAt`, `resolveDueAt`.
3. **`src/main/java/com/bql/feedback/service/FeedbackService.java`**
   - **Thêm:** `createFeedback(...)`:
     - Kiểm tra `customer.canSubmit()` (trả `403` nếu bị khóa/không hoạt động).
     - **Mức 1 (Idempotency):** cùng `customerId`, `title`, `content` trong vòng 2 phút → trả về phiếu vừa tạo (`200 OK`) thay vì tạo trùng.
     - Sinh mã `FB-yyyyMMdd-0001` theo ngày qua `countByCreatedAtBetween` + `existsByCode`.
     - Phân loại tự động theo `keywords` của `FeedbackCategory` đang `active`, tính `autoConfidence` (`0.0–1.0`).
     - Gọi `feedback.applyPriority(...)` để tính SLA `responseDueAt`, `resolveDueAt` và `requiresManagerApproval`.
     - **Mức 2 (Nghi trùng nghiệp vụ):** vẫn tạo phiếu ở trạng thái `NEW` (trạng thái mở ban đầu trong `FeedbackStatus`), quét ứng viên qua `findDuplicateCandidates` + `isPossibleDuplicateOf` (24h) và trả kèm `suspectedDuplicateIds` cho CS duyệt.
     - Ghi lịch sử `StatusHistory` ban đầu (`from = null`).
   - **Thêm:** `mergeDuplicateFeedback(...)` để CS duyệt gộp qua `duplicate.markDuplicateOf(original, now)` → chuyển trạng thái `MERGED`, tăng `duplicateCount` ở phiếu gốc và ghi `StatusHistory`.
   - **Thêm:** `getMyFeedbacks(customerId, pageable)`, `getMyFeedbackById(id, customerId)`, `getFeedbackByCode(code)`.
   - **Sửa:** Chuyển lỗi không tìm thấy sang `ResponseStatusException` (`400`/`403`/`404`).
4. **`src/main/java/com/bql/feedback/controller/FeedbackController.java`**
   - **Thêm:** `POST /api/feedbacks`, `GET /api/feedbacks/mine` (Header `X-Customer-Id`, có phân trang), `GET /api/feedbacks/mine/{id}` (Header `X-Customer-Id`), `GET /api/feedbacks/code/{code}`, `POST /api/feedbacks/{id}/merge`.
5. **`src/main/resources/application.properties`**
   - **Thêm:** Cấu hình multipart (`10MB`), `app.upload.*` (`uploads`, `5` file/phiếu, `10485760` bytes), `app.feedback.idempotency-window-minutes=2`, `app.feedback.duplicate-window-hours=24`.
6. **`.gitignore`**
   - **Thêm:** `uploads/` để không commit tệp upload cục bộ lên Git.

### Kết quả kiểm tra

- **Lệnh chạy:** `.\mvnw.cmd test-compile -o`
- **Kết quả:** Biên dịch thành công toàn bộ 46 file nguồn chính và 1 file test (`BUILD SUCCESS`).

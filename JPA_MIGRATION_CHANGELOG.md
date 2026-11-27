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

---

## 8. Nhật ký chuẩn hóa xử lý ngoại lệ tập trung (`exception/`) & cấu hình CORS (`config/`)

- **Ngày:** 2026-11-21
- **Mục tiêu:** Xây dựng tầng xử lý lỗi tập trung với `@RestControllerAdvice` trả JSON `ErrorResponse` thống nhất, cấu hình CORS cho Frontend trên `/api/**`, và chuyển toàn bộ ngoại lệ nghiệp vụ / không tìm thấy trong Model & Service sang `BusinessException` và `NotFoundException`.
- **Phạm vi:** Thêm mới **5 file** Java (`exception/` và `config/`), chỉnh sửa **7 file** Java (2 model + 5 service) và `application.properties`.

### Chi tiết các file thêm mới

1. **`src/main/java/com/bql/feedback/exception/NotFoundException.java`**
   - Ngoại lệ `RuntimeException` dùng khi không tìm thấy dữ liệu (trả HTTP `404 Not Found`).
2. **`src/main/java/com/bql/feedback/exception/BusinessException.java`**
   - Ngoại lệ `RuntimeException` dùng khi vi phạm quy tắc nghiệp vụ hoặc dữ liệu không hợp lệ (trả HTTP `400 Bad Request`).
3. **`src/main/java/com/bql/feedback/exception/ErrorResponse.java`**
   - Record chuẩn hóa JSON lỗi trả về gồm 4 trường: `timestamp`, `status`, `error`, `message` (kèm factory `ErrorResponse.of(HttpStatus, String)`).
4. **`src/main/java/com/bql/feedback/exception/GlobalExceptionHandler.java`**
   - `@RestControllerAdvice` bắt và chuẩn hóa các ngoại lệ: `NotFoundException` (`404`), `BusinessException` (`400`), `MethodArgumentNotValidException` (`400`), `MissingRequestHeaderException` (`400`), `MaxUploadSizeExceededException` (`400`), `HttpMessageNotReadableException` (`400`), `MethodArgumentTypeMismatchException` (`400`), và `Exception` chung (`500`).
5. **`src/main/java/com/bql/feedback/config/CorsConfig.java`**
   - `@Configuration` triển khai `WebMvcConfigurer`, bật CORS cho `/api/**` với các origin đọc từ `app.cors.allowed-origins` (`localhost:5500`, `127.0.0.1:5500`, `localhost:3000`, `localhost:5173`), đầy đủ method (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`), `allowedHeaders("*")`, `exposedHeaders("Content-Disposition")` và `allowCredentials(true)`. *(Dự án hiện chưa có Spring Security; khi bổ sung sau này cần bật thêm `.cors()` trong `SecurityFilterChain`.)*

### Chi tiết các file chỉnh sửa

1. **`src/main/java/com/bql/feedback/model/Feedback.java`**
   - **Sửa:** Đổi 4 chỗ `throw new IllegalStateException(...)` trong `changeStatus(...)` sang `throw new BusinessException(...)`.
2. **`src/main/java/com/bql/feedback/model/SatisfactionRating.java`**
   - **Sửa:** Đổi 2 chỗ `throw new IllegalArgumentException("stars phải từ 1 đến 5")` trong constructor và `setStars(...)` sang `throw new BusinessException(...)`.
3. **`src/main/java/com/bql/feedback/service/CustomerService.java`**, **`StaffService.java`**, **`FeedbackCategoryService.java`**
   - **Sửa:** Đổi `RuntimeException` khi không tìm thấy theo `id` sang `NotFoundException`.
4. **`src/main/java/com/bql/feedback/service/FeedbackService.java`**
   - **Sửa:** Đổi toàn bộ `ResponseStatusException` sang `NotFoundException` (404) và `BusinessException` (400).
   - **Xóa:** Khối `try-catch (IllegalStateException)` trong `mergeDuplicateFeedback(...)` để `BusinessException` từ `Feedback.changeStatus(...)` tự động chuyển về `GlobalExceptionHandler`.
5. **`src/main/java/com/bql/feedback/service/FeedbackAttachmentService.java`**
   - **Sửa:** Đổi `ResponseStatusException` sang `NotFoundException` (404), `BusinessException` (400), và `RuntimeException` (500 qua `GlobalExceptionHandler`).
6. **`src/main/resources/application.properties`**
   - **Thêm:** `app.cors.allowed-origins=http://localhost:5500,http://127.0.0.1:5500,http://localhost:3000,http://localhost:5173`.

### Kết quả kiểm tra

- **Lệnh chạy:** `.\mvnw.cmd test-compile -o`
- **Kết quả:** Biên dịch thành công toàn bộ 51 file nguồn chính và 1 file test (`BUILD SUCCESS`).

---

## 9. Nhật ký bổ sung chức năng đăng nhập cơ bản với bảng riêng `user_accounts`

- **Ngày:** 2026-11-21
- **Mục tiêu:** Triển khai API đăng nhập cơ bản (`POST /api/auth/login`) theo **Phương án 2** (tách bảng tài khoản `user_accounts` riêng, không sửa đổi entity `Customer` và `Staff`, tạm thời chưa áp dụng Spring Security/JWT).
- **Phạm vi:** Thêm mới **6 file** Java (`model`, `repository`, `dto`, `service`, `controller`).

### Chi tiết 6 file thêm mới

1. **`src/main/java/com/bql/feedback/model/UserAccount.java`**
   - Entity ánh xạ bảng `user_accounts` gồm: `id`, `username` (`unique`, `nullable = false`), `password` (`nullable = false`), `role` (`@Enumerated(EnumType.STRING)`), `customerId`, `staffId`, `active`, `createdAt`, `lastLoginAt`.
2. **`src/main/java/com/bql/feedback/repository/UserAccountRepository.java`**
   - Kế thừa `JpaRepository<UserAccount, Long>`, bổ sung `findByUsername`, `existsByUsername`, `findByCustomerId`, `findByStaffId`.
3. **`src/main/java/com/bql/feedback/dto/LoginRequestDto.java`**
   - Record nhận `username` và `password`.
4. **`src/main/java/com/bql/feedback/dto/LoginResponseDto.java`**
   - Record trả thông tin phiên đăng nhập gồm `accountId`, `username`, `role`, `customerId`, `staffId`, `fullName`, `department`, `apartmentCode`, `blocked`, `lastLoginAt` (không trả `password`).
5. **`src/main/java/com/bql/feedback/service/AuthService.java`**
   - Xác thực `username`/`password`, kiểm tra `active` của `UserAccount` và hồ sơ `Customer`/`Staff` tương ứng, cập nhật `lastLoginAt` khi đăng nhập thành công; ném `BusinessException`/`NotFoundException` về `GlobalExceptionHandler`.
6. **`src/main/java/com/bql/feedback/controller/AuthController.java`**
   - Cung cấp endpoint `POST /api/auth/login`.

### Kết quả kiểm tra

- **Lệnh chạy:** `.\mvnw.cmd test-compile -o`
- **Kết quả:** Biên dịch thành công toàn bộ 57 file nguồn chính và 1 file test (`BUILD SUCCESS`).

---

## 10. Nhật ký bổ sung API đăng ký cơ bản cho cư dân (`POST /api/auth/register`)

- **Ngày:** 2026-11-21
- **Mục tiêu:** Bổ sung chức năng đăng ký tài khoản cơ bản chỉ dành riêng cho cư dân (`Role.CUSTOMER`), tự động tạo đồng thời hồ sơ `Customer` và tài khoản `UserAccount` trong cùng transaction.
- **Phạm vi:** Thêm mới **1 file** (`RegisterRequestDto.java`), chỉnh sửa **2 file** (`AuthService.java`, `AuthController.java`).

### Chi tiết thay đổi

1. **`src/main/java/com/bql/feedback/dto/RegisterRequestDto.java`** *(thêm mới)*
   - Record nhận dữ liệu đăng ký cư dân: `username`, `password`, `fullName`, `phone`, `email`, `apartmentCode`.
2. **`src/main/java/com/bql/feedback/service/AuthService.java`** *(chỉnh sửa)*
   - **Thêm:** Method `registerCustomer(RegisterRequestDto request)` (`@Transactional`):
     - Kiểm tra đầy đủ trường bắt buộc; kiểm tra trùng `username` (`existsByUsername`), trùng `email` (`existsByEmail`), và trùng `phone` (`findByPhone`) → ném `BusinessException` nếu vi phạm.
     - Tạo mới `Customer` với `verifiedResident = false`, `active = true`, `ratingTrustScore = 100`.
     - Tạo mới `UserAccount` gắn cứng `role = Role.CUSTOMER`, liên kết `customerId`, `staffId = null`, `active = true`.
     - Trả về `LoginResponseDto` (không trả `password`).
3. **`src/main/java/com/bql/feedback/controller/AuthController.java`** *(chỉnh sửa)*
   - **Thêm:** Endpoint `POST /api/auth/register` trả HTTP `201 Created`.

### Kết quả kiểm tra

- **Lệnh chạy:** `.\mvnw.cmd test-compile -o`
- **Kết quả:** Biên dịch thành công toàn bộ 58 file nguồn chính và 1 file test (`BUILD SUCCESS`).

---

## 11. Nhật ký phân loại tài khoản chủ căn hộ và người thuê

> **Lưu ý:** Thiết kế trong mục này đã được điều chỉnh theo mục 12: role cư dân chỉ còn `CUSTOMER`, và thông tin hợp đồng được chuyển sang entity riêng.

- **Ngày:** 2026-11-26
- **Mục tiêu:** Phân biệt loại tài khoản chủ căn hộ và người thuê; lưu thời hạn hợp đồng thuê để phục vụ bước kiểm tra hiệu lực tài khoản sau này.
- **Phạm vi:** Thêm mới **1 file** model; chỉnh sửa **7 file** model, DTO và service.

### Chi tiết thay đổi

1. **`src/main/java/com/bql/feedback/model/ResidentType.java`** *(thêm mới)*
   - Khai báo loại cư dân `OWNER` (chủ căn hộ) và `TENANT` (người thuê).
2. **`src/main/java/com/bql/feedback/model/Role.java`**
   - Bổ sung role `OWNER` và `TENANT`; giữ nguyên `CUSTOMER` để tương thích các tài khoản hiện có.
3. **`src/main/java/com/bql/feedback/model/Customer.java`**
   - Bổ sung `residentType`, `rentalContractStartDate`, `rentalContractEndDate`.
   - Hồ sơ cũ chưa có loại cư dân được trả về mặc định là `OWNER`; constructor hiện có tiếp tục được giữ để tương thích.
4. **`src/main/java/com/bql/feedback/dto/RegisterRequestDto.java`**
   - Bổ sung loại cư dân và ngày bắt đầu/kết thúc hợp đồng thuê vào dữ liệu đăng ký.
5. **`src/main/java/com/bql/feedback/service/AuthService.java`**
   - Đăng ký với loại `TENANT` tạo tài khoản role `TENANT` và yêu cầu có đủ ngày bắt đầu/kết thúc hợp đồng; từ chối nếu ngày kết thúc trước ngày bắt đầu.
   - Đăng ký không truyền loại cư dân mặc định là `OWNER`; chủ căn hộ không nhận ngày hợp đồng thuê.
   - Đăng nhập nhận diện các role cư dân `CUSTOMER`, `OWNER`, `TENANT` và trả về loại cư dân cùng thời hạn hợp đồng.
   - Chưa chặn đăng nhập hoặc thao tác khi hợp đồng chưa hiệu lực/hết hạn; kiểm tra hiệu lực và phân quyền được để lại cho giai đoạn sau theo yêu cầu.
6. **`src/main/java/com/bql/feedback/dto/LoginResponseDto.java`**, **`CustomerResponseDto.java`** và **`src/main/java/com/bql/feedback/service/CustomerService.java`**
   - Bổ sung loại cư dân và thời hạn hợp đồng vào dữ liệu phản hồi đăng nhập, danh sách/chi tiết cư dân.

### Kết quả kiểm tra

- Công cụ kiểm tra lỗi mã nguồn không phát hiện lỗi sau thay đổi.
- Bộ test Maven chưa được chạy trong lượt cập nhật này.

---

## 12. Điều chỉnh mô hình cư dân, hợp đồng thuê và trạng thái chờ duyệt

- **Ngày:** 2026-11-26
- **Mục tiêu:** Giữ role xác thực duy nhất `CUSTOMER` cho cư dân, tách hợp đồng thuê khỏi hồ sơ cư dân để lưu được nhiều hợp đồng, và để tài khoản tự đăng ký ở trạng thái chờ duyệt.
- **Phạm vi:** Thêm mới **5 file** model/repository/DTO; chỉnh sửa **8 file** model, DTO và service; thêm **1 file test**.

### Chi tiết thay đổi

1. **`src/main/java/com/bql/feedback/model/Role.java`**
   - Xóa các role cư dân `OWNER` và `TENANT`; role cư dân dùng chung `CUSTOMER`.
2. **`src/main/java/com/bql/feedback/model/ResidentType.java`** và **`Customer.java`**
   - Giữ `ResidentType.OWNER`/`TENANT` để phân loại nghiệp vụ.
   - Gỡ ngày hợp đồng khỏi `Customer`; hồ sơ cư dân không còn bị giới hạn vào một hợp đồng duy nhất.
3. **`src/main/java/com/bql/feedback/model/RentalContract.java`**, **`RentalContractStatus.java`**, **`repository/RentalContractRepository.java`** *(thêm mới)*
   - Tạo bảng `rental_contracts`, lưu riêng người thuê, mã căn hộ, ngày bắt đầu/kết thúc, trạng thái (`PENDING`, `ACTIVE`, `EXPIRED`, `TERMINATED`) và thời điểm tạo.
   - Repository truy vấn danh sách hợp đồng theo cư dân, sắp xếp hợp đồng mới nhất trước.
4. **`src/main/java/com/bql/feedback/model/AccountStatus.java`** và **`UserAccount.java`**
   - Bổ sung trạng thái tài khoản `PENDING`, `APPROVED`, `REJECTED`, `DISABLED`.
   - Tài khoản cư dân đăng ký mới được lưu với `role = CUSTOMER`, `accountStatus = PENDING`, `active = false`; đăng nhập bị từ chối khi tài khoản đang chờ duyệt hoặc đã bị từ chối.
   - Dữ liệu tài khoản cũ chưa có `accountStatus` tiếp tục suy ra trạng thái từ cột `active`.
5. **`RegisterRequestDto.java`**, **`AuthService.java`**, **`LoginResponseDto.java`**, **`CustomerResponseDto.java`**, **`CustomerService.java`**, **`RentalContractResponseDto.java`**
   - Đăng ký người thuê tạo hồ sơ `Customer` loại `TENANT` và một hợp đồng `PENDING`; đăng ký chủ căn hộ không tạo hợp đồng thuê.
   - Phản hồi đăng ký/đăng nhập và API đọc cư dân trả về loại cư dân, trạng thái tài khoản và danh sách hợp đồng tương ứng.
   - Chưa triển khai API duyệt tài khoản, xác thực phân quyền theo role hoặc kiểm tra quyền truy cập dựa trên hiệu lực hợp đồng.
6. **`src/test/java/com/bql/feedback/service/AuthServiceResidentRegistrationTests.java`** *(thêm mới)*
   - Kiểm thử đăng ký người thuê tạo tài khoản `CUSTOMER` đang `PENDING` cùng hợp đồng thuê `PENDING`, và tài khoản chờ duyệt không đăng nhập được.

### Chuyển đổi dữ liệu hiện có

`spring.jpa.hibernate.ddl-auto=update` tạo cột/bảng mới nhưng không chuyển dữ liệu role và hợp đồng cũ. Sau khi ứng dụng tạo schema mới, cần sao chép hợp đồng cũ trước, sau đó đổi role `OWNER`/`TENANT` về `CUSTOMER`; tài khoản cư dân đã đăng ký theo cấu trúc cũ được đưa về chờ duyệt để BQL xác nhận:

```sql
INSERT INTO rental_contracts
    (tenant_customer_id, apartment_code, start_date, end_date, status, created_at)
SELECT c.id, c.apartment_code, c.rental_contract_start_date,
       c.rental_contract_end_date, 'PENDING', COALESCE(c.created_at, GETDATE())
FROM customers c
WHERE c.resident_type = 'TENANT'
  AND c.rental_contract_start_date IS NOT NULL
  AND c.rental_contract_end_date IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM rental_contracts rc
      WHERE rc.tenant_customer_id = c.id
        AND rc.start_date = c.rental_contract_start_date
        AND rc.end_date = c.rental_contract_end_date
  );

UPDATE user_accounts
SET role = 'CUSTOMER', account_status = 'PENDING', active = 0
WHERE customer_id IS NOT NULL;
```

### Kết quả kiểm tra

- Đã bổ sung kiểm thử hồi quy cho đăng ký người thuê và đăng nhập khi tài khoản đang chờ duyệt.
- **Lệnh chạy:** `\.\mvnw.cmd test`
- **Kết quả:** `BUILD SUCCESS`; 3 test chạy, không có lỗi hoặc thất bại (1 test tải context ứng dụng và 2 test nghiệp vụ đăng ký/đăng nhập).

---

## 13. Bổ sung khóa ngoại và ràng buộc toàn vẹn cho SQL Server

- **Ngày:** 2026-11-26
- **Mục tiêu:** Bảo vệ các liên kết đang lưu bằng ID `Long` thuần bằng FK thực trong SQL Server, đồng thời thêm các ràng buộc unique/check và index cần thiết mà không phải đổi service/DTO sang quan hệ JPA.
- **Migration:** [database/migrations/V1__relational_integrity_constraints.sql](database/migrations/V1__relational_integrity_constraints.sql)
- **Chiến lược xóa:** Các FK dùng mặc định `NO ACTION` (RESTRICT); không xóa cứng phiếu hay lịch sử theo cascade. Tệp vật lý được quản lý riêng, nên cascade metadata của file đính kèm sẽ không tự xóa file trên ổ đĩa.

### Quan hệ được ràng buộc

- `feedbacks` → `customers`, `feedback_categories` (danh mục chính và tự phân loại), `staff` (nhân viên được phản ánh, được giao xử lý, manager duyệt) và `feedbacks` (phiếu gốc trùng).
- `feedback_assignments` → `feedbacks` và `staff` (nhân viên nhận/ giao việc).
- `feedback_attachments`, `feedback_responses`, `status_histories`, `escalation_histories`, `satisfaction_ratings` → `feedbacks`.
- `escalation_histories` → `staff` (người chuyển cấp và manager nhận cấp).
- `satisfaction_ratings` → `customers`.
- `user_accounts` → `customers` hoặc `staff`; mỗi cư dân/nhân viên chỉ liên kết tối đa một tài khoản.
- `feedback_category_keywords` → `feedback_categories` và `rental_contracts` → `customers`.

Các cột đa hình `uploaded_by_id`, `author_id`, `changed_by_id` không có FK trực tiếp vì ID có thể thuộc nhiều loại đối tượng. `feedback_assignments.assigned_by_id` được xác định trong dữ liệu nghiệp vụ hiện tại là ID nhân viên và có FK đến `staff`.

### Ràng buộc bổ sung

- Bắt buộc `feedbacks.customer_id`, `feedbacks.status`, `feedbacks.created_at`.
- CHECK `satisfaction_ratings.stars` từ 1 đến 5 và cấm feedback tự trỏ làm bản trùng của chính nó.
- Filtered unique index cho email/điện thoại cư dân và ID cư dân/nhân viên ở `user_accounts`, tránh vấn đề SQL Server chỉ cho phép một giá trị `NULL` trong unique index không lọc.
- Index các FK chính, `(feedbacks.customer_id, feedbacks.created_at)` và danh sách hợp đồng theo cư dân/ngày bắt đầu.
- Các ràng buộc unique `feedbacks.code`, `feedback_categories.code`, `user_accounts.username`, `satisfaction_ratings.feedback_id` đã được khai báo từ trước trong entity; migration không tạo bản sao.

### Preflight và áp dụng

- Trước migration, đã chạy truy vấn chỉ đọc trên database `feedbackdb`: không phát hiện orphan FK, dữ liệu bắt buộc bị null, email/điện thoại hoặc liên kết tài khoản bị trùng, rating ngoài khoảng, hay feedback tự trỏ.
- Migration chạy trong transaction, kiểm tra dữ liệu trước khi thêm constraint và rollback nếu phát hiện dữ liệu không hợp lệ.
- Đã áp dụng migration lên SQL Server cấu hình cục bộ; xác minh có **22 FK**, **2 CHECK**, 4 filtered unique index, và 3 cột bắt buộc ở `feedbacks` đang `NOT NULL`.
- Đây là migration SQL Server thủ công, không bật Flyway và giữ `spring.jpa.hibernate.ddl-auto=update`; cần chạy migration này trên từng môi trường sau backup và preflight, trước khi triển khai bản ứng dụng cần các ràng buộc.
- Chưa đổi sang `ddl-auto=validate`: dự án chưa có migration baseline tạo toàn bộ schema cho database trống.

### Kết quả kiểm tra

- Bộ test Maven được chạy sau migration; kết quả sẽ được ghi nhận sau khi hoàn tất.
- **Lệnh chạy:** `\.\mvnw.cmd test`
- **Kết quả:** `BUILD SUCCESS`; 3 test chạy, không có lỗi hoặc thất bại. Spring Boot khởi động và kết nối SQL Server thành công sau khi thêm các constraint.

---

## 14. Bổ sung API xem và cập nhật hồ sơ cư dân (`GET/PUT /api/customers/me`, `PUT /api/customers/{id}`)

- **Ngày:** 2026-11-26
- **Mục tiêu:** Cung cấp API để cư dân tự xem/cập nhật thông tin liên hệ hồ sơ cá nhân qua Header `X-Customer-Id` và cho BQL cập nhật thông tin liên hệ cư dân theo ID, đảm bảo không làm lệch `apartmentCode` (với `rental_contracts`) hay reset nhầm `verifiedResident`.
- **Phạm vi:** Thêm mới **2 file** (`UpdateCustomerProfileRequestDto.java`, `CustomerServiceProfileTests.java`), chỉnh sửa **3 file** (`CustomerRepository.java`, `CustomerService.java`, `CustomerController.java`).

### Chi tiết thay đổi

1. **`src/main/java/com/bql/feedback/dto/UpdateCustomerProfileRequestDto.java`** *(thêm mới)*
   - Record rút gọn chỉ nhận 3 trường thông tin liên hệ: `fullName`, `phone`, `email` (không nhận `apartmentCode` và các trường quản trị hệ thống).
2. **`src/main/java/com/bql/feedback/repository/CustomerRepository.java`** *(chỉnh sửa)*
   - **Thêm:** `Optional<Customer> findByEmailIgnoreCase(String email);` để kiểm tra trùng email không phân biệt hoa thường đồng nhất giữa Java và Filtered Unique Index dưới SQL Server.
3. **`src/main/java/com/bql/feedback/service/CustomerService.java`** *(chỉnh sửa)*
   - **Thêm:** `updateProfile(Long customerId, UpdateCustomerProfileRequestDto request, boolean isSelfUpdate)` (`@Transactional`):
     - Kiểm tra `request` và 3 trường `fullName`, `phone`, `email` không null/blank (`BusinessException`), chuẩn hóa `trim()` cả 3 trường và đưa `email` về `toLowerCase(Locale.ROOT)`.
     - Tìm `Customer` theo `customerId` (`NotFoundException` nếu không tồn tại); chặn hồ sơ `!active` (`BusinessException`) và chặn tài khoản bị khóa spam tự cập nhật khi `isSelfUpdate && blocked` (`BusinessException`), trong khi BQL (`isSelfUpdate = false`) vẫn sửa được thông tin liên hệ khi cần.
     - Kiểm tra trùng `email` (`findByEmailIgnoreCase`) và `phone` (`findByPhone`) với cư dân khác (`id != customerId`).
     - Chỉ cập nhật `fullName`, `phone`, `email`; giữ nguyên `apartmentCode`, `residentType`, `verifiedResident` và các trường quản trị.
4. **`src/main/java/com/bql/feedback/controller/CustomerController.java`** *(chỉnh sửa)*
   - **Thêm:**
     - `GET /api/customers/me` (Header `X-Customer-Id`) → gọi `getCustomerById(customerId)`.
     - `PUT /api/customers/me` (Header `X-Customer-Id`) → gọi `updateProfile(customerId, request, true)`.
     - `PUT /api/customers/{id}` → gọi `updateProfile(id, request, false)`.
5. **`src/test/java/com/bql/feedback/service/CustomerServiceProfileTests.java`** *(thêm mới)*
   - Bổ sung 8 unit test kiểm thử: chuẩn hóa `trim`/`toLowerCase` và giữ nguyên `apartmentCode`/`verifiedResident`, giữ nguyên email/phone của chính mình, chặn trùng email/phone người khác, chặn cư dân `blocked` tự sửa nhưng cho phép BQL sửa, chặn hồ sơ `!active`, báo `NotFoundException` khi không tồn tại, và báo `BusinessException` khi bỏ trống trường bắt buộc.

### Kết quả kiểm tra

- **Biên dịch:** Thành công toàn bộ **65 file** nguồn chính và **3 file** test (`BUILD SUCCESS`).
- **Kiểm thử unit test:** `.\mvnw.cmd test -o -Dtest=CustomerServiceProfileTests,AuthServiceResidentRegistrationTests` → **`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`** (`CustomerServiceProfileTests`: 8 passed, `AuthServiceResidentRegistrationTests`: 2 passed, `BUILD SUCCESS`).

---

## 15. Chuẩn hóa & kiểm tra định dạng Email/SĐT dùng chung (`ContactValidationUtil`)

- **Ngày:** 2026-11-26
- **Mục tiêu:** Chặn dữ liệu `email` và `phone` sai định dạng đồng nhất giữa luồng đăng ký (`AuthService`) và cập nhật hồ sơ (`CustomerService`), đồng thời chuẩn hóa SĐT đầu `+84` về `0` trước khi kiểm tra trùng lặp và lưu xuống DB; giữ nguyên `user_accounts.username` độc lập.
- **Phạm vi:** Thêm mới **1 file** (`ContactValidationUtil.java`), chỉnh sửa **4 file** (`CustomerService.java`, `AuthService.java`, `CustomerServiceProfileTests.java`, `AuthServiceResidentRegistrationTests.java`).

### Chi tiết thay đổi

1. **`src/main/java/com/bql/feedback/util/ContactValidationUtil.java`** *(thêm mới)*
   - `normalizeAndValidateEmail(String rawEmail)`: kiểm tra rỗng, chuẩn hóa `trim()` + `toLowerCase(Locale.ROOT)`, kiểm tra Regex `^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$` (sai định dạng ném `BusinessException`).
   - `normalizeAndValidatePhone(String rawPhone)`: kiểm tra rỗng, chuẩn hóa `trim()`, kiểm tra Regex `^(0|\+84)[0-9]{9}$` (sai định dạng ném `BusinessException`), và chuẩn hóa đầu `+84` về `0` (`"0" + phone.substring(3)`) để tránh lệch giữa `0901234567` và `+84901234567` khi kiểm tra trùng `findByPhone` và Filtered Unique Index dưới SQL Server.
2. **`src/main/java/com/bql/feedback/service/CustomerService.java`** *(chỉnh sửa)*
   - **Sửa:** Gọi `ContactValidationUtil.normalizeAndValidatePhone(...)` và `ContactValidationUtil.normalizeAndValidateEmail(...)` trong `updateProfile(...)`. Không đồng bộ sang `user_accounts.username`.
3. **`src/main/java/com/bql/feedback/service/AuthService.java`** *(chỉnh sửa)*
   - **Sửa:** Gọi cùng 2 hàm chuẩn hóa/kiểm tra trên trong `registerCustomer(...)` để đồng nhất luật giữa đăng ký và cập nhật hồ sơ.
4. **`src/test/java/com/bql/feedback/service/CustomerServiceProfileTests.java`** & **`AuthServiceResidentRegistrationTests.java`** *(chỉnh sửa)*
   - Bổ sung 5 unit test cho email sai định dạng, SĐT sai định dạng, chuẩn hóa `+84` → `0` + `email.toLowerCase()`, và phát hiện trùng giữa `+84...` với `0...`.

### Kết quả kiểm tra

- **Biên dịch:** Thành công toàn bộ **66 file** nguồn chính và **3 file** test (`BUILD SUCCESS`).
- **Kiểm thử unit test:** `.\mvnw.cmd test -o -Dtest=CustomerServiceProfileTests,AuthServiceResidentRegistrationTests` → **`Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`** (`CustomerServiceProfileTests`: 11 passed, `AuthServiceResidentRegistrationTests`: 4 passed, `BUILD SUCCESS`).

---

## 16. Bổ sung API duyệt / từ chối tài khoản cư dân (`/api/accounts/**`, chỉ `CS_MANAGER`)

- **Ngày:** 2026-11-27
- **Mục tiêu:** Cung cấp luồng quản trị duyệt/từ chối tài khoản cư dân đang ở trạng thái `PENDING` (`PENDING` → `APPROVED` / `REJECTED`), chỉ cho phép `CS_MANAGER` đang hoạt động thực hiện, đồng bộ trạng thái `verifiedResident`, `active` và hợp đồng thuê `RentalContract`, đồng thời tránh N+1 query khi xem danh sách chờ duyệt.
- **Phạm vi:** Thêm mới **5 file** (`ForbiddenException.java`, `AccountApprovalResponseDto.java`, `AccountApprovalService.java`, `AccountApprovalController.java`, `AccountApprovalServiceTests.java`), chỉnh sửa **3 file** (`GlobalExceptionHandler.java`, `UserAccountRepository.java`, `RentalContractRepository.java`).

### Chi tiết thay đổi

1. **`src/main/java/com/bql/feedback/exception/ForbiddenException.java`** *(thêm mới)* & **`GlobalExceptionHandler.java`** *(chỉnh sửa)*
   - Bổ sung ngoại lệ `ForbiddenException` và handler trả HTTP **`403 Forbidden`** đúng chuẩn `ErrorResponse`.
2. **`src/main/java/com/bql/feedback/repository/UserAccountRepository.java`** *(chỉnh sửa)*
   - **Thêm:** `List<UserAccount> findByRoleAndAccountStatusOrderByCreatedAtAsc(Role role, AccountStatus accountStatus);`
3. **`src/main/java/com/bql/feedback/repository/RentalContractRepository.java`** *(chỉnh sửa)*
   - **Thêm:** `List<RentalContract> findByTenantCustomerIdInOrderByStartDateDesc(Collection<Long> tenantCustomerIds);` phục vụ truy vấn gom theo lô.
4. **`src/main/java/com/bql/feedback/dto/AccountApprovalResponseDto.java`** *(thêm mới)*
   - Record trả về thông tin tài khoản, hồ sơ cư dân và danh sách hợp đồng thuê (không trả `password`).
5. **`src/main/java/com/bql/feedback/service/AccountApprovalService.java`** *(thêm mới)*
   - **Phân quyền:** Kiểm tra `staffId` từ `StaffRepository` phải tồn tại, `active = true` và có `role == Role.CS_MANAGER`; mọi vai trò khác (`CS_STAFF`, `DEPT_STAFF`, `CUSTOMER`...) bị chặn bằng `ForbiddenException` (`403`). *(Khi nâng cấp Spring Security + JWT, chỉ cần thêm `@PreAuthorize("hasRole('CS_MANAGER')")` ở Controller và đọc `staffId` từ SecurityContext.)*
   - **`getPendingAccounts(Long staffId)`:** Query đúng 3 lượt theo lô (`findByRoleAndAccountStatusOrderByCreatedAtAsc` → `customerRepository.findAllById(customerIds)` → `rentalContractRepository.findByTenantCustomerIdInOrderByStartDateDesc(customerIds)`) rồi map theo `Map<Long, ...>` để tránh hoàn toàn N+1 query.
   - **`approve(Long accountId, Long staffId)` (`@Transactional`):**
     - Chỉ duyệt tài khoản `Role.CUSTOMER` đang `PENDING`.
     - Với `ResidentType.TENANT`: bắt buộc phải có hợp đồng `PENDING` và **chặn duyệt bằng `BusinessException`** nếu hợp đồng đã hết hạn (`endDate < LocalDate.now()`); nếu hợp lệ thì chuyển hợp đồng `PENDING` → `ACTIVE`.
     - Đặt `Customer` (`verifiedResident = true`, `active = true`) và `UserAccount` (`accountStatus = APPROVED`, `active = true`).
   - **`reject(Long accountId, Long staffId)` (`@Transactional`):**
     - Chỉ từ chối tài khoản `Role.CUSTOMER` đang `PENDING`.
     - Đặt `Customer` (`verifiedResident = false`, `active = false`), `UserAccount` (`accountStatus = REJECTED`, `active = false`), và chuyển các hợp đồng `PENDING` của `TENANT` sang `TERMINATED`.
6. **`src/main/java/com/bql/feedback/controller/AccountApprovalController.java`** *(thêm mới)*
   - Cung cấp 3 endpoint dưới `/api/accounts` (nhận Header `X-Staff-Id`):
     - `GET /api/accounts/pending`
     - `POST /api/accounts/{accountId}/approve`
     - `POST /api/accounts/{accountId}/reject`
7. **`src/test/java/com/bql/feedback/service/AccountApprovalServiceTests.java`** *(thêm mới)*
   - 7 unit test kiểm thử: phân quyền chặt chỉ `CS_MANAGER` active (`403` cho `CS_STAFF`/`DEPT_STAFF`/inactive/unknown), duyệt `OWNER`, duyệt `TENANT` kích hoạt hợp đồng hợp lệ, chặn duyệt `TENANT` khi hợp đồng hết hạn (không lưu DB), từ chối `TENANT` hủy hợp đồng & set inactive, chặn duyệt tài khoản không phải `PENDING`/tài khoản Staff/`404`, và xác minh `getPendingAccounts` dùng batch query không phát sinh N+1.

### Kết quả kiểm tra

- **Biên dịch:** Thành công toàn bộ **70 file** nguồn chính và **4 file** test (`BUILD SUCCESS`).
- **Kiểm thử unit test:** `.\mvnw.cmd test -o -Dtest=AccountApprovalServiceTests,CustomerServiceProfileTests,AuthServiceResidentRegistrationTests` → **`Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`** (`AccountApprovalServiceTests`: 7 passed, `CustomerServiceProfileTests`: 11 passed, `AuthServiceResidentRegistrationTests`: 4 passed, `BUILD SUCCESS`).

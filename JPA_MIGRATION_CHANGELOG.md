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

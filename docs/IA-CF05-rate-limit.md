# IA-CF05: Rate Limiting (Abuse Protection)

Người thực hiện: Thiện · Nhánh: `ia-cf05-cf06`

## Mục đích
Giới hạn số request theo địa chỉ IP vào các endpoint nhạy cảm, nhằm chống dò mật khẩu (brute-force), spam đăng ký và spam gửi phiếu phản hồi / upload.

## Hành vi
Dùng thuật toán Token Bucket (thư viện Bucket4j), tính theo từng IP:

| Nhóm | Endpoint (chỉ POST) | Giới hạn mặc định |
|---|---|---|
| auth | `/api/auth/login`, `/api/auth/register` | 10 request/phút |
| feedback | `/api/feedbacks`, `/api/feedbacks/{id}/attachments` | 20 request/phút |

Khi vượt giới hạn: trả **HTTP 429** kèm header `Retry-After`, và ghi log mức WARN có dạng:
`RATE_LIMIT_BLOCKED ip=... group=... method=... path=... retryAfter=...s`
(dùng làm nguồn dữ liệu cho IA-CF07 Security Monitoring).

Request GET không bị giới hạn. Cơ chế khóa theo username của `LoginAttemptService` (CF01) vẫn giữ nguyên; filter này chặn theo IP nên hai lớp bổ trợ nhau.

## File thay đổi
| File | Thay đổi |
|---|---|
| `pom.xml` | Thêm dependency `com.bucket4j:bucket4j-core:8.10.1` |
| `security/RateLimitFilter.java` | **Mới.** Filter giới hạn tần suất |
| `config/SecurityConfig.java` | Thêm 3 tham số `@Value` cho `filterChain` và đăng ký `RateLimitFilter` trước `JwtAuthenticationFilter` |
| `src/main/resources/application.properties` | Thêm 3 cấu hình `app.security.rate-limit.*` |
| `src/test/resources/application.properties` | Nâng limit lên 1000 để các test tích hợp cũ không bị 429 |
| `security/RateLimitFilterTests.java` | **Mới.** 3 unit test |

## Cấu hình
| Thuộc tính | Biến môi trường | Mặc định |
|---|---|---|
| `app.security.rate-limit.auth-per-minute` | `RATE_LIMIT_AUTH_PER_MIN` | 10 |
| `app.security.rate-limit.feedback-per-minute` | `RATE_LIMIT_FEEDBACK_PER_MIN` | 20 |
| `app.security.rate-limit.trust-forwarded-for` | `RATE_LIMIT_TRUST_XFF` | false |

Chỉ đặt `trust-forwarded-for=true` khi ứng dụng chạy phía sau reverse proxy (Nginx) của nhóm. Nếu bật khi chạy trực tiếp, client có thể giả header `X-Forwarded-For` để né giới hạn.

## Lưu ý cho người sửa `SecurityConfig.java`
Hàm `filterChain` giờ nhận thêm 3 tham số `@Value`, và cuối chuỗi `http` có thêm một lệnh `addFilterBefore(new RateLimitFilter(...), JwtAuthenticationFilter.class)`. Khi merge, vui lòng giữ lại hai phần này.

## Đã kiểm tra
- `./mvnw clean test -Dtest=RateLimitFilterTests`: 3 test qua.
- **Chưa kiểm tra**: chạy cả ứng dụng (cần SQL Server) và các test tích hợp cũ với filter mới.

## Còn lại
- IA-CF06 (WAF Nginx + ModSecurity) sẽ được thêm ở một commit/PR riêng.
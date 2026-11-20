// ============================================================
// CẤU HÌNH CHUNG
// ============================================================

// true  = chạy bằng API giả lập lưu trong localStorage (js/mock.js)
// false = gọi backend thật tại API_BASE_URL
export const USE_MOCK = true;

// Khi backend khác domain/port: 'http://localhost:3000' (backend phải bật CORS)
export const API_BASE_URL = '';

// ---- Vai trò (SRS mục 1.1) -----------------------------------
export const ROLES = {
  customer: 'Khách hàng',
  cs_staff: 'Nhân viên CSKH',
  dept_staff: 'Nhân viên bộ phận',
  manager: 'Quản lý CSKH',
  admin: 'Quản trị hệ thống',
};

// Trang chủ theo vai trò (SC-06)
export const HOME = {
  customer: '/customer/history',
  cs_staff: '/staff/queue',
  dept_staff: '/dept/tasks',
  manager: '/manager/dashboard',
  admin: '/admin',
};

// ---- Loại phản hồi (BR-02, SC-07) ----------------------------
export const TYPE_LABEL = {
  complaint: 'Khiếu nại',
  suggestion: 'Góp ý, đề xuất',
  praise: 'Khen ngợi',
  staff_feedback: 'Phản hồi về tiến độ / thái độ nhân viên',
};
// BR-09: góp ý/khen ngợi đi luồng đóng đơn giản (không điều tra, không duyệt)
export const SIMPLE_TYPES = ['suggestion', 'praise', 'staff_feedback'];

// Dịch vụ liên quan (bối cảnh chung cư)
export const SERVICES = [
  'Vệ sinh', 'Điện nước', 'Thang máy', 'Gửi xe',
  'Tiếng ồn', 'An ninh', 'Phí dịch vụ', 'Khác',
];

// ---- Vị trí xảy ra sự cố ----------------------------------------
// ⚠ Danh sách tòa là ví dụ — sửa theo chung cư thật (hoặc lấy từ backend qua /api/meta).
export const BUILDINGS = ['Tòa 1', 'Tòa 2', 'Tòa 3', 'Khu vực chung (sân, hầm xe, tiện ích)'];
// Tầng: số 1–60 hoặc hầm B1–B3. Phòng: chữ/số/dấu gạch, tối đa 10 ký tự (không bắt buộc).
export const FLOOR_RE = /^(B[1-3]|[1-9]\d?)$/i;
export const ROOM_RE = /^[A-Za-z0-9\-]{1,10}$/;

// ---- Vòng đời trạng thái (BR-07; các trạng thái trung gian là đề xuất) ----
export const STATUS_LABEL = {
  New: 'Mới',
  Assigned: 'Đã phân công',
  InProgress: 'Đang xử lý',
  PendingApproval: 'Chờ duyệt',
  Resolved: 'Đã giải quyết',
  Closed: 'Đã đóng',
  Reopened: 'Mở lại',
};

export const PRIORITY_LABEL = {
  Low: 'Thấp',
  Medium: 'Trung bình',
  High: 'Cao',
  Critical: 'Khẩn cấp',
};

// Chuyển trạng thái nhân viên được phép (UI chỉ gợi ý; backend mới là nơi quyết định)
export const TRANSITIONS = {
  staff_like: { // cs_staff + manager
    New: ['Resolved'],            // chỉ cho góp ý/khen ngợi (BR-09)
    Assigned: ['InProgress'],
    InProgress: ['Resolved'],
    Reopened: ['InProgress'],
    Resolved: ['Closed'],
  },
  dept_staff: {
    Assigned: ['InProgress'],
    Reopened: ['InProgress'],
  },
};

// ---- Quy tắc tệp đính kèm (SecRS IA-CF04) ---------------------
// ⚠ Tài liệu CHƯA nêu số cụ thể — đây là giá trị tạm, hỏi nhóm backend để chốt.
export const FILE_RULES = {
  maxFiles: 5,
  maxBytes: 5 * 1024 * 1024,
  exts: ['jpg', 'jpeg', 'png', 'pdf'],
};

// ---- Khóa đăng nhập (IA-CF01) — giá trị demo cho mock ----------
export const LOCK = { maxFails: 5, seconds: 60 };

export const MAX_REOPEN = 3;

// Lỗi chuẩn hoá từ API: 400, 401, 403, 404, 409, 429 (Security Design mục 4)
export class ApiError extends Error {
  constructor(status, message, { code = null, fields = null } = {}) {
    super(message || `Lỗi ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;     // ví dụ 'LOCKED'
    this.fields = fields; // { tenTruong: 'thông báo' } cho lỗi 400
  }
}

// Thông báo mặc định theo mã lỗi khi server không trả message
export function defaultMessage(status) {
  return {
    400: 'Dữ liệu chưa hợp lệ, vui lòng kiểm tra lại.',
    401: 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.',
    403: 'Bạn không có quyền thực hiện thao tác này.',
    404: 'Không tìm thấy dữ liệu (hoặc không thuộc phạm vi của bạn).',
    409: 'Dữ liệu vừa được người khác cập nhật. Hãy tải lại trang rồi thử lại.',
    429: 'Bạn thao tác quá nhanh. Vui lòng đợi một lát rồi thử lại.',
  }[status] || 'Có lỗi xảy ra, vui lòng thử lại.';
}

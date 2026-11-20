// Mọi lời gọi API đi qua đây. Màn hình không bao giờ gọi fetch trực tiếp.
import { USE_MOCK, API_BASE_URL } from './config.js';
import { ApiError, defaultMessage } from './errors.js';
import { getSession, clearSession } from './auth.js';
import { toast } from './ui.js';

/**
 * @param {string} path  ví dụ '/api/complaints/mine'
 * @param {{method?:string, body?:object, form?:FormData, auth?:boolean}} opts
 *   - body: gửi JSON; form: gửi multipart (upload file)
 *   - auth:false cho login/register (401 không có nghĩa là hết phiên)
 */
export async function api(path, { method = 'GET', body, form, auth = true } = {}) {
  const session = getSession();
  try {
    if (USE_MOCK) {
      const { handle } = await import('./mock.js');
      return await handle({ method, path, body, form, token: session?.token });
    }

    const headers = {};
    if (session?.token) headers.Authorization = `Bearer ${session.token}`;
    if (body) headers['Content-Type'] = 'application/json';

    let res;
    try {
      res = await fetch(API_BASE_URL + path, {
        method,
        headers,
        body: form ?? (body ? JSON.stringify(body) : undefined),
        // credentials: 'include',   // bật nếu backend dùng cookie
      });
    } catch {
      throw new ApiError(0, 'Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.');
    }
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      throw new ApiError(res.status, data.message || defaultMessage(res.status),
        { code: data.code, fields: data.fields });
    }
    return data;
  } catch (e) {
    if (e instanceof ApiError && e.status === 401 && auth) {
      clearSession();
      toast(defaultMessage(401), 'er');
      location.hash = '#/login';
    }
    throw e;
  }
}

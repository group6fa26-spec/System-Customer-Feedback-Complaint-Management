// Phiên đăng nhập phía client.
// ⚠ Tài liệu chưa chốt JWT hay session. Hiện lưu token trong sessionStorage (mất khi đóng tab).
// Nếu backend dùng cookie HttpOnly thì bỏ token ở đây, thêm `credentials: 'include'` trong api.js.
import { HOME } from './config.js';

const KEY = 'ccms_session';

export function getSession() {
  try { return JSON.parse(sessionStorage.getItem(KEY)); } catch { return null; }
}
export function setSession(session) {
  try { sessionStorage.setItem(KEY, JSON.stringify(session)); } catch { /* bỏ qua */ }
}
export function clearSession() {
  try { sessionStorage.removeItem(KEY); } catch { /* bỏ qua */ }
}
export const getUser = () => getSession()?.user || null;
export const homeFor = (role) => HOME[role] || '/';

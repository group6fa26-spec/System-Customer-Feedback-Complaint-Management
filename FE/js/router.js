// Hash router: #/duong-dan?tham-so=gia-tri
import { getUser, homeFor } from './auth.js';
import { ApiError } from './errors.js';
import { html, mount, toast } from './ui.js';

const routes = [];

// roles: null = công khai | 'guest' = chỉ khi CHƯA đăng nhập | [..] = các vai trò được vào
export function route(path, roles, page) {
  const keys = [];
  const re = new RegExp('^' + path.replace(/:([a-z]+)/gi, (_, k) => { keys.push(k); return '([^/]+)'; }) + '/?$');
  routes.push({ re, keys, roles, page });
}

function parseHash() {
  const raw = location.hash.slice(1) || '/';
  const [path, qs = ''] = raw.split('?');
  return { path, query: Object.fromEntries(new URLSearchParams(qs)) };
}

export const go = (path) => { location.hash = '#' + path; };

export async function render() {
  // Mỗi lần hiển thị dùng MỘT vùng chứa riêng. Nếu người dùng chuyển trang khi trang trước còn đang
  // tải, trang cũ chỉ ghi vào vùng chứa đã bị gỡ khỏi DOM → không đè lên trang mới (tránh race).
  const host = document.getElementById('app');
  const app = document.createElement('div');
  host.replaceChildren(app);
  const { path, query } = parseHash();
  const user = getUser();

  const hit = routes.map((r) => ({ r, m: r.re.exec(path) })).find((x) => x.m);
  if (!hit) {
    mount(app, html`<div class="card"><h2>404</h2><p>Trang không tồn tại. <a href="#/">Về trang chủ</a></p></div>`);
    return;
  }
  const { r, m } = hit;

  if (r.roles === 'guest' && user) return go(homeFor(user.role));
  if (Array.isArray(r.roles)) {
    if (!user) return go('/login?next=' + encodeURIComponent(path));
    // Chỉ ẩn/chặn ở UI cho gọn — backend vẫn kiểm tra quyền thật (IA-CF02)
    if (!r.roles.includes(user.role)) {
      mount(app, html`<div class="card"><h2>Không có quyền truy cập</h2>
        <p>Vai trò của bạn không được xem trang này. <a href="${'#' + homeFor(user.role)}">Về trang của tôi</a></p></div>`);
      return;
    }
  }

  const params = Object.fromEntries(r.keys.map((k, i) => [k, decodeURIComponent(m[i + 1])]));
  mount(app, html`<p class="mu" role="status">Đang tải…</p>`);
  try {
    await r.page({ app, params, query, user });
    if (app.isConnected) host.focus({ preventScroll: true });
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) return;   // api.js đã chuyển về /login
    const msg = e instanceof ApiError ? e.message : 'Không thể hiển thị trang này.';
    if (!(e instanceof ApiError)) console.error(e);
    mount(app, html`<div class="card"><h2>Có lỗi</h2><div class="msg er">${msg}</div>
      <p><a href="javascript:history.back()">← Quay lại</a></p></div>`);
    if (e instanceof ApiError && e.status === 429) toast(msg, 'er');
  }
}

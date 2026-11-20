// Tiện ích giao diện. Quan trọng nhất: html`` TỰ ĐỘNG escape mọi giá trị nhúng vào
// (chống XSS – IA-CF03). Chỉ chèn HTML thô khi bọc bằng raw() hoặc dùng html`` lồng nhau.
import { ApiError } from './errors.js';
import { STATUS_LABEL, PRIORITY_LABEL } from './config.js';

export const esc = (v) => String(v ?? '').replace(/[&<>"']/g,
  (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

export class Raw { constructor(s) { this.s = s; } }
export const raw = (s) => new Raw(String(s));

const part = (v) => v instanceof Raw ? v.s
  : Array.isArray(v) ? v.map(part).join('')
  : (v === null || v === undefined || v === false) ? '' : esc(v);

export function html(strings, ...vals) {
  let out = '';
  strings.forEach((s, i) => { out += s; if (i < vals.length) out += part(vals[i]); });
  return new Raw(out);
}
export const mount = (el, r) => { el.innerHTML = r.s; };

// <option> từ mảng [value, label]
export const options = (items, selected, empty) => raw(
  (empty !== undefined ? `<option value="">${esc(empty)}</option>` : '') +
  items.map(([v, l]) => `<option value="${esc(v)}"${String(v) === String(selected) ? ' selected' : ''}>${esc(l)}</option>`).join(''));

export const fmt = (iso) => iso
  ? new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : '–';

export const statusBadge = (s) => html`<span class="tag st-${s}">${STATUS_LABEL[s] || s}</span>`;
export const priorityBadge = (p) => p
  ? html`<span class="tag pr-${p}">${PRIORITY_LABEL[p] || p}</span>`
  : html`<span class="tag pr-none">Chưa phân loại</span>`;

// "Tòa 1 · Tầng 3 · Phòng 302 (cạnh thang máy)". Phiếu cũ chưa có vị trí → "–"
export function locationText(loc) {
  if (!loc || !loc.building) return '–';
  const parts = [loc.building];
  if (loc.floor) parts.push(/^B/i.test(loc.floor) ? `Hầm ${String(loc.floor).toUpperCase()}` : `Tầng ${loc.floor}`);
  if (loc.room) parts.push(`Phòng ${loc.room}`);
  return parts.join(' · ') + (loc.note ? ` (${loc.note})` : '');
}

export const sizeText = (b) => b < 1024 ? `${b} B` : b < 1048576 ? `${(b / 1024).toFixed(0)} KB` : `${(b / 1048576).toFixed(1)} MB`;

// ---- Toast ---------------------------------------------------
export function toast(message, type = 'ok', ms = 5000) {
  const box = document.getElementById('toasts');
  if (!box) return;
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.textContent = message;               // textContent, không dùng innerHTML
  box.append(el);
  setTimeout(() => el.remove(), ms);
}

// ---- Form ----------------------------------------------------
export function showFormError(form, e) {
  const known = e instanceof ApiError;
  if (!known) console.error(e);
  const text = known ? e.message : 'Có lỗi không mong muốn, vui lòng thử lại.';
  const box = form.querySelector('.form-msg');
  if (box) {
    box.replaceChildren();
    const d = document.createElement('div');
    d.className = 'msg er'; d.setAttribute('role', 'alert'); d.textContent = text;
    box.append(d);
  } else toast(text, 'er');
  for (const [name, m] of Object.entries(e?.fields || {})) {
    const el = form.elements[name];
    if (!el) continue;
    el.classList.add('is-invalid');
    const s = document.createElement('div');
    s.className = 'field-err'; s.textContent = m;
    el.insertAdjacentElement('afterend', s);
  }
}

// Gắn xử lý submit: khóa nút khi đang gửi (chống gửi đúp), xóa lỗi cũ, hiện lỗi mới
export function onSubmit(form, handler) {
  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const btn = form.querySelector('button[type=submit]');
    form.querySelector('.form-msg')?.replaceChildren();
    form.querySelectorAll('.is-invalid').forEach((x) => x.classList.remove('is-invalid'));
    form.querySelectorAll('.field-err').forEach((x) => x.remove());
    if (btn) btn.disabled = true;
    try { await handler(new FormData(form), form); }
    catch (e) { showFormError(form, e); }
    finally { if (btn) btn.disabled = false; }
  });
}

export const spinner = html`<p class="mu" role="status">Đang tải…</p>`;

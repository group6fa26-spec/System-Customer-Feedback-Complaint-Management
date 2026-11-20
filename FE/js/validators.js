// Kiểm tra phía client CHỈ để báo lỗi sớm cho người dùng.
// Server vẫn kiểm tra lại toàn bộ (IA-CF03, IA-CF04) — không bao giờ tin client.
import { FILE_RULES, BUILDINGS, FLOOR_RE, ROOM_RE } from './config.js';
import { sizeText } from './ui.js';

export const isEmail = (v) => /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(v);
export const isPhone = (v) => /^0\d{9}$/.test(v);               // giữ quy tắc của file mẫu
export const isStrongPassword = (v) => v.length >= 8 && /[A-Za-z]/.test(v) && /\d/.test(v);

// Trả về { tenTruong: 'thông báo' } (rỗng nếu hợp lệ)
export function checkLocation({ building, floor, room, note }) {
  const f = {};
  if (!BUILDINGS.includes(building)) f.building = 'Vui lòng chọn tòa / khu vực.';
  if (!FLOOR_RE.test(floor || '')) f.floor = 'Nhập số tầng (1–99) hoặc hầm B1–B3.';
  if (room && !ROOM_RE.test(room)) f.room = 'Số phòng chỉ gồm chữ, số, dấu “-” (tối đa 10 ký tự).';
  if (note && note.length > 100) f.location_note = 'Tối đa 100 ký tự.';
  return f;
}

// Trả về chuỗi lỗi, hoặc null nếu hợp lệ
export function checkFiles(files) {
  if (files.length > FILE_RULES.maxFiles) return `Tối đa ${FILE_RULES.maxFiles} tệp.`;
  for (const f of files) {
    const ext = f.name.split('.').pop().toLowerCase();
    if (!FILE_RULES.exts.includes(ext)) {
      return `“${f.name}”: loại tệp không được hỗ trợ (chỉ ${FILE_RULES.exts.join(', ')}).`;
    }
    if (f.size > FILE_RULES.maxBytes) {
      return `“${f.name}” (${sizeText(f.size)}) vượt giới hạn ${sizeText(FILE_RULES.maxBytes)}.`;
    }
  }
  return null;
}

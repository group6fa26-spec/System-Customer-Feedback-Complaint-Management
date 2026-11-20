// SC-13: Hàng đợi tiếp nhận của CS Staff, tách theo loại phiếu
import { api } from '../api.js';
import { TYPE_LABEL, STATUS_LABEL, BUILDINGS } from '../config.js';
import { html, mount, options, fmt, statusBadge, priorityBadge, locationText } from '../ui.js';

const STAFF_TABS = ['complaint', 'suggestion', 'praise'];   // phản hồi về nhân viên do Manager xử lý

export async function queue({ app, query }) {
  const type = STAFF_TABS.includes(query.type) ? query.type : 'complaint';
  const qs = new URLSearchParams({ type });
  for (const k of ['status', 'building', 'q', 'from', 'to']) if (query[k]) qs.set(k, query[k]);
  const list = await api('/api/complaints?' + qs);

  mount(app, html`
    <h2>Hàng đợi tiếp nhận</h2>
    <div class="tabs" role="tablist">
      ${STAFF_TABS.map((t) => html`<a role="tab" class="${t === type ? 'on' : ''}" href="${'#/staff/queue?type=' + t}">${TYPE_LABEL[t]}</a>`)}
    </div>
    <div class="card">
      <form id="flt" class="filters">
        <input name="q" placeholder="Tìm theo mã, tiêu đề, người gửi" value="${query.q || ''}" aria-label="Tìm kiếm">
        <select name="status" aria-label="Trạng thái">${options(Object.entries(STATUS_LABEL), query.status, 'Mọi trạng thái')}</select>
        <select name="building" aria-label="Tòa / khu vực">${options(BUILDINGS.map((b) => [b, b]), query.building, 'Mọi tòa')}</select>
        <input type="date" name="from" value="${query.from || ''}" aria-label="Từ ngày">
        <input type="date" name="to" value="${query.to || ''}" aria-label="Đến ngày">
        <button type="submit" class="sec">Lọc</button>
      </form>
      ${list.length ? html`
        <div class="tw"><table>
          <thead><tr><th>Mã</th><th>Tiêu đề</th><th>Người gửi</th><th>Ưu tiên</th><th>Trạng thái</th><th>Cập nhật</th></tr></thead>
          <tbody>${list.map((c) => html`<tr>
            <td><a href="${'#/staff/complaints/' + c.id}"><b>${c.tracking_code}</b></a></td>
            <td>${c.title}<div class="mu"> ${locationText(c.location)} · ${c.service}</div></td>
            <td>${c.customer_name}</td>
            <td>${priorityBadge(c.priority)}${c.escalated ? html` <span class="tag esc">Chuyển cấp</span>` : ''}</td>
            <td>${statusBadge(c.status)}</td>
            <td>${fmt(c.updated_at)}</td></tr>`)}</tbody>
        </table></div>` : html`<p class="mu">Không có phiếu nào.</p>`}
    </div>`);

  app.querySelector('#flt').addEventListener('submit', (ev) => {
    ev.preventDefault();
    const p = new URLSearchParams({ type });
    for (const [k, v] of new FormData(ev.target)) if (v) p.set(k, v);
    location.hash = '#/staff/queue?' + p;
  });
}

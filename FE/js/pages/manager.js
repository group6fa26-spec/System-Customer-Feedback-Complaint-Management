// SC-17 Dashboard, SC-18 Danh sách chuyển cấp / chờ duyệt (Manager)
import { api } from '../api.js';
import { TYPE_LABEL, STATUS_LABEL, PRIORITY_LABEL } from '../config.js';
import { html, mount, fmt, statusBadge, priorityBadge, locationText } from '../ui.js';

// Thanh ngang thuần CSS: nhẹ, không cần thư viện biểu đồ
const bars = (title, labels, data) => {
  const max = Math.max(1, ...Object.values(data));
  return html`<div class="card"><h3>${title}</h3>${Object.entries(data).map(([k, v]) => html`
    <div class="barrow"><span class="lbl">${labels[k] || k}</span>
      <span class="track"><span class="fill" style="${'width:' + (v / max) * 100 + '%'}"></span></span><b>${v}</b></div>`)}</div>`;
};

export async function dashboard({ app }) {
  const d = await api('/api/dashboard');
  mount(app, html`
    <h2>Dashboard báo cáo</h2>
    <div class="stats">
      <div class="stat"><b>${d.total}</b>Tổng phiếu</div>
      <div class="stat"><b>${d.pending_approval}</b>Chờ duyệt</div>
      <div class="stat"><b>${d.escalated}</b>Đã chuyển cấp</div>
      <div class="stat"><b>${d.resolved_rate}%</b>Tỷ lệ giải quyết</div>
      <div class="stat"><b>${d.avg_rating ?? '–'} ★</b>Hài lòng TB (${d.rating_count} đánh giá)</div>
    </div>
    <div class="grid3">
      ${bars('Theo trạng thái', STATUS_LABEL, d.by_status)}
      ${bars('Theo mức ưu tiên', PRIORITY_LABEL, d.by_priority)}
      ${bars('Theo loại', TYPE_LABEL, d.by_type)}
    </div>
    <p><a class="btn" href="#/manager/escalated">Xem phiếu chuyển cấp / chờ duyệt →</a></p>`);
}

export async function escalated({ app, query }) {
  const view = query.view === 'pending' ? 'pending' : query.view === 'all' ? 'all' : 'escalated';
  const list = await api('/api/complaints' + (view === 'all' ? '' : '?view=' + view));
  const tabs = [['escalated', 'Chuyển cấp'], ['pending', 'Chờ duyệt (Critical)'], ['all', 'Tất cả']];
  mount(app, html`
    <h2>Phiếu cần xử lý</h2>
    <div class="tabs">${tabs.map(([v, l]) => html`<a class="${v === view ? 'on' : ''}" href="${'#/manager/escalated?view=' + v}">${l}</a>`)}</div>
    <div class="card">
      ${list.length ? html`<div class="tw"><table>
        <thead><tr><th>Mã</th><th>Tiêu đề</th><th>Người gửi</th><th>Ưu tiên</th><th>Trạng thái</th><th>Cập nhật</th></tr></thead>
        <tbody>${list.map((c) => html`<tr>
          <td><a href="${'#/manager/complaints/' + c.id}"><b>${c.tracking_code}</b></a></td>
          <td>${c.title}<div class="mu"> ${locationText(c.location)} · ${TYPE_LABEL[c.type]}</div></td>
          <td>${c.customer_name}</td><td>${priorityBadge(c.priority)}</td>
          <td>${statusBadge(c.status)}</td><td>${fmt(c.updated_at)}</td></tr>`)}</tbody></table></div>`
        : html`<p class="mu">Không có phiếu nào.</p>`}
    </div>`);
}

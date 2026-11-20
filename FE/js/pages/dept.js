// SC-15 Danh sách việc được giao, SC-16 Cập nhật kết quả (Department Staff)
import { api } from '../api.js';
import { ApiError } from '../errors.js';
import { html, mount, onSubmit, toast, fmt, statusBadge, priorityBadge, sizeText, locationText } from '../ui.js';

const ASSIGN_LABEL = { Open: 'Chờ xử lý', Done: 'Đã báo kết quả' };

export async function myTasks({ app }) {
  const list = await api('/api/assignments/mine');
  mount(app, html`
    <h2>Việc được giao</h2>
    <div class="card">
      ${list.length ? html`<div class="tw"><table>
        <thead><tr><th>Mã</th><th>Nội dung</th><th>Ưu tiên</th><th>Hạn xử lý</th><th>Tình trạng</th></tr></thead>
        <tbody>${list.map((a) => html`<tr>
          <td><a href="${'#/dept/tasks/' + a.id}"><b>${a.complaint.tracking_code}</b></a></td>
          <td>${a.complaint.title}<div class="mu"> ${locationText(a.complaint.location)} · ${a.complaint.service}</div></td>
          <td>${priorityBadge(a.complaint.priority)}</td>
          <td>${fmt(a.due_at)}</td>
          <td>${ASSIGN_LABEL[a.status] || a.status}</td></tr>`)}</tbody></table></div>`
        : html`<p class="mu">Chưa có việc nào được giao cho bộ phận của bạn.</p>`}
    </div>`);
}

export async function taskDetail({ app, params }) {
  const list = await api('/api/assignments/mine');
  const a = list.find((x) => String(x.id) === params.id);
  if (!a) {
    mount(app, html`<div class="card"><h2>Không tìm thấy việc</h2><a href="#/dept/tasks">← Quay lại</a></div>`);
    return;
  }
  const c = a.complaint;
  const canEdit = a.status === 'Open';

  mount(app, html`
    <div class="bar"><h2>${c.title}</h2><a class="btn sec" href="#/dept/tasks">← Danh sách</a></div>
    <div class="card">
      <div class="meta"><span><b>Mã:</b> ${c.tracking_code}</span><span><b>Dịch vụ:</b> ${c.service}</span>
        <span><b>Ưu tiên:</b> ${priorityBadge(c.priority)}</span><span><b>Trạng thái phiếu:</b> ${statusBadge(c.status)}</span>
        <span><b>Hạn:</b> ${fmt(a.due_at)}</span></div>
      <div class="meta"><span><b> Vị trí sự cố:</b> ${locationText(c.location)}</span></div>
      <h3>Nội dung khiếu nại</h3><p class="pre">${c.description}</p>
      ${c.attachments.length ? html`<ul class="filelist">${c.attachments.map((f) => html`<li>📎 ${f.original_name} (${sizeText(f.size_bytes)})</li>`)}</ul>` : ''}
      ${c.rejected_reason ? html`<div class="msg er">Quản lý đã từ chối phương án trước: ${c.rejected_reason}. Vui lòng sửa lại.</div>` : ''}
    </div>

    <div class="card">
      <h3>Kết quả điều tra &amp; giải pháp (UC-10)</h3>
      ${canEdit ? '' : html`<div class="msg ok">Đã gửi kết quả.</div>`}
      <form id="f" novalidate>
        <label for="result_note">Kết quả điều tra</label>
        <textarea id="result_note" name="result_note" rows="4" ${canEdit ? '' : 'disabled'}>${a.result_note}</textarea>
        <label for="solution">Đề xuất giải pháp</label>
        <textarea id="solution" name="solution" rows="3" ${canEdit ? '' : 'disabled'}>${a.solution}</textarea>
        ${canEdit ? html`<button type="submit">Gửi kết quả</button>` : ''}
        <div class="form-msg"></div>
      </form>
      ${c.priority === 'Critical' ? html`<p class="mu">Phiếu khẩn cấp: kết quả sẽ được chuyển cho Quản lý duyệt trước khi gửi khách.</p>` : ''}
    </div>`);

  if (canEdit) onSubmit(app.querySelector('#f'), async (fd) => {
    const fields = {};
    if (String(fd.get('result_note')).trim().length < 10) fields.result_note = 'Tối thiểu 10 ký tự.';
    if (String(fd.get('solution')).trim().length < 5) fields.solution = 'Vui lòng đề xuất giải pháp.';
    if (Object.keys(fields).length) throw new ApiError(400, 'Vui lòng kiểm tra các trường được đánh dấu.', { fields });
    await api(`/api/assignments/${a.id}/result`, { method: 'PATCH', body: { result_note: fd.get('result_note'), solution: fd.get('solution') } });
    toast('Đã gửi kết quả.');
    await taskDetail({ app, params });
  });
}

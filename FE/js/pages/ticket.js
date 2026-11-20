// SC-14 (CS Staff) và SC-18/19 (Manager): chi tiết phiếu + các hành động theo vai trò.
// Hiển thị/ẩn nút ở đây chỉ để giao diện gọn — backend mới là nơi kiểm tra quyền thật (IA-CF02).
import { api } from '../api.js';
import { ApiError } from '../errors.js';
import { TYPE_LABEL, SERVICES, PRIORITY_LABEL, STATUS_LABEL, SIMPLE_TYPES, TRANSITIONS } from '../config.js';
import { html, mount, options, onSubmit, toast, fmt, statusBadge, priorityBadge, sizeText, locationText } from '../ui.js';

export async function ticketDetail({ app, params, user }) {
  const meta = await api('/api/meta');
  const load = async () => draw(await api('/api/complaints/' + encodeURIComponent(params.id)));
  const back = user.role === 'manager' ? '#/manager/escalated' : '#/staff/queue';

  // Chạy một hành động; 409 → báo "tải lại" thay vì ghi đè (NFR toàn vẹn dữ liệu)
  const act = (form, fn, okMsg) => onSubmit(form, async (fd) => {
    try { await fn(fd); }
    catch (e) {
      if (e instanceof ApiError && e.status === 409) { toast(e.message, 'er', 8000); await load(); return; }
      throw e;
    }
    toast(okMsg); await load();
  });

  function draw(c) {
    const closed = c.status === 'Closed';
    const simple = SIMPLE_TYPES.includes(c.type);
    const nextStatuses = (TRANSITIONS.staff_like[c.status] || [])
      .filter((s) => !(c.status === 'New' && !simple));            // khiếu nại phải phân công trước
    const critNeedsApproval = c.priority === 'Critical' && c.type === 'complaint' && c.approval?.decision !== 'approved';

    mount(app, html`
      <div class="bar"><h2>${c.title}</h2><a class="btn sec" href="${back}">← Danh sách</a></div>

      <div class="card">
        <div class="meta">
          <span><b>Mã:</b> ${c.tracking_code}</span>
          <span><b>Loại:</b> ${TYPE_LABEL[c.type]}</span>
          <span><b>Dịch vụ:</b> ${c.service}</span>
          <span><b>Trạng thái:</b> ${statusBadge(c.status)}</span>
          <span><b>Ưu tiên:</b> ${priorityBadge(c.priority)}</span>
          ${c.escalated ? html`<span class="tag esc">Đã chuyển cấp</span>` : ''}
        </div>
        <div class="meta"><span><b> Vị trí sự cố:</b> ${locationText(c.location)}</span></div>
        <div class="meta"><span><b>Người gửi:</b> ${c.customer.name}</span><span><b>SĐT:</b> ${c.customer.phone}</span>
          <span><b>Gửi lúc:</b> ${fmt(c.created_at)}</span></div>
        <h3>Nội dung</h3>
        <p class="pre">${c.description}</p>
        ${c.attachments.length ? html`<ul class="filelist">${c.attachments.map((a) => html`<li>📎 ${a.original_name} (${sizeText(a.size_bytes)}) · quét: ${a.scan_status}</li>`)}</ul>` : ''}
      </div>

      ${c.approval && c.approval.decision !== 'pending' ? html`<div class="msg ${c.approval.decision === 'approved' ? 'ok' : 'er'}">
        Phê duyệt: <b>${c.approval.decision === 'approved' ? 'Đã duyệt' : 'Đã từ chối'}</b>${c.approval.reason ? ' — ' + c.approval.reason : ''}</div>` : ''}

      ${user.role === 'manager' && c.status === 'PendingApproval' ? html`
        <div class="card hl">
          <h3>Phê duyệt phương án (BR-11)</h3>
          ${c.assignments.filter((a) => a.status === 'Done').map((a) => html`
            <div class="reply"><div class="mu">${a.department_name} đề xuất:</div>
              <p class="pre">${a.result_note}</p><p><b>Giải pháp:</b> ${a.solution}</p></div>`)}
          <form id="approval" novalidate>
            <label for="reason">Lý do / ghi chú (bắt buộc khi từ chối)</label>
            <textarea id="reason" name="reason" rows="2"></textarea>
            <div class="actions">
              <button type="submit" name="decision" value="approve">Duyệt</button>
              <button type="submit" name="decision" value="reject" class="danger">Từ chối</button>
            </div>
            <div class="form-msg"></div>
          </form>
        </div>` : ''}

      ${!closed ? html`
      <div class="card">
        <h3>Phân loại &amp; mức ưu tiên (UC-07)</h3>
        <form id="classify" class="inline" novalidate>
          <input type="hidden" name="expected_updated_at" value="${c.updated_at}">
          <div><label for="type">Loại</label><select id="type" name="type">${options(Object.entries(TYPE_LABEL), c.type)}</select></div>
          <div><label for="service">Dịch vụ</label><select id="service" name="service">${options(SERVICES.map((s) => [s, s]), c.service)}</select></div>
          <div><label for="priority">Ưu tiên</label><select id="priority" name="priority">${options(Object.entries(PRIORITY_LABEL), c.priority)}</select></div>
          <button type="submit">Lưu</button>
          <div class="form-msg"></div>
        </form>
      </div>` : ''}

      ${!simple ? html`
      <div class="card">
        <h3>Phân công bộ phận (UC-08)</h3>
        ${c.assignments.length ? html`<div class="tw"><table><thead><tr><th>Bộ phận</th><th>Hạn xử lý</th><th>Trạng thái</th><th>Kết quả</th></tr></thead>
          <tbody>${c.assignments.map((a) => html`<tr><td>${a.department_name}</td><td>${fmt(a.due_at)}</td><td>${a.status}</td>
            <td>${a.result_note || '–'}${a.solution ? html`<div class="mu">Giải pháp: ${a.solution}</div>` : ''}</td></tr>`)}</tbody></table></div>` : html`<p class="mu">Chưa phân công.</p>`}
        ${!['Resolved', 'Closed'].includes(c.status) ? html`
        <form id="assign" class="inline" novalidate>
          <div><label for="department_id">Bộ phận</label>
            <select id="department_id" name="department_id">${options(meta.departments.map((d) => [d.id, d.name]), '', '— Chọn —')}</select></div>
          <div><label for="due_at">Hạn xử lý (SLA)</label><input id="due_at" name="due_at" type="datetime-local"></div>
          <button type="submit">Giao việc</button>
          <div class="form-msg"></div>
        </form>` : ''}
      </div>` : ''}

      <div class="card">
        <h3>Trao đổi &amp; phản hồi (UC-09)</h3>
        ${c.responses.length ? c.responses.map((r) => html`
          <div class="reply ${r.is_internal ? 'internal' : ''}"><div class="mu">${r.author} · ${fmt(r.created_at)}${r.is_internal ? ' · nội bộ' : ''}</div>
            <p class="pre">${r.content}</p>${r.solution ? html`<p><b>Giải pháp:</b> ${r.solution}</p>` : ''}</div>`) : html`<p class="mu">Chưa có phản hồi.</p>`}
        ${!closed ? html`
        ${critNeedsApproval ? html`<div class="msg warn">Phiếu khẩn cấp: cần Quản lý duyệt phương án trước khi gửi kết quả cho khách (server sẽ từ chối nếu chưa duyệt).</div>` : ''}
        <form id="respond" novalidate>
          <label for="content">Nội dung trả lời</label>
          <textarea id="content" name="content" rows="3"></textarea>
          <label for="solution">Giải pháp</label>
          <input id="solution" name="solution">
          <label class="check"><input type="checkbox" name="is_internal" value="1"> Ghi chú nội bộ (khách không thấy)</label>
          <button type="submit">Gửi</button>
          <div class="form-msg"></div>
        </form>` : ''}
      </div>

      ${nextStatuses.length ? html`
      <div class="card">
        <h3>Đổi trạng thái (UC-11)</h3>
        <form id="status" class="inline" novalidate>
          <input type="hidden" name="expected_updated_at" value="${c.updated_at}">
          <div><label for="to_status">Chuyển sang</label>
            <select id="to_status" name="to_status">${options(nextStatuses.map((s) => [s, STATUS_LABEL[s]]))}</select></div>
          <div><label for="note">Ghi chú</label><input id="note" name="note"></div>
          <button type="submit">Cập nhật</button>
          <div class="form-msg"></div>
        </form>
        <p class="mu">Phiếu “Đã giải quyết” bắt buộc có giải pháp (BR-08).</p>
      </div>` : ''}

      ${user.role === 'cs_staff' && !c.escalated && !closed ? html`
      <div class="card">
        <h3>Chuyển cấp lên Quản lý (UC-12)</h3>
        <form id="escalate" novalidate>
          <label for="ereason">Lý do</label><input id="ereason" name="reason">
          <button type="submit" class="danger">Chuyển cấp</button>
          <div class="form-msg"></div>
        </form>
      </div>` : ''}

      <div class="card">
        <h3>Lịch sử thao tác</h3>
        <ol class="timeline">${[...c.history].reverse().map((h) => html`<li><b>${h.action}</b>
          ${h.from && h.to && h.from !== h.to ? html` ${STATUS_LABEL[h.from] || h.from} → ${STATUS_LABEL[h.to] || h.to}` : ''}
          <div class="mu">${h.by} · ${fmt(h.at)}${h.note ? ' · ' + h.note : ''}</div></li>`)}</ol>
      </div>
    `);

    const $ = (s) => app.querySelector(s);
    const id = c.id;
    if ($('#classify')) act($('#classify'), (fd) => api(`/api/complaints/${id}/classification`, { method: 'PATCH', body: Object.fromEntries(fd) }), 'Đã lưu phân loại.');
    if ($('#assign')) act($('#assign'), (fd) => {
      const due = fd.get('due_at');
      return api(`/api/complaints/${id}/assignments`, { method: 'POST',
        body: { department_id: fd.get('department_id'), due_at: due ? new Date(due).toISOString() : '' } });
    }, 'Đã giao việc.');
    if ($('#respond')) act($('#respond'), (fd) => api(`/api/complaints/${id}/responses`, { method: 'POST',
      body: { content: fd.get('content'), solution: fd.get('solution'), is_internal: fd.get('is_internal') === '1' } }), 'Đã gửi.');
    if ($('#status')) act($('#status'), (fd) => api(`/api/complaints/${id}/status`, { method: 'PATCH', body: Object.fromEntries(fd) }), 'Đã đổi trạng thái.');
    if ($('#escalate')) act($('#escalate'), (fd) => api(`/api/complaints/${id}/escalation`, { method: 'POST', body: { reason: fd.get('reason') } }), 'Đã chuyển cấp.');

    const ap = $('#approval');
    if (ap) {
      // Nút nào được bấm quyết định approve/reject
      let decision = 'approve';
      ap.querySelectorAll('button[name=decision]').forEach((b) => b.addEventListener('click', () => { decision = b.value; }));
      act(ap, (fd) => {
        const reason = String(fd.get('reason')).trim();
        if (decision === 'reject' && !reason) throw new ApiError(400, 'Từ chối bắt buộc ghi lý do.', { fields: { reason: 'Bắt buộc khi từ chối.' } });
        return api(`/api/complaints/${id}/approval`, { method: 'POST', body: { decision, reason } });
      }, 'Đã ghi nhận quyết định.');
    }
  }

  await load();
}

// Luồng Customer: SC-07 → SC-11
import { api } from '../api.js';
import { go } from '../router.js';
import { ApiError } from '../errors.js';
import { TYPE_LABEL, SERVICES, FILE_RULES, STATUS_LABEL, MAX_REOPEN, BUILDINGS } from '../config.js';
import { html, mount, options, onSubmit, toast, fmt, statusBadge, priorityBadge, sizeText, locationText } from '../ui.js';
import { checkFiles, checkLocation } from '../validators.js';

// ---------- SC-07: Tạo phiếu ----------
export function newTicket({ app }) {
  mount(app, html`
    <div class="card wide">
      <h2>Gửi phản hồi</h2>
      <p class="mu">Sau khi gửi, hệ thống cấp ngay một mã tra cứu để bạn theo dõi tiến độ.</p>
      <form id="f" novalidate>
        <div class="row">
          <div><label for="type">Loại phản hồi</label>
            <select id="type" name="type">${options(Object.entries(TYPE_LABEL))}</select></div>
          <div><label for="service">Dịch vụ liên quan</label>
            <select id="service" name="service">${options(SERVICES.map((s) => [s, s]))}</select></div>
        </div>
        <fieldset class="loc">
          <legend>Vị trí xảy ra sự cố</legend>
          <div class="row">
            <div><label for="building">Tòa / khu vực</label>
              <select id="building" name="building">${options(BUILDINGS.map((b) => [b, b]), '', '— Chọn —')}</select></div>
            <div><label for="floor">Tầng</label>
              <input id="floor" name="floor" inputmode="text" placeholder="Ví dụ: 3 hoặc B1" maxlength="3" autocomplete="off"></div>
          </div>
          <div class="row">
            <div><label for="room">Số phòng / căn hộ (nếu có)</label>
              <input id="room" name="room" placeholder="Ví dụ: 302" maxlength="10" autocomplete="off"></div>
            <div><label for="location_note">Mô tả thêm vị trí (nếu có)</label>
              <input id="location_note" name="location_note" placeholder="Ví dụ: hành lang, cạnh thang máy" maxlength="100" autocomplete="off"></div>
          </div>
        </fieldset>
        <label for="title">Tiêu đề</label>
        <input id="title" name="title" maxlength="150" autocomplete="off">
        <label for="description">Mô tả chi tiết</label>
        <textarea id="description" name="description" rows="5" maxlength="2000"></textarea>
        <label for="files">Ảnh / bằng chứng (không bắt buộc)</label>
        <input id="files" name="files" type="file" multiple accept=".${FILE_RULES.exts.join(',.')}">
        <div class="mu">Tối đa ${FILE_RULES.maxFiles} tệp, mỗi tệp ≤ ${sizeText(FILE_RULES.maxBytes)}; định dạng: ${FILE_RULES.exts.join(', ')}.</div>
        <ul id="filelist" class="filelist"></ul>
        <div id="fileerr"></div>
        <button type="submit" class="block">Gửi phản hồi</button>
        <div class="form-msg"></div>
      </form>
    </div>`);

  const form = app.querySelector('#f');
  const input = form.elements.files;
  input.addEventListener('change', () => {
    const files = [...input.files];
    const err = checkFiles(files);
    app.querySelector('#filelist').replaceChildren(...files.map((f) => {
      const li = document.createElement('li'); li.textContent = `${f.name} (${sizeText(f.size)})`; return li;
    }));
    const box = app.querySelector('#fileerr'); box.replaceChildren();
    if (err) { const d = document.createElement('div'); d.className = 'msg er'; d.textContent = err; box.append(d); }
  });

  onSubmit(form, async (fd) => {
    const title = String(fd.get('title')).trim(), description = String(fd.get('description')).trim();
    const files = fd.getAll('files').filter((f) => f.size > 0 && f.name);
    const fields = checkLocation({
      building: fd.get('building'), floor: String(fd.get('floor')).trim(),
      room: String(fd.get('room')).trim(), note: String(fd.get('location_note')).trim(),
    });
    if (title.length < 5) fields.title = 'Tiêu đề tối thiểu 5 ký tự.';
    if (description.length < 10) fields.description = 'Mô tả tối thiểu 10 ký tự.';
    const fe = checkFiles(files); if (fe) fields.files = fe;
    if (Object.keys(fields).length) throw new ApiError(400, 'Vui lòng kiểm tra các trường được đánh dấu.', { fields });

    const res = await api('/api/complaints', { method: 'POST', form: fd });  // multipart
    showSuccess(app, res.tracking_code);
  });
}

function showSuccess(app, code) {
  mount(app, html`
    <div class="card narrow center">
      <h2> Đã gửi phản hồi</h2>
      <p>Mã tra cứu của bạn:</p>
      <div class="code" id="code">${code}</div>
      <p class="mu">Hãy lưu lại mã này để theo dõi tiến độ.</p>
      <div class="actions">
        <button type="button" id="copy">Sao chép mã</button>
        <a class="btn sec" href="${'#/customer/complaints/' + code}">Xem trạng thái</a>
        <a class="btn sec" href="#/customer/new">Gửi phản hồi khác</a>
      </div>
    </div>`);
  app.querySelector('#copy').onclick = async () => {
    try { await navigator.clipboard.writeText(code); toast('Đã sao chép mã.'); }
    catch { toast('Không sao chép được, hãy chọn và copy thủ công.', 'er'); }
  };
}

// ---------- SC-08: Lịch sử phiếu ----------
export async function history({ app }) {
  const list = await api('/api/complaints/mine');
  mount(app, html`
    <div class="bar"><h2>Phiếu của tôi</h2><a class="btn" href="#/customer/new">+ Gửi phản hồi</a></div>
    <div class="card">
      ${list.length ? html`
        <div class="tw"><table>
          <thead><tr><th>Mã</th><th>Tiêu đề</th><th>Loại</th><th>Trạng thái</th><th>Ngày gửi</th></tr></thead>
          <tbody>${list.map((c) => html`<tr>
            <td><a href="${'#/customer/complaints/' + c.tracking_code}"><b>${c.tracking_code}</b></a></td>
            <td>${c.title}<div class="mu"> ${locationText(c.location)}</div></td>
            <td>${TYPE_LABEL[c.type]}</td>
            <td>${statusBadge(c.status)}</td>
            <td>${fmt(c.created_at)}</td></tr>`)}</tbody>
        </table></div>`
      : html`<p class="mu">Bạn chưa gửi phản hồi nào.</p>`}
    </div>`);
}

// ---------- SC-09: Tra cứu theo mã ----------
export function track({ app }) {
  mount(app, html`
    <div class="card narrow">
      <h2>Tra cứu phiếu theo mã</h2>
      <form id="f" novalidate>
        <label for="code">Mã phiếu</label>
        <input id="code" name="code" placeholder="FB-XXXXXX" autocomplete="off">
        <button type="submit" class="block">Tra cứu</button>
        <div class="form-msg"></div>
      </form>
      <p class="mu">Bạn chỉ xem được phiếu do chính mình gửi (BR-13).</p> 
    </div>`);
  onSubmit(app.querySelector('#f'), async (fd) => {
    const code = String(fd.get('code')).trim().toUpperCase();
    if (!/^FB-[A-Z0-9]{6}$/.test(code)) throw new ApiError(400, 'Mã phiếu có dạng FB-XXXXXX.', { fields: { code: 'Mã không đúng định dạng.' } });
    go('/customer/complaints/' + code);
  });
}

// ---------- SC-10 + SC-11: Chi tiết, đánh giá, mở lại ----------
export async function customerDetail({ app, params }) {
  let c;
  try { c = await api('/api/complaints/' + encodeURIComponent(params.code)); }
  catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      mount(app, html`<div class="card narrow"><h2>Không tìm thấy phiếu</h2>
        <p>Mã không tồn tại hoặc không thuộc tài khoản của bạn.</p><a href="#/customer/track">← Tra cứu lại</a></div>`);
      return;
    }
    throw e;
  }
  draw(app, c);
}

function draw(app, c) {
  mount(app, html`
    <div class="bar"><h2>${c.title}</h2><a class="btn sec" href="#/customer/history">← Danh sách</a></div>
    <div class="card">
      <div class="meta">
        <span><b>Mã:</b> ${c.tracking_code}</span>
        <span><b>Loại:</b> ${TYPE_LABEL[c.type]}</span>
        <span><b>Dịch vụ:</b> ${c.service}</span>
        <span><b>Trạng thái:</b> ${statusBadge(c.status)}</span>
        <span><b>Ưu tiên:</b> ${priorityBadge(c.priority)}</span>
      </div>
      <div class="meta"><span><b> Vị trí:</b> ${locationText(c.location)}</span></div>
      <h3>Nội dung bạn đã gửi</h3>
      <p class="pre">${c.description}</p>
      ${c.attachments.length ? html`<ul class="filelist">${c.attachments.map((a) => html`<li>📎 ${a.original_name} (${sizeText(a.size_bytes)})</li>`)}</ul>` : ''}
    </div>

    <div class="card">
      <h3>Tiến trình</h3>
      <ol class="timeline">${c.history.map((h) => html`<li><b>${STATUS_LABEL[h.to] || h.to}</b><span class="mu"> · ${fmt(h.at)}</span></li>`)}</ol>
    </div>

    <div class="card">
      <h3>Phản hồi từ ban quản lý</h3>
      ${c.responses.length ? c.responses.map((r) => html`
        <div class="reply"><div class="mu">${r.author} · ${fmt(r.created_at)}</div>
          <p class="pre">${r.content}</p>
          ${r.solution ? html`<p><b>Giải pháp:</b> ${r.solution}</p>` : ''}</div>`)
        : html`<p class="mu">Chưa có phản hồi.</p>`}
    </div>

    ${c.rating ? html`<div class="card"><h3>Đánh giá của bạn</h3>
        <div class="stars-ro" aria-label="${c.rating.stars} sao">${'★'.repeat(c.rating.stars)}${'☆'.repeat(5 - c.rating.stars)}</div>
        ${c.rating.comment ? html`<p class="pre">${c.rating.comment}</p>` : ''}</div>` : ''}

    ${c.can_rate ? html`
      <div class="card">
        <h3>Bạn có hài lòng với kết quả?</h3>
        <form id="rate" novalidate>
          <div class="stars" id="stars" role="radiogroup" aria-label="Số sao"></div>
          <input type="hidden" name="stars" value="0">
          <label for="comment">Nhận xét (không bắt buộc)</label>
          <textarea id="comment" name="comment" rows="3" maxlength="500"></textarea>
          <button type="submit">Gửi đánh giá</button>
          <div class="form-msg"></div>
        </form>
        ${c.can_reopen ? html`
          <hr>
          <p class="mu">Chưa hài lòng? Bạn có thể mở lại phiếu (tối đa ${MAX_REOPEN} lần).</p>
          <form id="reopen" novalidate>
            <label for="reason">Lý do mở lại</label>
            <textarea id="reason" name="reason" rows="2"></textarea>
            <button type="submit" class="sec">Mở lại phiếu</button>
            <div class="form-msg"></div>
          </form>` : ''}
      </div>` : ''}
  `);

  // Chọn sao
  const starsEl = app.querySelector('#stars');
  if (starsEl) {
    const hidden = app.querySelector('input[name=stars]');
    const paint = (n) => starsEl.querySelectorAll('button').forEach((b, i) => b.classList.toggle('f', i < n));
    for (let i = 1; i <= 5; i++) {
      const b = document.createElement('button');
      b.type = 'button'; b.textContent = '★'; b.setAttribute('role', 'radio'); b.setAttribute('aria-label', `${i} sao`);
      b.onclick = () => { hidden.value = i; paint(i); };
      starsEl.append(b);
    }
    onSubmit(app.querySelector('#rate'), async (fd) => {
      const stars = Number(fd.get('stars'));
      if (!stars) throw new ApiError(400, 'Vui lòng chọn số sao.');
      const res = await api(`/api/complaints/${c.id}/rating`, { method: 'POST', body: { stars, comment: fd.get('comment') } });
      toast('Cảm ơn bạn đã đánh giá!');
      draw(app, res);
    });
  }
  const reopen = app.querySelector('#reopen');
  if (reopen) onSubmit(reopen, async (fd) => {
    const reason = String(fd.get('reason')).trim();
    if (reason.length < 5) throw new ApiError(400, 'Vui lòng nêu lý do.', { fields: { reason: 'Tối thiểu 5 ký tự.' } });
    const res = await api(`/api/complaints/${c.id}/reopen`, { method: 'POST', body: { reason } });
    toast('Phiếu đã được mở lại.');
    draw(app, res);
  });
}

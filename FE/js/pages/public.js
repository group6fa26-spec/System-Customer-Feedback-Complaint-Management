// SC-01 Landing, SC-02 Đăng ký, SC-03 OTP, SC-04 Đăng nhập, SC-05 Quên mật khẩu
import { api } from '../api.js';
import { USE_MOCK } from '../config.js';
import { setSession, homeFor } from '../auth.js';
import { go } from '../router.js';
import { ApiError } from '../errors.js';
import { html, mount, onSubmit, toast } from '../ui.js';
import { isEmail, isPhone, isStrongPassword } from '../validators.js';

export function landing({ app, user }) {
  mount(app, html`
    <div class="center-screen">
    <section class="hero card">
      <h1>Chung cư</h1>
      <p class="lead">Cư dân gửi khiếu nại, góp ý và đề xuất . Ban quản lý tiếp nhận, phân công và phản hồi — vui lòng theo dõi tiến độ theo thời gian thực.</p>
      <div class="actions">
        ${user
          ? html`<a class="btn" href="${'#' + homeFor(user.role)}">Vào trang của tôi</a>`
          : html`<a class="btn" href="#/register">Đăng ký tài khoản</a> <a class="btn sec" href="#/login">Đăng nhập</a>`}
      </div>
    </section>
    </div>
`);
}

export function registerPage({ app }) {
  mount(app, html`
    <div class="center-screen">
    <div class="card narrow">
      <h2>Đăng ký tài khoản</h2>
      <p class="mu">Xác thực bằng mã OTP trước khi tạo tài khoản (BR-04).</p>
      <form id="f" novalidate>
        <label for="full_name">Họ và tên</label>
        <input id="full_name" name="full_name" autocomplete="name">
        <label for="email">Email</label>
        <input id="email" name="email" type="email" autocomplete="email">
        <label for="phone">Số điện thoại</label>
        <input id="phone" name="phone" inputmode="tel" placeholder="0901234567" autocomplete="tel">
        <label for="password">Mật khẩu</label>
        <input id="password" name="password" type="password" autocomplete="new-password">
        <div class="mu">Tối thiểu 8 ký tự, gồm chữ và số.</div>
        <label for="password2">Nhập lại mật khẩu</label>
        <input id="password2" name="password2" type="password" autocomplete="new-password">
        <button type="submit" class="block">Nhận mã OTP</button>
        <div class="form-msg"></div>
      </form>
      <p class="mu">Đã có tài khoản? <a href="#/login">Đăng nhập</a></p>
    </div>`);

  onSubmit(app.querySelector('#f'), async (fd) => {
    const v = Object.fromEntries([...fd].map(([k, x]) => [k, String(x).trim()]));
    const fields = {};
    if (v.full_name.length < 2) fields.full_name = 'Vui lòng nhập họ tên.';
    if (!isEmail(v.email)) fields.email = 'Email không hợp lệ.';
    if (!isPhone(v.phone)) fields.phone = 'Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.';
    if (!isStrongPassword(fd.get('password'))) fields.password = 'Mật khẩu tối thiểu 8 ký tự, gồm chữ và số.';
    if (fd.get('password') !== fd.get('password2')) fields.password2 = 'Mật khẩu nhập lại không khớp.';
    if (Object.keys(fields).length) throw new ApiError(400, 'Vui lòng kiểm tra các trường được đánh dấu.', { fields });

// Backend tạo và gửi OTP (email/SMS). FE chỉ gọi API rồi chuyển sang màn nhập mã.
    await api('/api/auth/register', { method: 'POST', auth: false,
  body: { full_name: v.full_name, email: v.email, phone: v.phone, password: fd.get('password') } });
try { sessionStorage.setItem('ccms_pending_email', v.email); } catch { /* bỏ qua */ }
go('/otp');
    go('/otp');
  });
}

export function otpPage({ app }) {
  let email = '';
  try { email = sessionStorage.getItem('ccms_pending_email') || ''; } catch { /* bỏ qua */ }
  if (!email) { go('/register'); return; }
  mount(app, html`
    <div class="card narrow">
      <h2>Nhập mã OTP</h2>
      <p class="mu">Mã gồm 6 chữ số đã được gửi cho <b>${email}</b>.</p>
      <form id="f" novalidate>
        <label for="otp">Mã OTP</label>
        <input id="otp" name="otp" inputmode="numeric" maxlength="6" autocomplete="one-time-code">
        <button type="submit" class="block">Xác thực</button>
        <div class="form-msg"></div>
      </form>
      <p class="mu"><a href="#/register">← Quay lại đăng ký</a></p>
    </div>`);
  onSubmit(app.querySelector('#f'), async (fd) => {
    const otp = String(fd.get('otp')).trim();
    if (!/^\d{6}$/.test(otp)) throw new ApiError(400, 'Mã OTP gồm 6 chữ số.', { fields: { otp: 'Nhập đủ 6 chữ số.' } });
    await api('/api/auth/otp/verify', { method: 'POST', auth: false, body: { email, otp } });
    try { sessionStorage.removeItem('ccms_pending_email'); } catch { /* bỏ qua */ }
    toast('Tạo tài khoản thành công. Hãy đăng nhập.');
    go('/login');
  });
}

export function loginPage({ app, query }) {
    mount(app, html`
    <div class="login-box">
      <div class="login-logo">
        <svg viewBox="0 0 64 64" width="56" height="56" aria-hidden="true">
          <path d="M8 6h48v30c0 12-10 20-24 24C18 56 8 48 8 36z" fill="#0b4a8f"/>
          <path d="M20 44V24h10v20zm14 0V16h10v28z" fill="#fff"/>
        </svg>
        <div class="login-school">CHUNG CƯ SUNRISE</div>
      </div>

      <div class="login-head">
        <div class="login-title">Cổng thông tin phản hồi &amp; khiếu nại</div>
        <div class="login-sep"><span>-*-</span></div>
      </div>

      <form id="f" novalidate>
        <div class="login-body">
          <input name="email" type="email" placeholder="Email đăng nhập" autocomplete="username" aria-label="Email">
          <input name="password" type="password" placeholder="Mật khẩu" autocomplete="current-password" aria-label="Mật khẩu">
          <div class="form-msg"></div>
          <button type="submit" class="login-btn">Đăng nhập</button>
        </div>
      </form>

      <div class="login-alt">
        <a class="alt-btn" href="#/register"><span class="alt-ico"></span><span class="alt-txt">Đăng ký tài khoản cư dân</span></a>
        <a class="alt-link" href="#/forgot">Quên mật khẩu?</a>
      </div>

      ${USE_MOCK ? html`
        <details class="demo login-demo"><summary>Tài khoản demo (chỉ khi chạy mock)</summary>
          <table>
            <tr><td>customer@demo.vn</td><td>Khách hàng</td></tr>
            <tr><td>staff@demo.vn</td><td>Nhân viên CSKH</td></tr>
            <tr><td>dept@demo.vn</td><td>Nhân viên bộ phận</td></tr>
            <tr><td>manager@demo.vn</td><td>Quản lý</td></tr>
            <tr><td>admin@demo.vn</td><td>Quản trị</td></tr>
          </table>
          <p class="mu">Mật khẩu chung: <b>Demo@1234</b></p>
        </details>` : ''}
    </div>`);

  onSubmit(app.querySelector('#f'), async (fd) => {
    const email = String(fd.get('email')).trim(), password = String(fd.get('password'));
    if (!email || !password) throw new ApiError(400, 'Vui lòng nhập email và mật khẩu.');
    const res = await api('/api/auth/login', { method: 'POST', auth: false, body: { email, password } });
    setSession({ token: res.token, user: res.user });
    // Chỉ cho phép chuyển hướng nội bộ (tránh open redirect)
    const next = query.next && query.next.startsWith('/') && !query.next.startsWith('//') ? query.next : homeFor(res.user.role);
    go(next);
  });
}

export function forgotPage({ app }) {
  mount(app, html`<div class="card narrow"><h2>Quên mật khẩu</h2>
    <div class="msg warn">Màn hình này có trong Screen Flow nhưng chưa có use case và API tương ứng (Security Design mục 5). Cần nhóm chốt trước khi làm.</div>
    <p><a href="#/login">← Về đăng nhập</a></p></div>`);
}

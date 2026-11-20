import { route, render, go } from './router.js';
import { getUser, clearSession, homeFor } from './auth.js';
import { ROLES } from './config.js';
import { html, mount } from './ui.js';

import { landing, registerPage, otpPage, loginPage, forgotPage } from './pages/public.js';
import { newTicket, history, track, customerDetail } from './pages/customer.js';
import { queue } from './pages/staff.js';
import { ticketDetail } from './pages/ticket.js';
import { myTasks, taskDetail } from './pages/dept.js';
import { dashboard, escalated } from './pages/manager.js';

const C = ['customer'], S = ['cs_staff'], D = ['dept_staff'], M = ['manager'];

// ---- Bảng route (theo Screen Flow) ----------------------------
route('/', null, ({ user }) => go(user ? homeFor(user.role) : '/login'));
route('/register', 'guest', registerPage);
route('/otp', 'guest', otpPage);
route('/login', 'guest', loginPage);
route('/forgot', 'guest', forgotPage);

route('/customer/new', C, newTicket);
route('/customer/history', C, history);
route('/customer/track', C, track);
route('/customer/complaints/:code', C, customerDetail);

route('/staff/queue', S, queue);
route('/staff/complaints/:id', S, ticketDetail);

route('/dept/tasks', D, myTasks);
route('/dept/tasks/:id', D, taskDetail);

route('/manager/dashboard', M, dashboard);
route('/manager/escalated', M, escalated);
route('/manager/complaints/:id', M, ticketDetail);

route('/admin', ['admin'], ({ app }) => mount(app, html`<div class="card"><h2>Quản trị hệ thống</h2>
  <p>Chưa có màn hình trong Screen Flow cho UC-18 → UC-21. Cần nhóm chốt trước khi làm.</p></div>`));

// ---- Thanh điều hướng theo vai trò ----------------------------
const NAV = {
  customer: [['/customer/new', 'Gửi phản hồi'], ['/customer/history', 'Phiếu của tôi'], ['/customer/track', 'Tra cứu mã']],
  cs_staff: [['/staff/queue', 'Hàng đợi']],
  dept_staff: [['/dept/tasks', 'Việc được giao']],
  manager: [['/manager/dashboard', 'Dashboard'], ['/manager/escalated', 'Chuyển cấp / Chờ duyệt']],
  admin: [['/admin', 'Quản trị']],
};

function renderNav() {
  const nav = document.getElementById('nav');
  const user = getUser();
  const here = (location.hash.slice(1) || '/').split('?')[0];
  document.body.classList.toggle('is-landing', here === '/' && !user)
  const NO_HEADER = ['/', '/login', '/register', '/otp', '/forgot'];
  document.body.classList.toggle('no-header', !user && NO_HEADER.includes(here));
  if (!user) {
    mount(nav, html`<a href="#/login" class="${here === '/login' ? 'on' : ''}">Đăng nhập</a>
      <a href="#/register" class="btn-link ${here === '/register' ? 'on' : ''}">Đăng ký</a>`);
    return;
  }
  mount(nav, html`
    ${(NAV[user.role] || []).map(([p, l]) => html`<a href="${'#' + p}" class="${here.startsWith(p) ? 'on' : ''}">${l}</a>`)}
    <span class="who" title="${ROLES[user.role]}">${user.name} · ${ROLES[user.role]}</span>
    <button type="button" class="sec" id="logout">Đăng xuất</button>`);
  document.getElementById('logout').onclick = () => {
    clearSession();
    if (location.hash === '#/' || location.hash === '') refresh(); else go('/');
  };
}

const refresh = () => { renderNav(); render(); };
window.addEventListener('hashchange', refresh);
refresh();
export { homeFor };

/* 图标精灵：内联注入，避免 file:// 跨文件 <use> 被 CORS 拦截 */
(function () {
  var sprite = `<svg xmlns="http://www.w3.org/2000/svg" style="display:none" aria-hidden="true">
  <symbol id="i-dash" viewBox="0 0 24 24"><path d="M3 13h8V3H3v10zm10 8h8V11h-8v10zM3 21h8v-6H3v6zM13 3v6h8V3h-8z" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linejoin="round"/></symbol>
  <symbol id="i-cover" viewBox="0 0 24 24"><circle cx="12" cy="12" r="8.5" fill="none" stroke="currentColor" stroke-width="1.75"/><circle cx="12" cy="12" r="4" fill="none" stroke="currentColor" stroke-width="1.5" stroke-dasharray="2 2"/><path d="M12 3.5v3M12 17.5v3M3.5 12h3M17.5 12h3" stroke="currentColor" stroke-width="1.5"/></symbol>
  <symbol id="i-route" viewBox="0 0 24 24"><circle cx="6" cy="18" r="2.5" fill="none" stroke="currentColor" stroke-width="1.75"/><circle cx="18" cy="6" r="2.5" fill="none" stroke="currentColor" stroke-width="1.75"/><path d="M8.5 18h5a3 3 0 0 0 0-6h-3a3 3 0 0 1 0-6h5" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"/></symbol>
  <symbol id="i-task" viewBox="0 0 24 24"><rect x="4" y="4.5" width="16" height="16" rx="2" fill="none" stroke="currentColor" stroke-width="1.75"/><path d="M8 3v3M16 3v3M8 11h5M8 15h8" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"/></symbol>
  <symbol id="i-alert" viewBox="0 0 24 24"><path d="M12 4.5 21 20H3l9-15.5z" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linejoin="round"/><path d="M12 10v4.5M12 17.2v.1" stroke="currentColor" stroke-width="1.9" stroke-linecap="round"/></symbol>
  <symbol id="i-order" viewBox="0 0 24 24"><path d="M6 3.5h9l4 4v13H6z" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linejoin="round"/><path d="M14.5 3.5v4.5H19M9 12h6M9 16h4" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"/></symbol>
  <symbol id="i-tool" viewBox="0 0 24 24"><path d="M14.7 6.3a3.8 3.8 0 0 0 5 5L20 11l-8.6 8.6a2 2 0 1 1-2.8-2.8L17.2 8.2l-2.5-1.9z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></symbol>
  <symbol id="i-check" viewBox="0 0 24 24"><path d="M4.5 12.5 9 17l10-10" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"/></symbol>
  <symbol id="i-box" viewBox="0 0 24 24"><path d="M3.5 7.5 12 3l8.5 4.5v9L12 21l-8.5-4.5v-9z" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/><path d="M3.5 7.5 12 12l8.5-4.5M12 12v9" fill="none" stroke="currentColor" stroke-width="1.7"/></symbol>
  <symbol id="i-chart" viewBox="0 0 24 24"><path d="M4 20V10M10 20V5M16 20v-7M22 20H2" stroke="currentColor" stroke-width="1.8" fill="none" stroke-linecap="round"/></symbol>
  <symbol id="i-rule" viewBox="0 0 24 24"><path d="M5 6h14M5 12h9M5 18h6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><circle cx="17.5" cy="12" r="2.2" fill="none" stroke="currentColor" stroke-width="1.7"/><circle cx="14" cy="18" r="2.2" fill="none" stroke="currentColor" stroke-width="1.7"/></symbol>
  <symbol id="i-msg" viewBox="0 0 24 24"><path d="M4 5.5h16v11H9l-5 3.5v-14.5z" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/></symbol>
  <symbol id="i-user" viewBox="0 0 24 24"><circle cx="12" cy="8.5" r="3.6" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M4.8 20c.7-3.6 3.6-5.6 7.2-5.6s6.5 2 7.2 5.6" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></symbol>
  <symbol id="i-set" viewBox="0 0 24 24"><circle cx="12" cy="12" r="3" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M12 3.5v2.2M12 18.3v2.2M4.9 7.8l1.9 1.1M17.2 15.1l1.9 1.1M4.9 16.2l1.9-1.1M17.2 8.9l1.9-1.1" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></symbol>
  <symbol id="i-menu" viewBox="0 0 24 24"><path d="M4 7h16M4 12h16M4 17h10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></symbol>
  <symbol id="i-loc" viewBox="0 0 24 24"><path d="M12 21s7-6.3 7-11a7 7 0 1 0-14 0c0 4.7 7 11 7 11z" fill="none" stroke="currentColor" stroke-width="1.7"/><circle cx="12" cy="10" r="2.5" fill="none" stroke="currentColor" stroke-width="1.7"/></symbol>
  <symbol id="i-clock" viewBox="0 0 24 24"><circle cx="12" cy="12" r="8.5" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M12 7.5V12l3 2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></symbol>
  <symbol id="i-up" viewBox="0 0 24 24"><path d="M12 16V5m0 0L7.5 9.5M12 5l4.5 4.5M5 19h14" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"/></symbol>
  <symbol id="i-empty" viewBox="0 0 24 24"><rect x="3.5" y="6" width="17" height="13" rx="2" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M3.5 10h17M8 6V3.5M16 6V3.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></symbol>
  <symbol id="i-search" viewBox="0 0 24 24"><circle cx="11" cy="11" r="6.5" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="m16 16 4.5 4.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></symbol>
  </svg>`;
  function inject() {
    if (document.getElementById('i-dash')) return;
    var d = document.createElement('div');
    d.innerHTML = sprite;
    document.body.insertBefore(d.firstElementChild, document.body.firstChild);
  }
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', inject);
  } else {
    inject();
  }
})();

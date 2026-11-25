/* 统一外壳：按 data-role 注入侧边栏 / 顶栏，模拟多角色视觉区分 */
(function () {
  var MENUS = {
    admin: {
      label: '管理员', color: '#234E3B', name: '张启明', dept: '市政设施管理科',
      nav: [
        ['i-dash', '工作台', '02-admin-dashboard.html'],
        ['i-cover', '井盖台账', '03-cover-list.html'],
        ['i-route', '巡检路线', '05-route-plan.html'],
        ['i-task', '巡检任务', '06-patrol-task.html'],
        ['i-alert', '问题上报', '08-issue-list.html'],
        ['i-order', '维修工单', '10-order-detail.html'],
        ['i-check', '验收记录', '12-acceptance.html'],
        ['i-box', '备件库存', '13-spare-parts.html'],
        ['i-rule', '风险规则', '14-risk-rule.html'],
        ['i-chart', '统计分析', '16-analysis.html'],
        ['i-msg', '消息中心', '16-analysis.html'],
        ['i-set', '系统管理', '14-risk-rule.html']
      ]
    },
    dispatcher: {
      label: '调度员', color: '#1F4A52', name: '李文博', dept: '养护调度中心',
      nav: [
        ['i-dash', '派单工作台', '09-dispatch-board.html'],
        ['i-alert', '待派问题', '08-issue-list.html'],
        ['i-order', '维修工单', '10-order-detail.html'],
        ['i-clock', '逾期跟踪', '09-dispatch-board.html'],
        ['i-chart', '风险分级', '15-risk-explain.html'],
        ['i-msg', '消息中心', '16-analysis.html']
      ]
    },
    acceptor: {
      label: '验收人员', color: '#2B5140', name: '陈默', dept: '质量监督组',
      nav: [
        ['i-check', '验收待办', '12-acceptance.html'],
        ['i-order', '验收档案', '15-risk-explain.html'],
        ['i-clock', '返工跟踪', '12-acceptance.html'],
        ['i-chart', '验收统计', '16-analysis.html'],
        ['i-msg', '消息中心', '16-analysis.html']
      ]
    },
    inspector: {
      label: '巡检员', color: '#3A7D5A', name: '王大海', dept: '第一巡检班组',
      portal: true,
      nav: [
        ['i-task', '我的任务', '06-patrol-task.html'],
        ['i-up', '巡检上报', '07-patrol-report.html'],
        ['i-loc', '井盖地图', '03-cover-list.html'],
        ['i-msg', '消息', '16-analysis.html']
      ]
    },
    repairer: {
      label: '维修人员', color: '#4A5B3C', name: '赵国强', dept: '井盖维修一队',
      portal: true,
      nav: [
        ['i-order', '我的工单', '11-my-orders.html'],
        ['i-tool', '现场维修', '11-my-orders.html'],
        ['i-box', '备件领用', '13-spare-parts.html'],
        ['i-msg', '消息', '16-analysis.html']
      ]
    }
  };

  var body = document.body;
  var role = body.dataset.role || 'admin';
  var active = body.dataset.active || '';
  var cfg = MENUS[role];
  if (!cfg) return;
  document.documentElement.style.setProperty('--brand', cfg.color);

  var icon = function (id, cls) {
    return '<svg class="' + (cls || '') + '" aria-hidden="true"><use href="#' + id + '"></use></svg>';
  };
  var navHtml = cfg.nav.map(function (n) {
    var on = n[1] === active ? ' class="active"' : '';
    return '<a href="' + n[2] + '"' + on + '>' + icon(n[0]) + '<span>' + n[1] + '</span></a>';
  }).join('');

  var side = document.createElement('aside');
  side.className = 'sidebar';
  side.innerHTML =
    '<div class="brand">' + icon('i-cover') + '<span>井盖巡检管理</span></div>' +
    '<div class="role">' + cfg.label + ' 端</div>' +
    '<nav>' + navHtml + '</nav>' +
    '<div class="foot">v1.0 · ' + cfg.dept + '</div>';

  var top = document.createElement('header');
  top.className = 'topbar';
  top.innerHTML =
    '<span class="crumb">' + cfg.dept + ' / <b>' + (active || '首页') + '</b></span>' +
    '<span class="grow"></span>' +
    '<a class="iconbtn" href="16-analysis.html" title="消息">' + icon('i-msg') + '<i class="dot"></i></a>' +
    '<a class="iconbtn" href="index.html" title="切换角色">' + icon('i-set') + '</a>' +
    '<div class="avatar"><img src="../assets/avatar.png" alt="头像" onerror="this.style.visibility=\'hidden\'">' +
    '<span>' + cfg.name + '<small>' + cfg.label + '</small></span></div>';

  if (cfg.portal) {
    /* 门户形态：顶栏 + 横向导航，无侧边栏 */
    top.classList.add('portal');
    top.innerHTML =
      '<div class="brand" style="color:var(--brand);font-weight:600;display:flex;align-items:center;gap:8px">' +
      icon('i-cover') + '井盖巡检 · ' + cfg.label + '端</div>' +
      '<nav class="portalnav">' + navHtml + '</nav>' +
      '<span class="grow"></span>' +
      '<a class="iconbtn" href="16-analysis.html">' + icon('i-msg') + '<i class="dot"></i></a>' +
      '<div class="avatar"><img src="../assets/avatar.png" alt="头像" onerror="this.style.visibility=\'hidden\'">' +
      '<span>' + cfg.name + '<small>' + cfg.dept + '</small></span></div>';
    var layout = document.querySelector('.layout');
    if (layout) layout.insertBefore(top, layout.firstChild);
    body.classList.add('is-portal');
    return;
  }

  var main = document.querySelector('.main');
  var layout = document.querySelector('.layout');
  if (main) main.insertBefore(top, main.firstChild);
  if (layout) layout.insertBefore(side, layout.firstChild);
})();

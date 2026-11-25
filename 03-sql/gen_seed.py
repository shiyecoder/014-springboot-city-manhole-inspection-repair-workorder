# -*- coding: utf-8 -*-
"""生成 NEW-014 种子数据 SQL，输出到 stdout，由 init.sql 追加。
风险分算法与后端 RiskRuleService 保持一致：
道路25 + 严重度25 + 历史维修15 + 人流15 + 未处理时长10 + 重复报修10 = 100
"""
import datetime as dt
import json

NOW = dt.datetime(2026, 10, 31, 20, 0, 0)
DEPTS = [
    ('市政设施管理科', 'DEPT_ADMIN', 'DEPT', '张启明', '0571-88010001', 1),
    ('养护调度中心', 'DEPT_DISPATCH', 'DEPT', '李文博', '0571-88010002', 2),
    ('质量监督组', 'DEPT_ACCEPT', 'DEPT', '陈默', '0571-88010003', 3),
    ('第一巡检班组', 'TEAM_PATROL_1', 'TEAM', '王大海', '0571-88010004', 4),
    ('第二巡检班组', 'TEAM_PATROL_2', 'TEAM', '陈立', '0571-88010005', 5),
    ('井盖维修一队', 'TEAM_REPAIR_1', 'TEAM', '赵国强', '0571-88010006', 6),
    ('井盖维修二队', 'TEAM_REPAIR_2', 'TEAM', '孙磊', '0571-88010007', 7),
    ('排水抢修队', 'TEAM_REPAIR_3', 'TEAM', '钱伟', '0571-88010008', 8),
]


def q(v):
    if v is None:
        return 'NULL'
    if isinstance(v, (int, float)):
        return str(v)
    return "'" + str(v).replace("\\", "\\\\").replace("'", "''") + "'"


def rows(table, cols, data):
    out = ['-- %s' % table]
    out.append('INSERT INTO %s (%s) VALUES' % (table, ','.join(cols)))
    vals = []
    for r in data:
        vals.append('(' + ','.join(q(x) for x in r) + ')')
    out.append(',\n'.join(vals) + ';')
    return '\n'.join(out)


out = []
w = out.append

w('\n-- ============================================================')
w('-- 种子数据（由 gen_seed.py 生成，创建时间分散在最近 6 个月）')
w('-- ============================================================')

# ---------------- 组织 / 角色 / 用户 ----------------
w(rows('sys_dept', ['id', 'dept_name', 'dept_code', 'dept_type', 'leader', 'phone', 'sort'],
       [(i + 1,) + d for i, d in enumerate(DEPTS)]))

w(rows('sys_role', ['id', 'role_name', 'role_code', 'data_scope', 'remark', 'status'], [
    (1, '管理员', 'ADMIN', 'ALL', '维护区域、井盖类型、路线、队伍、备件与统计', 1),
    (2, '调度员', 'DISPATCHER', 'ALL', '分类问题、设置级别、派发维修与逾期跟踪', 1),
    (3, '巡检员', 'INSPECTOR', 'SELF', '按路线检查井盖，登记状态与现场图片', 1),
    (4, '维修人员', 'REPAIRER', 'SELF', '接单、围挡、更换修复、领用备件、上传结果', 1),
    (5, '验收人员', 'ACCEPTOR', 'ALL', '现场复查并确认合格或返工', 1),
]))

PW = '$2b$10$QJ8LA8dbeepgx1mIXVKiFOHk5F4rd9Q1qusHdHNahXTTRqZ.2dRX.'  # admin123
users = [
    ('admin', '张启明', '13800006621', 'zhangqm@city.gov.cn', 1, 1, None, '/uploads/seed/avatar/01.jpg'),
    ('dispatch01', '李文博', '13800006622', 'liwbo@city.gov.cn', 2, 2, None, '/uploads/seed/avatar/02.jpg'),
    ('dispatch02', '吴倩', '13800006623', 'wuqian@city.gov.cn', 2, 2, None, None),
    ('patrol01', '王大海', '13800006624', 'wangdh@city.gov.cn', 4, 3, '第一巡检班组', '/uploads/seed/avatar/03.jpg'),
    ('patrol02', '刘敏', '13800006625', 'liumin@city.gov.cn', 4, 3, '第一巡检班组', None),
    ('patrol03', '陈立', '13800006626', 'chenli@city.gov.cn', 5, 3, '第二巡检班组', None),
    ('patrol04', '周翔', '13800006627', 'zhouxiang@city.gov.cn', 5, 3, '第二巡检班组', None),
    ('repair01', '赵国强', '13800006628', 'zhaogq@city.gov.cn', 6, 4, '井盖维修一队', '/uploads/seed/avatar/04.jpg'),
    ('repair02', '钱伟', '13800006629', 'qianwei@city.gov.cn', 8, 4, '排水抢修队', None),
    ('repair03', '孙磊', '13800006630', 'sunlei@city.gov.cn', 7, 4, '井盖维修二队', None),
    ('accept01', '陈默', '13800006631', 'chenmo@city.gov.cn', 3, 5, None, None),
    ('accept02', '赵雅', '13800006632', 'zhaoya@city.gov.cn', 3, 5, None, None),
]
urows = []
for i, (un, rn, ph, em, dept, role, team, av) in enumerate(users):
    created = (NOW - dt.timedelta(days=180 - i * 3)).strftime('%Y-%m-%d %H:%M:%S')
    last = (NOW - dt.timedelta(hours=1 + i)).strftime('%Y-%m-%d %H:%M:%S')
    urows.append((i + 1, un, PW, rn, ph, em, av, dept, role, team, 1, 0, None, last, 0, 0, created, last))
w(rows('sys_user', ['id', 'username', 'password', 'real_name', 'phone', 'email', 'avatar', 'dept_id',
                    'role_id', 'team_name', 'status', 'fail_count', 'lock_time', 'last_login', 'deleted',
                    'version', 'create_time', 'update_time'], urows))

# ---------------- 菜单 ----------------
menus = [
    (1, 0, '工作台', '/dashboard', 'Dashboard', 'dashboard', 'MENU', 'dashboard:view', 1),
    (2, 0, '井盖台账', '/covers', 'CoverList', 'cover', 'MENU', 'cover:list', 2),
    (3, 0, '巡检路线', '/routes', 'RouteList', 'route', 'MENU', 'route:list', 3),
    (4, 0, '巡检任务', '/tasks', 'TaskList', 'task', 'MENU', 'task:list', 4),
    (5, 0, '问题上报', '/issues', 'IssueList', 'alert', 'MENU', 'issue:list', 5),
    (6, 0, '维修工单', '/orders', 'OrderList', 'order', 'MENU', 'order:list', 6),
    (7, 0, '验收记录', '/acceptances', 'AcceptanceList', 'check', 'MENU', 'acceptance:list', 7),
    (8, 0, '备件库存', '/parts', 'PartList', 'box', 'MENU', 'part:list', 8),
    (9, 0, '风险分级', '/risk', None, 'rule', 'DIR', None, 9),
    (10, 9, '规则配置', '/risk/rules', 'RiskRule', 'rule', 'MENU', 'risk:rule', 1),
    (11, 9, '计算记录', '/risk/scores', 'RiskScore', 'chart', 'MENU', 'risk:score', 2),
    (12, 0, '统计分析', '/analysis', 'Analysis', 'chart', 'MENU', 'analysis:view', 10),
    (13, 0, '消息中心', '/messages', 'MessageList', 'msg', 'MENU', 'message:list', 11),
    (14, 0, '系统管理', '/system', None, 'setting', 'DIR', None, 12),
    (15, 14, '用户管理', '/system/users', 'UserList', 'user', 'MENU', 'system:user', 1),
    (16, 14, '角色管理', '/system/roles', 'RoleList', 'user', 'MENU', 'system:role', 2),
    (17, 14, '菜单管理', '/system/menus', 'MenuList', 'menu', 'MENU', 'system:menu', 3),
    (18, 14, '字典管理', '/system/dicts', 'DictList', 'menu', 'MENU', 'system:dict', 4),
    (19, 0, '个人中心', '/profile', 'Profile', 'user', 'MENU', 'profile:view', 13),
    (20, 0, '我的任务', '/my/tasks', 'MyTask', 'task', 'MENU', 'mytask:list', 20),
    (21, 0, '巡检上报', '/my/report', 'PatrolReport', 'upload', 'MENU', 'mytask:report', 21),
    (22, 0, '我的工单', '/my/orders', 'MyOrder', 'tool', 'MENU', 'myorder:list', 22),
]
w(rows('sys_menu', ['id', 'parent_id', 'menu_name', 'path', 'component', 'icon', 'menu_type', 'perm', 'sort', 'visible', 'status'],
       [(m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8], 1, 1) for m in menus]))

rm = []
for mid in range(1, 23):
    rm.append((1, mid))                       # 管理员：全部
for mid in [1, 2, 5, 6, 8, 9, 10, 11, 12, 13, 19]:
    rm.append((2, mid))                       # 调度员
for mid in [1, 4, 5, 20, 21, 13, 19]:
    rm.append((3, mid))                       # 巡检员
for mid in [1, 6, 8, 22, 13, 19]:
    rm.append((4, mid))                       # 维修人员
for mid in [1, 6, 7, 9, 11, 12, 13, 19]:
    rm.append((5, mid))                       # 验收人员
rm = sorted(set(rm))
w(rows('sys_role_menu', ['role_id', 'menu_id'], rm))

# ---------------- 字典 ----------------
dicts = [
    ('cover_type', '雨水井盖', 'RAIN', 1), ('cover_type', '污水井盖', 'SEWAGE', 2),
    ('cover_type', '给水井盖', 'WATER', 3), ('cover_type', '燃气井盖', 'GAS', 4),
    ('cover_type', '电力井盖', 'POWER', 5), ('cover_type', '通信井盖', 'TELECOM', 6),
    ('risk_level', '重大', 'RED', 1), ('risk_level', '紧急', 'YELLOW', 2), ('risk_level', '一般', 'BLUE', 3),
    ('road_level', '主干道', 'MAIN', 1), ('road_level', '次干道', 'SUB', 2), ('road_level', '支路', 'BRANCH', 3),
    ('crowd_level', '密集', 'HIGH', 1), ('crowd_level', '一般', 'MID', 2), ('crowd_level', '稀疏', 'LOW', 3),
    ('damage_type', '破损', 'BREAK', 1), ('damage_type', '松动', 'LOOSE', 2), ('damage_type', '丢失', 'LOST', 3),
    ('damage_type', '沉降', 'SINK', 4), ('damage_type', '异响', 'NOISE', 5), ('damage_type', '井圈破损', 'RIM', 6),
    ('severity', '轻微', 'LIGHT', 1), ('severity', '一般', 'NORMAL', 2), ('severity', '严重', 'HEAVY', 3),
    ('issue_status', '待确认', 'PENDING_CONFIRM', 1), ('issue_status', '待派单', 'PENDING_DISPATCH', 2),
    ('issue_status', '处理中', 'PROCESSING', 3), ('issue_status', '待验收', 'PENDING_ACCEPT', 4),
    ('issue_status', '返工中', 'REWORK', 5), ('issue_status', '已关闭', 'CLOSED', 6), ('issue_status', '已取消', 'CANCELED', 7),
    ('cover_status', '正常', 'NORMAL', 1), ('cover_status', '待派单', 'PENDING_DISPATCH', 2),
    ('cover_status', '维修中', 'REPAIRING', 3), ('cover_status', '待验收', 'PENDING_ACCEPT', 4),
    ('cover_status', '已关闭', 'CLOSED', 5),
    ('source_type', '巡检发现', 'PATROL', 1), ('source_type', '人工上报', 'MANUAL', 2), ('source_type', '市民热线', 'HOTLINE', 3),
    ('safety_measure', '临时围挡', 'BARRIER', 1), ('safety_measure', '警示锥', 'CONE', 2),
    ('safety_measure', '反光标识', 'REFLECT', 3), ('safety_measure', '夜间警示灯', 'LIGHT', 4),
    ('part_category', '井盖', 'COVER', 1), ('part_category', '井圈', 'RIM', 2),
    ('part_category', '警示物料', 'WARN', 3), ('part_category', '其他', 'OTHER', 4),
]
w(rows('sys_dict', ['id', 'dict_type', 'dict_label', 'dict_value', 'sort', 'status'],
       [(i + 1,) + d + (1,) for i, d in enumerate(dicts)]))

# ---------------- 风险规则 ----------------
rules = [
    (1, 'v1.0', '井盖风险分级巡检初版', 30, 30, 20, 10, 5, 5, 71, 46, 0, '2025-12-01 00:00:00', 1),
    (2, 'v1.1', '未处理时长权重上调', 30, 30, 20, 10, 5, 5, 71, 46, 0, '2026-03-15 00:00:00', 2),
    (3, 'v1.2', '新增重复报修因子', 25, 25, 15, 15, 10, 10, 71, 46, 0, '2026-06-01 00:00:00', 1),
    (4, 'v1.3', '阈值调整，主干道权重提高', 25, 25, 15, 15, 10, 10, 71, 46, 1, '2026-09-01 00:00:00', 1),
]
w(rows('risk_rule', ['id', 'rule_version', 'rule_name', 'w_road', 'w_severity', 'w_repair', 'w_crowd',
                     'w_duration', 'w_repeat', 'red_threshold', 'yellow_threshold', 'enabled',
                     'effect_time', 'create_by'],
       [(r[0], r[1], r[2], r[3], r[4], r[5], r[6], r[7], r[8], r[9], r[10], r[11], r[12], r[13]) for r in rules]))

# ---------------- 井盖台账 ----------------
ROAD_CFG = {'解放东路': ('主干道', '密集'), '中山路': ('主干道', '密集'),
            '建设南路': ('次干道', '一般'), '环城北路': ('次干道', '稀疏'), '人民西路': ('支路', '一般')}
COVERS = [
    ('JHD-0142', '雨水井盖', '解放东路', 118.372100, 31.241800, '解放东路与中山路交叉口东 35 米，非机动车道内侧', '市政一处', 'Φ700 重型', '球墨铸铁', '2019-06-12', 4, 4),
    ('JHD-0141', '雨水井盖', '解放东路', 118.372600, 31.242100, '解放东路 118 号门前', '市政一处', 'Φ700 重型', '球墨铸铁', '2019-06-12', 5, 4),
    ('JHD-0139', '污水井盖', '解放东路', 118.371900, 31.241200, '解放东路与中山路交叉口西 20 米', '排水公司', 'Φ700 重型', '球墨铸铁', '2018-11-03', 4, 4),
    ('JHD-0138', '污水井盖', '解放东路', 118.371500, 31.240900, '解放东路 96 号非机动车道', '排水公司', 'Φ700 重型', '球墨铸铁', '2018-11-03', 5, 4),
    ('JHD-0136', '雨水井盖', '解放东路', 118.370900, 31.242400, '解放东路 82 号商铺前', '市政一处', 'Φ700 重型', '球墨铸铁', '2020-04-18', 4, 4),
    ('JHD-0132', '雨水井盖', '解放东路', 118.370800, 31.242600, '解放东路与中山路口东 60 米', '市政一处', 'Φ700 重型', '球墨铸铁', '2020-04-18', 4, 4),
    ('JHD-0129', '污水井盖', '解放东路', 118.370200, 31.241600, '解放东路 60 号西侧', '排水公司', 'Φ700 重型', '球墨铸铁', '2017-08-21', 5, 4),
    ('JHD-0128', '雨水井盖', '解放东路', 118.369600, 31.242000, '解放东路 44 号门前', '市政一处', 'Φ700 重型', '球墨铸铁', '2021-03-09', 5, 4),
    ('JHD-0124', '给水井盖', '中山路', 118.368200, 31.246800, '中山路 12 号人行道', '市政二处', 'Φ600 轻型', '球墨铸铁', '2021-03-25', 6, 5),
    ('JHD-0123', '给水井盖', '中山路', 118.368500, 31.246200, '中山路 26 号商铺前', '市政二处', 'Φ600 轻型', '球墨铸铁', '2021-03-25', 6, 5),
    ('JHD-0121', '给水井盖', '中山路', 118.369000, 31.245100, '中山路与解放东路交叉口北 15 米', '市政二处', 'Φ600 轻型', '球墨铸铁', '2020-09-11', 6, 5),
    ('JHD-0119', '电力井盖', '中山路', 118.368800, 31.245600, '中山路 58 号电缆通道', '供电公司', 'Φ800 重型', '球墨铸铁', '2016-05-30', 6, 5),
    ('JHD-0117', '燃气井盖', '建设南路', 118.365200, 31.238800, '建设南路 8 号门前', '燃气公司', 'Φ700 重型', '球墨铸铁', '2017-09-18', 7, 5),
    ('JHD-0115', '通信井盖', '建设南路', 118.364800, 31.238200, '建设南路 22 号人行道', '通信管线', 'Φ600 轻型', '复合材料', '2020-12-07', 7, 5),
    ('JHD-0106', '电力井盖', '中山路', 118.368800, 31.247000, '中山路 88 号环网柜旁', '供电公司', 'Φ800 重型', '球墨铸铁', '2016-05-30', 6, 5),
    ('JHD-0208', '雨水井盖', '中山路', 118.369500, 31.244200, '中山路与建设路口人行横道旁', '市政二处', 'Φ700 重型', '球墨铸铁', '2018-07-16', 7, 5),
    ('JHD-0201', '污水井盖', '环城北路', 118.360200, 31.250100, '环城北路 3 号桥东侧', '排水公司', 'Φ700 重型', '球墨铸铁', '2019-10-22', 4, 4),
    ('JHD-0188', '雨水井盖', '中山路', 118.370100, 31.243800, '中山路 106 号非机动车道', '市政二处', 'Φ700 重型', '球墨铸铁', '2022-05-30', 7, 5),
    ('JHD-0174', '给水井盖', '解放东路', 118.373200, 31.243000, '解放东路 168 号门前', '市政一处', 'Φ600 轻型', '球墨铸铁', '2021-11-08', 4, 4),
    ('JHD-0166', '雨水井盖', '建设南路', 118.363900, 31.237600, '建设南路 45 号商铺前', '市政一处', 'Φ700 重型', '球墨铸铁', '2020-06-14', 7, 5),
    ('JHD-0161', '通信井盖', '建设南路', 118.363300, 31.237100, '建设南路 63 号人行道', '通信管线', 'Φ600 轻型', '复合材料', '2021-02-19', 6, 5),
    ('JHD-0158', '燃气井盖', '中山路', 118.367600, 31.245900, '中山路 34 号燃气阀室前', '燃气公司', 'Φ700 重型', '球墨铸铁', '2017-04-27', 6, 5),
    ('JHD-0148', '电力井盖', '人民西路', 118.355600, 31.239400, '人民西路 12 号电缆井', '供电公司', 'Φ800 重型', '球墨铸铁', '2018-03-12', 5, 4),
    ('JHD-0112', '雨水井盖', '人民西路', 118.354900, 31.238900, '人民西路 36 号门前', '市政一处', 'Φ700 重型', '球墨铸铁', '2022-09-02', 5, 4),
    ('JHD-0100', '污水井盖', '人民西路', 118.354200, 31.238300, '人民西路 58 号西侧', '排水公司', 'Φ700 重型', '球墨铸铁', '2019-01-25', 5, 4),
    ('JHD-0094', '通信井盖', '建设南路', 118.364100, 31.237100, '建设南路 51 号人行道', '通信管线', 'Φ600 轻型', '复合材料', '2020-12-07', 7, 5),
]
crows = []
cover_id_of = {}
for i, c in enumerate(COVERS):
    cid = i + 1
    cover_id_of[c[0]] = cid
    road, crowd = ROAD_CFG[c[2]]
    created = (NOW - dt.timedelta(days=200 - i * 4, hours=i * 3 % 12)).strftime('%Y-%m-%d %H:%M:%S')
    crows.append((cid, c[0], c[1], c[2], road, crowd, c[3], c[4], c[5], c[6], c[7], c[8],
                  c[9], c[10], '一般', '正常', '/uploads/seed/cover/%02d.jpg' % (i % 16 + 1),
                  None, c[11], 0, 0, 1, created, created))
w(rows('manhole_cover', ['id', 'cover_no', 'cover_type', 'road_name', 'road_level', 'crowd_level',
                         'longitude', 'latitude', 'location_desc', 'owner_unit', 'spec', 'material',
                         'install_date', 'duty_user_id', 'risk_level', 'cover_status', 'image_url',
                         'remark', 'dept_id', 'deleted', 'version', 'create_by', 'create_time', 'update_time'], crows))

# ---------------- 巡检路线 ----------------
ROUTES = [
    ('R-01', '解放东路西段', '解放东路', '中山路口—人民路口', 'DAY', '每日 08:00', 4, 5, 95,
     ['JHD-0128', 'JHD-0129', 'JHD-0132', 'JHD-0136']),
    ('R-03', '解放东路东段', '解放东路', '中山路口—建设路口', 'WEEK', '每周一、周四 08:00', 4, 4, 100,
     ['JHD-0142', 'JHD-0141', 'JHD-0139', 'JHD-0138']),
    ('R-05', '中山路北段', '中山路', '环城北路口—解放路口', 'WEEK', '每周二 08:30', 5, 6, 130,
     ['JHD-0124', 'JHD-0123', 'JHD-0121', 'JHD-0119', 'JHD-0106']),
    ('R-06', '中山路南段', '中山路', '解放路口—建设路口', 'WEEK', '每周三 08:30', 5, 7, 110,
     ['JHD-0208', 'JHD-0158', 'JHD-0188']),
    ('R-07', '建设南路全线', '建设南路', '建设路口—环城北路口', 'WEEK', '每周三 14:00', 5, 7, 150,
     ['JHD-0117', 'JHD-0115', 'JHD-0166', 'JHD-0161', 'JHD-0094']),
    ('R-09', '人民西路环线', '人民西路', '人民西路全段', 'HALF_MONTH', '每半月 09:00', 4, 5, 70,
     ['JHD-0148', 'JHD-0112', 'JHD-0100']),
    ('R-10', '环城北路东段', '环城北路', '环城北路 1—5 号桥', 'WEEK', '每周五 09:00', 4, 4, 80,
     ['JHD-0201', 'JHD-0174']),
]
rrows, irows = [], []
for i, r in enumerate(ROUTES):
    rid = i + 1
    rrows.append((rid, r[0], r[1], r[2], r[3], r[4], r[5], r[6], r[7], len(r[9]), r[8], 1 if rid != 7 else 1,
                  None, (NOW - dt.timedelta(days=150 - i * 5)).strftime('%Y-%m-%d %H:%M:%S')))
    for s, cno in enumerate(r[9]):
        irows.append((len(irows) + 1, rid, cover_id_of[cno], s + 1))
w(rows('patrol_route', ['id', 'route_no', 'route_name', 'road_name', 'road_range', 'cycle_type', 'cycle_desc',
                        'dept_id', 'leader_id', 'cover_count', 'estimate_min', 'status', 'remark', 'create_time'], rrows))
w(rows('patrol_route_item', ['id', 'route_id', 'cover_id', 'seq'], irows))

# ---------------- 巡检任务与记录 ----------------
TASKS = [
    ('XJ-20261031-003', 2, 0, '进行中', 4, 5, 2),
    ('XJ-20261031-005', 5, 0, '待执行', 4, 0, 0),
    ('XJ-20261031-001', 1, 0, '已完成', 5, 4, 1),
    ('XJ-20261030-004', 4, 1, '已完成', 7, 3, 1),
    ('XJ-20261030-002', 3, 1, '已完成', 6, 5, 0),
    ('XJ-20261029-003', 7, 3, '已完成', 4, 2, 0),
    ('XJ-20261029-001', 6, 3, '已完成', 5, 3, 0),
    ('XJ-20261028-002', 2, 4, '已完成', 5, 4, 1),
    ('XJ-20261027-005', 5, 5, '已完成', 7, 5, 0),
    ('XJ-20261026-001', 1, 7, '已完成', 6, 4, 1),
    ('XJ-20261024-004', 4, 9, '已完成', 6, 3, 0),
    ('XJ-20261022-002', 3, 11, '已完成', 5, 5, 0),
    ('XJ-20261020-003', 7, 13, '已完成', 4, 2, 0),
    ('XJ-20261018-001', 6, 15, '已完成', 5, 3, 0),
]
trows, recrows = [], []
recdot = dt.datetime(2026, 10, 31, 20, 0, 0)
for i, t in enumerate(TASKS):
    tid = i + 1
    plan_date = (NOW - dt.timedelta(days=t[2])).strftime('%Y-%m-%d')
    route = ROUTES[t[1] - 1]
    total = len(route[9])
    fin = None
    if t[3] == '已完成':
        fin = (NOW - dt.timedelta(days=t[2], hours=1)).strftime('%Y-%m-%d %H:%M:%S')
    trows.append((tid, t[0], t[1], plan_date, route[5][-5:] if route[5][-5:]
                  else '08:00', t[4], t[3], total, t[5], t[6], fin, None,
                  (NOW - dt.timedelta(days=t[2], hours=6)).strftime('%Y-%m-%d %H:%M:%S')))
    done = t[5]
    for s, cno in enumerate(route[9][:done] if done else []):
        abnormal = (s % 4 == 2 and t[6] > 0)
        res = '破损' if abnormal else '完好'
        recrows.append((len(recrows) + 1, tid, cover_id_of[cno], res,
                        '一般' if abnormal else None,
                        '/uploads/seed/issue/%02d.jpg' % (s % 12 + 1) if abnormal else None,
                        '巡检发现缺损，已上报' if abnormal else None,
                        t[4], (NOW - dt.timedelta(days=t[2], hours=3, minutes=35 - s * 7)).strftime('%Y-%m-%d %H:%M:%S')))
w(rows('patrol_task', ['id', 'task_no', 'route_id', 'plan_date', 'plan_start', 'inspector_id', 'task_status',
                       'total_count', 'done_count', 'abnormal_count', 'finish_time', 'remark', 'create_time'], trows))
w(rows('patrol_record', ['id', 'task_id', 'cover_id', 'check_result', 'severity', 'image_url', 'remark',
                         'inspector_id', 'check_time'], recrows))

# ---------------- 备件 ----------------
PARTS = [
    ('SP-001', '球墨铸铁井盖', '井盖', 'Φ700 重型', '套', 42, 20, 680, 'A-01-01', 1),
    ('SP-002', '球墨铸铁井盖', '井盖', 'Φ600 轻型', '套', 8, 15, 420, 'A-01-02', 1),
    ('SP-003', '复合树脂井盖', '井盖', 'Φ700 中型', '套', 26, 10, 380, 'A-01-03', 1),
    ('SP-004', '铸铁井圈', '井圈', 'Φ700', '个', 15, 12, 260, 'A-02-01', 1),
    ('SP-005', '铸铁井圈', '井圈', 'Φ600', '个', 3, 8, 210, 'A-02-02', 1),
    ('SP-006', '混凝土井圈', '井圈', 'Φ700', '个', 22, 10, 180, 'A-02-03', 1),
    ('SP-007', '防坠网', '其他', 'Φ700 尼龙', '张', 40, 15, 95, 'B-01-01', 1),
    ('SP-008', '减震橡胶垫', '其他', 'Φ700', '条', 55, 20, 32, 'B-01-02', 1),
    ('SP-009', '井盖防盗锁', '其他', '通用型', '套', 30, 10, 68, 'B-01-03', 1),
    ('SP-010', '沥青冷补料', '其他', '25kg/袋', '袋', 46, 20, 85, 'B-02-01', 1),
    ('SP-011', '警示锥', '警示物料', '70cm 反光', '个', 64, 30, 35, 'C-01-01', 1),
    ('SP-012', '临时围挡栏', '警示物料', '1.2m 折叠', '个', 18, 20, 120, 'C-01-02', 1),
    ('SP-013', '夜间警示灯', '警示物料', '太阳能', '个', 26, 10, 78, 'C-01-03', 1),
    ('SP-014', '反光背心', '警示物料', '均码', '件', 34, 15, 45, 'C-02-01', 1),
    ('SP-015', '反光警示贴', '警示物料', '50mm×5m', '卷', 36, 15, 18, 'C-02-02', 1),
    ('SP-016', '球墨铸铁井盖', '井盖', 'Φ800 重型', '套', 12, 8, 890, 'A-01-04', 1),
    ('SP-017', '井盖编号喷漆', '其他', '白色 400ml', '罐', 48, 20, 22, 'B-02-02', 1),
    ('SP-018', '水泥砂浆', '其他', '40kg/袋', '袋', 28, 15, 42, 'B-02-03', 0),
]
w(rows('spare_part_stock', ['id', 'part_no', 'part_name', 'category', 'spec', 'unit', 'stock_qty',
                            'warn_qty', 'unit_price', 'location', 'status'],
       [(i + 1,) + p for i, p in enumerate(PARTS)]))

# ---------------- 风险打分（与后端 RiskRuleService 同算法） ----------------
ROAD_PTS = {'主干道': 25, '次干道': 15, '支路': 8}
DMG_PTS = {'丢失': 25, '破损': 22, '井圈破损': 18, '沉降': 16, '松动': 12, '异响': 8}
CROWD_PTS = {'密集': 15, '一般': 9, '稀疏': 4}


def calc(road, damage, repair_cnt, crowd, hours, repeat_cnt, rule_id):
    r = 4 if rule_id == 4 else 3
    wr = {4: (25, 25, 15, 15, 10, 10), 3: (25, 25, 15, 15, 10, 10)}[r]
    rs = round(ROAD_PTS[road] * wr[0] / 25)
    sv = round(DMG_PTS[damage] * wr[1] / 25)
    rp = min(repair_cnt * 4, wr[2])
    cs = round(CROWD_PTS[crowd] * wr[3] / 15)
    ds = min(int(hours // 24) * 2, wr[4])
    rps = min(repeat_cnt * 2, wr[5])
    total = rs + sv + rp + cs + ds + rps
    lvl = '重大' if total >= 71 else ('紧急' if total >= 46 else '一般')
    return rs, sv, rp, cs, ds, rps, total, lvl


ISSUES = [
    # (cover_no, damage, severity, status, days_ago, source, reporter, repair_cnt, repeat_cnt)
    ('JHD-0132', '松动', '一般', '待确认', 0, '巡检发现', 4, 2, 2),
    ('JHD-0115', '异响', '轻微', '待确认', 0, '巡检发现', 7, 1, 1),
    ('JHD-0148', '松动', '一般', '待确认', 1, '巡检发现', 5, 3, 1),
    ('JHD-0129', '沉降', '一般', '待确认', 1, '人工上报', 2, 1, 2),
    ('JHD-0100', '异响', '轻微', '待确认', 1, '巡检发现', 5, 2, 1),
    ('JHD-0142', '破损', '严重', '待派单', 2, '巡检发现', 4, 3, 2),
    ('JHD-0208', '丢失', '严重', '待派单', 0, '人工上报', 6, 1, 1),
    ('JHD-0117', '沉降', '严重', '待派单', 1, '巡检发现', 7, 2, 2),
    ('JHD-0139', '松动', '一般', '待派单', 1, '巡检发现', 4, 1, 1),
    ('JHD-0166', '破损', '一般', '待派单', 2, '巡检发现', 7, 1, 1),
    ('JHD-0121', '异响', '轻微', '待派单', 3, '巡检发现', 6, 0, 1),
    ('JHD-0188', '沉降', '一般', '待派单', 3, '市民热线', 2, 0, 0),
    ('JHD-0106', '松动', '一般', '待派单', 4, '巡检发现', 6, 1, 1),
    ('JHD-0112', '破损', '严重', '待派单', 2, '巡检发现', 5, 2, 2),
    ('JHD-0138', '沉降', '严重', '处理中', 3, '巡检发现', 5, 2, 1),
    ('JHD-0201', '破损', '一般', '处理中', 4, '巡检发现', 4, 1, 1),
    ('JHD-0188', '破损', '一般', '处理中', 5, '人工上报', 7, 0, 1),
    ('JHD-0161', '松动', '一般', '处理中', 5, '巡检发现', 6, 1, 0),
    ('JHD-0129', '破损', '一般', '处理中', 6, '巡检发现', 5, 2, 2),
    ('JHD-0141', '井圈破损', '一般', '处理中', 6, '巡检发现', 4, 1, 1),
    ('JHD-0123', '松动', '轻微', '处理中', 7, '巡检发现', 6, 0, 0),
    ('JHD-0174', '破损', '一般', '处理中', 7, '巡检发现', 4, 1, 1),
    ('JHD-0094', '破损', '严重', '处理中', 8, '市民热线', 2, 3, 3),
    ('JHD-0119', '松动', '一般', '处理中', 9, '巡检发现', 6, 1, 1),
    ('JHD-0121', '松动', '一般', '待验收', 4, '巡检发现', 6, 1, 1),
    ('JHD-0174', '破损', '一般', '待验收', 5, '巡检发现', 4, 2, 1),
    ('JHD-0166', '破损', '一般', '待验收', 5, '巡检发现', 7, 1, 1),
    ('JHD-0128', '沉降', '一般', '待验收', 6, '巡检发现', 5, 1, 0),
    ('JHD-0119', '松动', '轻微', '待验收', 7, '巡检发现', 6, 0, 0),
    ('JHD-0161', '异响', '轻微', '待验收', 8, '巡检发现', 6, 2, 1),
    ('JHD-0141', '破损', '一般', '待验收', 9, '巡检发现', 4, 1, 1),
    ('JHD-0158', '破损', '严重', '返工中', 12, '巡检发现', 6, 3, 2),
    ('JHD-0201', '沉降', '严重', '返工中', 14, '巡检发现', 4, 2, 2),
    ('JHD-0136', '破损', '一般', '返工中', 10, '巡检发现', 4, 1, 1),
    ('JHD-0112', '松动', '一般', '返工中', 11, '巡检发现', 5, 2, 1),
    ('JHD-0094', '破损', '严重', '已关闭', 16, '巡检发现', 7, 2, 2),
    ('JHD-0142', '破损', '一般', '已关闭', 45, '巡检发现', 4, 2, 1),
    ('JHD-0117', '沉降', '一般', '已关闭', 62, '巡检发现', 7, 1, 1),
    ('JHD-0124', '松动', '轻微', '已关闭', 78, '巡检发现', 6, 0, 0),
    ('JHD-0158', '松动', '一般', '已关闭', 96, '巡检发现', 6, 1, 1),
    ('JHD-0132', '松动', '一般', '已关闭', 112, '巡检发现', 4, 2, 1),
    ('JHD-0100', '沉降', '一般', '已关闭', 134, '巡检发现', 5, 1, 1),
    ('JHD-0188', '异响', '轻微', '已关闭', 158, '巡检发现', 7, 0, 0),
    ('JHD-0128', '异响', '轻微', '已取消', 12, '人工上报', 2, 0, 0),
    ('JHD-0106', '异响', '轻微', '已取消', 18, '市民热线', 2, 1, 0),
]
TEAM_BY_ROAD = {'解放东路': ('井盖维修一队', 8, 6), '中山路': ('井盖维修二队', 10, 7),
                '建设南路': ('井盖维修二队', 10, 7), '环城北路': ('排水抢修队', 9, 8),
                '人民西路': ('井盖维修一队', 8, 6)}
ISTATUS_ORDER = {'处理中': '处理中', '待验收': '待验收', '返工中': '返工中', '已关闭': '已完成', '已取消': '已作废'}

irows, srows, orows, arows, mrows, frows, trows2, msgrows = [], [], [], [], [], [], [], []
cover_issues = {}
issue_seq = 0
order_seq = 0
mat_seq = 0
flow_seq = 0
stock = {p[0]: p[5] for p in PARTS}
for idx, it in enumerate(ISSUES):
    cno, damage, severity, status, ago, source, reporter, rep_cnt, rpt_cnt = it
    issue_seq += 1
    cid = cover_id_of[cno]
    cov = COVERS[[c[0] for c in COVERS].index(cno)]
    road, crowd = ROAD_CFG[cov[2]]
    report = NOW - dt.timedelta(days=ago, hours=(idx * 3) % 11, minutes=(idx * 7) % 60)
    hours = max((NOW - report).total_seconds() / 3600, 1)
    # 早期已关闭单使用旧规则版本，体现版本可追溯
    rule_id = 3 if (status == '已关闭' and ago > 90) else 4
    rs, sv, rp, cs, ds, rps, total, lvl = calc(road, damage, rep_cnt, crowd, hours, rpt_cnt, rule_id)
    issue_no = 'WT-%s-%03d' % (report.strftime('%Y%m%d'), issue_seq)
    deadline = report + dt.timedelta(hours={'重大': 4, '紧急': 8, '一般': 24}[lvl])
    close_time = (report + dt.timedelta(days=2, hours=5)).strftime('%Y-%m-%d %H:%M:%S') if status in ('已关闭',) else None
    desc = {'破损': '井盖边角破损约 %dcm，井圈周边出现细裂缝' % (8 + idx % 12),
            '松动': '井盖与井圈间隙约 2cm，车辆碾压时明显跳动',
            '丢失': '井盖缺失，井口裸露，已临时放置警示锥',
            '沉降': '井周路面沉降约 %dcm，井盖与路面形成高差' % (3 + idx % 5),
            '异响': '车辆经过时发出金属撞击异响，疑似井圈松动',
            '井圈破损': '井圈局部断裂，井盖承压不稳'}[damage]
    irows.append((issue_seq, issue_no, cid, source, damage, severity, road, crowd, lvl, total, status,
                  reporter, report.strftime('%Y-%m-%d %H:%M:%S'), desc,
                  '/uploads/seed/issue/%02d.jpg' % (idx % 12 + 1), deadline.strftime('%Y-%m-%d %H:%M:%S'),
                  close_time, 1 if rpt_cnt >= 2 else 0, rpt_cnt, rep_cnt,
                  '重复上报，与既有记录合并' if status == '已取消' else None,
                  0, 0, report.strftime('%Y-%m-%d %H:%M:%S'), (report + dt.timedelta(hours=1)).strftime('%Y-%m-%d %H:%M:%S')))
    cover_issues.setdefault(cid, []).append((report, lvl, status, total))

    # 风险计算记录
    top = sorted([('道路等级', rs, ROAD_PTS[road]), ('缺损严重度', sv, DMG_PTS[damage]),
                  ('历史维修次数', rp, 15), ('人流等级', cs, CROWD_PTS[crowd]),
                  ('未处理时长', ds, 10), ('重复报修次数', rps, 10)], key=lambda x: -x[1])[:2]
    explain = '主要影响因素：%s（%d 分）与 %s（%d 分），合计解释 %.0f%% 的风险。' % (
        top[0][0], top[0][1], top[1][0], top[1][1], (top[0][1] + top[1][1]) * 100.0 / max(total, 1))
    unmet = json.dumps(['未登记临时围挡'] if lvl == '重大' and status != '已关闭' else [], ensure_ascii=False)
    improve = []
    if rep_cnt >= 2:
        improve.append('历史维修 %d 次，建议纳入更换计划' % rep_cnt)
    if rpt_cnt >= 2:
        improve.append('重复报修 %d 次，建议核查井圈基础' % rpt_cnt)
    if hours > 48:
        improve.append('已等待 %d 小时，建议提高处置优先级' % int(hours))
    snap = json.dumps({'coverNo': cno, 'roadLevel': road, 'damageType': damage, 'severity': severity,
                       'repairCount': rep_cnt, 'crowdLevel': crowd, 'waitHours': int(hours),
                       'repeatCount': rpt_cnt}, ensure_ascii=False)
    srows.append((issue_seq, issue_seq, cid, rule_id, 'v1.2' if rule_id == 3 else 'v1.3', snap,
                  rs, sv, rp, cs, ds, rps, total, lvl, explain, unmet,
                  json.dumps(improve, ensure_ascii=False), None, None, None, None,
                  (report + dt.timedelta(minutes=1)).strftime('%Y-%m-%d %H:%M:%S')))

    # 时间轴：登记
    trows2.append((len(trows2) + 1, cid, issue_seq, None, '登记问题', None, status,
                   '%s · %s，风险分 %d（%s）' % (source, damage, total, lvl), reporter,
                   users[reporter - 1][1], report.strftime('%Y-%m-%d %H:%M:%S')))
    trows2.append((len(trows2) + 1, cid, issue_seq, None, '风险分级计算', None, None,
                   '规则 v1.3 得分 %d，等级 %s' % (total, lvl), None, '系统',
                   (report + dt.timedelta(minutes=1)).strftime('%Y-%m-%d %H:%M:%S')))

    if status not in ('待确认', '待派单'):
        order_seq += 1
        team, repairer, tdept = TEAM_BY_ROAD[cov[2]]
        ostatus = ISTATUS_ORDER[status]
        if status == '处理中' and idx % 5 == 0:
            ostatus = '待接单'
        dispatch_t = report + dt.timedelta(hours=1, minutes=25)
        if lvl == '重大':
            arrive_deadline = dispatch_t + dt.timedelta(hours=2)
        elif lvl == '紧急':
            arrive_deadline = dispatch_t + dt.timedelta(hours=4)
        else:
            arrive_deadline = dispatch_t + dt.timedelta(hours=8)
        if ostatus in ('已完成', '待验收', '返工中'):
            arrive = arrive_deadline + dt.timedelta(minutes=(idx % 3) * 55 - 40)
            finish = arrive + dt.timedelta(hours=1, minutes=50 + idx % 40)
        elif ostatus == '处理中':
            arrive = arrive_deadline + dt.timedelta(minutes=(idx % 4) * 30 - 20)
            finish = None
        else:
            arrive = None
            finish = None
        safety = '临时围挡,警示锥,反光标识' if lvl in ('重大', '紧急') else '警示锥'
        plan = {'破损': '更换同规格井盖 1 套，修复井圈基础，路面恢复约 0.6 ㎡',
                '丢失': '补装同规格井盖并加装防盗锁，井口周边加固',
                '沉降': '抬升井圈并重新浇筑基础，路面找平压实',
                '松动': '更换减震橡胶垫，紧固井圈螺栓',
                '异响': '紧固井圈螺栓，加装减震橡胶垫',
                '井圈破损': '更换铸铁井圈，复核井盖承压'}[damage]
        result = '已按方案完成处置，路面恢复，井盖编号标识已喷涂' if ostatus in ('已完成', '待验收', '返工中') else None
        order_no = 'GD-%s-%03d' % (dispatch_t.strftime('%Y%m%d'), order_seq)
        rework = 1 if status == '返工中' else 0
        orows.append((order_seq, order_no, issue_seq, cid, team, repairer, 2, 
                      arrive_deadline.strftime('%Y-%m-%d %H:%M:%S'),
                      arrive.strftime('%Y-%m-%d %H:%M:%S') if arrive else None,
                      finish.strftime('%Y-%m-%d %H:%M:%S') if finish else None,
                      safety, plan, result,
                      '/uploads/seed/issue/%02d.jpg' % (idx % 12 + 1) if arrive else None,
                      '/uploads/seed/order/%02d.jpg' % (idx % 12 + 1) if finish else None,
                      ostatus, 0, rework, 2 if idx % 11 == 0 else 0,
                      '重复上报，与既有记录合并处理' if ostatus == '已作废' else None,
                      0, 0, dispatch_t.strftime('%Y-%m-%d %H:%M:%S'), dispatch_t.strftime('%Y-%m-%d %H:%M:%S')))
        trows2.append((len(trows2) + 1, cid, issue_seq, order_seq, '派发工单', '待派单', '处理中',
                       '派发至%s · %s，要求 %s 前到场' % (team, users[repairer - 1][1],
                                                    arrive_deadline.strftime('%m-%d %H:%M')),
                       2, users[1][1], dispatch_t.strftime('%Y-%m-%d %H:%M:%S')))
        if arrive:
            trows2.append((len(trows2) + 1, cid, issue_seq, order_seq, '登记安全措施', None, None,
                           safety, repairer, users[repairer - 1][1], arrive.strftime('%Y-%m-%d %H:%M:%S')))
        if finish:
            trows2.append((len(trows2) + 1, cid, issue_seq, order_seq, '提交维修结果', '处理中', '待验收',
                           result, repairer, users[repairer - 1][1], finish.strftime('%Y-%m-%d %H:%M:%S')))

        # 备件领用
        part_plan = {'破损': [(1, 1), (4, 1), (11, 2)], '丢失': [(1, 1), (9, 1), (13, 1)],
                     '沉降': [(4, 1), (10, 2), (11, 2)], '松动': [(8, 1), (11, 1)],
                     '异响': [(8, 1)], '井圈破损': [(4, 1), (11, 2)]}[damage]
        if ostatus in ('待验收', '已完成', '返工中', '处理中'):
            for pid, qty in part_plan[:2]:
                mat_seq += 1
                p = PARTS[pid - 1]
                use = qty
                mrows.append((mat_seq, order_seq, pid, p[1], p[3], p[4], qty, use, 0, p[7],
                              repairer, dispatch_t.strftime('%Y-%m-%d %H:%M:%S')))
                stock[p[0]] = max(stock[p[0]] - qty, 0)
            # 流水
            flow_seq += 1
            p = PARTS[part_plan[0][0] - 1]
            frows.append((flow_seq, part_plan[0][0], 'OUT', part_plan[0][1], p[5], p[5] - part_plan[0][1],
                          order_seq, repairer, '工单领用', dispatch_t.strftime('%Y-%m-%d %H:%M:%S')))

        # 验收
        if status == '已关闭':
            arows.append((len(arows) + 1, order_seq, issue_seq, 11, '合格', 1, 1, 1, 1, 95,
                          '现场复查合格，路面平整，编号标识清晰', None,
                          (finish + dt.timedelta(hours=6)).strftime('%Y-%m-%d %H:%M:%S')))
            trows2.append((len(trows2) + 1, cid, issue_seq, order_seq, '验收合格关闭', '待验收', '已关闭',
                           '验收评分 95，井盖与路面高差 3mm', 11, users[10][1],
                           (finish + dt.timedelta(hours=6)).strftime('%Y-%m-%d %H:%M:%S')))
        if status == '返工中':
            arows.append((len(arows) + 1, order_seq, issue_seq, 12, '不合格', 0, 1, 0, 0, 62,
                          '路面恢复未压实，存在 1.5cm 高差', '路面恢复未压实，存在高差，需重新处理',
                          (finish + dt.timedelta(hours=10)).strftime('%Y-%m-%d %H:%M:%S')))
            trows2.append((len(trows2) + 1, cid, issue_seq, order_seq, '验收不合格转返工', '待验收', '返工中',
                           '路面恢复未压实，返工次数 +1', 12, users[11][1],
                           (finish + dt.timedelta(hours=10)).strftime('%Y-%m-%d %H:%M:%S')))

w(rows('cover_issue', ['id', 'issue_no', 'cover_id', 'source_type', 'damage_type', 'severity', 'road_level',
                       'crowd_level', 'risk_level', 'risk_score', 'issue_status', 'reporter_id', 'report_time',
                       'description', 'image_url', 'handle_deadline', 'close_time', 'repeat_flag',
                       'repeat_count', 'repair_count', 'cancel_reason', 'deleted', 'version',
                       'create_time', 'update_time'], irows))
w(rows('risk_score', ['id', 'issue_id', 'cover_id', 'rule_id', 'rule_version', 'input_snapshot', 'road_score',
                      'severity_score', 'repair_score', 'crowd_score', 'duration_score', 'repeat_score',
                      'total_score', 'risk_level', 'explain_text', 'unmet_json', 'improve_json',
                      'adjust_level', 'adjust_reason', 'adjust_user_id', 'adjust_time', 'create_time'], srows))
w(rows('repair_order', ['id', 'order_no', 'issue_id', 'cover_id', 'team_name', 'repairer_id', 'dispatch_user_id',
                        'require_arrive', 'arrive_time', 'finish_time', 'safety_measure', 'plan_desc',
                        'result_desc', 'before_image', 'after_image', 'order_status', 'material_cost',
                        'rework_count', 'urge_count', 'cancel_reason', 'deleted', 'version',
                        'create_time', 'update_time'], orows))
w(rows('repair_material', ['id', 'order_id', 'part_id', 'part_name', 'spec', 'unit', 'apply_qty', 'use_qty',
                           'return_qty', 'unit_price', 'apply_user_id', 'apply_time'], mrows))
w(rows('acceptance_record', ['id', 'order_id', 'issue_id', 'acceptor_id', 'check_result', 'surface_ok',
                             'sign_ok', 'barrier_ok', 'clean_ok', 'score', 'remark', 'rework_reason',
                             'accept_time'], arows))

# 备件补量与入库流水（保证库存与流水方向一致）
for i, p in enumerate(PARTS[:8]):
    flow_seq += 1
    frows.append((flow_seq, i + 1, 'IN', 20, p[5], p[5] + 20, None, 1, '月度补货入库',
                  (NOW - dt.timedelta(days=40 - i * 3)).strftime('%Y-%m-%d %H:%M:%S')))
w(rows('spare_part_flow', ['id', 'part_id', 'flow_type', 'qty', 'before_qty', 'after_qty', 'order_id',
                           'operator_id', 'remark', 'create_time'], frows))

# 回填井盖风险等级与设施状态（取最近一次问题）
cupd = []
for cid, lst in cover_issues.items():
    lst.sort(key=lambda x: x[0])
    latest = lst[-1]
    lvl = latest[1]
    st = {'待确认': '正常', '待派单': '待派单', '处理中': '维修中', '待验收': '待验收',
          '返工中': '维修中', '已关闭': '正常', '已取消': '正常'}[latest[2]]
    if len(lst) > 1 and lvl == '一般':
        lvl = max([x[1] for x in lst[-3:]], key=lambda v: {'重大': 3, '紧急': 2, '一般': 1}[v])
    cupd.append('UPDATE manhole_cover SET risk_level=%s, cover_status=%s WHERE id=%d;' % (q(lvl), q(st), cid))
w('\n-- 回填井盖风险等级 / 设施状态')
w('\n'.join(cupd))
w('UPDATE manhole_cover SET risk_level=%s WHERE risk_level IS NULL;' % q('一般'))

# ---------------- 站内消息 ----------------
msgs = [
    (2, '有 9 条问题待派单', '其中重大 2 条，请优先处置 WT-20261029-004。', 'TODO', 'WT-20261029-004', '/issues', 0, 2),
    (2, '工单 GD-20261028-017 已逾期', '井盖维修一队超过要求到场时间 3 小时。', 'OVERDUE', 'GD-20261028-017', '/orders', 0, 5),
    (2, '问题 WT-20261030-011 风险升级', '系统重算后由紧急升为重大，请复核派单。', 'SYSTEM', 'WT-20261030-011', '/risk/scores', 1, 20),
    (8, '新工单 GD-20261031-008 待接单', 'JHD-0208 井盖丢失，要求 21:30 前到场。', 'TODO', 'GD-20261031-008', '/my/orders', 0, 1),
    (8, '工单 GD-20261030-013 待提交结果', '已登记围挡，请上传维修后照片。', 'TODO', 'GD-20261030-013', '/my/orders', 0, 6),
    (10, '工单 GD-20261029-008 验收不合格', '路面恢复未压实，请重新处理。', 'REJECT', 'GD-20261029-008', '/my/orders', 0, 22),
    (11, '3 条工单待验收', '其中 1 条已等待 28 小时，请尽快复查。', 'ACCEPT', 'GD-20261030-011', '/acceptances', 0, 3),
    (11, '验收超时预警', 'GD-20261028-015 等待验收已超过 48 小时。', 'OVERDUE', 'GD-20261028-015', '/acceptances', 0, 9),
    (4, '任务 XJ-20261031-003 未完成', '剩余 7 个点位，请于 09:40 前完成。', 'TODO', 'XJ-20261031-003', '/my/tasks', 0, 2),
    (4, '巡检任务已生成', '今日 2 条路线，共 24 个点位。', 'SYSTEM', 'XJ-20261031-003', '/my/tasks', 1, 12),
    (5, '井盖 JHD-0138 维修完成待验收', '维修人员已提交结果。', 'SYSTEM', 'GD-20261030-013', '/covers', 0, 4),
    (1, '本月风险模块采纳率 95.2%', '人工调整 9 次，建议复核调整原因分布。', 'SYSTEM', None, '/risk/scores', 0, 8),
]
mrows2 = [(i + 1, m[0], m[1], m[2], m[3], m[4], m[5], m[6],
           (NOW - dt.timedelta(hours=m[7])).strftime('%Y-%m-%d %H:%M:%S')) for i, m in enumerate(msgs)]
w(rows('sys_message', ['id', 'user_id', 'title', 'content', 'msg_type', 'biz_no', 'link', 'read_flag',
                       'create_time'], mrows2))

# ---------------- 时间轴 / 操作日志 ----------------
w(rows('cover_timeline', ['id', 'cover_id', 'issue_id', 'order_id', 'event_type', 'from_status', 'to_status',
                          'content', 'operator_id', 'operator_name', 'create_time'], trows2))
logs = []
acts = [('井盖台账', '新增', 'POST /api/manhole-repair'), ('维修工单', '派发', 'POST /api/manhole-repair/1/transition'),
        ('风险规则', '发布', 'POST /api/risk-rule'), ('巡检任务', '提交', 'POST /api/patrol-task/1/finish'),
        ('备件库存', '领用', 'POST /api/repair-material'), ('验收记录', '验收', 'POST /api/acceptance')]
for i, a in enumerate(acts):
    logs.append((i + 1, [1, 2, 8, 4, 10, 11][i], ['张启明', '李文博', '赵国强', '王大海', '孙磊', '陈默'][i],
                 a[0], a[1], a[2], '{}', '127.0.0.1', 40 + i * 17, 1, None,
                 (NOW - dt.timedelta(hours=3 + i * 7)).strftime('%Y-%m-%d %H:%M:%S')))
w(rows('sys_oper_log', ['id', 'user_id', 'user_name', 'module', 'action', 'method', 'params', 'ip', 'cost_ms',
                        'success', 'error_msg', 'create_time'], logs))

w('\n-- 数据量自检')
w("SELECT '井盖' AS 表, COUNT(*) AS 条数 FROM manhole_cover UNION ALL "
  "SELECT '问题', COUNT(*) FROM cover_issue UNION ALL "
  "SELECT '工单', COUNT(*) FROM repair_order UNION ALL "
  "SELECT '风险计算', COUNT(*) FROM risk_score UNION ALL "
  "SELECT '验收', COUNT(*) FROM acceptance_record UNION ALL "
  "SELECT '巡检记录', COUNT(*) FROM patrol_record;")


print('\n'.join(out))

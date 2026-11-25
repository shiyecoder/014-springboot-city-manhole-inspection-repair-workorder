-- ============================================================
-- 城市井盖巡检与维修工单管理系统  数据库初始化脚本
-- DB: new_014   MySQL 8.0  utf8mb4
-- 说明：脚本可重复执行（先 DROP TABLE），不含 DROP DATABASE
-- ============================================================
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS new_014 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE new_014;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS sys_dept;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_message;
DROP TABLE IF EXISTS sys_dict;
DROP TABLE IF EXISTS sys_file;
DROP TABLE IF EXISTS sys_oper_log;
DROP TABLE IF EXISTS manhole_cover;
DROP TABLE IF EXISTS patrol_route;
DROP TABLE IF EXISTS patrol_route_item;
DROP TABLE IF EXISTS patrol_task;
DROP TABLE IF EXISTS patrol_record;
DROP TABLE IF EXISTS cover_issue;
DROP TABLE IF EXISTS repair_order;
DROP TABLE IF EXISTS repair_material;
DROP TABLE IF EXISTS spare_part_stock;
DROP TABLE IF EXISTS spare_part_flow;
DROP TABLE IF EXISTS acceptance_record;
DROP TABLE IF EXISTS risk_rule;
DROP TABLE IF EXISTS risk_score;
DROP TABLE IF EXISTS cover_timeline;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------- 组织 / 角色 / 用户 ----------------
CREATE TABLE sys_dept (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  dept_name   VARCHAR(80)  NOT NULL COMMENT '部门名称',
  dept_code   VARCHAR(40)  NOT NULL COMMENT '部门编码',
  dept_type   VARCHAR(20)  NOT NULL DEFAULT 'TEAM' COMMENT '类型：DEPT 科室 / TEAM 班组 / UNIT 权属单位',
  leader      VARCHAR(40)  DEFAULT NULL COMMENT '负责人',
  phone       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dept_code (dept_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织部门';

CREATE TABLE sys_role (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_name   VARCHAR(40) NOT NULL COMMENT '角色名称',
  role_code   VARCHAR(40) NOT NULL COMMENT '角色编码',
  data_scope  VARCHAR(20) NOT NULL DEFAULT 'SELF' COMMENT '数据范围 ALL/DEPT/SELF',
  remark      VARCHAR(200) DEFAULT NULL COMMENT '说明',
  status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE sys_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username      VARCHAR(50)  NOT NULL COMMENT '登录名',
  password      VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
  real_name     VARCHAR(40)  NOT NULL COMMENT '姓名',
  phone         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  email         VARCHAR(80)  DEFAULT NULL COMMENT '邮箱',
  avatar        VARCHAR(200) DEFAULT NULL COMMENT '头像相对路径',
  dept_id       BIGINT       DEFAULT NULL COMMENT '部门',
  role_id       BIGINT       NOT NULL COMMENT '角色',
  team_name     VARCHAR(80)  DEFAULT NULL COMMENT '所属队伍（维修人员）',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
  fail_count    INT          NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
  lock_time     DATETIME     DEFAULT NULL COMMENT '锁定截止时间',
  last_login    DATETIME     DEFAULT NULL COMMENT '最近登录时间',
  deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  version       INT          NOT NULL DEFAULT 0 COMMENT '版本号',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  KEY idx_user_role (role_id),
  KEY idx_user_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

CREATE TABLE sys_menu (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '上级菜单',
  menu_name   VARCHAR(40) NOT NULL COMMENT '菜单名称',
  path        VARCHAR(120) DEFAULT NULL COMMENT '路由地址',
  component   VARCHAR(120) DEFAULT NULL COMMENT '组件',
  icon        VARCHAR(40) DEFAULT NULL COMMENT '图标标识',
  menu_type   VARCHAR(10) NOT NULL DEFAULT 'MENU' COMMENT 'DIR 目录 / MENU 菜单 / BUTTON 按钮',
  perm        VARCHAR(80) DEFAULT NULL COMMENT '权限标识',
  sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
  visible     TINYINT     NOT NULL DEFAULT 1 COMMENT '是否显示',
  status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_menu_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单';

CREATE TABLE sys_role_menu (
  role_id BIGINT NOT NULL COMMENT '角色',
  menu_id BIGINT NOT NULL COMMENT '菜单',
  PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单';

CREATE TABLE sys_message (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT       NOT NULL COMMENT '接收人',
  title       VARCHAR(120) NOT NULL COMMENT '标题',
  content     VARCHAR(500) DEFAULT NULL COMMENT '内容',
  msg_type    VARCHAR(20)  NOT NULL DEFAULT 'SYSTEM' COMMENT '类型 TODO/OVERDUE/REJECT/ACCEPT/SYSTEM',
  biz_no      VARCHAR(40)  DEFAULT NULL COMMENT '业务编号',
  link        VARCHAR(200) DEFAULT NULL COMMENT '详情跳转地址',
  read_flag   TINYINT      NOT NULL DEFAULT 0 COMMENT '0 未读 1 已读',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_msg_user (user_id, read_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息';

CREATE TABLE sys_dict (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  dict_type   VARCHAR(40) NOT NULL COMMENT '字典类型',
  dict_label  VARCHAR(40) NOT NULL COMMENT '显示名',
  dict_value  VARCHAR(40) NOT NULL COMMENT '字典值',
  sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
  status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
  PRIMARY KEY (id),
  KEY idx_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典';

CREATE TABLE sys_file (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  biz_type    VARCHAR(30)  NOT NULL COMMENT '业务类型 COVER/ISSUE/ORDER/AVATAR',
  biz_id      BIGINT       DEFAULT NULL COMMENT '业务主键',
  file_name   VARCHAR(200) NOT NULL COMMENT '原始文件名',
  file_path   VARCHAR(300) NOT NULL COMMENT '相对路径',
  file_size   BIGINT       NOT NULL DEFAULT 0 COMMENT '字节',
  file_type   VARCHAR(40)  DEFAULT NULL COMMENT 'MIME',
  uploader_id BIGINT       DEFAULT NULL COMMENT '上传人',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (id),
  KEY idx_file_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件附件';

CREATE TABLE sys_oper_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT       DEFAULT NULL COMMENT '操作人',
  user_name   VARCHAR(40)  DEFAULT NULL COMMENT '操作人姓名',
  module      VARCHAR(40)  DEFAULT NULL COMMENT '模块',
  action      VARCHAR(40)  DEFAULT NULL COMMENT '动作',
  method      VARCHAR(200) DEFAULT NULL COMMENT '方法',
  params      VARCHAR(1000) DEFAULT NULL COMMENT '入参摘要',
  ip          VARCHAR(40)  DEFAULT NULL COMMENT 'IP',
  cost_ms     BIGINT       DEFAULT 0 COMMENT '耗时毫秒',
  success     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否成功',
  error_msg   VARCHAR(500) DEFAULT NULL COMMENT '异常信息',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (id),
  KEY idx_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- ---------------- 井盖主档 ----------------
CREATE TABLE manhole_cover (
  id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  cover_no      VARCHAR(50) NOT NULL COMMENT '井盖编号',
  cover_type    VARCHAR(30) NOT NULL COMMENT '设施类型（字典 cover_type）',
  road_name     VARCHAR(80) NOT NULL COMMENT '道路名称',
  road_level    VARCHAR(20) NOT NULL DEFAULT '次干道' COMMENT '道路等级：主干道/次干道/支路',
  crowd_level   VARCHAR(20) NOT NULL DEFAULT '一般' COMMENT '人流等级：密集/一般/稀疏',
  longitude     DECIMAL(10,6) NOT NULL COMMENT '经度',
  latitude      DECIMAL(10,6) NOT NULL COMMENT '纬度',
  location_desc VARCHAR(200) DEFAULT NULL COMMENT '位置描述',
  owner_unit    VARCHAR(80) NOT NULL COMMENT '权属单位',
  spec          VARCHAR(60)  DEFAULT NULL COMMENT '规格',
  material      VARCHAR(40)  DEFAULT NULL COMMENT '材质',
  install_date  DATE         DEFAULT NULL COMMENT '安装日期',
  duty_user_id  BIGINT       DEFAULT NULL COMMENT '负责人',
  risk_level    VARCHAR(20)  NOT NULL DEFAULT '一般' COMMENT '风险等级：重大/紧急/一般',
  cover_status  VARCHAR(20)  NOT NULL DEFAULT '正常' COMMENT '设施状态：正常/待派单/维修中/待验收/已关闭',
  image_url     VARCHAR(200) DEFAULT NULL COMMENT '现场图片相对路径',
  remark        VARCHAR(300) DEFAULT NULL COMMENT '备注',
  dept_id       BIGINT       DEFAULT NULL COMMENT '所属班组',
  deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  version       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  create_by     BIGINT       DEFAULT NULL COMMENT '创建人',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_cover_no (cover_no),
  KEY idx_cover_road (road_name),
  KEY idx_cover_status (cover_status),
  KEY idx_cover_risk (risk_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='井盖台账';

-- ---------------- 巡检路线 / 任务 ----------------
CREATE TABLE patrol_route (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  route_no    VARCHAR(30) NOT NULL COMMENT '路线编号',
  route_name  VARCHAR(80) NOT NULL COMMENT '路线名称',
  road_name   VARCHAR(80) NOT NULL COMMENT '覆盖道路',
  road_range  VARCHAR(120) DEFAULT NULL COMMENT '路段范围',
  cycle_type  VARCHAR(20) NOT NULL DEFAULT 'WEEK' COMMENT '周期：DAY/WEEK/HALF_MONTH/MONTH',
  cycle_desc  VARCHAR(60) DEFAULT NULL COMMENT '周期描述',
  dept_id     BIGINT      DEFAULT NULL COMMENT '所属班组',
  leader_id   BIGINT      DEFAULT NULL COMMENT '负责巡检员',
  cover_count INT         NOT NULL DEFAULT 0 COMMENT '井盖数量',
  estimate_min INT        NOT NULL DEFAULT 0 COMMENT '预计用时（分钟）',
  status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
  remark      VARCHAR(200) DEFAULT NULL COMMENT '备注',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_route_no (route_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检路线';

CREATE TABLE patrol_route_item (
  id       BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  route_id BIGINT NOT NULL COMMENT '路线',
  cover_id BIGINT NOT NULL COMMENT '井盖',
  seq      INT    NOT NULL DEFAULT 0 COMMENT '点位顺序',
  PRIMARY KEY (id),
  KEY idx_item_route (route_id),
  UNIQUE KEY uk_route_seq (route_id, seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路线点位';

CREATE TABLE patrol_task (
  id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  task_no        VARCHAR(40) NOT NULL COMMENT '任务编号',
  route_id       BIGINT      NOT NULL COMMENT '路线',
  plan_date      DATE        NOT NULL COMMENT '计划日期',
  plan_start     VARCHAR(10) DEFAULT NULL COMMENT '计划开始 HH:mm',
  inspector_id   BIGINT      NOT NULL COMMENT '巡检员',
  task_status    VARCHAR(20) NOT NULL DEFAULT '待执行' COMMENT '待执行/进行中/已完成/已逾期',
  total_count    INT         NOT NULL DEFAULT 0 COMMENT '点位数',
  done_count     INT         NOT NULL DEFAULT 0 COMMENT '已完成点位',
  abnormal_count INT         NOT NULL DEFAULT 0 COMMENT '异常点位数',
  finish_time    DATETIME    DEFAULT NULL COMMENT '完成时间',
  remark         VARCHAR(200) DEFAULT NULL COMMENT '备注',
  create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_no (task_no),
  KEY idx_task_inspector (inspector_id, plan_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检任务';

CREATE TABLE patrol_record (
  id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  task_id      BIGINT      NOT NULL COMMENT '任务',
  cover_id     BIGINT      NOT NULL COMMENT '井盖',
  check_result VARCHAR(20) NOT NULL COMMENT '完好/松动/破损/丢失/沉降/异响',
  severity     VARCHAR(20) DEFAULT NULL COMMENT '轻微/一般/严重',
  image_url    VARCHAR(200) DEFAULT NULL COMMENT '现场图片',
  remark       VARCHAR(300) DEFAULT NULL COMMENT '说明',
  inspector_id BIGINT      NOT NULL COMMENT '巡检员',
  check_time   DATETIME    NOT NULL COMMENT '巡检时间',
  PRIMARY KEY (id),
  KEY idx_rec_task (task_id),
  KEY idx_rec_cover (cover_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检记录';

-- ---------------- 问题上报 ----------------
CREATE TABLE cover_issue (
  id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  issue_no      VARCHAR(40) NOT NULL COMMENT '问题编号',
  cover_id      BIGINT      NOT NULL COMMENT '井盖',
  source_type   VARCHAR(20) NOT NULL DEFAULT '巡检发现' COMMENT '巡检发现/人工上报/市民热线',
  damage_type   VARCHAR(20) NOT NULL COMMENT '缺损类型：破损/松动/丢失/沉降/异响/井圈破损',
  severity      VARCHAR(20) NOT NULL DEFAULT '一般' COMMENT '轻微/一般/严重',
  road_level    VARCHAR(20) DEFAULT NULL COMMENT '计算时道路等级快照',
  crowd_level   VARCHAR(20) DEFAULT NULL COMMENT '计算时人流等级快照',
  risk_level    VARCHAR(20) NOT NULL DEFAULT '一般' COMMENT '重大/紧急/一般',
  risk_score    INT         NOT NULL DEFAULT 0 COMMENT '风险总分快照',
  issue_status  VARCHAR(20) NOT NULL DEFAULT '待确认' COMMENT '待确认/待派单/处理中/待验收/返工中/已关闭/已取消',
  reporter_id   BIGINT      DEFAULT NULL COMMENT '上报人',
  report_time   DATETIME    NOT NULL COMMENT '上报时间',
  description   VARCHAR(500) DEFAULT NULL COMMENT '现场描述',
  image_url     VARCHAR(200) DEFAULT NULL COMMENT '隐患照片',
  handle_deadline DATETIME  DEFAULT NULL COMMENT '处置截止时间',
  close_time    DATETIME    DEFAULT NULL COMMENT '关闭时间',
  repeat_flag   TINYINT     NOT NULL DEFAULT 0 COMMENT '是否复发井盖',
  repeat_count  INT         NOT NULL DEFAULT 0 COMMENT '30 天内重复报修次数',
  repair_count  INT         NOT NULL DEFAULT 0 COMMENT '历史维修次数',
  cancel_reason VARCHAR(300) DEFAULT NULL COMMENT '取消原因',
  deleted       TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  version       INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_issue_no (issue_no),
  KEY idx_issue_cover (cover_id),
  KEY idx_issue_status (issue_status),
  KEY idx_issue_time (report_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='隐患问题上报';

-- ---------------- 维修工单 ----------------
CREATE TABLE repair_order (
  id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_no        VARCHAR(40) NOT NULL COMMENT '工单编号',
  issue_id        BIGINT      NOT NULL COMMENT '关联问题',
  cover_id        BIGINT      NOT NULL COMMENT '井盖',
  team_name       VARCHAR(80) NOT NULL COMMENT '维修队伍',
  repairer_id     BIGINT      NOT NULL COMMENT '维修人员',
  dispatch_user_id BIGINT     DEFAULT NULL COMMENT '派单人',
  require_arrive  DATETIME    NOT NULL COMMENT '要求到场时间',
  arrive_time     DATETIME    DEFAULT NULL COMMENT '实际到场时间',
  finish_time     DATETIME    DEFAULT NULL COMMENT '完成时间',
  safety_measure  VARCHAR(200) DEFAULT NULL COMMENT '安全措施',
  plan_desc       VARCHAR(500) DEFAULT NULL COMMENT '处理方案',
  result_desc     VARCHAR(500) DEFAULT NULL COMMENT '处理结果',
  before_image    VARCHAR(200) DEFAULT NULL COMMENT '维修前照片',
  after_image     VARCHAR(200) DEFAULT NULL COMMENT '维修后照片',
  order_status    VARCHAR(20) NOT NULL DEFAULT '待接单' COMMENT '待接单/处理中/待验收/返工中/已完成/已作废',
  material_cost   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '备件费用',
  rework_count    INT         NOT NULL DEFAULT 0 COMMENT '返工次数',
  urge_count      INT         NOT NULL DEFAULT 0 COMMENT '催办次数',
  cancel_reason   VARCHAR(300) DEFAULT NULL COMMENT '作废原因',
  deleted         TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  version         INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_order_issue (issue_id),
  KEY idx_order_repairer (repairer_id, order_status),
  KEY idx_order_status (order_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维修工单';

CREATE TABLE repair_material (
  id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id     BIGINT      NOT NULL COMMENT '工单',
  part_id      BIGINT      NOT NULL COMMENT '备件',
  part_name    VARCHAR(80) NOT NULL COMMENT '备件名称快照',
  spec         VARCHAR(60) DEFAULT NULL COMMENT '规格快照',
  unit         VARCHAR(10) NOT NULL DEFAULT '件' COMMENT '单位',
  apply_qty    INT         NOT NULL DEFAULT 0 COMMENT '领用数量',
  use_qty      INT         NOT NULL DEFAULT 0 COMMENT '使用数量',
  return_qty   INT         NOT NULL DEFAULT 0 COMMENT '退回数量',
  unit_price   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '单价',
  apply_user_id BIGINT     DEFAULT NULL COMMENT '申请人',
  apply_time   DATETIME    NOT NULL COMMENT '申请时间',
  PRIMARY KEY (id),
  KEY idx_mat_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单备件领用';

CREATE TABLE spare_part_stock (
  id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  part_no    VARCHAR(30) NOT NULL COMMENT '备件编号',
  part_name  VARCHAR(80) NOT NULL COMMENT '备件名称',
  category   VARCHAR(30) NOT NULL COMMENT '类别：井盖/井圈/警示物料/其他',
  spec       VARCHAR(60) DEFAULT NULL COMMENT '规格',
  unit       VARCHAR(10) NOT NULL DEFAULT '件' COMMENT '单位',
  stock_qty  INT         NOT NULL DEFAULT 0 COMMENT '当前库存',
  warn_qty   INT         NOT NULL DEFAULT 0 COMMENT '预警线',
  unit_price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '单价',
  location   VARCHAR(80) DEFAULT NULL COMMENT '库位',
  status     TINYINT     NOT NULL DEFAULT 1 COMMENT '1 在用 0 停用',
  update_time DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_part_no (part_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备件库存';

CREATE TABLE spare_part_flow (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  part_id     BIGINT      NOT NULL COMMENT '备件',
  flow_type   VARCHAR(20) NOT NULL COMMENT 'IN 入库 / OUT 领用 / BACK 退回',
  qty         INT         NOT NULL COMMENT '数量（正数）',
  before_qty  INT         NOT NULL COMMENT '变更前库存',
  after_qty   INT         NOT NULL COMMENT '变更后库存',
  order_id    BIGINT      DEFAULT NULL COMMENT '关联工单',
  operator_id BIGINT      DEFAULT NULL COMMENT '操作人',
  remark      VARCHAR(200) DEFAULT NULL COMMENT '备注',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (id),
  KEY idx_flow_part (part_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备件出入库流水';

CREATE TABLE acceptance_record (
  id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id       BIGINT      NOT NULL COMMENT '工单',
  issue_id       BIGINT      NOT NULL COMMENT '问题',
  acceptor_id    BIGINT      NOT NULL COMMENT '验收人',
  check_result   VARCHAR(20) NOT NULL COMMENT '合格/不合格',
  surface_ok     TINYINT     NOT NULL DEFAULT 0 COMMENT '路面恢复',
  sign_ok        TINYINT     NOT NULL DEFAULT 0 COMMENT '编号标识',
  barrier_ok     TINYINT     NOT NULL DEFAULT 0 COMMENT '围挡撤除',
  clean_ok       TINYINT     NOT NULL DEFAULT 0 COMMENT '周边清洁',
  score          INT         NOT NULL DEFAULT 0 COMMENT '验收评分',
  remark         VARCHAR(300) DEFAULT NULL COMMENT '说明',
  rework_reason  VARCHAR(300) DEFAULT NULL COMMENT '返工原因',
  accept_time    DATETIME    NOT NULL COMMENT '验收时间',
  PRIMARY KEY (id),
  KEY idx_acc_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='验收记录';

-- ---------------- 创新点：风险规则 / 计算记录 ----------------
CREATE TABLE risk_rule (
  id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  rule_version   VARCHAR(20) NOT NULL COMMENT '版本号',
  rule_name      VARCHAR(80) NOT NULL COMMENT '规则名称',
  w_road         INT         NOT NULL COMMENT '道路等级权重（%）',
  w_severity     INT         NOT NULL COMMENT '缺损严重度权重（%）',
  w_repair       INT         NOT NULL COMMENT '历史维修次数权重（%）',
  w_crowd        INT         NOT NULL COMMENT '人流等级权重（%）',
  w_duration     INT         NOT NULL COMMENT '未处理时长权重（%）',
  w_repeat       INT         NOT NULL COMMENT '重复报修次数权重（%）',
  red_threshold  INT         NOT NULL DEFAULT 71 COMMENT '重大下限',
  yellow_threshold INT       NOT NULL DEFAULT 46 COMMENT '紧急下限',
  enabled        TINYINT     NOT NULL DEFAULT 1 COMMENT '是否启用',
  effect_time    DATETIME    NOT NULL COMMENT '生效时间',
  create_by      BIGINT      DEFAULT NULL COMMENT '发布人',
  create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_rule_version (rule_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险分级规则版本';

CREATE TABLE risk_score (
  id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  issue_id       BIGINT      NOT NULL COMMENT '问题',
  cover_id       BIGINT      NOT NULL COMMENT '井盖',
  rule_id        BIGINT      NOT NULL COMMENT '规则',
  rule_version   VARCHAR(20) NOT NULL COMMENT '规则版本快照',
  input_snapshot VARCHAR(1000) NOT NULL COMMENT '输入快照 JSON',
  road_score     INT         NOT NULL DEFAULT 0 COMMENT '道路等级得分',
  severity_score INT         NOT NULL DEFAULT 0 COMMENT '缺损严重度得分',
  repair_score   INT         NOT NULL DEFAULT 0 COMMENT '历史维修得分',
  crowd_score    INT         NOT NULL DEFAULT 0 COMMENT '人流等级得分',
  duration_score INT         NOT NULL DEFAULT 0 COMMENT '未处理时长得分',
  repeat_score   INT         NOT NULL DEFAULT 0 COMMENT '重复报修得分',
  total_score    INT         NOT NULL DEFAULT 0 COMMENT '总分',
  risk_level     VARCHAR(20) NOT NULL COMMENT '重大/紧急/一般',
  explain_text   VARCHAR(600) DEFAULT NULL COMMENT '结果解释',
  unmet_json     VARCHAR(600) DEFAULT NULL COMMENT '未满足条件',
  improve_json   VARCHAR(600) DEFAULT NULL COMMENT '可改进项',
  adjust_level   VARCHAR(20) DEFAULT NULL COMMENT '人工调整后等级',
  adjust_reason  VARCHAR(300) DEFAULT NULL COMMENT '人工调整原因',
  adjust_user_id BIGINT      DEFAULT NULL COMMENT '调整人',
  adjust_time    DATETIME    DEFAULT NULL COMMENT '调整时间',
  create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',
  PRIMARY KEY (id),
  KEY idx_score_issue (issue_id),
  KEY idx_score_cover (cover_id),
  KEY idx_score_rule (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险计算记录';

CREATE TABLE cover_timeline (
  id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  cover_id   BIGINT      NOT NULL COMMENT '井盖',
  issue_id   BIGINT      DEFAULT NULL COMMENT '问题',
  order_id   BIGINT      DEFAULT NULL COMMENT '工单',
  event_type VARCHAR(30) NOT NULL COMMENT '事件类型',
  from_status VARCHAR(20) DEFAULT NULL COMMENT '变更前状态',
  to_status  VARCHAR(20) DEFAULT NULL COMMENT '变更后状态',
  content    VARCHAR(400) DEFAULT NULL COMMENT '说明',
  operator_id BIGINT     DEFAULT NULL COMMENT '操作人',
  operator_name VARCHAR(40) DEFAULT NULL COMMENT '操作人姓名',
  create_time DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (id),
  KEY idx_tl_cover (cover_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务时间轴';

-- 基础索引补充
CREATE INDEX idx_issue_risk ON cover_issue (risk_level, issue_status);
CREATE INDEX idx_order_team ON repair_order (team_name, order_status);

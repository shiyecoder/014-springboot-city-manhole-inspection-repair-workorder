package com.city.manhole.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.city.manhole.common.BizException;
import com.city.manhole.common.LoginUser;
import com.city.manhole.common.PageResult;
import com.city.manhole.common.UserContext;
import com.city.manhole.entity.*;
import com.city.manhole.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 维修工单：派发、接单、登记安全措施、提交结果、作废（退备件）、催办。
 * 硬性规则：重大问题未登记围挡措施不允许提交维修结果；已关闭问题不可删除。
 */
@Service
public class OrderService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RepairOrderMapper orderMapper;
    private final CoverIssueMapper issueMapper;
    private final ManholeCoverMapper coverMapper;
    private final SysUserMapper userMapper;
    private final SysMessageMapper messageMapper;
    private final PartService partService;
    private final CoverService coverService;

    public OrderService(RepairOrderMapper orderMapper, CoverIssueMapper issueMapper, ManholeCoverMapper coverMapper,
                        SysUserMapper userMapper, SysMessageMapper messageMapper, PartService partService,
                        CoverService coverService) {
        this.orderMapper = orderMapper;
        this.issueMapper = issueMapper;
        this.coverMapper = coverMapper;
        this.userMapper = userMapper;
        this.messageMapper = messageMapper;
        this.partService = partService;
        this.coverService = coverService;
    }

    public PageResult<Map<String, Object>> page(long page, long size, String keyword, String status, String team,
                                                Boolean mine, Boolean overdue, Boolean pendingFirst) {
        LoginUser cu = UserContext.require();
        LambdaQueryWrapper<RepairOrderEntity> w = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(mine)) {
            w.eq(RepairOrderEntity::getRepairerId, cu.getId());
        } else if (!cu.isAllScope()) {
            w.eq(RepairOrderEntity::getRepairerId, cu.getId());
        }
        w.eq(status != null && !status.isBlank(), RepairOrderEntity::getOrderStatus, status)
                .eq(team != null && !team.isBlank(), RepairOrderEntity::getTeamName, team)
                .lt(Boolean.TRUE.equals(overdue), RepairOrderEntity::getRequireArrive, LocalDateTime.now())
                .and(keyword != null && !keyword.isBlank(), q -> q.like(RepairOrderEntity::getOrderNo, keyword)
                        .or().like(RepairOrderEntity::getTeamName, keyword));
        if (Boolean.TRUE.equals(pendingFirst) && !Boolean.TRUE.equals(mine)) {
            w.orderByAsc(RepairOrderEntity::getRequireArrive);
        } else {
            w.orderByDesc(RepairOrderEntity::getUpdateTime);
        }
        Page<RepairOrderEntity> p = orderMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> list = new ArrayList<>();
        for (RepairOrderEntity o : p.getRecords()) {
            list.add(view(o));
        }
        return new PageResult<>(p.getTotal(), page, size, list);
    }

    public Map<String, Object> view(RepairOrderEntity o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getId());
        m.put("orderNo", o.getOrderNo());
        m.put("issueId", o.getIssueId());
        m.put("coverId", o.getCoverId());
        m.put("teamName", o.getTeamName());
        m.put("repairerId", o.getRepairerId());
        m.put("requireArrive", o.getRequireArrive());
        m.put("arriveTime", o.getArriveTime());
        m.put("finishTime", o.getFinishTime());
        m.put("safetyMeasure", o.getSafetyMeasure());
        m.put("planDesc", o.getPlanDesc());
        m.put("resultDesc", o.getResultDesc());
        m.put("beforeImage", o.getBeforeImage());
        m.put("afterImage", o.getAfterImage());
        m.put("orderStatus", o.getOrderStatus());
        m.put("materialCost", o.getMaterialCost());
        m.put("reworkCount", o.getReworkCount());
        m.put("urgeCount", o.getUrgeCount());
        m.put("cancelReason", o.getCancelReason());
        m.put("version", o.getVersion());
        m.put("createTime", o.getCreateTime());
        m.put("updateTime", o.getUpdateTime());
        SysUserEntity u = o.getRepairerId() == null ? null : userMapper.selectById(o.getRepairerId());
        m.put("repairerName", u == null ? "" : u.getRealName());
        CoverIssueEntity issue = issueMapper.selectById(o.getIssueId());
        ManholeCoverEntity cover = coverMapper.selectById(o.getCoverId());
        m.put("issueNo", issue == null ? "" : issue.getIssueNo());
        m.put("riskLevel", issue == null ? "" : issue.getRiskLevel());
        m.put("damageType", issue == null ? "" : issue.getDamageType());
        m.put("coverNo", cover == null ? "" : cover.getCoverNo());
        m.put("roadName", cover == null ? "" : cover.getRoadName());
        m.put("coverType", cover == null ? "" : cover.getCoverType());
        m.put("ownerUnit", cover == null ? "" : cover.getOwnerUnit());
        boolean over = o.getArriveTime() == null && o.getRequireArrive() != null
                && o.getRequireArrive().isBefore(LocalDateTime.now())
                && !List.of("已完成", "已作废").contains(o.getOrderStatus());
        m.put("overdue", over);
        m.put("needSafety", issue != null && "重大".equals(issue.getRiskLevel()));
        return m;
    }

    /** 派单：生成工单，问题转处理中，井盖转维修中 */
    @Transactional(rollbackFor = Exception.class)
    public RepairOrderEntity dispatch(Long issueId, String teamName, Long repairerId, String requireArrive,
                                      String safetyMeasure, String planDesc) {
        if (teamName == null || teamName.isBlank()) {
            throw new BizException("请选择维修队伍");
        }
        if (repairerId == null) {
            throw new BizException("请选择维修人员");
        }
        CoverIssueEntity issue = issueMapper.selectById(issueId);
        if (issue == null) {
            throw new BizException("问题不存在");
        }
        if (!List.of("待派单", "待确认").contains(issue.getIssueStatus())) {
            throw new BizException("当前问题状态「" + issue.getIssueStatus() + "」不可派单");
        }
        if ("待确认".equals(issue.getIssueStatus())) {
            throw new BizException("请先确认问题再派单");
        }
        RepairOrderEntity exist = orderMapper.selectOne(new LambdaQueryWrapper<RepairOrderEntity>()
                .eq(RepairOrderEntity::getIssueId, issueId)
                .ne(RepairOrderEntity::getOrderStatus, "已作废").last("limit 1"));
        if (exist != null) {
            throw new BizException("该问题已存在工单 " + exist.getOrderNo());
        }
        LoginUser cu = UserContext.require();
        RepairOrderEntity o = new RepairOrderEntity();
        o.setOrderNo("GD-" + LocalDateTime.now().format(DAY) + "-" + String.format("%03d",
                orderMapper.selectCount(new LambdaQueryWrapper<RepairOrderEntity>()
                        .ge(RepairOrderEntity::getCreateTime, LocalDate.now().atStartOfDay())) + 1));
        o.setIssueId(issueId);
        o.setCoverId(issue.getCoverId());
        o.setTeamName(teamName);
        o.setRepairerId(repairerId);
        o.setDispatchUserId(cu.getId());
        LocalDateTime require = requireArrive == null || requireArrive.isBlank()
                ? LocalDateTime.now().plusHours("重大".equals(issue.getRiskLevel()) ? 4 : 8)
                : LocalDateTime.parse(requireArrive.length() == 16 ? requireArrive + ":00" : requireArrive,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        o.setRequireArrive(require);
        o.setSafetyMeasure(safetyMeasure);
        o.setPlanDesc(planDesc);
        o.setOrderStatus("待接单");
        o.setMaterialCost(java.math.BigDecimal.ZERO);
        o.setReworkCount(0);
        o.setUrgeCount(0);
        o.setDeleted(0);
        o.setVersion(0);
        o.setCreateTime(LocalDateTime.now());
        o.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(o);

        issue.setIssueStatus("处理中");
        issue.setUpdateTime(LocalDateTime.now());
        issueMapper.updateById(issue);
        coverService.updateStatus(issue.getCoverId(), "维修中", null);
        coverService.addTimeline(issue.getCoverId(), issueId, o.getId(), "派发工单", "待派单", "处理中",
                "派发至 " + teamName + "，要求 " + require + " 前到场", cu);
        send(o.getRepairerId(), "新工单 " + o.getOrderNo() + " 待接单",
                "请于 " + require + " 前到场处置，风险等级 " + issue.getRiskLevel(), "TODO", o.getOrderNo(), "/my/orders");
        return o;
    }

    @Transactional(rollbackFor = Exception.class)
    public void accept(Long orderId) {
        RepairOrderEntity o = require(orderId);
        if (!"待接单".equals(o.getOrderStatus())) {
            throw new BizException("只有待接单的工单可以接单");
        }
        o.setOrderStatus("处理中");
        o.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(o);
        coverService.addTimeline(o.getCoverId(), o.getIssueId(), orderId, "接单", "待接单", "处理中",
                "维修人员已接单", UserContext.get());
    }

    /** 登记安全措施，同时视为到场打卡 */
    @Transactional(rollbackFor = Exception.class)
    public void registerSafety(Long orderId, String safety, String beforeImage) {
        if (safety == null || safety.isBlank()) {
            throw new BizException("请至少选择一项安全措施");
        }
        RepairOrderEntity o = require(orderId);
        if (List.of("已完成", "已作废").contains(o.getOrderStatus())) {
            throw new BizException("工单已结束，不能登记安全措施");
        }
        o.setSafetyMeasure(safety);
        if (o.getArriveTime() == null) {
            o.setArriveTime(LocalDateTime.now());
        }
        if (beforeImage != null && !beforeImage.isBlank()) {
            o.setBeforeImage(beforeImage);
        }
        o.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(o);
        coverService.addTimeline(o.getCoverId(), o.getIssueId(), orderId, "登记安全措施", null, null,
                safety, UserContext.get());
    }

    /** 提交维修结果：重大问题必须有围挡措施 */
    @Transactional(rollbackFor = Exception.class)
    public void submitResult(Long orderId, String resultDesc, String afterImage) {
        RepairOrderEntity o = require(orderId);
        CoverIssueEntity issue = issueMapper.selectById(o.getIssueId());
        if (!List.of("处理中", "返工中").contains(o.getOrderStatus())) {
            throw new BizException("当前工单状态「" + o.getOrderStatus() + "」不能提交结果");
        }
        if (issue != null && "重大".equals(issue.getRiskLevel())
                && (o.getSafetyMeasure() == null || o.getSafetyMeasure().isBlank())) {
            throw new BizException("重大风险工单必须登记临时围挡措施后才能提交维修结果");
        }
        if (resultDesc == null || resultDesc.trim().length() < 5) {
            throw new BizException("处理结果说明不少于 5 个字");
        }
        if (afterImage == null || afterImage.isBlank()) {
            throw new BizException("请上传维修后照片");
        }
        o.setResultDesc(resultDesc.trim());
        o.setAfterImage(afterImage);
        o.setFinishTime(LocalDateTime.now());
        o.setOrderStatus("待验收");
        o.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(o);

        if (issue != null) {
            issue.setIssueStatus("待验收");
            issue.setUpdateTime(LocalDateTime.now());
            issueMapper.updateById(issue);
        }
        coverService.updateStatus(o.getCoverId(), "待验收", null);
        coverService.addTimeline(o.getCoverId(), o.getIssueId(), orderId, "提交维修结果", "处理中", "待验收",
                resultDesc, UserContext.get());
    }

    /** 作废：未使用备件自动退回，问题回到待派单 */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, String reason) {
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("作废原因不少于 5 个字");
        }
        RepairOrderEntity o = require(orderId);
        if ("已完成".equals(o.getOrderStatus())) {
            throw new BizException("已完成的工单不能作废");
        }
        int returned = partService.returnUnused(orderId);
        o.setOrderStatus("已作废");
        o.setCancelReason(reason.trim());
        o.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(o);

        CoverIssueEntity issue = issueMapper.selectById(o.getIssueId());
        if (issue != null) {
            issue.setIssueStatus("待派单");
            issue.setUpdateTime(LocalDateTime.now());
            issueMapper.updateById(issue);
            coverService.updateStatus(issue.getCoverId(), "待派单", null);
        }
        coverService.addTimeline(o.getCoverId(), o.getIssueId(), orderId, "作废工单", o.getOrderStatus(), "已作废",
                reason + (returned > 0 ? "，退回备件 " + returned + " 件" : ""), UserContext.get());
    }

    @Transactional(rollbackFor = Exception.class)
    public void urge(Long orderId) {
        RepairOrderEntity o = require(orderId);
        o.setUrgeCount((o.getUrgeCount() == null ? 0 : o.getUrgeCount()) + 1);
        o.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(o);
        send(o.getRepairerId(), "工单 " + o.getOrderNo() + " 催办",
                "调度中心已催办，请尽快到场处置", "OVERDUE", o.getOrderNo(), "/my/orders");
    }

    public Map<String, Object> detail(Long id) {
        RepairOrderEntity o = require(id);
        Map<String, Object> m = view(o);
        m.put("materials", partService.materialsOf(id));
        m.put("timeline", coverService.detail(o.getCoverId()).get("timeline"));
        m.put("issue", issueMapper.selectById(o.getIssueId()));
        return m;
    }

    private RepairOrderEntity require(Long id) {
        RepairOrderEntity o = orderMapper.selectById(id);
        if (o == null) {
            throw new BizException("工单不存在");
        }
        return o;
    }

    private void send(Long userId, String title, String content, String type, String bizNo, String link) {
        if (userId == null) {
            return;
        }
        SysMessageEntity msg = new SysMessageEntity();
        msg.setUserId(userId);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setMsgType(type);
        msg.setBizNo(bizNo);
        msg.setLink(link);
        msg.setReadFlag(0);
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);
    }
}

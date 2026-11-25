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
 * 问题上报：防重、状态流转、取消（必须填原因）、详情。
 * 状态机：待确认→待派单→处理中→待验收→已关闭，验收不合格转返工中
 */
@Service
public class IssueService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Map<String, List<String>> FLOW = Map.of(
            "待确认", List.of("待派单", "已取消"),
            "待派单", List.of("处理中", "已取消"),
            "处理中", List.of("待验收", "已取消"),
            "待验收", List.of("已关闭", "返工中", "已取消"),
            "返工中", List.of("待验收", "已取消"),
            "已关闭", List.of()
    );

    private final CoverIssueMapper issueMapper;
    private final ManholeCoverMapper coverMapper;
    private final RepairOrderMapper orderMapper;
    private final RiskScoreMapper scoreMapper;
    private final SysUserMapper userMapper;
    private final SysMessageMapper messageMapper;
    private final RiskService riskService;
    private final CoverService coverService;

    public IssueService(CoverIssueMapper issueMapper, ManholeCoverMapper coverMapper, RepairOrderMapper orderMapper,
                        RiskScoreMapper scoreMapper, SysUserMapper userMapper, SysMessageMapper messageMapper,
                        RiskService riskService, CoverService coverService) {
        this.issueMapper = issueMapper;
        this.coverMapper = coverMapper;
        this.orderMapper = orderMapper;
        this.scoreMapper = scoreMapper;
        this.userMapper = userMapper;
        this.messageMapper = messageMapper;
        this.riskService = riskService;
        this.coverService = coverService;
    }

    public PageResult<Map<String, Object>> page(long page, long size, String keyword, String damageType,
                                                String riskLevel, String status, String source,
                                                String beginDate, String endDate, boolean mine) {
        LambdaQueryWrapper<CoverIssueEntity> w = new LambdaQueryWrapper<>();
        LoginUser cu = UserContext.require();
        if (mine && !cu.isAllScope()) {
            w.eq(CoverIssueEntity::getReporterId, cu.getId());
        }
        w.eq(damageType != null && !damageType.isBlank(), CoverIssueEntity::getDamageType, damageType)
                .eq(riskLevel != null && !riskLevel.isBlank(), CoverIssueEntity::getRiskLevel, riskLevel)
                .eq(status != null && !status.isBlank(), CoverIssueEntity::getIssueStatus, status)
                .eq(source != null && !source.isBlank(), CoverIssueEntity::getSourceType, source)
                .ge(beginDate != null && !beginDate.isBlank(), CoverIssueEntity::getReportTime,
                        beginDate == null || beginDate.isBlank() ? null : LocalDate.parse(beginDate).atStartOfDay())
                .le(endDate != null && !endDate.isBlank(), CoverIssueEntity::getReportTime,
                        endDate == null || endDate.isBlank() ? null : LocalDate.parse(endDate).atTime(23, 59, 59))
                .and(keyword != null && !keyword.isBlank(), q -> q.like(CoverIssueEntity::getIssueNo, keyword)
                        .or().like(CoverIssueEntity::getDescription, keyword))
                .orderByDesc(CoverIssueEntity::getReportTime);
        Page<CoverIssueEntity> p = issueMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> list = new ArrayList<>();
        for (CoverIssueEntity i : p.getRecords()) {
            list.add(view(i));
        }
        return new PageResult<>(p.getTotal(), page, size, list);
    }

    public Map<String, Object> view(CoverIssueEntity i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("issueNo", i.getIssueNo());
        m.put("coverId", i.getCoverId());
        m.put("sourceType", i.getSourceType());
        m.put("damageType", i.getDamageType());
        m.put("severity", i.getSeverity());
        m.put("riskLevel", i.getRiskLevel());
        m.put("riskScore", i.getRiskScore());
        m.put("issueStatus", i.getIssueStatus());
        m.put("reporterId", i.getReporterId());
        m.put("reportTime", i.getReportTime());
        m.put("description", i.getDescription());
        m.put("imageUrl", i.getImageUrl());
        m.put("handleDeadline", i.getHandleDeadline());
        m.put("closeTime", i.getCloseTime());
        m.put("repeatFlag", i.getRepeatFlag());
        m.put("repeatCount", i.getRepeatCount());
        m.put("repairCount", i.getRepairCount());
        m.put("version", i.getVersion());
        ManholeCoverEntity c = coverMapper.selectById(i.getCoverId());
        m.put("coverNo", c == null ? "" : c.getCoverNo());
        m.put("roadName", c == null ? "" : c.getRoadName());
        m.put("coverType", c == null ? "" : c.getCoverType());
        m.put("longitude", c == null ? null : c.getLongitude());
        m.put("latitude", c == null ? null : c.getLatitude());
        SysUserEntity u = i.getReporterId() == null ? null : userMapper.selectById(i.getReporterId());
        m.put("reporterName", u == null ? "" : u.getRealName());
        RepairOrderEntity order = orderMapper.selectOne(new LambdaQueryWrapper<RepairOrderEntity>()
                .eq(RepairOrderEntity::getIssueId, i.getId())
                .ne(RepairOrderEntity::getOrderStatus, "已作废")
                .orderByDesc(RepairOrderEntity::getId).last("limit 1"));
        m.put("orderId", order == null ? null : order.getId());
        m.put("orderNo", order == null ? null : order.getOrderNo());
        m.put("orderStatus", order == null ? null : order.getOrderStatus());
        boolean overdue = i.getHandleDeadline() != null && i.getHandleDeadline().isBefore(LocalDateTime.now())
                && !"已关闭".equals(i.getIssueStatus()) && !"已取消".equals(i.getIssueStatus());
        m.put("overdue", overdue);
        return m;
    }

    /**
     * 新建问题：同一井盖存在未关闭问题时拒绝，避免重复上报；
     * 提交后立即执行风险分级计算并回写等级与处置时限。
     */
    @Transactional(rollbackFor = Exception.class)
    public CoverIssueEntity create(CoverIssueEntity issue, boolean force) {
        ManholeCoverEntity cover = coverMapper.selectById(issue.getCoverId());
        if (cover == null) {
            throw new BizException("井盖不存在");
        }
        if (issue.getDamageType() == null || issue.getDamageType().isBlank()) {
            throw new BizException("请选择缺损类型");
        }
        if (issue.getDescription() == null || issue.getDescription().trim().length() < 5) {
            throw new BizException("现场描述不少于 5 个字");
        }
        CoverIssueEntity open = issueMapper.selectOne(new LambdaQueryWrapper<CoverIssueEntity>()
                .eq(CoverIssueEntity::getCoverId, issue.getCoverId())
                .in(CoverIssueEntity::getIssueStatus, List.of("待确认", "待派单", "处理中", "待验收", "返工中"))
                .orderByDesc(CoverIssueEntity::getReportTime).last("limit 1"));
        if (open != null && !force) {
            throw new BizException(409, "该井盖已有未关闭问题 " + open.getIssueNo() + "（" + open.getIssueStatus()
                    + "），同一井盖重复故障请关闭后再新建");
        }
        LoginUser cu = UserContext.require();
        String no = "WT-" + LocalDateTime.now().format(DAY) + "-" + String.format("%03d",
                (issueMapper.selectCount(new LambdaQueryWrapper<CoverIssueEntity>()
                        .ge(CoverIssueEntity::getReportTime, LocalDate.now().atStartOfDay())) + 1));
        issue.setIssueNo(no);
        issue.setReporterId(cu.getId());
        issue.setReportTime(LocalDateTime.now());
        issue.setIssueStatus("待确认");
        issue.setRiskLevel("一般");
        issue.setRiskScore(0);
        issue.setSeverity(issue.getSeverity() == null ? "一般" : issue.getSeverity());
        issue.setSourceType(issue.getSourceType() == null ? "巡检发现" : issue.getSourceType());
        issue.setDeleted(0);
        issue.setVersion(0);
        issue.setCreateTime(LocalDateTime.now());
        issue.setUpdateTime(LocalDateTime.now());
        issueMapper.insert(issue);
        coverService.addTimeline(cover.getId(), issue.getId(), null, "登记问题", null, "待确认",
                issue.getSourceType() + " · " + issue.getDamageType() + "：" + issue.getDescription(), cu);
        // 立即计算风险等级（创新点：每次上报都留痕）
        riskService.evaluate(issue.getId());
        coverService.updateStatus(cover.getId(), "待派单", null);
        return issueMapper.selectById(issue.getId());
    }

    public Map<String, Object> detail(Long id) {
        CoverIssueEntity issue = issueMapper.selectById(id);
        if (issue == null) {
            throw new BizException("问题不存在");
        }
        Map<String, Object> m = view(issue);
        m.put("timeline", coverService.detail(issue.getCoverId()).get("timeline"));
        m.put("scores", scoreMapper.selectList(new LambdaQueryWrapper<RiskScoreEntity>()
                .eq(RiskScoreEntity::getIssueId, id).orderByDesc(RiskScoreEntity::getCreateTime)));
        m.put("order", orderMapper.selectOne(new LambdaQueryWrapper<RepairOrderEntity>()
                .eq(RepairOrderEntity::getIssueId, id).orderByDesc(RepairOrderEntity::getId).last("limit 1")));
        return m;
    }

    /** 通用流转：校验当前状态是否允许目标状态 */
    @Transactional(rollbackFor = Exception.class)
    public void transition(Long id, String target, String reason) {
        CoverIssueEntity issue = issueMapper.selectById(id);
        if (issue == null) {
            throw new BizException("问题不存在");
        }
        if (issue.getVersion() != null) {
            issue.setVersion(issue.getVersion());
        }
        List<String> allowed = FLOW.getOrDefault(issue.getIssueStatus(), List.of());
        if (!allowed.contains(target)) {
            throw new BizException("当前状态「" + issue.getIssueStatus() + "」不允许流转到「" + target + "」");
        }
        if (List.of("已取消", "返工中").contains(target) && (reason == null || reason.trim().length() < 5)) {
            throw new BizException("该操作必须填写原因，且不少于 5 个字");
        }
        String from = issue.getIssueStatus();
        issue.setIssueStatus(target);
        if ("已关闭".equals(target)) {
            issue.setCloseTime(LocalDateTime.now());
        }
        if ("已取消".equals(target)) {
            issue.setCancelReason(reason.trim());
        }
        issue.setUpdateTime(LocalDateTime.now());
        issueMapper.updateById(issue);
        coverService.addTimeline(issue.getCoverId(), id, null, "问题流转", from, target,
                reason == null ? "状态流转" : reason, UserContext.get());
        notifyReporter(issue, target);
    }

    private void notifyReporter(CoverIssueEntity issue, String target) {
        if (issue.getReporterId() == null) {
            return;
        }
        SysMessageEntity msg = new SysMessageEntity();
        msg.setUserId(issue.getReporterId());
        msg.setTitle("问题 " + issue.getIssueNo() + " 状态更新为 " + target);
        msg.setContent("缺损类型：" + issue.getDamageType() + "，风险等级：" + issue.getRiskLevel());
        msg.setMsgType("SYSTEM");
        msg.setBizNo(issue.getIssueNo());
        msg.setLink("/issues");
        msg.setReadFlag(0);
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);
    }

    /** 快捷确认：待确认 → 待派单 */
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        transition(id, "待派单", null);
    }
}

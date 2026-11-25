package com.city.manhole.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.city.manhole.common.BizException;
import com.city.manhole.common.PageResult;
import com.city.manhole.common.UserContext;
import com.city.manhole.entity.*;
import com.city.manhole.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 验收：复查处理结果、路面恢复与编号标识。合格则关闭问题，不合格转返工并累计返工次数。
 */
@Service
public class AcceptanceService {

    private final AcceptanceRecordMapper recordMapper;
    private final RepairOrderMapper orderMapper;
    private final CoverIssueMapper issueMapper;
    private final CoverService coverService;
    private final SysMessageMapper messageMapper;

    public AcceptanceService(AcceptanceRecordMapper recordMapper, RepairOrderMapper orderMapper,
                             CoverIssueMapper issueMapper, CoverService coverService, SysMessageMapper messageMapper) {
        this.recordMapper = recordMapper;
        this.orderMapper = orderMapper;
        this.issueMapper = issueMapper;
        this.coverService = coverService;
        this.messageMapper = messageMapper;
    }

    public PageResult<Map<String, Object>> page(long page, long size, String status, String keyword, Boolean pending) {
        LambdaQueryWrapper<RepairOrderEntity> w = new LambdaQueryWrapper<RepairOrderEntity>()
                .eq(status != null && !status.isBlank(), RepairOrderEntity::getOrderStatus, status)
                .eq(Boolean.TRUE.equals(pending), RepairOrderEntity::getOrderStatus, "待验收")
                .and(keyword != null && !keyword.isBlank(), q -> q.like(RepairOrderEntity::getOrderNo, keyword)
                        .or().like(RepairOrderEntity::getTeamName, keyword))
                .orderByAsc(RepairOrderEntity::getFinishTime);
        Page<RepairOrderEntity> p = orderMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> list = new ArrayList<>();
        for (RepairOrderEntity o : p.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderId", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("coverId", o.getCoverId());
            m.put("teamName", o.getTeamName());
            m.put("finishTime", o.getFinishTime());
            m.put("orderStatus", o.getOrderStatus());
            m.put("reworkCount", o.getReworkCount());
            m.put("beforeImage", o.getBeforeImage());
            m.put("afterImage", o.getAfterImage());
            m.put("resultDesc", o.getResultDesc());
            CoverIssueEntity issue = issueMapper.selectById(o.getIssueId());
            m.put("issueId", o.getIssueId());
            m.put("issueNo", issue == null ? "" : issue.getIssueNo());
            m.put("riskLevel", issue == null ? "" : issue.getRiskLevel());
            m.put("damageType", issue == null ? "" : issue.getDamageType());
            m.put("coverNo", issue == null ? "" : coverService.detail(issue.getCoverId()).get("coverNo"));
            m.put("roadName", issue == null ? "" : coverService.detail(issue.getCoverId()).get("roadName"));
            if (o.getFinishTime() != null) {
                long hours = java.time.Duration.between(o.getFinishTime(), LocalDateTime.now()).toHours();
                m.put("waitHours", hours);
            }
            list.add(m);
        }
        return new PageResult<>(p.getTotal(), page, size, list);
    }

    /** 提交验收 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submit(Long orderId, boolean pass, boolean surfaceOk, boolean signOk,
                                      boolean barrierOk, boolean cleanOk, Integer score, String remark) {
        RepairOrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("工单不存在");
        }
        if (!"待验收".equals(order.getOrderStatus())) {
            throw new BizException("当前工单状态「" + order.getOrderStatus() + "」不可验收");
        }
        if (!pass && (remark == null || remark.trim().length() < 5)) {
            throw new BizException("判定返工必须填写不合格原因，且不少于 5 个字");
        }
        CoverIssueEntity issue = issueMapper.selectById(order.getIssueId());

        AcceptanceRecordEntity r = new AcceptanceRecordEntity();
        r.setOrderId(orderId);
        r.setIssueId(order.getIssueId());
        r.setAcceptorId(UserContext.id());
        r.setCheckResult(pass ? "合格" : "不合格");
        r.setSurfaceOk(surfaceOk ? 1 : 0);
        r.setSignOk(signOk ? 1 : 0);
        r.setBarrierOk(barrierOk ? 1 : 0);
        r.setCleanOk(cleanOk ? 1 : 0);
        r.setScore(score == null ? (pass ? 95 : 60) : score);
        r.setRemark(remark);
        r.setReworkReason(pass ? null : remark.trim());
        r.setAcceptTime(LocalDateTime.now());
        recordMapper.insert(r);

        if (pass) {
            order.setOrderStatus("已完成");
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
            if (issue != null) {
                issue.setIssueStatus("已关闭");
                issue.setCloseTime(LocalDateTime.now());
                issue.setUpdateTime(LocalDateTime.now());
                issueMapper.updateById(issue);
            }
            coverService.updateStatus(order.getCoverId(), "正常", "一般");
            coverService.addTimeline(order.getCoverId(), order.getIssueId(), orderId, "验收合格关闭",
                    "待验收", "已关闭", "验收评分 " + r.getScore() + "，问题关闭", UserContext.get());
        } else {
            order.setOrderStatus("返工中");
            order.setReworkCount((order.getReworkCount() == null ? 0 : order.getReworkCount()) + 1);
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
            if (issue != null) {
                issue.setIssueStatus("返工中");
                issue.setUpdateTime(LocalDateTime.now());
                issueMapper.updateById(issue);
            }
            coverService.updateStatus(order.getCoverId(), "维修中", null);
            coverService.addTimeline(order.getCoverId(), order.getIssueId(), orderId, "验收不合格转返工",
                    "待验收", "返工中", remark + "，返工次数 +1", UserContext.get());
            SysMessageEntity msg = new SysMessageEntity();
            msg.setUserId(order.getRepairerId());
            msg.setTitle("工单 " + order.getOrderNo() + " 验收不合格");
            msg.setContent(remark);
            msg.setMsgType("REJECT");
            msg.setBizNo(order.getOrderNo());
            msg.setLink("/my/orders");
            msg.setReadFlag(0);
            msg.setCreateTime(LocalDateTime.now());
            messageMapper.insert(msg);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("result", pass ? "合格" : "不合格");
        res.put("orderStatus", order.getOrderStatus());
        res.put("reworkCount", order.getReworkCount());
        return res;
    }

    public List<AcceptanceRecordEntity> records(Long orderId) {
        return recordMapper.selectList(new LambdaQueryWrapper<AcceptanceRecordEntity>()
                .eq(AcceptanceRecordEntity::getOrderId, orderId).orderByDesc(AcceptanceRecordEntity::getAcceptTime));
    }
}

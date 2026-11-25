package com.city.manhole.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.city.manhole.common.BizException;
import com.city.manhole.common.LoginUser;
import com.city.manhole.common.PageResult;
import com.city.manhole.common.UserContext;
import com.city.manhole.entity.*;
import com.city.manhole.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 井盖台账：分页检索、详情（关联单据 + 时间轴）、新增编辑（编号唯一 + 版本校验）
 */
@Service
public class CoverService {

    private final ManholeCoverMapper coverMapper;
    private final CoverIssueMapper issueMapper;
    private final RepairOrderMapper orderMapper;
    private final CoverTimelineMapper timelineMapper;
    private final SysFileMapper fileMapper;
    private final SysUserMapper userMapper;
    private final PatrolRecordMapper recordMapper;

    public CoverService(ManholeCoverMapper coverMapper, CoverIssueMapper issueMapper, RepairOrderMapper orderMapper,
                        CoverTimelineMapper timelineMapper, SysFileMapper fileMapper, SysUserMapper userMapper,
                        PatrolRecordMapper recordMapper) {
        this.coverMapper = coverMapper;
        this.issueMapper = issueMapper;
        this.orderMapper = orderMapper;
        this.timelineMapper = timelineMapper;
        this.fileMapper = fileMapper;
        this.userMapper = userMapper;
        this.recordMapper = recordMapper;
    }

    public PageResult<Map<String, Object>> page(long page, long size, String road, String coverType, String ownerUnit,
                                                String riskLevel, String coverStatus, String keyword,
                                                String lastPatrolDate) {
        LambdaQueryWrapper<ManholeCoverEntity> w = new LambdaQueryWrapper<ManholeCoverEntity>()
                .like(road != null && !road.isBlank(), ManholeCoverEntity::getRoadName, road)
                .eq(coverType != null && !coverType.isBlank(), ManholeCoverEntity::getCoverType, coverType)
                .eq(ownerUnit != null && !ownerUnit.isBlank(), ManholeCoverEntity::getOwnerUnit, ownerUnit)
                .eq(riskLevel != null && !riskLevel.isBlank(), ManholeCoverEntity::getRiskLevel, riskLevel)
                .eq(coverStatus != null && !coverStatus.isBlank(), ManholeCoverEntity::getCoverStatus, coverStatus)
                .and(keyword != null && !keyword.isBlank(), q -> q
                        .like(ManholeCoverEntity::getCoverNo, keyword)
                        .or().like(ManholeCoverEntity::getRoadName, keyword)
                        .or().like(ManholeCoverEntity::getLocationDesc, keyword))
                .orderByDesc(ManholeCoverEntity::getUpdateTime);
        Page<ManholeCoverEntity> p = coverMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> list = new ArrayList<>();
        for (ManholeCoverEntity c : p.getRecords()) {
            list.add(view(c));
        }
        return new PageResult<>(p.getTotal(), page, size, list);
    }

    public Map<String, Object> view(ManholeCoverEntity c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("coverNo", c.getCoverNo());
        m.put("coverType", c.getCoverType());
        m.put("roadName", c.getRoadName());
        m.put("roadLevel", c.getRoadLevel());
        m.put("crowdLevel", c.getCrowdLevel());
        m.put("longitude", c.getLongitude());
        m.put("latitude", c.getLatitude());
        m.put("locationDesc", c.getLocationDesc());
        m.put("ownerUnit", c.getOwnerUnit());
        m.put("spec", c.getSpec());
        m.put("material", c.getMaterial());
        m.put("installDate", c.getInstallDate());
        m.put("dutyUserId", c.getDutyUserId());
        m.put("riskLevel", c.getRiskLevel());
        m.put("coverStatus", c.getCoverStatus());
        m.put("imageUrl", c.getImageUrl());
        m.put("remark", c.getRemark());
        m.put("version", c.getVersion());
        m.put("createTime", c.getCreateTime());
        m.put("updateTime", c.getUpdateTime());
        SysUserEntity u = c.getDutyUserId() == null ? null : userMapper.selectById(c.getDutyUserId());
        m.put("dutyUserName", u == null ? "" : u.getRealName());
        return m;
    }

    /** 详情：基础信息 + 关联问题/工单 + 时间轴 + 附件 + 最近巡检 */
    public Map<String, Object> detail(Long id) {
        ManholeCoverEntity cover = coverMapper.selectById(id);
        if (cover == null) {
            throw new BizException("井盖不存在");
        }
        Map<String, Object> m = view(cover);
        m.put("issues", issueMapper.selectList(new LambdaQueryWrapper<CoverIssueEntity>()
                .eq(CoverIssueEntity::getCoverId, id).orderByDesc(CoverIssueEntity::getReportTime).last("limit 10")));
        m.put("orders", orderMapper.selectList(new LambdaQueryWrapper<RepairOrderEntity>()
                .eq(RepairOrderEntity::getCoverId, id).orderByDesc(RepairOrderEntity::getCreateTime).last("limit 10")));
        m.put("timeline", timelineMapper.selectList(new LambdaQueryWrapper<CoverTimelineEntity>()
                .eq(CoverTimelineEntity::getCoverId, id).orderByDesc(CoverTimelineEntity::getCreateTime).last("limit 30")));
        m.put("files", fileMapper.selectList(new LambdaQueryWrapper<SysFileEntity>()
                .eq(SysFileEntity::getBizType, "COVER").eq(SysFileEntity::getBizId, id)));
        m.put("recentPatrol", recordMapper.selectList(new LambdaQueryWrapper<PatrolRecordEntity>()
                .eq(PatrolRecordEntity::getCoverId, id).orderByDesc(PatrolRecordEntity::getCheckTime).last("limit 5")));
        Long repairCount = orderMapper.selectCount(new LambdaQueryWrapper<RepairOrderEntity>()
                .eq(RepairOrderEntity::getCoverId, id).in(RepairOrderEntity::getOrderStatus, List.of("待验收", "已完成", "返工中")));
        m.put("repairCount", repairCount);
        return m;
    }

    @Transactional(rollbackFor = Exception.class)
    public ManholeCoverEntity save(ManholeCoverEntity cover) {
        if (cover.getCoverNo() == null || cover.getCoverNo().isBlank()) {
            throw new BizException("井盖编号不能为空");
        }
        cover.setCoverNo(cover.getCoverNo().trim());
        if (cover.getCoverNo().length() > 50) {
            throw new BizException("井盖编号长度不能超过 50 个字符");
        }
        if (cover.getRoadName() == null || cover.getRoadName().isBlank()) {
            throw new BizException("道路名称不能为空");
        }
        if (cover.getLongitude() == null || cover.getLatitude() == null) {
            throw new BizException("坐标位置不能为空");
        }
        if (cover.getOwnerUnit() == null || cover.getOwnerUnit().isBlank()) {
            throw new BizException("权属单位不能为空");
        }
        if (cover.getInstallDate() != null && cover.getInstallDate().isAfter(java.time.LocalDate.now())) {
            throw new BizException("安装日期不能晚于今天");
        }
        Long dup = coverMapper.selectCount(new LambdaQueryWrapper<ManholeCoverEntity>()
                .eq(ManholeCoverEntity::getCoverNo, cover.getCoverNo())
                .ne(cover.getId() != null, ManholeCoverEntity::getId, cover.getId()));
        if (dup != null && dup > 0) {
            throw new BizException("井盖编号已存在：" + cover.getCoverNo());
        }
        LoginUser cu = UserContext.require();
        if (cover.getId() == null) {
            cover.setCreateBy(cu.getId());
            cover.setCreateTime(LocalDateTime.now());
            cover.setVersion(0);
            cover.setRiskLevel(cover.getRiskLevel() == null ? "一般" : cover.getRiskLevel());
            cover.setCoverStatus(cover.getCoverStatus() == null ? "正常" : cover.getCoverStatus());
            coverMapper.insert(cover);
            addTimeline(cover.getId(), null, null, "新建井盖", null, null,
                    "登记井盖档案 " + cover.getCoverNo(), cu);
        } else {
            ManholeCoverEntity old = coverMapper.selectById(cover.getId());
            if (old == null) {
                throw new BizException("井盖不存在");
            }
            if (cover.getVersion() != null && !cover.getVersion().equals(old.getVersion())) {
                throw new BizException("该记录已被他人更新，请刷新后重试");
            }
            cover.setVersion(old.getVersion() + 1);
            coverMapper.updateById(cover);
            addTimeline(cover.getId(), null, null, "编辑井盖", null, null,
                    "更新井盖档案 " + cover.getCoverNo(), cu);
        }
        return cover;
    }

    /** 删除前检查是否被业务单据引用 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long issues = issueMapper.selectCount(new LambdaQueryWrapper<CoverIssueEntity>()
                .eq(CoverIssueEntity::getCoverId, id).ne(CoverIssueEntity::getIssueStatus, "已关闭"));
        if (issues != null && issues > 0) {
            throw new BizException("该井盖存在未关闭的问题记录，不能删除");
        }
        Long orders = orderMapper.selectCount(new LambdaQueryWrapper<RepairOrderEntity>()
                .eq(RepairOrderEntity::getCoverId, id).ne(RepairOrderEntity::getOrderStatus, "已完成"));
        if (orders != null && orders > 0) {
            throw new BizException("该井盖存在未完成工单，不能删除");
        }
        coverMapper.deleteById(id);
    }

    public void addTimeline(Long coverId, Long issueId, Long orderId, String type, String from, String to,
                            String content, LoginUser cu) {
        CoverTimelineEntity t = new CoverTimelineEntity();
        t.setCoverId(coverId);
        t.setIssueId(issueId);
        t.setOrderId(orderId);
        t.setEventType(type);
        t.setFromStatus(from);
        t.setToStatus(to);
        t.setContent(content);
        t.setOperatorId(cu == null ? null : cu.getId());
        t.setOperatorName(cu == null ? "系统" : cu.getRealName());
        t.setCreateTime(LocalDateTime.now());
        timelineMapper.insert(t);
    }

    /** 更新设施状态（内部调用，避免整对象覆盖） */
    public void updateStatus(Long coverId, String status, String riskLevel) {
        LambdaUpdateWrapper<ManholeCoverEntity> w = new LambdaUpdateWrapper<ManholeCoverEntity>()
                .eq(ManholeCoverEntity::getId, coverId)
                .set(ManholeCoverEntity::getCoverStatus, status)
                .set(ManholeCoverEntity::getUpdateTime, LocalDateTime.now());
        if (riskLevel != null) {
            w.set(ManholeCoverEntity::getRiskLevel, riskLevel);
        }
        coverMapper.update(null, w);
    }

    public List<ManholeCoverEntity> options(String keyword) {
        return coverMapper.selectList(new LambdaQueryWrapper<ManholeCoverEntity>()
                .and(keyword != null && !keyword.isBlank(), q -> q.like(ManholeCoverEntity::getCoverNo, keyword)
                        .or().like(ManholeCoverEntity::getRoadName, keyword))
                .orderByAsc(ManholeCoverEntity::getCoverNo).last("limit 50"));
    }
}

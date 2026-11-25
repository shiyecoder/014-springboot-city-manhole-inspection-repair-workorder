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
 * 巡检路线与任务：按路线组点、按周期批量生成任务、逐点登记、汇总完成情况
 */
@Service
public class PatrolService {

    private final PatrolRouteMapper routeMapper;
    private final PatrolRouteItemMapper itemMapper;
    private final PatrolTaskMapper taskMapper;
    private final PatrolRecordMapper recordMapper;
    private final ManholeCoverMapper coverMapper;
    private final SysUserMapper userMapper;
    private final IssueService issueService;

    public PatrolService(PatrolRouteMapper routeMapper, PatrolRouteItemMapper itemMapper, PatrolTaskMapper taskMapper,
                         PatrolRecordMapper recordMapper, ManholeCoverMapper coverMapper, SysUserMapper userMapper,
                         IssueService issueService) {
        this.routeMapper = routeMapper;
        this.itemMapper = itemMapper;
        this.taskMapper = taskMapper;
        this.recordMapper = recordMapper;
        this.coverMapper = coverMapper;
        this.userMapper = userMapper;
        this.issueService = issueService;
    }

    public PageResult<Map<String, Object>> routes(long page, long size, String keyword, String cycleType) {
        LambdaQueryWrapper<PatrolRouteEntity> w = new LambdaQueryWrapper<PatrolRouteEntity>()
                .and(keyword != null && !keyword.isBlank(), q -> q.like(PatrolRouteEntity::getRouteName, keyword)
                        .or().like(PatrolRouteEntity::getRoadName, keyword))
                .eq(cycleType != null && !cycleType.isBlank(), PatrolRouteEntity::getCycleType, cycleType)
                .orderByAsc(PatrolRouteEntity::getRouteNo);
        Page<PatrolRouteEntity> p = routeMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> list = new ArrayList<>();
        for (PatrolRouteEntity r : p.getRecords()) {
            list.add(routeView(r));
        }
        return new PageResult<>(p.getTotal(), page, size, list);
    }

    public Map<String, Object> routeView(PatrolRouteEntity r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("routeNo", r.getRouteNo());
        m.put("routeName", r.getRouteName());
        m.put("roadName", r.getRoadName());
        m.put("roadRange", r.getRoadRange());
        m.put("cycleType", r.getCycleType());
        m.put("cycleDesc", r.getCycleDesc());
        m.put("coverCount", r.getCoverCount());
        m.put("estimateMin", r.getEstimateMin());
        m.put("status", r.getStatus());
        m.put("leaderId", r.getLeaderId());
        SysUserEntity u = r.getLeaderId() == null ? null : userMapper.selectById(r.getLeaderId());
        m.put("leaderName", u == null ? "" : u.getRealName());
        List<PatrolRouteItemEntity> items = itemMapper.selectList(new LambdaQueryWrapper<PatrolRouteItemEntity>()
                .eq(PatrolRouteItemEntity::getRouteId, r.getId()).orderByAsc(PatrolRouteItemEntity::getSeq));
        List<Map<String, Object>> points = new ArrayList<>();
        for (PatrolRouteItemEntity it : items) {
            ManholeCoverEntity c = coverMapper.selectById(it.getCoverId());
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("seq", it.getSeq());
            pm.put("coverId", it.getCoverId());
            pm.put("coverNo", c == null ? "" : c.getCoverNo());
            pm.put("roadName", c == null ? "" : c.getRoadName());
            pm.put("coverType", c == null ? "" : c.getCoverType());
            pm.put("riskLevel", c == null ? "" : c.getRiskLevel());
            points.add(pm);
        }
        m.put("points", points);
        return m;
    }

    @Transactional(rollbackFor = Exception.class)
    public int generateTasks(String planDate, Long routeId) {
        LocalDate date = planDate == null || planDate.isBlank() ? LocalDate.now() : LocalDate.parse(planDate);
        List<PatrolRouteEntity> routes = routeMapper.selectList(new LambdaQueryWrapper<PatrolRouteEntity>()
                .eq(PatrolRouteEntity::getStatus, 1)
                .eq(routeId != null, PatrolRouteEntity::getId, routeId));
        int created = 0;
        for (PatrolRouteEntity r : routes) {
            Long exists = taskMapper.selectCount(new LambdaQueryWrapper<PatrolTaskEntity>()
                    .eq(PatrolTaskEntity::getRouteId, r.getId())
                    .eq(PatrolTaskEntity::getPlanDate, date));
            if (exists != null && exists > 0) {
                continue;
            }
            PatrolTaskEntity t = new PatrolTaskEntity();
            t.setTaskNo("XJ-" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%03d", created + 1));
            t.setRouteId(r.getId());
            t.setPlanDate(date);
            t.setPlanStart(r.getCycleDesc());
            t.setInspectorId(r.getLeaderId());
            t.setTaskStatus("待执行");
            t.setTotalCount(r.getCoverCount());
            t.setDoneCount(0);
            t.setAbnormalCount(0);
            t.setCreateTime(LocalDateTime.now());
            t.setUpdateTime(LocalDateTime.now());
            taskMapper.insert(t);
            created++;
        }
        if (created == 0) {
            throw new BizException("所选日期已生成过巡检任务，无需重复生成");
        }
        return created;
    }

    public PageResult<Map<String, Object>> tasks(long page, long size, String status, String keyword, Boolean mine,
                                                 String planDate) {
        LoginUser cu = UserContext.require();
        LambdaQueryWrapper<PatrolTaskEntity> w = new LambdaQueryWrapper<PatrolTaskEntity>()
                .eq(status != null && !status.isBlank(), PatrolTaskEntity::getTaskStatus, status)
                .eq(planDate != null && !planDate.isBlank(), PatrolTaskEntity::getPlanDate, planDate)
                .and(keyword != null && !keyword.isBlank(), q -> q.like(PatrolTaskEntity::getTaskNo, keyword))
                .orderByDesc(PatrolTaskEntity::getPlanDate).orderByAsc(PatrolTaskEntity::getId);
        if (Boolean.TRUE.equals(mine) || !cu.isAllScope()) {
            w.eq(PatrolTaskEntity::getInspectorId, cu.getId());
        }
        Page<PatrolTaskEntity> p = taskMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> list = new ArrayList<>();
        for (PatrolTaskEntity t : p.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("taskNo", t.getTaskNo());
            m.put("planDate", t.getPlanDate());
            m.put("planStart", t.getPlanStart());
            m.put("taskStatus", t.getTaskStatus());
            m.put("totalCount", t.getTotalCount());
            m.put("doneCount", t.getDoneCount());
            m.put("abnormalCount", t.getAbnormalCount());
            m.put("finishTime", t.getFinishTime());
            PatrolRouteEntity r = routeMapper.selectById(t.getRouteId());
            m.put("routeName", r == null ? "" : r.getRouteName());
            m.put("roadName", r == null ? "" : r.getRoadName());
            SysUserEntity u = t.getInspectorId() == null ? null : userMapper.selectById(t.getInspectorId());
            m.put("inspectorName", u == null ? "" : u.getRealName());
            list.add(m);
        }
        return new PageResult<>(p.getTotal(), page, size, list);
    }

    /** 任务详情：点位列表 + 已登记记录 */
    public Map<String, Object> taskDetail(Long id) {
        PatrolTaskEntity t = taskMapper.selectById(id);
        if (t == null) {
            throw new BizException("巡检任务不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("taskNo", t.getTaskNo());
        m.put("planDate", t.getPlanDate());
        m.put("taskStatus", t.getTaskStatus());
        m.put("totalCount", t.getTotalCount());
        m.put("doneCount", t.getDoneCount());
        m.put("abnormalCount", t.getAbnormalCount());
        PatrolRouteEntity r = routeMapper.selectById(t.getRouteId());
        m.put("route", r == null ? null : routeView(r));
        List<PatrolRecordEntity> records = recordMapper.selectList(new LambdaQueryWrapper<PatrolRecordEntity>()
                .eq(PatrolRecordEntity::getTaskId, id).orderByAsc(PatrolRecordEntity::getCheckTime));
        Map<Long, PatrolRecordEntity> doneMap = new HashMap<>();
        for (PatrolRecordEntity rec : records) {
            doneMap.put(rec.getCoverId(), rec);
        }
        List<Map<String, Object>> points = new ArrayList<>();
        if (r != null) {
            for (Map<String, Object> pm : ((List<Map<String, Object>>) routeView(r).get("points"))) {
                Long coverId = ((Number) pm.get("coverId")).longValue();
                PatrolRecordEntity rec = doneMap.get(coverId);
                pm.put("checked", rec != null);
                pm.put("checkResult", rec == null ? null : rec.getCheckResult());
                pm.put("checkTime", rec == null ? null : rec.getCheckTime());
                points.add(pm);
            }
        }
        m.put("points", points);
        return m;
    }

    /** 登记巡检点位：完好只留记录；异常自动生成问题并触发风险计算 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitRecord(Long taskId, Long coverId, String checkResult, String severity,
                                            String imageUrl, String remark) {
        PatrolTaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException("巡检任务不存在");
        }
        if ("已完成".equals(task.getTaskStatus())) {
            throw new BizException("任务已完成，不能继续登记");
        }
        Long dup = recordMapper.selectCount(new LambdaQueryWrapper<PatrolRecordEntity>()
                .eq(PatrolRecordEntity::getTaskId, taskId).eq(PatrolRecordEntity::getCoverId, coverId));
        if (dup != null && dup > 0) {
            throw new BizException("该点位已登记，请勿重复提交");
        }
        LoginUser cu = UserContext.require();
        PatrolRecordEntity rec = new PatrolRecordEntity();
        rec.setTaskId(taskId);
        rec.setCoverId(coverId);
        rec.setCheckResult(checkResult);
        rec.setSeverity(severity);
        rec.setImageUrl(imageUrl);
        rec.setRemark(remark);
        rec.setInspectorId(cu.getId());
        rec.setCheckTime(LocalDateTime.now());
        recordMapper.insert(rec);

        boolean abnormal = !"完好".equals(checkResult);
        task.setDoneCount((task.getDoneCount() == null ? 0 : task.getDoneCount()) + 1);
        if (abnormal) {
            task.setAbnormalCount((task.getAbnormalCount() == null ? 0 : task.getAbnormalCount()) + 1);
        }
        if (task.getTaskStatus().equals("待执行")) {
            task.setTaskStatus("进行中");
        }
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("abnormal", abnormal);
        if (abnormal) {
            CoverIssueEntity issue = new CoverIssueEntity();
            issue.setCoverId(coverId);
            issue.setDamageType(checkResult);
            issue.setSeverity(severity == null ? "一般" : severity);
            issue.setDescription(remark == null ? "巡检发现" + checkResult : remark);
            issue.setImageUrl(imageUrl);
            issue.setSourceType("巡检发现");
            CoverIssueEntity saved = issueService.create(issue, false);
            res.put("issueNo", saved.getIssueNo());
            res.put("riskLevel", saved.getRiskLevel());
            res.put("riskScore", saved.getRiskScore());
        }
        return res;
    }

    /** 结束任务：未登记的点位不阻断，但会在结果中提示 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> finish(Long taskId) {
        PatrolTaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException("巡检任务不存在");
        }
        task.setTaskStatus("已完成");
        task.setFinishTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("taskNo", task.getTaskNo());
        m.put("doneCount", task.getDoneCount());
        m.put("totalCount", task.getTotalCount());
        m.put("abnormalCount", task.getAbnormalCount());
        m.put("unfinished", task.getTotalCount() - task.getDoneCount());
        return m;
    }
}

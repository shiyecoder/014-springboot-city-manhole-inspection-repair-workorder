package com.city.manhole.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.city.manhole.entity.*;
import com.city.manhole.mapper.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 首页看板：6 个核心指标 + 6 个图表。所有聚合在后端完成，前端不做全表二次聚合。
 */
@Service
public class DashboardService {

    private final ManholeCoverMapper coverMapper;
    private final CoverIssueMapper issueMapper;
    private final RepairOrderMapper orderMapper;
    private final RiskScoreMapper scoreMapper;
    private final SparePartStockMapper partMapper;
    private final CoverTimelineMapper timelineMapper;

    public DashboardService(ManholeCoverMapper coverMapper, CoverIssueMapper issueMapper, RepairOrderMapper orderMapper,
                            RiskScoreMapper scoreMapper, SparePartStockMapper partMapper, CoverTimelineMapper timelineMapper) {
        this.coverMapper = coverMapper;
        this.issueMapper = issueMapper;
        this.orderMapper = orderMapper;
        this.scoreMapper = scoreMapper;
        this.partMapper = partMapper;
        this.timelineMapper = timelineMapper;
    }

    public Map<String, Object> overview() {
        LocalDateTime since = LocalDate.now().minusDays(30).atStartOfDay();
        List<ManholeCoverEntity> covers = coverMapper.selectList(null);
        List<CoverIssueEntity> issues = issueMapper.selectList(new LambdaQueryWrapper<CoverIssueEntity>()
                .ge(CoverIssueEntity::getReportTime, since));
        List<RepairOrderEntity> orders = orderMapper.selectList(new LambdaQueryWrapper<RepairOrderEntity>()
                .ge(RepairOrderEntity::getCreateTime, since));

        Map<String, Object> m = new LinkedHashMap<>();
        long total = covers.size();
        long abnormal = covers.stream().filter(c -> !"正常".equals(c.getCoverStatus())).count();
        double avgResponse = orders.stream()
                .filter(o -> o.getArriveTime() != null && o.getCreateTime() != null)
                .mapToDouble(o -> Duration.between(o.getCreateTime(), o.getArriveTime()).toMinutes() / 60.0)
                .average().orElse(0);
        long finished = orders.stream().filter(o -> "已完成".equals(o.getOrderStatus())).count();
        double doneRate = orders.isEmpty() ? 0 : Math.round(finished * 1000.0 / orders.size()) / 10.0;
        long repeatCovers = issues.stream().collect(Collectors.groupingBy(CoverIssueEntity::getCoverId, Collectors.counting()))
                .values().stream().filter(v -> v > 1).count();
        double repeatRate = total == 0 ? 0 : Math.round(repeatCovers * 1000.0 / total) / 10.0;
        long highRiskRoad = issues.stream().filter(i -> "重大".equals(i.getRiskLevel()))
                .map(i -> {
                    ManholeCoverEntity c = covers.stream().filter(x -> x.getId().equals(i.getCoverId())).findFirst().orElse(null);
                    return c == null ? null : c.getRoadName();
                }).filter(Objects::nonNull).distinct().count();

        List<Map<String, Object>> metrics = new ArrayList<>();
        metrics.add(metric("设施总数", total, "座", "覆盖 " + covers.stream().map(ManholeCoverEntity::getRoadName).distinct().count() + " 条道路", "up"));
        metrics.add(metric("异常井盖", abnormal, "座", "待派单与维修中合计", "warn"));
        metrics.add(metric("平均响应时长", Math.round(avgResponse * 10) / 10.0, "小时", "按派单到到场计算", "normal"));
        metrics.add(metric("维修完成率", doneRate, "%", "近 30 天工单口径", "normal"));
        metrics.add(metric("重复报修率", repeatRate, "%", "同一井盖多次上报", "danger"));
        metrics.add(metric("高风险路段", highRiskRoad, "条", "含重大风险问题", "warn"));
        m.put("metrics", metrics);

        // 图表 1：趋势（近 30 天每日上报/关闭）
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 29; i >= 0; i -= 3) {
            LocalDate d = LocalDate.now().minusDays(i);
            long rep = issues.stream().filter(x -> x.getReportTime() != null
                    && x.getReportTime().toLocalDate().equals(d)).count();
            long clo = issues.stream().filter(x -> x.getCloseTime() != null
                    && x.getCloseTime().toLocalDate().equals(d)).count();
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("date", d.toString().substring(5));
            p.put("report", rep);
            p.put("close", clo);
            trend.add(p);
        }
        m.put("trend", trend);

        // 图表 2：工单状态分布
        Map<String, Long> statusCount = orders.stream()
                .collect(Collectors.groupingBy(RepairOrderEntity::getOrderStatus, LinkedHashMap::new, Collectors.counting()));
        m.put("statusDist", toPairs(statusCount));

        // 图表 3：缺损类型分布
        Map<String, Long> damageCount = issues.stream()
                .collect(Collectors.groupingBy(CoverIssueEntity::getDamageType, LinkedHashMap::new, Collectors.counting()));
        m.put("damageDist", toPairs(damageCount));

        // 图表 4：责任单位 / 队伍处置量
        Map<String, Long> teamCount = orders.stream()
                .collect(Collectors.groupingBy(RepairOrderEntity::getTeamName, LinkedHashMap::new, Collectors.counting()));
        m.put("teamRank", toPairs(teamCount));

        // 图表 5：时效对比（按风险等级平均到场小时）
        List<Map<String, Object>> timeliness = new ArrayList<>();
        for (String level : List.of("重大", "紧急", "一般")) {
            List<RepairOrderEntity> group = orders.stream().filter(o -> {
                CoverIssueEntity i = issues.stream().filter(x -> x.getId().equals(o.getIssueId())).findFirst().orElse(null);
                return i != null && level.equals(i.getRiskLevel());
            }).toList();
            double avg = group.stream().filter(o -> o.getArriveTime() != null)
                    .mapToDouble(o -> Duration.between(o.getCreateTime(), o.getArriveTime()).toMinutes() / 60.0)
                    .average().orElse(0);
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("name", level);
            p.put("value", Math.round(avg * 10) / 10.0);
            p.put("count", group.size());
            timeliness.add(p);
        }
        m.put("timeliness", timeliness);

        // 图表 6：创新模块结果分布（规则版本可切换）
        Map<String, Long> riskDist = scoreMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(RiskScoreEntity::getRiskLevel, LinkedHashMap::new, Collectors.counting()));
        m.put("riskDist", toPairs(riskDist));

        // 片区热力（按道路未关闭问题数）
        Map<String, Long> roadCount = issues.stream()
                .filter(i -> !"已关闭".equals(i.getIssueStatus()))
                .collect(Collectors.groupingBy(i -> {
                    ManholeCoverEntity c = covers.stream().filter(x -> x.getId().equals(i.getCoverId())).findFirst().orElse(null);
                    return c == null ? "其他" : c.getRoadName();
                }, LinkedHashMap::new, Collectors.counting()));
        m.put("roadHeat", toPairs(roadCount));

        m.put("lowStock", partMapper.selectList(null).stream().filter(p -> p.getStockQty() <= p.getWarnQty()).count());
        m.put("recentTimeline", timelineMapper.selectList(new LambdaQueryWrapper<CoverTimelineEntity>()
                .orderByDesc(CoverTimelineEntity::getCreateTime).last("limit 8")));
        return m;
    }

    private Map<String, Object> metric(String name, Object value, String unit, String desc, String tone) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        m.put("unit", unit);
        m.put("desc", desc);
        m.put("tone", tone);
        return m;
    }

    private List<Map<String, Object>> toPairs(Map<String, Long> map) {
        List<Map<String, Object>> list = new ArrayList<>();
        map.forEach((k, v) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", k == null ? "未设置" : k);
            m.put("value", v);
            list.add(m);
        });
        list.sort((a, b) -> Long.compare((Long) b.get("value"), (Long) a.get("value")));
        return list;
    }
}

package com.city.manhole.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.city.manhole.common.BizException;
import com.city.manhole.common.LoginUser;
import com.city.manhole.common.PageResult;
import com.city.manhole.common.UserContext;
import com.city.manhole.entity.*;
import com.city.manhole.mapper.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 创新点：井盖风险分级巡检。
 * 六因子加权（道路等级 / 缺损严重度 / 历史维修 / 人流等级 / 未处理时长 / 重复报修），
 * 权重合计 100%，每次计算保存输入快照 + 明细分 + 规则版本，结果可解释、可追溯、可人工修正。
 */
@Service
public class RiskService {

    /** 各因子满分，与权重上限一致，保证合计 100 分 */
    public static final int MAX_ROAD = 25, MAX_SEVERITY = 25, MAX_REPAIR = 15, MAX_CROWD = 15, MAX_DURATION = 10, MAX_REPEAT = 10;

    private static final Map<String, Integer> ROAD_PTS = Map.of("主干道", 25, "次干道", 15, "支路", 8);
    private static final Map<String, Integer> DMG_PTS = Map.of("丢失", 25, "破损", 22, "井圈破损", 18,
            "沉降", 16, "松动", 12, "异响", 8);
    private static final Map<String, Integer> CROWD_PTS = Map.of("密集", 15, "一般", 9, "稀疏", 4);
    private static final String[] FACTOR_NAMES = {"道路等级", "缺损严重度", "历史维修次数", "人流等级", "未处理时长", "重复报修次数"};

    private final RiskRuleMapper ruleMapper;
    private final RiskScoreMapper scoreMapper;
    private final CoverIssueMapper issueMapper;
    private final ManholeCoverMapper coverMapper;
    private final RepairOrderMapper orderMapper;
    private final ObjectMapper json = new ObjectMapper();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public RiskService(RiskRuleMapper ruleMapper, RiskScoreMapper scoreMapper, CoverIssueMapper issueMapper,
                       ManholeCoverMapper coverMapper, RepairOrderMapper orderMapper) {
        this.ruleMapper = ruleMapper;
        this.scoreMapper = scoreMapper;
        this.issueMapper = issueMapper;
        this.coverMapper = coverMapper;
        this.orderMapper = orderMapper;
    }

    public RiskRuleEntity activeRule() {
        RiskRuleEntity rule = ruleMapper.selectOne(new LambdaQueryWrapper<RiskRuleEntity>()
                .eq(RiskRuleEntity::getEnabled, 1).orderByDesc(RiskRuleEntity::getEffectTime).last("limit 1"));
        if (rule == null) {
            throw new BizException("未找到启用的风险规则，请先在规则配置页发布版本");
        }
        return rule;
    }

    public List<RiskRuleEntity> rules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<RiskRuleEntity>().orderByDesc(RiskRuleEntity::getEffectTime));
    }

    /** 校验并发布新规则版本：权重合计必须 100%，阈值必须连续覆盖 0-100 */
    @Transactional(rollbackFor = Exception.class)
    public RiskRuleEntity publishRule(RiskRuleEntity rule) {
        int sum = rule.getWRoad() + rule.getWSeverity() + rule.getWRepair() + rule.getWCrowd()
                + rule.getWDuration() + rule.getWRepeat();
        if (sum != 100) {
            throw new BizException("权重合计必须等于 100%，当前为 " + sum + "%");
        }
        if (rule.getYellowThreshold() == null || rule.getRedThreshold() == null
                || rule.getYellowThreshold() >= rule.getRedThreshold()
                || rule.getYellowThreshold() <= 0 || rule.getRedThreshold() > 100) {
            throw new BizException("阈值不合法：需满足 0 < 紧急下限 < 重大下限 ≤ 100");
        }
        RiskRuleEntity last = ruleMapper.selectOne(new LambdaQueryWrapper<RiskRuleEntity>()
                .orderByDesc(RiskRuleEntity::getId).last("limit 1"));
        String next = nextVersion(last == null ? null : last.getRuleVersion());
        rule.setId(null);
        rule.setRuleVersion(next);
        rule.setRuleName(rule.getRuleName() == null ? "井盖风险分级规则 " + next : rule.getRuleName());
        rule.setEnabled(1);
        rule.setEffectTime(LocalDateTime.now());
        rule.setCreateBy(UserContext.id());
        rule.setCreateTime(LocalDateTime.now());
        ruleMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<RiskRuleEntity>()
                .set(RiskRuleEntity::getEnabled, 0));
        ruleMapper.insert(rule);
        return rule;
    }

    private String nextVersion(String current) {
        if (current == null) {
            return "v1.0";
        }
        String num = current.replace("v", "");
        String[] parts = num.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) + 1 : 1;
        if (minor >= 10) {
            major++;
            minor = 0;
        }
        return "v" + major + "." + minor;
    }

    /** 对一个问题执行计算，保存快照与明细，并把结果回写到问题与井盖 */
    @Transactional(rollbackFor = Exception.class)
    public RiskScoreEntity evaluate(Long issueId) {
        CoverIssueEntity issue = issueMapper.selectById(issueId);
        if (issue == null) {
            throw new BizException("问题不存在");
        }
        ManholeCoverEntity cover = coverMapper.selectById(issue.getCoverId());
        if (cover == null) {
            throw new BizException("井盖档案不存在，无法计算风险");
        }
        RiskRuleEntity rule = activeRule();
        return doEvaluate(issue, cover, rule);
    }

    private RiskScoreEntity doEvaluate(CoverIssueEntity issue, ManholeCoverEntity cover, RiskRuleEntity rule) {
        LocalDateTime report = issue.getReportTime() == null ? LocalDateTime.now() : issue.getReportTime();
        long hours = Math.max(ChronoUnit.HOURS.between(report, LocalDateTime.now()), 0);
        // 历史维修次数取该井盖已关闭工单数；重复报修取 30 天内同井盖问题数
        Long repairCnt = orderMapper.selectCount(new LambdaQueryWrapper<RepairOrderEntity>()
                .eq(RepairOrderEntity::getCoverId, cover.getId())
                .in(RepairOrderEntity::getOrderStatus, List.of("待验收", "已完成", "返工中")));
        Long repeatCnt = issueMapper.selectCount(new LambdaQueryWrapper<CoverIssueEntity>()
                .eq(CoverIssueEntity::getCoverId, cover.getId())
                .ge(CoverIssueEntity::getReportTime, report.minusDays(30))
                .le(CoverIssueEntity::getReportTime, report.plusMinutes(1)));

        String road = cover.getRoadLevel() == null ? "" : cover.getRoadLevel();
        String crowd = cover.getCrowdLevel() == null ? "" : cover.getCrowdLevel();
        String damage = issue.getDamageType() == null ? "" : issue.getDamageType();

        List<String> missing = new ArrayList<>();
        if (!ROAD_PTS.containsKey(road)) {
            missing.add("道路等级");
        }
        if (!CROWD_PTS.containsKey(crowd)) {
            missing.add("人流等级");
        }
        if (!DMG_PTS.containsKey(damage)) {
            missing.add("缺损类型");
        }
        if (!missing.isEmpty()) {
            throw new BizException("缺少必要数据：" + String.join("、", missing) + "，请先补录后再计算");
        }

        // 得分按权重缩放：某因子满分 = 该因子权重
        int roadScore = Math.round(ROAD_PTS.get(road) * rule.getWRoad() / (float) MAX_ROAD);
        int severityScore = Math.round(DMG_PTS.get(damage) * rule.getWSeverity() / (float) MAX_SEVERITY);
        int repairScore = Math.min(repairCnt.intValue() * 4, rule.getWRepair());
        int crowdScore = Math.round(CROWD_PTS.get(crowd) * rule.getWCrowd() / (float) MAX_CROWD);
        int durationScore = Math.min((int) (hours / 24) * 2, rule.getWDuration());
        int repeatScore = Math.min(repeatCnt.intValue() * 2, rule.getWRepeat());
        int total = roadScore + severityScore + repairScore + crowdScore + durationScore + repeatScore;
        total = Math.min(total, 100);
        String level = total >= rule.getRedThreshold() ? "重大" : (total >= rule.getYellowThreshold() ? "紧急" : "一般");

        int[] scores = {roadScore, severityScore, repairScore, crowdScore, durationScore, repeatScore};
        int[] maxes = {rule.getWRoad(), rule.getWSeverity(), rule.getWRepair(), rule.getWCrowd(),
                rule.getWDuration(), rule.getWRepeat()};
        String[] inputs = {road, damage, repairCnt + " 次", crowd, hours + " 小时", repeatCnt + " 次"};

        // 主要影响因素：得分最高的两项
        Integer[] order = {0, 1, 2, 3, 4, 5};
        Arrays.sort(order, (a, b) -> scores[b] - scores[a]);
        int first = order[0], second = order[1];
        double share = total == 0 ? 0 : (scores[first] + scores[second]) * 100.0 / total;

        StringBuilder explain = new StringBuilder();
        explain.append(String.format("总分 %d 分，等级 %s。主要影响因素：%s（%d 分）与 %s（%d 分），合计解释 %.0f%% 的风险。",
                total, level, FACTOR_NAMES[first], scores[first], FACTOR_NAMES[second], scores[second], share));
        explain.append("建议处置时限：").append("重大".equals(level) ? "4 小时内" : "紧急".equals(level) ? "8 小时内" : "24 小时内").append("到场。");

        // 未满足条件：重大风险必须有围挡；档案缺安装日期
        List<String> unmet = new ArrayList<>();
        if ("重大".equals(level)) {
            unmet.add("重大风险需登记临时围挡措施后方可提交维修结果");
        }
        if (cover.getInstallDate() == null) {
            unmet.add("井盖档案缺少安装日期，影响使用年限判断，建议补录");
        }
        // 可改进项：可降低的分数
        List<String> improve = new ArrayList<>();
        if (repairCnt >= 2) {
            improve.add("历史维修 " + repairCnt + " 次，若纳入更换计划可降低 " + repairScore + " 分");
        }
        if (repeatCnt >= 2) {
            improve.add("近 30 天重复报修 " + repeatCnt + " 次，建议核查井圈基础，可降低 " + repeatScore + " 分");
        }
        if (hours > 24) {
            improve.add("已等待 " + hours + " 小时，尽快处置可降低 " + durationScore + " 分");
        }
        if (improve.isEmpty()) {
            improve.add("当前各因子均处于合理区间，无需额外改进");
        }

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("coverNo", cover.getCoverNo());
        snapshot.put("roadLevel", road);
        snapshot.put("damageType", damage);
        snapshot.put("severity", issue.getSeverity());
        snapshot.put("repairCount", repairCnt);
        snapshot.put("crowdLevel", crowd);
        snapshot.put("waitHours", hours);
        snapshot.put("repeatCount", repeatCnt);
        snapshot.put("ruleVersion", rule.getRuleVersion());

        RiskScoreEntity score = new RiskScoreEntity();
        score.setIssueId(issue.getId());
        score.setCoverId(cover.getId());
        score.setRuleId(rule.getId());
        score.setRuleVersion(rule.getRuleVersion());
        try {
            score.setInputSnapshot(json.writeValueAsString(snapshot));
            score.setUnmetJson(json.writeValueAsString(unmet));
            score.setImproveJson(json.writeValueAsString(improve));
        } catch (Exception e) {
            throw new BizException("计算快照序列化失败");
        }
        score.setRoadScore(roadScore);
        score.setSeverityScore(severityScore);
        score.setRepairScore(repairScore);
        score.setCrowdScore(crowdScore);
        score.setDurationScore(durationScore);
        score.setRepeatScore(repeatScore);
        score.setTotalScore(total);
        score.setRiskLevel(level);
        score.setExplainText(explain.toString());
        score.setCreateTime(LocalDateTime.now());
        scoreMapper.insert(score);

        // 回写问题与井盖的风险快照，列表页无需二次计算
        issue.setRiskLevel(level);
        issue.setRiskScore(total);
        issue.setRoadLevel(road);
        issue.setCrowdLevel(crowd);
        issue.setRepairCount(repairCnt.intValue());
        issue.setRepeatCount(repeatCnt.intValue());
        issue.setRepeatFlag(repeatCnt >= 2 ? 1 : 0);
        issue.setHandleDeadline(LocalDateTime.now().plusHours("重大".equals(level) ? 4 : "紧急".equals(level) ? 8 : 24));
        issueMapper.updateById(issue);
        cover.setRiskLevel(rankLevel(level, cover.getRiskLevel()));
        coverMapper.updateById(cover);
        return score;
    }

    private String rankLevel(String a, String b) {
        Map<String, Integer> rank = Map.of("一般", 1, "紧急", 2, "重大", 3);
        if (b == null) {
            return a;
        }
        return rank.getOrDefault(a, 1) >= rank.getOrDefault(b, 1) ? a : b;
    }

    /** 人工修正：只改最终等级，原始分与明细不覆盖，必须填写原因 */
    @Transactional(rollbackFor = Exception.class)
    public void adjust(Long scoreId, String level, String reason) {
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("人工调整必须填写原因，且不少于 5 个字");
        }
        if (!List.of("重大", "紧急", "一般").contains(level)) {
            throw new BizException("调整等级不合法");
        }
        RiskScoreEntity score = scoreMapper.selectById(scoreId);
        if (score == null) {
            throw new BizException("计算记录不存在");
        }
        LoginUser cu = UserContext.require();
        score.setAdjustLevel(level);
        score.setAdjustReason(reason.trim());
        score.setAdjustUserId(cu.getId());
        score.setAdjustTime(LocalDateTime.now());
        scoreMapper.updateById(score);
    }

    public Map<String, Object> detail(Long scoreId) {
        RiskScoreEntity s = scoreMapper.selectById(scoreId);
        if (s == null) {
            throw new BizException("计算记录不存在");
        }
        Map<String, Object> m = baseView(s);
        CoverIssueEntity issue = issueMapper.selectById(s.getIssueId());
        ManholeCoverEntity cover = coverMapper.selectById(s.getCoverId());
        m.put("issue", issue);
        m.put("cover", cover);
        m.put("rule", ruleMapper.selectById(s.getRuleId()));
        return m;
    }

    private Map<String, Object> baseView(RiskScoreEntity s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("issueId", s.getIssueId());
        m.put("coverId", s.getCoverId());
        m.put("ruleVersion", s.getRuleVersion());
        m.put("totalScore", s.getTotalScore());
        m.put("riskLevel", s.getRiskLevel());
        m.put("adjustLevel", s.getAdjustLevel());
        m.put("adjustReason", s.getAdjustReason());
        m.put("adjustTime", s.getAdjustTime() == null ? null : FMT.format(s.getAdjustTime()));
        m.put("explainText", s.getExplainText());
        m.put("createTime", s.getCreateTime() == null ? null : FMT.format(s.getCreateTime()));
        m.put("inputSnapshot", readJson(s.getInputSnapshot(), new TypeReference<Map<String, Object>>() {
        }));
        m.put("unmet", readJson(s.getUnmetJson(), new TypeReference<List<String>>() {
        }));
        m.put("improve", readJson(s.getImproveJson(), new TypeReference<List<String>>() {
        }));
        int[] scores = {s.getRoadScore(), s.getSeverityScore(), s.getRepairScore(), s.getCrowdScore(),
                s.getDurationScore(), s.getRepeatScore()};
        List<Map<String, Object>> factors = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("name", FACTOR_NAMES[i]);
            f.put("score", scores[i]);
            factors.add(f);
        }
        m.put("factors", factors);
        return m;
    }

    private <T> T readJson(String text, TypeReference<T> type) {
        try {
            return text == null ? null : json.readValue(text, type);
        } catch (Exception e) {
            return null;
        }
    }

    public PageResult<Map<String, Object>> page(long page, long size, String keyword, String level, String version) {
        LambdaQueryWrapper<RiskScoreEntity> w = new LambdaQueryWrapper<RiskScoreEntity>()
                .eq(level != null && !level.isBlank(), RiskScoreEntity::getRiskLevel, level)
                .eq(version != null && !version.isBlank(), RiskScoreEntity::getRuleVersion, version)
                .orderByDesc(RiskScoreEntity::getCreateTime);
        Page<RiskScoreEntity> p = scoreMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> list = new ArrayList<>();
        for (RiskScoreEntity s : p.getRecords()) {
            Map<String, Object> m = baseView(s);
            ManholeCoverEntity c = coverMapper.selectById(s.getCoverId());
            CoverIssueEntity i = issueMapper.selectById(s.getIssueId());
            m.put("coverNo", c == null ? "" : c.getCoverNo());
            m.put("roadName", c == null ? "" : c.getRoadName());
            m.put("damageType", i == null ? "" : i.getDamageType());
            m.put("issueNo", i == null ? "" : i.getIssueNo());
            list.add(m);
        }
        return new PageResult<>(p.getTotal(), page, size, list);
    }

    /** 创新模块效果统计：推荐采纳率、人工调整率、调整方向 */
    public Map<String, Object> stats() {
        List<RiskScoreEntity> all = scoreMapper.selectList(new LambdaQueryWrapper<>());
        long total = all.size();
        long adjusted = all.stream().filter(s -> s.getAdjustLevel() != null).count();
        Map<String, Integer> levelCount = new LinkedHashMap<>();
        levelCount.put("重大", 0);
        levelCount.put("紧急", 0);
        levelCount.put("一般", 0);
        for (RiskScoreEntity s : all) {
            levelCount.merge(s.getRiskLevel(), 1, Integer::sum);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", total);
        m.put("adopted", total - adjusted);
        m.put("adjusted", adjusted);
        m.put("adoptRate", total == 0 ? 0 : Math.round((total - adjusted) * 1000.0 / total) / 10.0);
        m.put("adjustRate", total == 0 ? 0 : Math.round(adjusted * 1000.0 / total) / 10.0);
        m.put("levelCount", levelCount);
        m.put("upgraded", all.stream().filter(s -> s.getAdjustLevel() != null
                && rank(s.getAdjustLevel()) > rank(s.getRiskLevel())).count());
        m.put("downgraded", all.stream().filter(s -> s.getAdjustLevel() != null
                && rank(s.getAdjustLevel()) < rank(s.getRiskLevel())).count());
        return m;
    }

    private int rank(String level) {
        return switch (level == null ? "" : level) {
            case "重大" -> 3;
            case "紧急" -> 2;
            default -> 1;
        };
    }
}

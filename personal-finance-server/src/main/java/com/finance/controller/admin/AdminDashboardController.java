package com.finance.controller.admin;

import com.finance.annotation.SensitiveRead;
import com.finance.mapper.BillMapper;
import com.finance.service.SysUserService;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Tag(name = "管理员看板", description = "后台总看板数据")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final SysUserService sysUserService;
    private final BillMapper billMapper;

    @Operation(summary = "后台总看板（支持区间切换）")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(
            @Parameter(description = "统计区间月数（3=近3月 / 6=近6月 / 12=近12月）", example = "6") @RequestParam(required = false, defaultValue = "6") int months) {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String lastMonth = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));

        long totalUsers = sysUserService.count();
        Long activeUsers = billMapper.countMonthlyActiveUsers(currentMonth);
        Long monthlyBills = billMapper.countMonthlyBills(currentMonth);
        BigDecimal monthlyAmount = billMapper.sumMonthlyAmount(currentMonth);

        // 环比
        Long lastMonthActiveUsers = billMapper.countMonthlyActiveUsers(lastMonth);
        Long lastMonthBills = billMapper.countMonthlyBills(lastMonth);
        BigDecimal lastMonthAmount = billMapper.sumMonthlyAmount(lastMonth);

        // 趋势（补齐缺失月份）
        List<Map<String, Object>> rawTrend = billMapper.recent6MonthTrend();
        List<Map<String, Object>> trend = fillMissingMonths(rawTrend, months);

        // 衍生指标
        long active = activeUsers != null ? activeUsers : 0;
        long bills = monthlyBills != null ? monthlyBills : 0;
        BigDecimal amount = monthlyAmount != null ? monthlyAmount : BigDecimal.ZERO;
        BigDecimal avgBillsPerUser = active > 0
                ? BigDecimal.valueOf(bills).divide(BigDecimal.valueOf(active), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal avgOrderValue = bills > 0
                ? amount.divide(BigDecimal.valueOf(bills), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", totalUsers);
        data.put("activeUsers", active);
        data.put("currentMonthBillCount", bills);
        data.put("currentMonthTotalAmount", amount);

        data.put("activeUsersMoM", calcMoM(activeUsers, lastMonthActiveUsers));
        data.put("billCountMoM", calcMoM(monthlyBills, lastMonthBills));
        data.put("totalAmountMoM", calcMoMAmount(monthlyAmount, lastMonthAmount));

        data.put("avgBillsPerUser", avgBillsPerUser);
        data.put("avgOrderValue", avgOrderValue);
        data.put("currentMonth", currentMonth);

        data.put("recent6MonthTrend", trend);
        return Result.ok(data);
    }

    @Operation(summary = "异常检测告警")
    @GetMapping("/dashboard/alerts")
    public Result<List<Map<String, Object>>> alerts() {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<Map<String, Object>> alerts = new ArrayList<>();

        List<Map<String, Object>> trend = billMapper.recent6MonthTrend();
        if (trend != null && trend.size() >= 3) {
            int declineCount = 0;
            for (int i = trend.size() - 1; i >= 1; i--) {
                Object curr = trend.get(i).get("userCount");
                Object prev = trend.get(i - 1).get("userCount");
                long currVal = curr instanceof Number ? ((Number) curr).longValue() : 0;
                long prevVal = prev instanceof Number ? ((Number) prev).longValue() : 0;
                if (currVal < prevVal)
                    declineCount++;
                else
                    break;
            }
            if (declineCount >= 3) {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("type", "warning");
                a.put("message", "活跃用户已连续" + declineCount + "个月下降，建议启动用户召回计划");
                alerts.add(a);
            }
        }

        String lastMonth = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        BigDecimal currAmount = billMapper.sumMonthlyAmount(currentMonth);
        BigDecimal lastAmount = billMapper.sumMonthlyAmount(lastMonth);
        if (currAmount != null && lastAmount != null && lastAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal change = currAmount.subtract(lastAmount).divide(lastAmount, 4, RoundingMode.HALF_UP);
            if (change.compareTo(BigDecimal.valueOf(-0.30)) < 0) {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("type", "danger");
                a.put("message",
                        "本月交易额环比下降" + change.abs().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP)
                                + "%，请关注业务异常");
                alerts.add(a);
            }
        }

        List<Map<String, Object>> anomalyUsers = billMapper.anomalyUsers(currentMonth);
        if (anomalyUsers != null) {
            for (Map<String, Object> u : anomalyUsers) {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("type", "warning");
                a.put("message",
                        "用户\"" + (u.get("username") != null ? u.get("username") : "未知") + "\"本月消费超月均3倍，建议人工复核");
                alerts.add(a);
            }
        }
        return Result.ok(alerts);
    }

    @Operation(summary = "导出看板报表CSV（含总览+趋势+排行+分类）")
    @SensitiveRead("导出看板报表")
    @GetMapping("/dashboard/export")
    public ResponseEntity<byte[]> export(
            @Parameter(description = "导出区间月数（3/6/12）", example = "6") @RequestParam(required = false, defaultValue = "6") int months) {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        long totalUsers = sysUserService.count();
        Long activeUsers = billMapper.countMonthlyActiveUsers(currentMonth);
        Long monthlyBills = billMapper.countMonthlyBills(currentMonth);
        BigDecimal monthlyAmount = billMapper.sumMonthlyAmount(currentMonth);

        List<Map<String, Object>> rawTrend = billMapper.recent6MonthTrend();
        List<Map<String, Object>> trend = fillMissingMonths(rawTrend, months);
        List<Map<String, Object>> userRanking = billMapper.userRanking(currentMonth);
        List<Map<String, Object>> categoryDist = billMapper.categoryDistribution(currentMonth);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            // BOM
            bos.write(0xEF);
            bos.write(0xBB);
            bos.write(0xBF);

            writeLine(bos, "============================================================");
            writeLine(bos, "  财务看板导出报表");
            writeLine(bos, "  导出时间: " + now);
            writeLine(bos, "  统计区间: 近" + months + "个月");
            writeLine(bos, "============================================================");
            writeLine(bos, "");

            // 一、月度总览
            writeLine(bos, "[一] 月度总览");
            writeLine(bos, "指标,数值");
            writeLine(bos, "平台用户总数," + totalUsers);
            writeLine(bos, "当月活跃用户," + (activeUsers != null ? activeUsers : 0));
            writeLine(bos, "当月账单总量," + (monthlyBills != null ? monthlyBills : 0));
            writeLine(bos, "当月交易总额," + (monthlyAmount != null ? monthlyAmount : "0.00"));
            writeLine(bos, "");

            // 二、趋势数据
            writeLine(bos, "[二] 近" + months + "个月趋势数据");
            writeLine(bos, "月份,账单数,活跃用户,交易额");
            if (trend != null) {
                for (Map<String, Object> row : trend) {
                    writeLine(bos, row.get("yearMonth") + "," +
                            row.get("billCount") + "," + row.get("userCount") + "," +
                            row.get("totalAmount"));
                }
            }
            writeLine(bos, "");

            // 三、用户消费排行 TOP10
            writeLine(bos, "[三] 用户消费排行 TOP10");
            writeLine(bos, "排名,用户名,消费总额,账单数");
            if (userRanking != null) {
                int rank = 1;
                for (Map<String, Object> row : userRanking) {
                    writeLine(bos, (rank++) + "," +
                            row.get("username") + "," +
                            row.get("totalAmount") + "," +
                            row.get("billCount"));
                }
            }
            writeLine(bos, "");

            // 四、分类消费分布
            writeLine(bos, "[四] 分类消费分布");
            writeLine(bos, "分类名称,消费总额");
            if (categoryDist != null) {
                for (Map<String, Object> row : categoryDist) {
                    writeLine(bos, row.get("categoryName") + "," +
                            row.get("totalAmount"));
                }
            }
            writeLine(bos, "");
            writeLine(bos, "============================================================");
            writeLine(bos, "  --- 报表结束 ---");
        } catch (Exception e) {
            throw new RuntimeException("导出CSV失败", e);
        }

        byte[] csvBytes = bos.toByteArray();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("dashboard_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv")
                .build());
        headers.setContentLength(csvBytes.length);

        return ResponseEntity.ok().headers(headers).body(csvBytes);
    }

    private void writeLine(ByteArrayOutputStream bos, String line) {
        try {
            bos.write(line.getBytes(StandardCharsets.UTF_8));
            bos.write('\n');
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 补齐缺失月份，SQL只返回有数据的月份，空白月份补0
     */
    private List<Map<String, Object>> fillMissingMonths(List<Map<String, Object>> raw, int months) {
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        if (raw != null) {
            for (Map<String, Object> row : raw) {
                Object ym = row.get("yearMonth");
                if (ym != null)
                    map.put(ym.toString(), row);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = months - 1; i >= 0; i--) {
            String key = LocalDate.now().minusMonths(i).format(fmt);
            Map<String, Object> row = map.get(key);
            if (row != null) {
                result.add(row);
            } else {
                Map<String, Object> empty = new LinkedHashMap<>();
                empty.put("yearMonth", key);
                empty.put("billCount", 0);
                empty.put("userCount", 0);
                empty.put("totalAmount", BigDecimal.ZERO);
                result.add(empty);
            }
        }
        return result;
    }

    private double calcMoM(Long current, Long last) {
        if (current == null)
            current = 0L;
        if (last == null || last == 0)
            return current > 0 ? 100.0 : 0.0;
        return (current - last) * 100.0 / last;
    }

    private double calcMoMAmount(BigDecimal current, BigDecimal last) {
        if (current == null)
            current = BigDecimal.ZERO;
        if (last == null || last.compareTo(BigDecimal.ZERO) == 0)
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        return current.subtract(last).divide(last, 4, RoundingMode.HALF_UP).doubleValue() * 100;
    }
}
package com.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.finance.entity.Bill;
import com.finance.entity.Category;
import com.finance.exception.BusinessException;
import com.finance.exception.ErrorCode;
import com.finance.mapper.BillMapper;
import com.finance.service.BillService;
import com.finance.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillServiceImpl extends ServiceImpl<BillMapper, Bill> implements BillService {

    private final CategoryService categoryService;

    @Override
    @Transactional
    public Bill addBill(Bill bill, Long userId) {
        if (bill.getAmount() == null || bill.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.AMOUNT_MUST_POSITIVE);
        }
        if (bill.getCategoryId() == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_AVAILABLE);
        }
        // 校验分类存在且用户可用
        Category category = categoryService.getById(bill.getCategoryId());
        if (category == null || (category.getUserId() > 0 && !category.getUserId().equals(userId))) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_AVAILABLE);
        }
        if (bill.getConsumeTime() == null) {
            bill.setConsumeTime(LocalDateTime.now());
        }
        bill.setUserId(userId);
        save(bill);
        log.info("用户{}新增账单 ID={}, 金额={}", userId, bill.getId(), bill.getAmount());
        return bill;
    }

    @Override
    @Transactional
    public Map<String, Object> syncBatch(List<Map<String, Object>> offlineBills, Long userId) {
        int successCount = 0;
        int failCount = 0;
        int duplicateCount = 0;
        List<Map<String, Object>> results = new ArrayList<>();

        if (offlineBills == null || offlineBills.isEmpty()) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("total", 0);
            summary.put("successCount", 0);
            summary.put("failCount", 0);
            summary.put("duplicateCount", 0);
            summary.put("results", results);
            return summary;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Map<String, Object> item : offlineBills) {
            String uuid = (String) item.get("uuid");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("uuid", uuid);

            try {
                // 去重检查
                if (uuid != null && baseMapper.findBySyncUuid(uuid) != null) {
                    result.put("status", "duplicate");
                    result.put("message", "该账单已同步过，跳过");
                    duplicateCount++;
                    results.add(result);
                    continue;
                }

                // 构造账单对象
                Bill bill = new Bill();
                bill.setUserId(userId);
                bill.setSyncUuid(uuid);

                Object amountObj = item.get("amount");
                bill.setAmount(amountObj != null ? new BigDecimal(amountObj.toString()) : BigDecimal.ZERO);
                if (bill.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    result.put("status", "fail");
                    result.put("message", "金额必须大于0");
                    failCount++;
                    results.add(result);
                    continue;
                }

                bill.setType((String) item.get("type"));

                Object catIdObj = item.get("categoryId");
                Long categoryId = catIdObj != null ? Long.valueOf(catIdObj.toString()) : null;
                bill.setCategoryId(categoryId);

                // 校验分类
                if (categoryId == null || categoryService.getById(categoryId) == null) {
                    result.put("status", "fail");
                    result.put("message", "分类ID不存在");
                    failCount++;
                    results.add(result);
                    continue;
                }

                bill.setRemark((String) item.get("remark"));

                String consumeTimeStr = (String) item.get("consumeTime");
                if (consumeTimeStr != null) {
                    bill.setConsumeTime(LocalDateTime.parse(consumeTimeStr, formatter));
                } else {
                    bill.setConsumeTime(LocalDateTime.now());
                }

                save(bill);
                result.put("status", "success");
                result.put("billId", bill.getId());
                result.put("message", "同步成功");
                successCount++;

            } catch (Exception e) {
                result.put("status", "fail");
                result.put("message", "同步失败: " + e.getMessage());
                failCount++;
            }
            results.add(result);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", offlineBills.size());
        summary.put("successCount", successCount);
        summary.put("failCount", failCount);
        summary.put("duplicateCount", duplicateCount);
        summary.put("results", results);

        log.info("用户{}批量同步: 总数={}, 成功={}, 失败={}, 重复={}",
                userId, offlineBills.size(), successCount, failCount, duplicateCount);
        return summary;
    }

    @Override
    public Map<String, Object> getBillDetail(Long billId, Long userId) {
        Bill bill = getById(billId);
        if (bill == null) {
            throw new BusinessException(ErrorCode.BILL_NOT_FOUND);
        }
        if (!bill.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.BILL_NOT_OWNED);
        }
        return buildBillMap(bill);
    }

    @Override
    @Transactional
    public Bill updateBill(Long billId, Bill update, Long userId, boolean isAdmin) {
        Bill bill = getById(billId);
        if (bill == null) {
            throw new BusinessException(ErrorCode.BILL_NOT_FOUND);
        }
        if (!isAdmin && !bill.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.BILL_NOT_OWNED);
        }
        if (update.getAmount() != null) {
            if (update.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.AMOUNT_MUST_POSITIVE);
            }
            bill.setAmount(update.getAmount());
        }
        if (update.getType() != null) bill.setType(update.getType());
        if (update.getCategoryId() != null) bill.setCategoryId(update.getCategoryId());
        if (update.getRemark() != null) bill.setRemark(update.getRemark());
        if (update.getConsumeTime() != null) bill.setConsumeTime(update.getConsumeTime());
        updateById(bill);
        return bill;
    }

    @Override
    @Transactional
    public void deleteBill(Long billId, Long userId, boolean isAdmin) {
        Bill bill = getById(billId);
        if (bill == null) {
            throw new BusinessException(ErrorCode.BILL_NOT_FOUND);
        }
        if (!isAdmin && !bill.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.BILL_NOT_OWNED);
        }
        // 逻辑删除
        removeById(billId);
        log.info("删除账单 ID={}", billId);
    }

    @Override
    @Transactional
    public void adminDeleteBill(Long billId) {
        removeById(billId);
    }

    private Map<String, Object> buildBillMap(Bill bill) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", bill.getId());
        map.put("amount", bill.getAmount());
        map.put("type", bill.getType());
        map.put("categoryId", bill.getCategoryId());
        map.put("remark", bill.getRemark());
        map.put("receiptImage", bill.getReceiptImage());
        map.put("consumeTime", bill.getConsumeTime() != null ?
                bill.getConsumeTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        map.put("createdAt", bill.getCreatedAt() != null ?
                bill.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        map.put("updatedAt", bill.getUpdatedAt() != null ?
                bill.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        map.put("hasImage", bill.getReceiptImage() != null && !bill.getReceiptImage().isEmpty());
        return map;
    }
}

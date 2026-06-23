package com.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.finance.entity.SaveTarget;
import com.finance.exception.BusinessException;
import com.finance.exception.ErrorCode;
import com.finance.mapper.SaveTargetMapper;
import com.finance.service.SaveTargetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaveTargetServiceImpl extends ServiceImpl<SaveTargetMapper, SaveTarget> implements SaveTargetService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<Map<String, Object>> getUserTargets(Long userId, Integer status) {
        LambdaQueryWrapper<SaveTarget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaveTarget::getUserId, userId);
        if (status != null) wrapper.eq(SaveTarget::getStatus, status);
        wrapper.orderByDesc(SaveTarget::getCreatedAt);
        List<SaveTarget> targets = list(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SaveTarget t : targets) {
            result.add(toMap(t));
        }
        return result;
    }

    @Override
    @Transactional
    public SaveTarget createTarget(SaveTarget target, Long userId) {
        if (target.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.AMOUNT_MUST_POSITIVE);
        }
        target.setUserId(userId);
        target.setSavedAmount(BigDecimal.ZERO);
        target.setStatus(0);
        save(target);
        log.info("用户{}创建存钱目标: {}", userId, target.getName());
        return target;
    }

    @Override
    @Transactional
    public SaveTarget updateTarget(Long targetId, String name, BigDecimal targetAmount,
                                    BigDecimal addAmount, Long userId, boolean isAdmin) {
        SaveTarget target = getById(targetId);
        if (target == null) throw new BusinessException(ErrorCode.BILL_NOT_FOUND);
        if (!isAdmin && !target.getUserId().equals(userId)) throw new BusinessException(ErrorCode.BILL_NOT_OWNED);

        // 追加存款
        if (addAmount != null && addAmount.compareTo(BigDecimal.ZERO) > 0) {
            target.setSavedAmount(target.getSavedAmount().add(addAmount));
            // 判断是否达成
            if (target.getSavedAmount().compareTo(target.getTargetAmount()) >= 0) {
                target.setStatus(1);
                target.setCompletedAt(LocalDateTime.now());
                log.info("用户{}的存钱目标{}已达成", userId, target.getName());
            }
        }
        if (name != null) target.setName(name);
        if (targetAmount != null) target.setTargetAmount(targetAmount);
        updateById(target);
        return target;
    }

    @Override
    @Transactional
    public void deleteTarget(Long targetId, Long userId, boolean isAdmin) {
        SaveTarget target = getById(targetId);
        if (target == null) throw new BusinessException(ErrorCode.BILL_NOT_FOUND);
        if (!isAdmin && !target.getUserId().equals(userId)) throw new BusinessException(ErrorCode.BILL_NOT_OWNED);
        removeById(targetId);
    }

    private Map<String, Object> toMap(SaveTarget t) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", t.getId());
        map.put("name", t.getName());
        map.put("targetAmount", t.getTargetAmount());
        map.put("savedAmount", t.getSavedAmount());
        BigDecimal progress = t.getTargetAmount().compareTo(BigDecimal.ZERO) > 0
                ? t.getSavedAmount().divide(t.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        map.put("progressPercent", progress);
        map.put("status", t.getStatus());
        map.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().format(FORMATTER) : null);
        map.put("completedAt", t.getCompletedAt() != null ? t.getCompletedAt().format(FORMATTER) : null);
        return map;
    }
}

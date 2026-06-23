package com.finance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.finance.entity.SaveTarget;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 存钱目标 Service
 */
public interface SaveTargetService extends IService<SaveTarget> {

    /** 获取用户目标列表 */
    List<Map<String, Object>> getUserTargets(Long userId, Integer status);

    /** 创建目标 */
    SaveTarget createTarget(SaveTarget target, Long userId);

    /** 更新目标（追加存款/修改信息） */
    SaveTarget updateTarget(Long targetId, String name, BigDecimal targetAmount,
                            BigDecimal addAmount, Long userId, boolean isAdmin);

    /** 删除目标 */
    void deleteTarget(Long targetId, Long userId, boolean isAdmin);
}

package com.finance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.finance.entity.Bill;

import java.util.List;
import java.util.Map;

/**
 * 账单 Service
 */
public interface BillService extends IService<Bill> {

    /** 新增账单（联网在线记账） */
    Bill addBill(Bill bill, Long userId);

    /** 批量离线同步 */
    Map<String, Object> syncBatch(List<Map<String, Object>> offlineBills, Long userId);

    /** 获取用户账单详情 */
    Map<String, Object> getBillDetail(Long billId, Long userId);

    /** 修改账单 */
    Bill updateBill(Long billId, Bill bill, Long userId, boolean isAdmin);

    /** 删除账单 */
    void deleteBill(Long billId, Long userId, boolean isAdmin);

    /** 管理员无条件删除 */
    void adminDeleteBill(Long billId);
}

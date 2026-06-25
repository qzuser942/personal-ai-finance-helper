package com.finance.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局业务错误码枚举
 *
 * @author 胡宪棋
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ==================== 用户认证 1xxxx ====================
    USERNAME_EXISTS(10001, "用户名已存在"),
    USERNAME_PASSWORD_ERROR(10002, "用户名或密码错误"),
    ACCOUNT_FROZEN(10003, "账号已被冻结，请联系管理员"),
    TOKEN_EXPIRED(10004, "登录已过期，请重新登录"),
    OLD_PASSWORD_ERROR(10005, "旧密码验证失败"),
    PASSWORD_NOT_MATCH(10006, "两次密码输入不一致"),
    USERNAME_FORMAT_ERROR(10007, "用户名格式不合法（4-20字符，字母数字下划线）"),
    PASSWORD_FORMAT_ERROR(10008, "密码格式不合法（6-20字符）"),
    TOKEN_INVALID(10009, "令牌无效"),
    UNAUTHORIZED(10010, "未登录，请先登录"),

    // ==================== 账单业务 2xxxx ====================
    BILL_NOT_OWNED(20001, "无权操作此账单"),
    BILL_NOT_FOUND(20002, "账单不存在"),
    AMOUNT_MUST_POSITIVE(20003, "金额必须大于0"),
    CATEGORY_NOT_AVAILABLE(20004, "分类不存在或不可用"),
    CONSUME_TIME_FORMAT_ERROR(20005, "消费时间格式不正确"),

    // ==================== 存钱目标 25xxx ====================
    // P1-2 修复：错误码语义错位（BILL_NOT_FOUND 表示"存钱目标不存在"）
    TARGET_NOT_FOUND(25001, "存钱目标不存在"),
    TARGET_NOT_OWNED(25002, "无权操作此存钱目标"),
    TARGET_ALREADY_COMPLETED(25003, "存钱目标已达成"),

    // ==================== 预算 26xxx ====================
    // P1-3 修复：错误码语义错位
    BUDGET_NOT_FOUND(26001, "预算不存在"),
    BUDGET_NOT_OWNED(26002, "无权操作此预算"),
    BUDGET_AMOUNT_INVALID(26003, "预算金额必须大于0"),

    // ==================== 分类业务 3xxxx ====================
    CATEGORY_NAME_EXISTS(30001, "分类名称已存在"),
    SYSTEM_CATEGORY_PROTECTED(30002, "系统内置分类不可修改或删除"),
    CATEGORY_HAS_BILLS(30003, "该分类下有关联账单，无法删除"),

    // ==================== AI分析 4xxxx ====================
    AI_NO_BILL_DATA(40001, "该月份无账单数据，无法分析"),
    AI_SERVICE_ERROR(40002, "AI服务调用失败，请稍后重试"),
    AI_PARSE_ERROR(40003, "AI返回结果解析失败，请重试"),
    AI_VECTOR_STORE_ERROR(40004, "向量数据库服务异常，请检查Qdrant服务状态"),
    AI_EMBEDDING_ERROR(40005, "特征向量生成失败，请检查阿里云百炼API配置"),
    AI_CONFIG_INVALID(40006, "AI配置参数无效，请检查application.yml配置"),

    // ==================== 文件 5xxxx ====================
    FILE_TYPE_NOT_ALLOWED(50001, "文件类型不允许，仅支持 jpg/png/webp"),
    FILE_SIZE_EXCEEDED(50002, "文件大小超过限制（10MB）"),
    FILE_UPLOAD_FAILED(50003, "文件上传失败，请重试"),
    FILE_NOT_FOUND(50004, "文件不存在"),

    // ==================== 管理员业务 6xxxx ====================
    NEED_SUPER_ADMIN(60001, "需要超级管理员权限"),
    CANNOT_OPERATE_SELF(60002, "不能操作自身账号"),
    TARGET_USER_NOT_FOUND(60003, "目标用户不存在"),
    DB_BACKUP_FAILED(60004, "数据库备份失败，请检查数据库服务"),
    TARGET_ADMIN_NOT_FOUND(60005, "目标管理员不存在"),
    LAST_SUPER_ADMIN(60006, "系统至少保留1名超级管理员"),
    PASSWORD_TOO_WEAK(60007, "密码强度不足，至少8位"),

    // ==================== 离线同步 7xxxx ====================
    SYNC_DATA_FORMAT_ERROR(70001, "批量同步数据格式错误"),
    SYNC_CATEGORY_NOT_FOUND(70002, "同步账单中分类ID不存在"),

    // ==================== 通用 ====================
    PARAM_ERROR(90001, "参数校验失败"),
    INTERNAL_ERROR(90002, "服务器内部错误"),
    FORBIDDEN(90003, "权限不足");

    private final Integer code;
    private final String message;
}

package com.finance.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.finance.entity.SysAdmin;
import com.finance.exception.BusinessException;
import com.finance.exception.ErrorCode;
import com.finance.mapper.SysAdminMapper;
import com.finance.service.SysAdminService;
import com.finance.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysAdminServiceImpl extends ServiceImpl<SysAdminMapper, SysAdmin> implements SysAdminService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil;

    @Override
    public String login(String username, String password) {
        SysAdmin admin = baseMapper.findByUsername(username);
        if (admin == null) {
            throw new BusinessException(ErrorCode.USERNAME_PASSWORD_ERROR);
        }
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new BusinessException(ErrorCode.USERNAME_PASSWORD_ERROR);
        }

        baseMapper.updateLastLoginTime(admin.getId());
        admin.setLastLoginTime(LocalDateTime.now());

        String token = jwtUtil.generateAdminToken(admin.getId(), admin.getUsername(), admin.getRole());
        log.info("管理员{}登录成功，角色: {}", username, admin.getRole());
        return token;
    }

    @Override
    @Transactional
    public SysAdmin register(String username, String password, String role) {
        SysAdmin existing = baseMapper.findByUsername(username);
        if (existing != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        SysAdmin admin = new SysAdmin();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(role != null ? role : "OPERATOR");
        save(admin);
        log.info("创建管理员账号: {}，角色: {}", username, role);
        return admin;
    }

    @Override
    @Transactional
    public String resetPassword(Long adminId) {
        SysAdmin admin = getById(adminId);
        if (admin == null) {
            throw new BusinessException(ErrorCode.TARGET_ADMIN_NOT_FOUND);
        }
        // 关键修复：使用 SecureRandom 生成 12 位字母+数字密码，强度从 8 位 UUID 截断提升到 12 位 62 字符集
        String newPassword = generateStrongPassword(12);
        admin.setPassword(passwordEncoder.encode(newPassword));
        updateById(admin);
        log.info("重置管理员{}密码", admin.getUsername());
        return newPassword;
    }

    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 生成指定长度的强密码（去掉了易混淆的 0/O/1/l/I） */
    private String generateStrongPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}

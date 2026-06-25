package com.finance.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.finance.entity.SysUser;
import com.finance.exception.BusinessException;
import com.finance.exception.ErrorCode;
import com.finance.mapper.SysUserMapper;
import com.finance.service.SysUserService;
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
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public SysUser register(String username, String password, String confirmPassword) {
        // 校验用户名格式
        if (!username.matches("^[a-zA-Z0-9_]{4,20}$")) {
            throw new BusinessException(ErrorCode.USERNAME_FORMAT_ERROR);
        }
        // 校验密码格式
        if (password.length() < 6 || password.length() > 20) {
            throw new BusinessException(ErrorCode.PASSWORD_FORMAT_ERROR);
        }
        // 校验两次密码一致
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }
        // 校验用户名唯一性
        SysUser existing = baseMapper.findByUsername(username);
        if (existing != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setStatus(1);
        save(user);

        log.info("用户注册成功: {}", username);
        return user;
    }

    @Override
    public String login(String username, String password) {
        SysUser user = baseMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.USERNAME_PASSWORD_ERROR);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_FROZEN);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.USERNAME_PASSWORD_ERROR);
        }

        // 更新最后登录时间
        baseMapper.updateLastLoginTime(user.getId());
        user.setLastLoginTime(LocalDateTime.now());

        String token = jwtUtil.generateUserToken(user.getId(), user.getUsername());
        log.info("用户登录成功: {}", username);
        return token;
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword, String confirmPassword) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USERNAME_PASSWORD_ERROR);
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_ERROR);
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            throw new BusinessException(ErrorCode.PASSWORD_FORMAT_ERROR);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
        log.info("用户{}修改密码成功", user.getUsername());
    }

    @Override
    @Transactional
    public SysUser updateProfile(Long userId, String username) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USERNAME_PASSWORD_ERROR);
        }
        if (username != null && !username.equals(user.getUsername())) {
            if (!username.matches("^[a-zA-Z0-9_]{4,20}$")) {
                throw new BusinessException(ErrorCode.USERNAME_FORMAT_ERROR);
            }
            SysUser existing = baseMapper.findByUsername(username);
            if (existing != null && !existing.getId().equals(userId)) {
                throw new BusinessException(ErrorCode.USERNAME_EXISTS);
            }
            user.setUsername(username);
        }
        updateById(user);
        log.info("用户{}修改信息成功", user.getUsername());
        return user;
    }

    @Override
    @Transactional
    public String resetPassword(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.TARGET_USER_NOT_FOUND);
        }
        // 关键修复：使用 SecureRandom 生成 12 位字母+数字密码，强度从 8 位 UUID 截断提升到 12 位 62 字符集
        String newPassword = generateStrongPassword(12);
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
        log.info("管理员重置用户{}密码", user.getUsername());
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

    @Override
    @Transactional
    public void updateStatus(Long userId, Integer status) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.TARGET_USER_NOT_FOUND);
        }
        user.setStatus(status);
        updateById(user);
        log.info("用户{}状态变更为: {}", user.getUsername(), status == 1 ? "正常" : "冻结");
    }
}

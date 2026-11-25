package com.city.manhole.controller;

import com.city.manhole.common.BizException;
import com.city.manhole.common.Result;
import com.city.manhole.entity.SysUserEntity;
import com.city.manhole.mapper.SysUserMapper;
import com.city.manhole.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证与个人中心接口
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SysUserMapper userMapper;

    public AuthController(AuthService authService, SysUserMapper userMapper) {
        this.authService = authService;
        this.userMapper = userMapper;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return Result.ok("登录成功", authService.login(body.get("username"), body.get("password")));
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.ok(authService.me());
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok("已退出登录", null);
    }

    @PutMapping("/password")
    public Result<Void> password(@RequestBody Map<String, String> body) {
        authService.changePassword(body.get("oldPassword"), body.get("newPassword"));
        return Result.ok("密码已更新，请重新登录", null);
    }

    @PutMapping("/profile")
    public Result<Void> profile(@RequestBody Map<String, String> body) {
        authService.updateProfile(body.get("realName"), body.get("phone"), body.get("email"), body.get("avatar"));
        return Result.ok("资料已保存", null);
    }

    /** 账号唯一性校验（新增用户时用） */
    @GetMapping("/exists")
    public Result<Boolean> exists(@RequestParam String username) {
        Long c = userMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, username));
        if (c == null) {
            throw new BizException("查询失败");
        }
        return Result.ok(c > 0);
    }
}

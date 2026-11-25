package com.city.manhole.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.city.manhole.common.BizException;
import com.city.manhole.common.JwtUtil;
import com.city.manhole.common.LoginUser;
import com.city.manhole.common.UserContext;
import com.city.manhole.entity.SysMenuEntity;
import com.city.manhole.entity.SysRoleEntity;
import com.city.manhole.entity.SysRoleMenuEntity;
import com.city.manhole.entity.SysUserEntity;
import com.city.manhole.mapper.SysMenuMapper;
import com.city.manhole.mapper.SysRoleMapper;
import com.city.manhole.mapper.SysRoleMenuMapper;
import com.city.manhole.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 登录、菜单、个人中心。
 * 连续 5 次失败锁定 15 分钟；密码 BCrypt 校验；任何返回都不含 password 字段
 */
@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(SysUserMapper userMapper, SysRoleMapper roleMapper, SysMenuMapper menuMapper,
                       SysRoleMenuMapper roleMenuMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> login(String username, String password) {
        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            throw new BizException("请输入用户名和密码");
        }
        SysUserEntity user = userMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, username.trim()));
        if (user == null) {
            throw new BizException("用户名或密码错误");
        }
        if (user.getLockTime() != null && user.getLockTime().isAfter(LocalDateTime.now())) {
            throw new BizException(423, "账号已锁定，请于 " + user.getLockTime().toLocalTime().withNano(0) + " 后重试");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            int fail = user.getFailCount() == null ? 0 : user.getFailCount() + 1;
            user.setFailCount(fail);
            if (fail >= 5) {
                user.setLockTime(LocalDateTime.now().plusMinutes(15));
                user.setFailCount(0);
            }
            userMapper.updateById(user);
            int left = Math.max(5 - fail, 0);
            throw new BizException("用户名或密码错误" + (left > 0 ? "，今日还可尝试 " + left + " 次" : "，账号已锁定 15 分钟"));
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已停用，请联系管理员");
        }
        user.setFailCount(0);
        user.setLockTime(null);
        user.setLastLogin(LocalDateTime.now());
        userMapper.updateById(user);

        SysRoleEntity role = roleMapper.selectById(user.getRoleId());
        String roleCode = role == null ? "USER" : role.getRoleCode();
        String token = jwtUtil.create(user.getId(), user.getUsername(), user.getRealName(), roleCode, user.getDeptId());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("user", profileOf(user, role));
        data.put("menus", menus(user.getRoleId()));
        return data;
    }

    public Map<String, Object> me() {
        LoginUser cu = UserContext.require();
        SysUserEntity user = userMapper.selectById(cu.getId());
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        SysRoleEntity role = roleMapper.selectById(user.getRoleId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", profileOf(user, role));
        data.put("menus", menus(user.getRoleId()));
        return data;
    }

    private Map<String, Object> profileOf(SysUserEntity user, SysRoleEntity role) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("realName", user.getRealName());
        m.put("phone", user.getPhone());
        m.put("email", user.getEmail());
        m.put("avatar", user.getAvatar());
        m.put("deptId", user.getDeptId());
        m.put("teamName", user.getTeamName());
        m.put("roleName", role == null ? "" : role.getRoleName());
        m.put("roleCode", role == null ? "" : role.getRoleCode());
        m.put("dataScope", role == null ? "SELF" : role.getDataScope());
        m.put("lastLogin", user.getLastLogin());
        return m;
    }

    /** 按角色取菜单并组装成树；菜单管理（id=17）不进入业务菜单树 */
    public List<Map<String, Object>> menus(Long roleId) {
        List<Long> ids = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenuEntity>()
                        .eq(SysRoleMenuEntity::getRoleId, roleId))
                .stream().map(SysRoleMenuEntity::getMenuId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return List.of();
        }
        List<SysMenuEntity> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenuEntity>()
                .in(SysMenuEntity::getId, ids)
                .eq(SysMenuEntity::getStatus, 1)
                .eq(SysMenuEntity::getVisible, 1)
                .orderByAsc(SysMenuEntity::getSort));
        Map<Long, List<SysMenuEntity>> byParent = all.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        return build(0L, byParent);
    }

    private List<Map<String, Object>> build(Long parent, Map<Long, List<SysMenuEntity>> byParent) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysMenuEntity m : byParent.getOrDefault(parent, List.of())) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", m.getId());
            node.put("name", m.getMenuName());
            node.put("path", m.getPath());
            node.put("component", m.getComponent());
            node.put("icon", m.getIcon());
            node.put("type", m.getMenuType());
            node.put("perm", m.getPerm());
            List<Map<String, Object>> children = build(m.getId(), byParent);
            node.put("children", children);
            result.add(node);
        }
        return result;
    }

    public void changePassword(String oldPwd, String newPwd) {
        if (newPwd == null || newPwd.length() < 8 || newPwd.length() > 20
                || !newPwd.matches(".*[A-Za-z].*") || !newPwd.matches(".*\\d.*")) {
            throw new BizException("新密码需 8-20 位且同时包含字母与数字");
        }
        SysUserEntity user = userMapper.selectById(UserContext.id());
        if (!passwordEncoder.matches(oldPwd == null ? "" : oldPwd, user.getPassword())) {
            throw new BizException("当前密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPwd));
        userMapper.updateById(user);
    }

    public void updateProfile(String realName, String phone, String email, String avatar) {
        SysUserEntity user = userMapper.selectById(UserContext.id());
        if (realName != null && !realName.isBlank()) {
            user.setRealName(realName.trim());
        }
        user.setPhone(phone);
        user.setEmail(email);
        if (avatar != null && !avatar.isBlank()) {
            user.setAvatar(avatar);
        }
        userMapper.updateById(user);
    }
}

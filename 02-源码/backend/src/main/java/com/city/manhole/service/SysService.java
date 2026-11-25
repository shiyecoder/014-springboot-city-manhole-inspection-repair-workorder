package com.city.manhole.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.city.manhole.common.BizException;
import com.city.manhole.common.PageResult;
import com.city.manhole.common.UserContext;
import com.city.manhole.entity.*;
import com.city.manhole.mapper.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统管理：用户、角色、菜单、字典、消息。
 * 菜单管理（id=17）不出现在「分配菜单」的可选项中，避免把系统管理入口二次下发。
 */
@Service
public class SysService {

    public static final long MENU_MANAGE_ID = 17L;

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysDictMapper dictMapper;
    private final SysMessageMapper messageMapper;
    private final SysDeptMapper deptMapper;
    private final PasswordEncoder passwordEncoder;

    public SysService(SysUserMapper userMapper, SysRoleMapper roleMapper, SysMenuMapper menuMapper,
                      SysRoleMenuMapper roleMenuMapper, SysDictMapper dictMapper, SysMessageMapper messageMapper,
                      SysDeptMapper deptMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.dictMapper = dictMapper;
        this.messageMapper = messageMapper;
        this.deptMapper = deptMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResult<Map<String, Object>> users(long page, long size, String keyword, Long roleId, Integer status) {
        LambdaQueryWrapper<SysUserEntity> w = new LambdaQueryWrapper<SysUserEntity>()
                .and(keyword != null && !keyword.isBlank(), q -> q.like(SysUserEntity::getUsername, keyword)
                        .or().like(SysUserEntity::getRealName, keyword)
                        .or().like(SysUserEntity::getPhone, keyword))
                .eq(roleId != null, SysUserEntity::getRoleId, roleId)
                .eq(status != null, SysUserEntity::getStatus, status)
                .orderByAsc(SysUserEntity::getId);
        Page<SysUserEntity> p = userMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> list = new ArrayList<>();
        for (SysUserEntity u : p.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("phone", u.getPhone());
            m.put("email", u.getEmail());
            m.put("avatar", u.getAvatar());
            m.put("roleId", u.getRoleId());
            m.put("deptId", u.getDeptId());
            m.put("teamName", u.getTeamName());
            m.put("status", u.getStatus());
            m.put("lastLogin", u.getLastLogin());
            m.put("createTime", u.getCreateTime());
            SysRoleEntity r = u.getRoleId() == null ? null : roleMapper.selectById(u.getRoleId());
            m.put("roleName", r == null ? "" : r.getRoleName());
            SysDeptEntity d = u.getDeptId() == null ? null : deptMapper.selectById(u.getDeptId());
            m.put("deptName", d == null ? "" : d.getDeptName());
            list.add(m);
        }
        return new PageResult<>(p.getTotal(), page, size, list);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveUser(Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(body.get("id").toString());
        String username = (String) body.get("username");
        String realName = (String) body.get("realName");
        if (username == null || username.isBlank()) {
            throw new BizException("登录名不能为空");
        }
        if (realName == null || realName.isBlank()) {
            throw new BizException("姓名不能为空");
        }
        Long dup = userMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, username.trim()).ne(id != null, SysUserEntity::getId, id));
        if (dup != null && dup > 0) {
            throw new BizException("登录名已存在");
        }
        SysUserEntity u = id == null ? new SysUserEntity() : userMapper.selectById(id);
        if (u == null) {
            throw new BizException("用户不存在");
        }
        u.setUsername(username.trim());
        u.setRealName(realName);
        u.setPhone((String) body.get("phone"));
        u.setEmail((String) body.get("email"));
        if (body.get("roleId") != null) {
            u.setRoleId(Long.valueOf(body.get("roleId").toString()));
        }
        if (body.get("deptId") != null) {
            u.setDeptId(Long.valueOf(body.get("deptId").toString()));
        }
        u.setTeamName((String) body.get("teamName"));
        u.setStatus(body.get("status") == null ? 1 : Integer.valueOf(body.get("status").toString()));
        if (id == null) {
            String pwd = (String) body.get("password");
            u.setPassword(passwordEncoder.encode(pwd == null || pwd.isBlank() ? "123456" : pwd));
            u.setDeleted(0);
            u.setVersion(0);
            u.setCreateTime(LocalDateTime.now());
            userMapper.insert(u);
        } else {
            u.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(u);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String password) {
        SysUserEntity u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException("用户不存在");
        }
        u.setPassword(passwordEncoder.encode(password == null || password.isBlank() ? "123456" : password));
        u.setFailCount(0);
        u.setLockTime(null);
        userMapper.updateById(u);
    }

    public List<SysRoleEntity> roles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>().orderByAsc(SysRoleEntity::getId));
    }

    public List<SysDeptEntity> depts() {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDeptEntity>().orderByAsc(SysDeptEntity::getSort));
    }

    /** 菜单树（管理用，包含所有菜单） */
    public List<Map<String, Object>> menuTree(boolean forAssign) {
        List<SysMenuEntity> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenuEntity>()
                .orderByAsc(SysMenuEntity::getSort));
        if (forAssign) {
            all = all.stream().filter(m -> !Objects.equals(m.getId(), MENU_MANAGE_ID)).toList();
        }
        Map<Long, List<SysMenuEntity>> byParent = all.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        return buildTree(0L, byParent, forAssign);
    }

    private List<Map<String, Object>> buildTree(Long parent, Map<Long, List<SysMenuEntity>> byParent, boolean forAssign) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (SysMenuEntity m : byParent.getOrDefault(parent, List.of())) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", m.getId());
            node.put("label", m.getMenuName());
            node.put("name", m.getMenuName());
            node.put("path", m.getPath());
            node.put("type", m.getMenuType());
            node.put("perm", m.getPerm());
            node.put("sort", m.getSort());
            List<Map<String, Object>> children = buildTree(m.getId(), byParent, forAssign);
            node.put("children", children);
            list.add(node);
        }
        return list;
    }

    public List<Long> roleMenus(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenuEntity>()
                        .eq(SysRoleMenuEntity::getRoleId, roleId))
                .stream().map(SysRoleMenuEntity::getMenuId)
                .filter(id -> !Objects.equals(id, MENU_MANAGE_ID)).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BizException("角色不存在");
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenuEntity>().eq(SysRoleMenuEntity::getRoleId, roleId));
        if (menuIds != null) {
            for (Long mid : menuIds.stream().distinct().toList()) {
                if (Objects.equals(mid, MENU_MANAGE_ID)) {
                    continue;
                }
                SysRoleMenuEntity rm = new SysRoleMenuEntity();
                rm.setRoleId(roleId);
                rm.setMenuId(mid);
                roleMenuMapper.insert(rm);
            }
        }
    }

    public List<SysDictEntity> dicts(String type) {
        return dictMapper.selectList(new LambdaQueryWrapper<SysDictEntity>()
                .eq(type != null && !type.isBlank(), SysDictEntity::getDictType, type)
                .orderByAsc(SysDictEntity::getDictType).orderByAsc(SysDictEntity::getSort));
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveDict(SysDictEntity dict) {
        if (dict.getDictType() == null || dict.getDictLabel() == null) {
            throw new BizException("字典类型与显示名不能为空");
        }
        if (dict.getId() == null) {
            dictMapper.insert(dict);
        } else {
            dictMapper.updateById(dict);
        }
    }

    public PageResult<SysMessageEntity> messages(long page, long size, Boolean unread) {
        Long uid = UserContext.id();
        LambdaQueryWrapper<SysMessageEntity> w = new LambdaQueryWrapper<SysMessageEntity>()
                .eq(SysMessageEntity::getUserId, uid)
                .eq(Boolean.TRUE.equals(unread), SysMessageEntity::getReadFlag, 0)
                .orderByDesc(SysMessageEntity::getCreateTime);
        Page<SysMessageEntity> p = messageMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(p.getTotal(), page, size, p.getRecords());
    }

    public long unreadCount() {
        Long c = messageMapper.selectCount(new LambdaQueryWrapper<SysMessageEntity>()
                .eq(SysMessageEntity::getUserId, UserContext.id()).eq(SysMessageEntity::getReadFlag, 0));
        return c == null ? 0 : c;
    }

    public void readMessage(Long id) {
        messageMapper.update(null, new LambdaUpdateWrapper<SysMessageEntity>()
                .eq(SysMessageEntity::getId, id)
                .eq(SysMessageEntity::getUserId, UserContext.id())
                .set(SysMessageEntity::getReadFlag, 1));
    }

    public void readAll() {
        messageMapper.update(null, new LambdaUpdateWrapper<SysMessageEntity>()
                .eq(SysMessageEntity::getUserId, UserContext.id())
                .set(SysMessageEntity::getReadFlag, 1));
    }
}

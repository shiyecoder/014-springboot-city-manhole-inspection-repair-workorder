package com.city.manhole.controller;

import com.city.manhole.common.PageResult;
import com.city.manhole.common.Result;
import com.city.manhole.entity.*;
import com.city.manhole.service.SysService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统管理接口：用户、角色、菜单、字典、消息
 */
@RestController
@RequestMapping("/api/sys")
public class SysController {

    private final SysService sysService;

    public SysController(SysService sysService) {
        this.sysService = sysService;
    }

    @GetMapping("/user/page")
    public Result<PageResult<Map<String, Object>>> users(@RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "10") long size,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) Long roleId,
                                                         @RequestParam(required = false) Integer status) {
        return Result.ok(sysService.users(page, size, keyword, roleId, status));
    }

    @PostMapping("/user")
    public Result<Void> saveUser(@RequestBody Map<String, Object> body) {
        sysService.saveUser(body);
        return Result.ok("已保存", null);
    }

    @PostMapping("/user/{id}/reset")
    public Result<Void> reset(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        sysService.resetPassword(id, body == null ? null : body.get("password"));
        return Result.ok("密码已重置为 123456", null);
    }

    @GetMapping("/roles")
    public Result<List<SysRoleEntity>> roles() {
        return Result.ok(sysService.roles());
    }

    @GetMapping("/depts")
    public Result<List<SysDeptEntity>> depts() {
        return Result.ok(sysService.depts());
    }

    @GetMapping("/menus")
    public Result<List<Map<String, Object>>> menus(@RequestParam(defaultValue = "false") boolean forAssign) {
        return Result.ok(sysService.menuTree(forAssign));
    }

    @GetMapping("/role/{roleId}/menus")
    public Result<List<Long>> roleMenus(@PathVariable Long roleId) {
        return Result.ok(sysService.roleMenus(roleId));
    }

    @PostMapping("/role/{roleId}/menus")
    public Result<Void> assign(@PathVariable Long roleId, @RequestBody Map<String, List<Long>> body) {
        sysService.assignMenus(roleId, body.get("menuIds"));
        return Result.ok("菜单权限已更新", null);
    }

    @GetMapping("/dicts")
    public Result<List<SysDictEntity>> dicts(@RequestParam(required = false) String type) {
        return Result.ok(sysService.dicts(type));
    }

    @PostMapping("/dict")
    public Result<Void> saveDict(@RequestBody SysDictEntity dict) {
        sysService.saveDict(dict);
        return Result.ok("已保存", null);
    }

    @GetMapping("/message/page")
    public Result<PageResult<SysMessageEntity>> messages(@RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "10") long size,
                                                         @RequestParam(required = false) Boolean unread) {
        return Result.ok(sysService.messages(page, size, unread));
    }

    @GetMapping("/message/unread")
    public Result<Long> unread() {
        return Result.ok(sysService.unreadCount());
    }

    @PostMapping("/message/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        sysService.readMessage(id);
        return Result.ok();
    }

    @PostMapping("/message/read-all")
    public Result<Void> readAll() {
        sysService.readAll();
        return Result.ok();
    }
}

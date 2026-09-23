package com.itheima.controller;

import com.itheima.anno.RequireAdmin;
import com.itheima.anno.RequireSuperAdmin;
import com.itheima.pojo.RegisterRequest;
import com.itheima.pojo.Result;
import com.itheima.pojo.User;
import com.itheima.service.UserService;
import com.itheima.utils.JwtUtil;
import com.itheima.utils.Md5Util;
import com.itheima.utils.ThreadLocalUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public Result register(@Valid @RequestBody RegisterRequest request) {
        userService.submitRegistration(request);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<String> login(@Pattern(regexp = "^\\S{5,16}$") String username,
                                @Pattern(regexp = "^\\S{5,16}$") String password) {
        User loginUser = userService.findByUserName(username);
        if (loginUser == null) {
            return Result.error("用户名错误");
        }
        if (!Md5Util.checkPassword(password, loginUser.getPassword())) {
            return Result.error("密码错误");
        }
        if ("待审核".equals(loginUser.getRegistrationStatus())) {
            return Result.error("账号已提交注册审核，请等待管理员审批");
        }
        if ("已拒绝".equals(loginUser.getRegistrationStatus())) {
            String reason = loginUser.getRegistrationRejectReason();
            return Result.error("注册审核未通过" + (StringUtils.hasText(reason) ? "：" + reason : ""));
        }
        if (!"已通过".equals(loginUser.getRegistrationStatus())) {
            return Result.error("账号状态异常，请联系管理员");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", loginUser.getId());
        claims.put("username", loginUser.getUsername());
        claims.put("role", loginUser.getRole());
        return Result.success(jwtUtil.genToken(claims));
    }

    @GetMapping("/userInfo")
    public Result<User> userInfo() {
        Map<String, Object> map = ThreadLocalUtil.get();
        String username = (String) map.get("username");
        return Result.success(userService.findByUserName(username));
    }

    @PatchMapping("/updatePwd")
    public Result updatePwd(@RequestBody Map<String, String> params) {
        String oldPwd = params.get("old_pwd");
        String newPwd = params.get("new_pwd");
        String rePwd = params.get("re_pwd");
        if (!StringUtils.hasLength(oldPwd) || !StringUtils.hasLength(newPwd) || !StringUtils.hasLength(rePwd)) {
            return Result.error("缺少必要的参数");
        }
        Map<String, Object> map = ThreadLocalUtil.get();
        String username = (String) map.get("username");
        User loginUser = userService.findByUserName(username);
        if (!Md5Util.checkPassword(oldPwd, loginUser.getPassword())) {
            return Result.error("原密码填写不正确");
        }
        if (!rePwd.equals(newPwd)) {
            return Result.error("两次密码不一致");
        }
        userService.updatePwd(newPwd);
        return Result.success();
    }

    @GetMapping("/list")
    @RequireSuperAdmin
    public Result<List<User>> list() {
        return Result.success(userService.list());
    }

    @GetMapping("/registration/list")
    @RequireAdmin
    public Result<List<User>> registrationList(@RequestParam(required = false) String status) {
        List<User> applications = userService.listRegistrationApplications();
        if (StringUtils.hasText(status)) {
            applications = applications.stream().filter(user -> status.equals(user.getRegistrationStatus())).toList();
        }
        return Result.success(applications);
    }

    @PutMapping("/registration/{id}/approve")
    @RequireAdmin
    public Result approveRegistration(@PathVariable Integer id) {
        userService.approveRegistration(id, currentUserId());
        return Result.success();
    }

    @PutMapping("/registration/{id}/reject")
    @RequireAdmin
    public Result rejectRegistration(@PathVariable Integer id, @RequestBody Map<String, String> params) {
        userService.rejectRegistration(id, currentUserId(), params.get("reason"));
        return Result.success();
    }

    @PutMapping("/{id}/role")
    @RequireSuperAdmin
    public Result updateRole(@PathVariable Integer id, @RequestBody Map<String, String> params) {
        String newRole = params.get("role");
        if (!"super_admin".equals(newRole) && !"admin".equals(newRole) && !"user".equals(newRole)) {
            return Result.error("无效的角色");
        }
        userService.updateRole(id, newRole);
        return Result.success();
    }

    private Integer currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return (Integer) claims.get("id");
    }
}

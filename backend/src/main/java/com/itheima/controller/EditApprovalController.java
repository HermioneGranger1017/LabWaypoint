package com.itheima.controller;

import com.itheima.anno.RequireAdmin;
import com.itheima.pojo.EditApproval;
import com.itheima.pojo.Result;
import com.itheima.service.EditApprovalService;
import com.itheima.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/edit-approval")
public class EditApprovalController {

    @Autowired
    private EditApprovalService editApprovalService;

    /** 发起编辑申请。 */
    @PostMapping
    @RequireAdmin
    public Result create(@RequestBody Map<String, Object> body) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        String role = (String) claims.get("role");
        String targetType = (String) body.get("targetType");
        Integer targetId = body.get("targetId") == null ? null
                : Integer.valueOf(String.valueOf(body.get("targetId")));
        String reason = (String) body.get("reason");
        editApprovalService.createRequest(targetType, targetId, reason, userId, role);
        return Result.success();
    }

    /** 审批：通过/驳回。 */
    @PutMapping("/{id}")
    @RequireAdmin
    public Result approve(@PathVariable Integer id, @RequestBody Map<String, String> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer ownerId = (Integer) claims.get("id");
        String role = (String) claims.get("role");
        editApprovalService.approveRequest(id, params.get("status"), ownerId, role);
        return Result.success();
    }

    /** 当前创建者收到的申请（待我审批）。 */
    @GetMapping("/inbox")
    @RequireAdmin
    public Result<List<EditApproval>> inbox(@RequestParam(required = false) String status) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return Result.success(editApprovalService.listInbox(userId, status));
    }

    /** 当前用户已发起的申请。 */
    @GetMapping("/outbox")
    @RequireAdmin
    public Result<List<EditApproval>> outbox(@RequestParam(required = false) String status) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return Result.success(editApprovalService.listOutbox(userId, status));
    }

    /** 查询当前用户可否编辑/申请。 */
    @GetMapping("/permission")
    public Result<EditApproval> permission(@RequestParam String targetType,
                                           @RequestParam Integer targetId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        String role = (String) claims.get("role");
        return Result.success(editApprovalService.getPermission(targetType, targetId, userId, role));
    }
}
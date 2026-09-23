package com.itheima.service.impl;

import com.itheima.mapper.DeviceMapper;
import com.itheima.mapper.EditApprovalMapper;
import com.itheima.mapper.InstructionMapper;
import com.itheima.pojo.EditApproval;
import com.itheima.service.EditApprovalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EditApprovalServiceImpl implements EditApprovalService {

    @Autowired
    private EditApprovalMapper editApprovalMapper;
    @Autowired
    private InstructionMapper instructionMapper;
    @Autowired
    private DeviceMapper deviceMapper;

    @Override
    public void createRequest(String targetType, Integer targetId, String reason,
                             Integer applicantId, String role) {
        if (!"admin".equals(role)) {
            // user 拒绝；super_admin 不需要申请
            if ("user".equals(role)) throw new RuntimeException("普通用户无编辑权限");
            throw new RuntimeException("超级管理员无需申请即可编辑");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new RuntimeException("申请理由至少 5 个字符");
        }
        if (!"device".equals(targetType) && !"instruction".equals(targetType)) {
            throw new RuntimeException("不支持的审核目标类型");
        }
        Integer ownerId = resolveOwner(targetType, targetId);
        if (ownerId == null) throw new RuntimeException("审核目标不存在");
        if (ownerId.equals(applicantId)) throw new RuntimeException("无需申请编辑自己的内容");
        if (editApprovalMapper.countPending(targetType, targetId, applicantId) > 0) {
            throw new RuntimeException("已有一条申请正在待审批，请等待对方处理");
        }
        EditApproval ea = new EditApproval();
        ea.setTargetType(targetType);
        ea.setTargetId(targetId);
        ea.setApplicantId(applicantId);
        ea.setOwnerId(ownerId);
        ea.setReason(reason.trim());
        ea.setStatus("pending");
        editApprovalMapper.add(ea);
    }

    @Override
    @Transactional
    public void approveRequest(Integer approvalId, String status, Integer ownerId, String role) {
        if (!"approved".equals(status) && !"rejected".equals(status)) {
            throw new RuntimeException("审批结果必须是 approved 或 rejected");
        }
        EditApproval ea = editApprovalMapper.findById(approvalId);
        if (ea == null) throw new RuntimeException("申请不存在");
        if (!"pending".equals(ea.getStatus())) throw new RuntimeException("该申请已处理");
        if (!ea.getOwnerId().equals(ownerId)) throw new RuntimeException("仅原作者可审批");
        editApprovalMapper.updateStatus(approvalId, status);
    }

    @Override
    public List<EditApproval> listInbox(Integer ownerId, String status) {
        return editApprovalMapper.listInbox(ownerId, status == null ? "" : status);
    }

    @Override
    public List<EditApproval> listOutbox(Integer applicantId, String status) {
        return editApprovalMapper.listOutbox(applicantId, status == null ? "" : status);
    }

    @Override
    public EditApproval getPermission(String targetType, Integer targetId, Integer userId, String role) {
        Integer ownerId = resolveOwner(targetType, targetId);
        boolean canEdit = "super_admin".equals(role) || (ownerId != null && ownerId.equals(userId));
        if (canEdit) {
            EditApproval r = new EditApproval();
            r.setTargetType(targetType);
            r.setTargetId(targetId);
            r.setStatus("edit_direct");
            return r;
        }
        // 查最近一条有效批准（未使用）
        EditApproval unused = editApprovalMapper.findApprovedUnused(targetType, targetId, userId);
        boolean hasPending = editApprovalMapper.countPending(targetType, targetId, userId) > 0;
        EditApproval r = new EditApproval();
        r.setTargetType(targetType);
        r.setTargetId(targetId);
        if (unused != null) {
            r.setStatus("edit_once");
            r.setId(unused.getId());
            r.setUsedAt(null);
        } else if (hasPending) {
            r.setStatus("pending");
        } else {
            r.setStatus("can_request");
        }
        return r;
    }

    @Override
    @Transactional
    public void consumeApprovedPermission(String targetType, Integer targetId, Integer userId) {
        EditApproval unused = editApprovalMapper.findApprovedUnused(targetType, targetId, userId);
        if (unused == null) {
            throw new RuntimeException("请先申请并获得批准");
        }
        // 消费许可：写 used_at
        editApprovalMapper.markUsed(unused.getId());
    }

    @Override
    public boolean hasApproval(String targetType, Integer targetId, Integer userId) {
        return editApprovalMapper.findApprovedUnused(targetType, targetId, userId) != null;
    }

    /** 依据目标表真实 created_by 计算 ownerId；不存在返回 null。 */
    private Integer resolveOwner(String targetType, Integer targetId) {
        if ("device".equals(targetType)) {
            return deviceMapper.findCreatedBy(targetId);
        }
        if ("instruction".equals(targetType)) {
            return instructionMapper.findCreatedBy(targetId);
        }
        return null;
    }
}
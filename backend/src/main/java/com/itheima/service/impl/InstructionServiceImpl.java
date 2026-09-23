package com.itheima.service.impl;

import com.itheima.mapper.EditApprovalMapper;
import com.itheima.mapper.InstructionMapper;
import com.itheima.pojo.Instruction;
import com.itheima.service.EditApprovalService;
import com.itheima.service.InstructionService;
import com.itheima.utils.HtmlSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstructionServiceImpl implements InstructionService {

    @Autowired
    private InstructionMapper instructionMapper;

    @Autowired
    private EditApprovalService editApprovalService;

    @Override
    public List<Instruction> listByDevice(Integer deviceId) {
        return instructionMapper.listByDevice(deviceId);
    }

    @Override
    public Instruction findById(Integer id) {
        return instructionMapper.findById(id);
    }

    @Override
    public void add(Instruction instruction) {
        // 富文本清洗 + 提取图片 URL
        String safe = HtmlSanitizer.clean(instruction.getContent());
        instruction.setContent(safe);
        instruction.setImageUrls(HtmlSanitizer.toJsonArray(HtmlSanitizer.extractImageUrls(safe)));
        // 未传排序时默认最大 +1
        if (instruction.getStepOrder() == null) {
            Integer max = instructionMapper.findMaxStepOrder(instruction.getDeviceId());
            instruction.setStepOrder((max == null ? 0 : max) + 1);
        }
        instructionMapper.add(instruction);
    }

    @Override
    @Transactional
    public void update(Instruction instruction, Integer userId, String role) {
        Integer creatorId = instructionMapper.findCreatedBy(instruction.getId());
        if (creatorId == null) {
            throw new RuntimeException("使用说明不存在");
        }
        boolean isSuperAdmin = "super_admin".equals(role);
        boolean isOwner = creatorId.equals(userId);
        boolean canEdit = isSuperAdmin || isOwner;
        if (!canEdit && "admin".equals(role)) {
            // 一次性审核许可消费：内部抛"请先申请并获得批准"
            editApprovalService.consumeApprovedPermission("instruction", instruction.getId(), userId);
        }
        if (!canEdit && !"admin".equals(role)) {
            throw new RuntimeException("无权限修改此使用说明");
        }
        // 富文本清洗
        String safe = HtmlSanitizer.clean(instruction.getContent());
        instruction.setContent(safe);
        instruction.setImageUrls(HtmlSanitizer.toJsonArray(HtmlSanitizer.extractImageUrls(safe)));
        instructionMapper.update(instruction);
    }

    @Override
    public void delete(Integer id, Integer userId, String role) {
        Integer creatorId = instructionMapper.findCreatedBy(id);
        if (creatorId == null) {
            throw new RuntimeException("使用说明不存在");
        }
        boolean isSuperAdmin = "super_admin".equals(role);
        boolean isOwner = creatorId.equals(userId);
        if (!isSuperAdmin && !isOwner) {
            throw new RuntimeException("无权限删除此使用说明");
        }
        instructionMapper.delete(id);
    }
}
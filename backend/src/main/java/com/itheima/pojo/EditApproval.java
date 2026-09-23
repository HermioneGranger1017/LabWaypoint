package com.itheima.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EditApproval {
    private Integer id;
    // —— 兼容旧字段，保留但新代码不再使用 ——
    private Integer instructionId;
    // —— 新通用审核字段 ——
    private String targetType;    // device / instruction
    private Integer targetId;
    private Integer applicantId;
    private Integer ownerId;
    private String reason;
    private String status;        // pending / approved / rejected
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // —— 以下为 JOIN 展示字段，不入库 ——
    private String applicantName;
    private String ownerName;
    private String targetTitle;   // 设备名或教程标题
    private Integer deviceId;     // 教程所属设备（device 没有则 null）
}
package com.itheima.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Instruction {
    private Integer id;
    private Integer deviceId;
    private Integer stepOrder;
    private String title;
    private String content;
    private String imageUrls;
    private String coverImageUrl;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // —— 仅 JOIN 返回前端，不入库 ——
    private String authorName;
}
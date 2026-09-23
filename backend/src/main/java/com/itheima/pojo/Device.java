package com.itheima.pojo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Device {
    private Integer id;
    private Integer categoryId;
    private String name;
    private String model;
    private String serialNo;
    private String location;
    private String status;
    private LocalDate purchaseDate;
    private BigDecimal price;
    private String description;
    private Integer createdBy;
    private String imageUrl;
    private String instructionUrl;
    private String qrCodeSvg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

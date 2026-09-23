package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Integer id;
    private String username;
    @JsonIgnore
    private String password;
    private String nickname;
    private String email;
    private String userPic;
    private String role;
    private String realName;
    private String phone;
    private String studentNo;
    private String major;
    private String registrationStatus;
    private Integer registrationReviewedBy;
    private LocalDateTime registrationReviewedAt;
    private String registrationRejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
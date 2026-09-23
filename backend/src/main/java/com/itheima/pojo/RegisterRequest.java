package com.itheima.pojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^\\S{5,16}$", message = "用户名必须为 5-16 位且不能包含空格")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^\\S{5,16}$", message = "密码必须为 5-16 位且不能包含空格")
    private String password;

    @NotBlank(message = "学号不能为空")
    @Size(max = 32, message = "学号长度不能超过 32 位")
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "学号只能包含字母、数字或连字符")
    private String studentNo;

    @NotBlank(message = "专业不能为空")
    @Size(min = 2, max = 100, message = "专业长度应为 2-100 个字符")
    private String major;
}
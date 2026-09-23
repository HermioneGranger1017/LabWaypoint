package com.itheima.service;

import com.itheima.pojo.RegisterRequest;
import com.itheima.pojo.User;

import java.util.List;

public interface UserService {
    User findByUserName(String username);
    void submitRegistration(RegisterRequest request);
    void updatePwd(String newPwd);
    List<User> list();
    List<User> listRegistrationApplications();
    void approveRegistration(Integer id, Integer reviewerId);
    void rejectRegistration(Integer id, Integer reviewerId, String reason);
    void updateRole(Integer id, String newRole);
}
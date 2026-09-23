package com.itheima.service.impl;

import com.itheima.mapper.UserMapper;
import com.itheima.pojo.RegisterRequest;
import com.itheima.pojo.User;
import com.itheima.service.UserService;
import com.itheima.utils.Md5Util;
import com.itheima.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class UserSeviceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User findByUserName(String username) {
        return userMapper.findByUserName(username);
    }

    @Override
    @Transactional
    public void submitRegistration(RegisterRequest request) {
        String username = request.getUsername().trim();
        String studentNo = request.getStudentNo().trim();
        String major = request.getMajor().trim();
        User sameUsername = userMapper.findByUserName(username);
        User sameStudentNo = userMapper.findByStudentNo(studentNo);

        if (sameUsername != null && sameStudentNo != null && !sameUsername.getId().equals(sameStudentNo.getId())) {
            throw new RuntimeException("用户名和学号已分别被不同账号使用");
        }

        User existing = sameUsername != null ? sameUsername : sameStudentNo;
        if (existing == null) {
            User registration = new User();
            registration.setUsername(username);
            registration.setPassword(Md5Util.getMD5String(request.getPassword()));
            registration.setStudentNo(studentNo);
            registration.setMajor(major);
            userMapper.insertRegistration(registration);
            return;
        }

        if (!"已拒绝".equals(existing.getRegistrationStatus())) {
            if ("待审核".equals(existing.getRegistrationStatus())) {
                throw new RuntimeException("该账号或学号已提交注册申请，请等待审核");
            }
            throw new RuntimeException("用户名或学号已被使用");
        }

        User registration = new User();
        registration.setId(existing.getId());
        registration.setUsername(username);
        registration.setPassword(Md5Util.getMD5String(request.getPassword()));
        registration.setStudentNo(studentNo);
        registration.setMajor(major);
        userMapper.resubmitRegistration(registration);
    }

    @Override
    public void updatePwd(String newPwd) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer id = (Integer) map.get("id");
        userMapper.updatePwd(Md5Util.getMD5String(newPwd), id);
    }

    @Override
    public List<User> list() {
        return userMapper.list();
    }

    @Override
    public List<User> listRegistrationApplications() {
        return userMapper.listRegistrationApplications();
    }

    @Override
    @Transactional
    public void approveRegistration(Integer id, Integer reviewerId) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("注册申请不存在");
        }
        if (!"待审核".equals(user.getRegistrationStatus())) {
            throw new RuntimeException("该注册申请已被处理");
        }
        if (userMapper.approveRegistration(id, reviewerId) != 1) {
            throw new RuntimeException("注册申请状态已变更，请刷新后重试");
        }
    }

    @Override
    @Transactional
    public void rejectRegistration(Integer id, Integer reviewerId, String reason) {
        if (reason == null || reason.trim().length() < 2 || reason.trim().length() > 500) {
            throw new RuntimeException("请填写 2-500 个字符的拒绝原因");
        }
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("注册申请不存在");
        }
        if (!"待审核".equals(user.getRegistrationStatus())) {
            throw new RuntimeException("该注册申请已被处理");
        }
        if (userMapper.rejectRegistration(id, reviewerId, reason.trim()) != 1) {
            throw new RuntimeException("注册申请状态已变更，请刷新后重试");
        }
    }

    @Override
    public void updateRole(Integer id, String newRole) {
        userMapper.updateRole(id, newRole);
    }
}
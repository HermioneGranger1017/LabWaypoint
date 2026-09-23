package com.itheima.mapper;

import com.itheima.pojo.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("select * from user where username = #{username}")
    User findByUserName(String username);

    @Select("select * from user where student_no = #{studentNo}")
    User findByStudentNo(String studentNo);

    @Select("select * from user where id = #{id}")
    User findById(Integer id);

    @Select("select * from user where registration_status = '已通过' order by id")
    List<User> list();

    @Select("select * from user order by case registration_status when '待审核' then 0 when '已拒绝' then 1 else 2 end, created_at desc, id desc")
    List<User> listRegistrationApplications();

    @Insert("insert into user(username, password, real_name, role, student_no, major, registration_status, created_at) " +
            "values(#{username}, #{password}, '用户', 'user', #{studentNo}, #{major}, '待审核', now())")
    void insertRegistration(User user);

    @Update("update user set username=#{username}, password=#{password}, student_no=#{studentNo}, major=#{major}, " +
            "registration_status='待审核', registration_reviewed_by=null, registration_reviewed_at=null, registration_reject_reason=null " +
            "where id=#{id}")
    void resubmitRegistration(User user);

    @Update("update user set registration_status='已通过', registration_reviewed_by=#{reviewerId}, " +
            "registration_reviewed_at=now(), registration_reject_reason=null where id=#{id} and registration_status='待审核'")
    int approveRegistration(@Param("id") Integer id, @Param("reviewerId") Integer reviewerId);

    @Update("update user set registration_status='已拒绝', registration_reviewed_by=#{reviewerId}, " +
            "registration_reviewed_at=now(), registration_reject_reason=#{reason} where id=#{id} and registration_status='待审核'")
    int rejectRegistration(@Param("id") Integer id, @Param("reviewerId") Integer reviewerId, @Param("reason") String reason);

    @Update("update user set password=#{md5Pwd} where id=#{id}")
    void updatePwd(@Param("md5Pwd") String md5Pwd, @Param("id") Integer id);

    @Update("update user set role=#{role} where id=#{id} and registration_status='已通过'")
    void updateRole(@Param("id") Integer id, @Param("role") String role);
}
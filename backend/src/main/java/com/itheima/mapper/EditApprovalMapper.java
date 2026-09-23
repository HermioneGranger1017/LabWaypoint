package com.itheima.mapper;

import com.itheima.pojo.EditApproval;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EditApprovalMapper {

    @Select("SELECT ea.id AS id, ea.target_type AS targetType, ea.target_id AS targetId, " +
            "ea.instruction_id AS instructionId, ea.applicant_id AS applicantId, ea.owner_id AS ownerId, " +
            "ea.reason AS reason, ea.status AS status, ea.used_at AS usedAt, " +
            "ea.created_at AS createdAt, ea.updated_at AS updatedAt, " +
            "app.username AS applicantName, own.username AS ownerName, " +
            "CASE WHEN ea.target_type = 'device' THEN d.name " +
            "WHEN ea.target_type = 'instruction' THEN ins.title END AS targetTitle, " +
            "CASE WHEN ea.target_type = 'instruction' THEN ins.device_id ELSE NULL END AS deviceId " +
            "FROM edit_approval ea " +
            "LEFT JOIN user app ON app.id = ea.applicant_id " +
            "LEFT JOIN user own ON own.id = ea.owner_id " +
            "LEFT JOIN device d ON d.id = ea.target_id AND ea.target_type = 'device' " +
            "LEFT JOIN instruction ins ON ins.id = ea.target_id AND ea.target_type = 'instruction' " +
            "WHERE ea.owner_id = #{ownerId} " +
            "AND (#{status} = '' OR ea.status = #{status}) " +
            "ORDER BY ea.created_at DESC")
    List<EditApproval> listInbox(@Param("ownerId") Integer ownerId, @Param("status") String status);

    @Select("SELECT ea.id AS id, ea.target_type AS targetType, ea.target_id AS targetId, " +
            "ea.instruction_id AS instructionId, ea.applicant_id AS applicantId, ea.owner_id AS ownerId, " +
            "ea.reason AS reason, ea.status AS status, ea.used_at AS usedAt, " +
            "ea.created_at AS createdAt, ea.updated_at AS updatedAt, " +
            "app.username AS applicantName, own.username AS ownerName, " +
            "CASE WHEN ea.target_type = 'device' THEN d.name " +
            "WHEN ea.target_type = 'instruction' THEN ins.title END AS targetTitle, " +
            "CASE WHEN ea.target_type = 'instruction' THEN ins.device_id ELSE NULL END AS deviceId " +
            "FROM edit_approval ea " +
            "LEFT JOIN user app ON app.id = ea.applicant_id " +
            "LEFT JOIN user own ON own.id = ea.owner_id " +
            "LEFT JOIN device d ON d.id = ea.target_id AND ea.target_type = 'device' " +
            "LEFT JOIN instruction ins ON ins.id = ea.target_id AND ea.target_type = 'instruction' " +
            "WHERE ea.applicant_id = #{applicantId} " +
            "AND (#{status} = '' OR ea.status = #{status}) " +
            "ORDER BY ea.created_at DESC")
    List<EditApproval> listOutbox(@Param("applicantId") Integer applicantId, @Param("status") String status);

    @Select("SELECT * FROM edit_approval WHERE id = #{id}")
    EditApproval findById(Integer id);

    @Select("SELECT COUNT(*) FROM edit_approval WHERE target_type = #{targetType} AND target_id = #{targetId} " +
            "AND applicant_id = #{applicantId} AND status = 'pending'")
    int countPending(@Param("targetType") String targetType, @Param("targetId") Integer targetId,
                     @Param("applicantId") Integer applicantId);

    @Select("SELECT * FROM edit_approval WHERE target_type = #{targetType} AND target_id = #{targetId} " +
            "AND applicant_id = #{applicantId} AND status = 'approved' AND used_at IS NULL " +
            "ORDER BY created_at DESC LIMIT 1")
    EditApproval findApprovedUnused(@Param("targetType") String targetType,
                                    @Param("targetId") Integer targetId,
                                    @Param("applicantId") Integer applicantId);

    @Insert("INSERT INTO edit_approval(target_type, target_id, applicant_id, owner_id, reason, status) " +
            "VALUES(#{targetType}, #{targetId}, #{applicantId}, #{ownerId}, #{reason}, 'pending')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(EditApproval editApproval);

    @Update("UPDATE edit_approval SET status = #{status} WHERE id = #{id}")
    void updateStatus(@Param("id") Integer id, @Param("status") String status);

    @Update("UPDATE edit_approval SET used_at = NOW() WHERE id = #{id}")
    void markUsed(Integer id);
}
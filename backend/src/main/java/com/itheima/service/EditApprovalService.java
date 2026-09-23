package com.itheima.service;

import com.itheima.pojo.EditApproval;
import java.util.List;

public interface EditApprovalService {

    /** 发起编辑申请。返回申请 ID。 */
    void createRequest(String targetType, Integer targetId, String reason, Integer applicantId, String role);

    /** 原创建者审批通过/驳回。 */
    void approveRequest(Integer approvalId, String status, Integer ownerId, String role);

    /** 当前创建者收到的待审批列表。 */
    List<EditApproval> listInbox(Integer ownerId, String status);

    /** 当前用户已发起的申请列表。 */
    List<EditApproval> listOutbox(Integer applicantId, String status);

    /** 查询当前用户对某目标可否编辑 / 可否申请 / pending / approvalId。 */
    EditApproval getPermission(String targetType, Integer targetId, Integer userId, String role);

    /** 申请人保存成功时消费最近一条有效批准（写 used_at）。无记录时抛出异常。 */
    void consumeApprovedPermission(String targetType, Integer targetId, Integer userId);

    /** 旧接口保留兼容：返回是否拥有未使用的批准。 */
    boolean hasApproval(String targetType, Integer targetId, Integer userId);
}
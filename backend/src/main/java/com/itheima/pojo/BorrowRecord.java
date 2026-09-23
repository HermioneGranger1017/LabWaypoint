package com.itheima.pojo;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class BorrowRecord {
 private Integer id,deviceId,borrowerId,approvedBy,rejectedBy,returnConfirmedBy;
 private String borrowerName,borrowerPhone,borrowerIdentity,purpose,status,remark,rejectReason,returnCheckNote,returnDeviceStatus;
 private LocalDateTime borrowTime,expectedReturnTime,actualReturnTime,approvedAt,rejectedAt,returnRequestedAt,returnConfirmedAt,createdAt;
 private String deviceName,deviceSerialNo,deviceImageUrl,deviceStatus,applicantUsername;
}

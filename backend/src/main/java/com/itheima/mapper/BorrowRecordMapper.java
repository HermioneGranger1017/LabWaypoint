package com.itheima.mapper;
import com.itheima.pojo.BorrowRecord;
import com.itheima.pojo.Device;
import org.apache.ibatis.annotations.*;
import java.util.*;
@Mapper public interface BorrowRecordMapper {
 @Insert("insert into borrow_record(device_id,borrower_id,purpose,borrower_name,borrower_phone,borrower_identity,expected_return_time,status,remark) values(#{deviceId},#{borrowerId},#{purpose},#{borrowerName},#{borrowerPhone},#{borrowerIdentity},#{expectedReturnTime},#{status},#{remark})") void insert(BorrowRecord r);
 @Select("select * from borrow_record where id=#{id} for update") BorrowRecord lock(@Param("id") Integer id);
 @Select("select * from device where id=#{id} for update") Device lockDevice(@Param("id") Integer id);
 @Update("update device set status=#{status} where id=#{id}") void setDeviceStatus(@Param("id") Integer id,@Param("status") String status);
 @Select("select count(*) from borrow_record where device_id=#{id} and status in ('待审核','借用中','逾期','待归还确认')") int activeCount(@Param("id") Integer id);
 @Update("update borrow_record set status='逾期' where status='借用中' and expected_return_time<now()") void markOverdue();
 @Update("update borrow_record set status='借用中',borrow_time=now(),approved_by=#{uid},approved_at=now() where id=#{id}") void approve(@Param("id") Integer id,@Param("uid") Integer uid);
 @Update("update borrow_record set status='已拒绝',rejected_by=#{uid},rejected_at=now(),reject_reason=#{reason} where id=#{id}") void reject(@Param("id") Integer id,@Param("uid") Integer uid,@Param("reason") String reason);
 @Update("update borrow_record set status='待归还确认',return_requested_at=now(),remark=#{remark} where id=#{id}") void requestReturn(@Param("id") Integer id,@Param("remark") String remark);
 @Update("update borrow_record set status='已归还',actual_return_time=now(),return_confirmed_by=#{uid},return_confirmed_at=now(),return_check_note=#{note},return_device_status=#{status} where id=#{id}") void confirm(@Param("id") Integer id,@Param("uid") Integer uid,@Param("note") String note,@Param("status") String status);
 @Select("select b.*,d.name deviceName,d.serial_no deviceSerialNo,d.image_url deviceImageUrl,d.status deviceStatus,u.username applicantUsername from borrow_record b join device d on d.id=b.device_id join user u on u.id=b.borrower_id where b.borrower_id=#{uid} order by b.created_at desc") List<BorrowRecord> mine(@Param("uid") Integer uid);
 @Select("select b.*,d.name deviceName,d.serial_no deviceSerialNo,d.image_url deviceImageUrl,d.status deviceStatus,u.username applicantUsername from borrow_record b join device d on d.id=b.device_id join user u on u.id=b.borrower_id order by b.created_at desc") List<BorrowRecord> admin();
 @Select("select b.*,d.name deviceName,d.status deviceStatus from borrow_record b join device d on d.id=b.device_id where b.device_id=#{id} and b.status in ('待审核','借用中','逾期','待归还确认') order by b.id desc limit 1") BorrowRecord active(@Param("id") Integer id);
}

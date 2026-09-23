package com.itheima.service;
import com.itheima.pojo.BorrowRecord; import java.time.LocalDateTime; import java.util.*;
public interface BorrowRecordService { void apply(Integer uid,Integer deviceId,String purpose,LocalDateTime expected,String remark); List<BorrowRecord> mine(Integer uid); List<BorrowRecord> admin(); BorrowRecord active(Integer deviceId); void approve(Integer id,Integer uid); void reject(Integer id,Integer uid,String reason); void requestReturn(Integer id,Integer uid,String remark); void confirm(Integer id,Integer uid,String finalStatus,String note); }

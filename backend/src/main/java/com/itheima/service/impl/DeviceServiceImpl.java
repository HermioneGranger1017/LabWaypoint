package com.itheima.service.impl;

import com.itheima.mapper.DeviceMapper;
import com.itheima.pojo.Device;
import com.itheima.service.DeviceService;
import com.itheima.service.EditApprovalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DeviceServiceImpl implements DeviceService {
 @Autowired private DeviceMapper deviceMapper; @Autowired private EditApprovalService editApprovalService;
 public List<Device> list(Integer categoryId){return categoryId!=null&&categoryId>0?deviceMapper.listByCategory(categoryId):deviceMapper.list();}
 public Device findById(Integer id){return deviceMapper.findById(id);} public void add(Device device){deviceMapper.add(device);}
 @Transactional public void update(Device device,Integer uid,String role){Integer creator=deviceMapper.findCreatedBy(device.getId());if(creator==null)throw new RuntimeException("设备不存在");if("super_admin".equals(role)||creator.equals(uid)){deviceMapper.update(device);return;}if("admin".equals(role)){editApprovalService.consumeApprovedPermission("device",device.getId(),uid);deviceMapper.update(device);return;}throw new RuntimeException("无权限修改此设备");}
 @Transactional public void updateMaintenanceStatus(Integer id,String targetStatus){if(!"在库".equals(targetStatus)&&!"报废".equals(targetStatus))throw new RuntimeException("维修完成后只能调整为“在库”或“报废”");Device device=deviceMapper.findById(id);if(device==null)throw new RuntimeException("设备不存在");if(!"维修中".equals(device.getStatus()))throw new RuntimeException("只有“维修中”的设备可以进行维修状态调整");deviceMapper.updateStatus(id,targetStatus);}
 public void delete(Integer id,Integer uid,String role){Integer creator=deviceMapper.findCreatedBy(id);if(creator==null)throw new RuntimeException("设备不存在");if(!"super_admin".equals(role)&&!("admin".equals(role)&&creator.equals(uid)))throw new RuntimeException("无权限删除此设备");deviceMapper.delete(id);}
}
package com.itheima.service;
import com.itheima.pojo.Device;
import java.util.List;
public interface DeviceService { List<Device> list(Integer categoryId); Device findById(Integer id); void add(Device device); void update(Device device, Integer currentUserId, String role); void updateMaintenanceStatus(Integer id, String targetStatus); void delete(Integer id, Integer currentUserId, String role); }
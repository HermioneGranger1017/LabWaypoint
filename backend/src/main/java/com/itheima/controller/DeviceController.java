package com.itheima.controller;

import com.itheima.anno.RequireAdmin;
import com.itheima.pojo.Device;
import com.itheima.pojo.Result;
import com.itheima.service.DeviceService;
import com.itheima.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/device")
public class DeviceController {
 @Autowired private DeviceService deviceService;
 @GetMapping public Result<List<Device>> list(@RequestParam(required=false) Integer categoryId){return Result.success(deviceService.list(categoryId));}
 @GetMapping("/{id}") public Result<Device> detail(@PathVariable Integer id){return Result.success(deviceService.findById(id));}
 @PostMapping @RequireAdmin public Result add(@RequestBody Device device){Map<String,Object> claims=ThreadLocalUtil.get();device.setCreatedBy((Integer)claims.get("id"));device.setStatus("在库");deviceService.add(device);return Result.success();}
 @PutMapping public Result update(@RequestBody Device device){Map<String,Object> claims=ThreadLocalUtil.get();deviceService.update(device,(Integer)claims.get("id"),(String)claims.get("role"));return Result.success();}
 @PutMapping("/{id}/maintenance-status") @RequireAdmin public Result updateMaintenanceStatus(@PathVariable Integer id,@RequestBody Map<String,String> body){deviceService.updateMaintenanceStatus(id,body.get("status"));return Result.success();}
 @DeleteMapping("/{id}") public Result delete(@PathVariable Integer id){Map<String,Object> claims=ThreadLocalUtil.get();deviceService.delete(id,(Integer)claims.get("id"),(String)claims.get("role"));return Result.success();}
}
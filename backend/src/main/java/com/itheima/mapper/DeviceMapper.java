package com.itheima.mapper;

import com.itheima.pojo.Device;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DeviceMapper {
    @Select("select *, category_id as categoryId, serial_no as serialNo, image_url as imageUrl, created_by as createdBy from device order by id") List<Device> list();
    @Select("select *, category_id as categoryId, serial_no as serialNo, image_url as imageUrl, created_by as createdBy from device where category_id = #{categoryId} order by id") List<Device> listByCategory(Integer categoryId);
    @Select("select *, category_id as categoryId, serial_no as serialNo, image_url as imageUrl, created_by as createdBy from device where id = #{id}") Device findById(Integer id);
    @Insert("insert into device(category_id, name, model, serial_no, location, status, description, image_url, created_by) values(#{categoryId}, #{name}, #{model}, #{serialNo}, #{location}, #{status}, #{description}, #{imageUrl}, #{createdBy})") @Options(useGeneratedKeys = true, keyProperty = "id") void add(Device device);
    @Update("update device set category_id=#{categoryId}, name=#{name}, model=#{model}, serial_no=#{serialNo}, location=#{location}, description=#{description}, image_url=#{imageUrl} where id=#{id}") void update(Device device);
    @Update("update device set status=#{status} where id=#{id}") void updateStatus(@Param("id") Integer id, @Param("status") String status);
    @Delete("delete from device where id = #{id}") void delete(Integer id);
    @Select("select created_by from device where id = #{id}") Integer findCreatedBy(Integer id);
}
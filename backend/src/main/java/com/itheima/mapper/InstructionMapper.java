package com.itheima.mapper;

import com.itheima.pojo.Instruction;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface InstructionMapper {

    @Select("SELECT i.id AS id, i.device_id AS deviceId, i.step_order AS stepOrder, " +
            "i.title AS title, i.content AS content, i.image_urls AS imageUrls, " +
            "i.cover_image_url AS coverImageUrl, i.created_by AS createdBy, " +
            "i.created_at AS createdAt, i.updated_at AS updatedAt, " +
            "u.username AS authorName " +
            "FROM instruction i LEFT JOIN user u ON u.id = i.created_by " +
            "WHERE i.device_id = #{deviceId} ORDER BY i.step_order ASC, i.id ASC")
    List<Instruction> listByDevice(Integer deviceId);

    @Select("SELECT i.id AS id, i.device_id AS deviceId, i.step_order AS stepOrder, " +
            "i.title AS title, i.content AS content, i.image_urls AS imageUrls, " +
            "i.cover_image_url AS coverImageUrl, i.created_by AS createdBy, " +
            "i.created_at AS createdAt, i.updated_at AS updatedAt, " +
            "u.username AS authorName " +
            "FROM instruction i LEFT JOIN user u ON u.id = i.created_by " +
            "WHERE i.id = #{id}")
    Instruction findById(Integer id);

    @Select("SELECT COALESCE(MAX(step_order), 0) FROM instruction WHERE device_id = #{deviceId}")
    Integer findMaxStepOrder(Integer deviceId);

    @Insert("INSERT INTO instruction(device_id, step_order, title, content, image_urls, cover_image_url, created_by) " +
            "VALUES(#{deviceId}, #{stepOrder}, #{title}, #{content}, #{imageUrls}, #{coverImageUrl}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(Instruction instruction);

    @Update("UPDATE instruction SET step_order=#{stepOrder}, title=#{title}, " +
            "content=#{content}, image_urls=#{imageUrls}, cover_image_url=#{coverImageUrl} WHERE id=#{id}")
    void update(Instruction instruction);

    @Delete("DELETE FROM instruction WHERE id = #{id}")
    void delete(Integer id);

    @Select("SELECT created_by FROM instruction WHERE id = #{id}")
    Integer findCreatedBy(Integer id);
}
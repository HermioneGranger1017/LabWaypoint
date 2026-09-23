package com.itheima.mapper;

import com.itheima.pojo.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryMapper {

    @Select("select * from category order by id")
    List<Category> list();

    @Select("select * from category where id = #{id}")
    Category findById(Integer id);

    @Insert("insert into category(name, description) values(#{name}, #{description})")
    void add(Category category);

    @Update("update category set name=#{name}, description=#{description} where id=#{id}")
    void update(Category category);

    @Delete("delete from category where id = #{id}")
    void delete(Integer id);
}

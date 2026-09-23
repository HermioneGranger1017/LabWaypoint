package com.itheima.service;

import com.itheima.pojo.Category;

import java.util.List;

public interface CategoryService {
    List<Category> list();
    Category findById(Integer id);
    void add(Category category);
    void update(Category category);
    void delete(Integer id);
}

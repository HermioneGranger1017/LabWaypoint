package com.itheima.controller;

import com.itheima.anno.RequireAdmin;
import com.itheima.pojo.Category;
import com.itheima.pojo.Result;
import com.itheima.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public Result<List<Category>> list() {
        List<Category> cs = categoryService.list();
        return Result.success(cs);
    }

    @GetMapping("/{id}")
    public Result<Category> detail(@PathVariable Integer id) {
        Category c = categoryService.findById(id);
        return Result.success(c);
    }

    @PostMapping
    @RequireAdmin
    public Result add(@RequestBody Category category) {
        categoryService.add(category);
        return Result.success();
    }

    @PutMapping
    @RequireAdmin
    public Result update(@RequestBody Category category) {
        categoryService.update(category);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    public Result delete(@PathVariable Integer id) {
        categoryService.delete(id);
        return Result.success();
    }
}

package com.itheima.controller;

import com.itheima.anno.RequireAdmin;
import com.itheima.pojo.Instruction;
import com.itheima.pojo.Result;
import com.itheima.service.InstructionService;
import com.itheima.utils.HtmlSanitizer;
import com.itheima.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/instruction")
public class InstructionController {

    @Autowired
    private InstructionService instructionService;

    @GetMapping
    public Result<List<Instruction>> list(@RequestParam Integer deviceId) {
        List<Instruction> list = instructionService.listByDevice(deviceId);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<Instruction> detail(@PathVariable Integer id) {
        Instruction instruction = instructionService.findById(id);
        return Result.success(instruction);
    }

    @PostMapping
    @RequireAdmin
    public Result add(@RequestBody Instruction instruction) {
        validateTitleAndContent(instruction);
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        instruction.setCreatedBy(userId);
        instructionService.add(instruction);
        return Result.success(instruction);
    }

    @PutMapping
    public Result update(@RequestBody Instruction instruction) {
        validateTitleAndContent(instruction);
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        String role = (String) claims.get("role");
        instructionService.update(instruction, userId, role);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        String role = (String) claims.get("role");
        instructionService.delete(id, userId, role);
        return Result.success();
    }

    private void validateTitleAndContent(Instruction instruction) {
        if (instruction == null) throw new RuntimeException("请填写教程内容");
        String title = instruction.getTitle();
        if (title == null || title.trim().length() < 2 || title.trim().length() > 100) {
            throw new RuntimeException("教程标题必须是 2-100 字");
        }
        String pure = HtmlSanitizer.stripHtml(instruction.getContent());
        if (pure.length() < 10) {
            throw new RuntimeException("教程正文至少需要 10 个字符（不含 HTML 标签）");
        }
    }
}
package com.itheima.service;

import com.itheima.pojo.Instruction;
import java.util.List;

public interface InstructionService {
    List<Instruction> listByDevice(Integer deviceId);
    Instruction findById(Integer id);
    void add(Instruction instruction);
    void update(Instruction instruction, Integer userId, String role);
    void delete(Integer id, Integer userId, String role);
}
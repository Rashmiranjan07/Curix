package com.curix.service;

import com.curix.dto.DepartmentDto;
import java.util.List;

public interface DepartmentService {
    DepartmentDto createDepartment(DepartmentDto dto);
    List<DepartmentDto> getAllActiveDepartments();
    DepartmentDto getDepartmentById(Long id);
}
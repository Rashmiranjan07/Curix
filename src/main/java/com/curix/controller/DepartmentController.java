package com.curix.controller;

import com.curix.dto.DepartmentDto;
import com.curix.dto.response.ApiResponse;
import com.curix.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentDto>>> getAllActiveDepartments() {
        List<DepartmentDto> list = departmentService.getAllActiveDepartments();
        return ResponseEntity.ok(ApiResponse.success("Departments retrieved successfully", list));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentDto>> createDepartment(@Valid @RequestBody DepartmentDto dto) {
        DepartmentDto created = departmentService.createDepartment(dto);
        return new ResponseEntity<>(ApiResponse.success("Department created successfully", created), HttpStatus.CREATED);
    }
}
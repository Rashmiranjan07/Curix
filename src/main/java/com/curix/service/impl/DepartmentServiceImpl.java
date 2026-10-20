package com.curix.service.impl;

import com.curix.dto.DepartmentDto;
import com.curix.entity.Department;
import com.curix.exception.ConflictException;
import com.curix.exception.ResourceNotFoundException;
import com.curix.repository.DepartmentRepository;
import com.curix.service.DepartmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    @Transactional
    public DepartmentDto createDepartment(DepartmentDto dto) {
        if (departmentRepository.existsByNameIgnoreCase(dto.getName().trim())) {
            throw new ConflictException("Department with name '" + dto.getName() + "' already exists");
        }

        // Direct POJO constructor instantiation to eliminate Lombok builder conflicts
        Department department = new Department(
                dto.getName().trim(),
                dto.getDescription(),
                true
        );

        Department saved = departmentRepository.save(department);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDto> getAllActiveDepartments() {
        return departmentRepository.findByActiveTrue()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDto getDepartmentById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
        return mapToDto(dept);
    }

    private DepartmentDto mapToDto(Department dept) {
        return new DepartmentDto(
                dept.getId(),
                dept.getName(),
                dept.getDescription(),
                dept.isActive()
        );
    }
}
package com.curix.dto.response;

import com.curix.dto.DoctorAvailabilityDto;

import java.math.BigDecimal;
import java.util.List;

public class DoctorResponse {

    private Long id;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String licenseNumber;
    private String departmentName;
    private Long departmentId;
    private String specialty;
    private String qualification;
    private Integer experienceYears;
    private BigDecimal consultationFee;
    private boolean active;
    private List<DoctorAvailabilityDto> availabilities;

    public DoctorResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public List<DoctorAvailabilityDto> getAvailabilities() { return availabilities; }
    public void setAvailabilities(List<DoctorAvailabilityDto> availabilities) { this.availabilities = availabilities; }
}
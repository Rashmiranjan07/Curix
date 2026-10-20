package com.curix.repository;

import com.curix.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {
    Optional<DoctorProfile> findByUserId(Long userId);
    Optional<DoctorProfile> findByLicenseNumber(String licenseNumber);
    boolean existsByLicenseNumber(String licenseNumber);

    @Query("SELECT d FROM DoctorProfile d JOIN d.user u " +
           "WHERE u.active = true " +
           "AND (:departmentId IS NULL OR d.department.id = :departmentId) " +
           "AND (:specialty IS NULL OR LOWER(d.specialty) LIKE LOWER(CONCAT('%', :specialty, '%')))")
    List<DoctorProfile> searchDoctors(@Param("departmentId") Long departmentId,
                                      @Param("specialty") String specialty);
}
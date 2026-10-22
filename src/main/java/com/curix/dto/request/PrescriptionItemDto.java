package com.curix.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PrescriptionItemDto {
    @NotBlank(message = "Medicine name is required")
    private String medicineName; // or medicationName

    @NotBlank(message = "Dosage is required")
    private String dosage;

    @NotBlank(message = "Frequency is required")
    private String frequency;

    @NotNull(message = "Duration in days is required")
    @Min(value = 1, message = "Duration must be at least 1 day")
    private Integer durationDays;

    private String specialInstructions; // or instructions
}

package com.curix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PrescriptionItemDto {

    private Long id;

    @NotBlank(message = "Medication name is required")
    @Size(max = 150, message = "Medication name cannot exceed 150 characters")
    private String medicationName;

    @NotBlank(message = "Dosage is required (e.g. 500mg)")
    @Size(max = 50, message = "Dosage cannot exceed 50 characters")
    private String dosage;

    @NotBlank(message = "Frequency is required (e.g. Twice daily)")
    @Size(max = 50, message = "Frequency cannot exceed 50 characters")
    private String frequency;

    @NotBlank(message = "Duration is required (e.g. 5 days)")
    @Size(max = 50, message = "Duration cannot exceed 50 characters")
    private String duration;

    @Size(max = 300, message = "Instructions cannot exceed 300 characters")
    private String instructions;

    public PrescriptionItemDto() {}

    public PrescriptionItemDto(Long id, String medicationName, String dosage, String frequency, String duration, String instructions) {
        this.id = id;
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.frequency = frequency;
        this.duration = duration;
        this.instructions = instructions;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
}
package com.curix.dto;

import com.curix.enums.DayOfWeek;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public class DoctorAvailabilityDto {

    private Long id;

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    private Integer slotMinutes = 30;
    private Boolean active = true;

    public DoctorAvailabilityDto() {}

    public DoctorAvailabilityDto(Long id, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, Integer slotMinutes, Boolean active) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotMinutes = slotMinutes;
        this.active = (active != null) ? active : true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Integer getSlotMinutes() { return slotMinutes; }
    public void setSlotMinutes(Integer slotMinutes) { this.slotMinutes = slotMinutes; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
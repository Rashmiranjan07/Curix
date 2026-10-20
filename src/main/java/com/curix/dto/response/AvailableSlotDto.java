package com.curix.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public class AvailableSlotDto {

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean available;

    public AvailableSlotDto() {}

    public AvailableSlotDto(LocalDate date, LocalTime startTime, LocalTime endTime, boolean available) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.available = available;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
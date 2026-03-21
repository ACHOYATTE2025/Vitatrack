package com.portfolio.VistaTrack.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthMetricRequestDto {
     /**
     * Body weight in kilograms.
     * Must be a positive value if provided.
     */
    @Positive(message = "Weight must be a positive value")
    private Double weightKg;

    /**
     * Body height in centimeters.
     * Used together with weightKg to auto-compute BMI.
     * Must be a positive value if provided.
     */
    @Positive(message = "Height must be a positive value")
    private Double heightCm;

    /**
     * Resting heart rate in beats per minute.
     * Normal range: 40–200 bpm.
     */
    @Min(value = 40,  message = "Heart rate must be at least 40 bpm")
    @Max(value = 200, message = "Heart rate must be at most 200 bpm")
    private Integer heartRate;

    /**
     * Systolic blood pressure in mmHg (the upper number).
     * Example: 120 in "120/80 mmHg".
     */
    @Positive(message = "Systolic pressure must be a positive value")
    private Integer systolic;

    /**
     * Diastolic blood pressure in mmHg (the lower number).
     * Example: 80 in "120/80 mmHg".
     */
    @Positive(message = "Diastolic pressure must be a positive value")
    private Integer diastolic;

    /**
     * Optional free-text notes about the health entry.
     * Example: "Feeling tired, high stress week."
     */
    private String notes;

}

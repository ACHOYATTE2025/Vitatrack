package com.portfolio.VistaTrack.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRequestDto {

    /**
     * Type of physical activity.
     * Examples: running, cycling, swimming, walking, gym.
     */
    @NotBlank(message = "Activity type is required")
    private String type;

    /**
     * Duration of the activity in minutes.
     * Must be at least 1 minute.
     */
    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    /**
     * Estimated calories burned during the activity.
     * Must be a positive value.
     */
    @NotNull(message = "Calories burned is required")
    @Min(value = 0, message = "Calories burned cannot be negative")
    private Double caloriesBurned;

    /**
     * Optional free-text notes about the activity.
     * Example: "Morning run in the park, felt great."
     */
    private String notes;

}

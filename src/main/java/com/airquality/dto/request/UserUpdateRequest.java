package com.airquality.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    @Size(min = 1, max = 200, message = "Full name must be between 1 and 200 characters")
    @JsonProperty("full_name")
    private String fullName;

    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;
}

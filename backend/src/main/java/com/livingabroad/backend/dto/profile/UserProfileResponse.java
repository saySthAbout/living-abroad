package com.livingabroad.backend.dto.profile;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record UserProfileResponse(
    Long profileId,
    Integer age,
    String education,
    String major,
    String occupation,
    BigDecimal experienceYears,
    OffsetDateTime updatedAt
) {
}

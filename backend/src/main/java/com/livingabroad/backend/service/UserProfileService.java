package com.livingabroad.backend.service;

import com.livingabroad.backend.dto.profile.UserProfileRequest;
import com.livingabroad.backend.dto.profile.UserProfileResponse;
import com.livingabroad.backend.entity.UserProfile;
import com.livingabroad.backend.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        return userProfileRepository.findByUserId(userId)
            .map(this::toResponse)
            .orElse(null);
    }

    @Transactional
    public UserProfileResponse saveMyProfile(Long userId, UserProfileRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        if (profile == null) {
            profile = new UserProfile(
                userId,
                request.age().shortValue(),
                request.education(),
                request.major(),
                request.occupation(),
                request.experienceYears()
            );
        } else {
            profile.update(
                request.age().shortValue(),
                request.education(),
                request.major(),
                request.occupation(),
                request.experienceYears()
            );
        }

        UserProfile saved = userProfileRepository.save(profile);
        return toResponse(saved);
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        return new UserProfileResponse(
            profile.getProfileId(),
            profile.getAge().intValue(),
            profile.getEducationLevel(),
            profile.getMajor(),
            profile.getCurrentOccupation(),
            profile.getExperienceYears(),
            profile.getUpdatedAt()
        );
    }
}

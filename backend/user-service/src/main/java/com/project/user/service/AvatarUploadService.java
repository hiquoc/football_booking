package com.project.user.service;
import com.project.user.dto.*;
import com.project.user.dto.UserDto;
import java.util.UUID;
public interface AvatarUploadService {
    AvatarUploadSlotDto issueSlot(UUID userId, AvatarUploadSlotRequest request);
    UserDto confirm(UUID userId, AvatarUploadConfirmRequest request);
    void cleanupStaleUploads();
}

package com.project.user.mapper;

import com.project.user.dto.UserDto;
import com.project.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
    @Mapping(target = "avatarPublicId", ignore = true)
    User toEntity(UserDto dto);
}

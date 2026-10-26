package kz.edu.lms.mapper;

import kz.edu.lms.dto.auth.KeycloakTokenResponse;
import kz.edu.lms.dto.auth.TokenResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthMapper {
    TokenResponse toResponse(KeycloakTokenResponse response);
}

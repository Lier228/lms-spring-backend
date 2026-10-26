package kz.edu.lms.mapper;

import kz.edu.lms.dto.user.KeycloakUserRequest;
import kz.edu.lms.dto.user.UserCreateRequest;
import kz.edu.lms.dto.user.UserResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(String id, UserCreateRequest request);

    default KeycloakUserRequest toKeycloakUser(UserCreateRequest request) {
        var password = new KeycloakUserRequest.Credential("password", request.password(), false);
        return new KeycloakUserRequest(request.username(), request.email(), request.firstName(),
                request.lastName(), false, List.of(password));
    }
}

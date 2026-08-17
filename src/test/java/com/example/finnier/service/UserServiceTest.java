package com.example.finnier.service;

import com.example.finnier.dto.PasswordChangeDto;
import com.example.finnier.dto.UserRequest;
import com.example.finnier.dto.UserResponse;
import com.example.finnier.entity.User;
import com.example.finnier.exception.InvalidCredentialsException;
import com.example.finnier.exception.PasswordMismatchException;
import com.example.finnier.exception.UserNotFoundException;
import com.example.finnier.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User existingUser;
    private UserRequest userRequest;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .userId(1L)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .password("encodedPassword")
                .role(User.RoleType.CUSTOMER)
                .status(User.UserStatus.ACTIVE)
                .build();

        userRequest = new UserRequest(
                "Jane",
                "Doe",
                "jane.doe@example.com",
                "plainPassword",
                User.RoleType.CUSTOMER,
                User.UserStatus.ACTIVE
        );
    }

    @Test
    void createUser_shouldEncodePasswordAndSaveUser() {
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        UserResponse response = userService.createUser(userRequest);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("jane.doe@example.com");
        verify(passwordEncoder).encode("plainPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getUserById_shouldReturnUser_whenUserExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        UserResponse response = userService.getUserById(1L);

        assertThat(response.userId()).isEqualTo(1L);
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getUserByEmail_shouldReturnUser_whenUserExists() {
        when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.of(existingUser));

        UserResponse response = userService.getUserByEmail("jane.doe@example.com");

        assertThat(response.email()).isEqualTo("jane.doe@example.com");
    }

    @Test
    void getUserByEmail_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail("missing@example.com"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("missing@example.com");
    }

    @Test
    void getAllUsers_shouldReturnMappedList() {
        User secondUser = User.builder()
                .userId(2L)
                .firstName("John")
                .lastName("Smith")
                .email("john.smith@example.com")
                .password("encoded")
                .role(User.RoleType.ADMIN)
                .status(User.UserStatus.ACTIVE)
                .build();

        when(userRepository.findAll()).thenReturn(List.of(existingUser, secondUser));

        List<UserResponse> responses = userService.getAllUsers();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(UserResponse::email)
                .containsExactlyInAnyOrder("jane.doe@example.com", "john.smith@example.com");
    }

    @Test
    void getAllUsers_shouldReturnEmptyList_whenNoUsersExist() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserResponse> responses = userService.getAllUsers();

        assertThat(responses).isEmpty();
    }

    @Test
    void getAllUsersByRole_shouldReturnFilteredUsers() {
        when(userRepository.findByRole(User.RoleType.ADMIN)).thenReturn(List.of(existingUser));

        List<UserResponse> responses = userService.getAllUsersByRole(User.RoleType.ADMIN);

        assertThat(responses).hasSize(1);
        verify(userRepository).findByRole(User.RoleType.ADMIN);
    }

    @Test
    void getAllUsersByStatus_shouldReturnOnlyMatchingStatus() {
        User inactiveUser = User.builder()
                .userId(2L)
                .firstName("Inactive")
                .lastName("User")
                .email("inactive@example.com")
                .password("encoded")
                .role(User.RoleType.CUSTOMER)
                .status(User.UserStatus.INACTIVE)
                .build();

        when(userRepository.findAll()).thenReturn(List.of(existingUser, inactiveUser));

        List<UserResponse> responses = userService.getAllUsersByStatus(User.UserStatus.INACTIVE);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).email()).isEqualTo("inactive@example.com");
    }

    @Test
    void updateUser_shouldUpdateFieldsAndSave() {
        UserRequest updateRequest = new UserRequest(
                "Janet",
                "Doe",
                "janet.doe@example.com",
                "newPlainPassword",
                User.RoleType.ADMIN,
                User.UserStatus.INACTIVE
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUser(1L, updateRequest);

        assertThat(response.firstName()).isEqualTo("Janet");
        assertThat(response.email()).isEqualTo("janet.doe@example.com");
        assertThat(response.role()).isEqualTo(User.RoleType.ADMIN);
        assertThat(response.status()).isEqualTo(User.UserStatus.INACTIVE);
        verify(userRepository).save(existingUser);
    }

    @Test
    void updateUser_shouldThrowException_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, userRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserStatus_shouldUpdateStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUserStatus(1L, User.UserStatus.SUSPENDED);

        assertThat(response.status()).isEqualTo(User.UserStatus.SUSPENDED);
    }

    @Test
    void updateUserStatus_shouldThrowException_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserStatus(99L, User.UserStatus.SUSPENDED))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void deactivateUserStatus_shouldSetStatusToInactive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        userService.deactivateUserStatus(1L);

        assertThat(existingUser.getStatus()).isEqualTo(User.UserStatus.INACTIVE);
        verify(userRepository).save(existingUser);
    }

    @Test
    void deactivateUserStatus_shouldThrowException_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deactivateUserStatus(99L))
                .isInstanceOf(RuntimeException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRole_shouldUpdateRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUserRole(1L, User.RoleType.ADMIN);

        assertThat(response.role()).isEqualTo(User.RoleType.ADMIN);
    }

    @Test
    void updateUserRole_shouldThrowException_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserRole(99L, User.RoleType.ADMIN))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void updateUserPassword_shouldEncodeAndSaveNewPassword_whenValid() {
        PasswordChangeDto passwordChangeDto = new PasswordChangeDto(
                "plainPassword", "newPassword123", "newPassword123"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("plainPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUserPassword(1L, passwordChangeDto);

        assertThat(response).isNotNull();
        assertThat(existingUser.getPassword()).isEqualTo("encodedNewPassword");
        verify(passwordEncoder).encode("newPassword123");
    }

    @Test
    void updateUserPassword_shouldThrowPasswordMismatchException_whenNewEqualsCurrent() {
        PasswordChangeDto passwordChangeDto = new PasswordChangeDto(
                "samePassword", "samePassword", "samePassword"
        );

        assertThatThrownBy(() -> userService.updateUserPassword(1L, passwordChangeDto))
                .isInstanceOf(PasswordMismatchException.class)
                .hasMessageContaining("cannot be the same");

        verifyNoInteractions(userRepository);
    }

    @Test
    void updateUserPassword_shouldThrowUserNotFoundOrRuntimeException_whenUserDoesNotExist() {
        PasswordChangeDto passwordChangeDto = new PasswordChangeDto(
                "plainPassword", "newPassword123", "newPassword123"
        );

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserPassword(99L, passwordChangeDto))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void updateUserPassword_shouldThrowInvalidCredentialsException_whenCurrentPasswordIncorrect() {
        PasswordChangeDto passwordChangeDto = new PasswordChangeDto(
                "wrongPassword", "newPassword123", "newPassword123"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.updateUserPassword(1L, passwordChangeDto))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("incorrect");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserPassword_shouldThrowPasswordMismatchException_whenConfirmationDoesNotMatch() {
        PasswordChangeDto passwordChangeDto = new PasswordChangeDto(
                "plainPassword", "newPassword123", "differentConfirmation"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("plainPassword", "encodedPassword")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUserPassword(1L, passwordChangeDto))
                .isInstanceOf(PasswordMismatchException.class)
                .hasMessageContaining("do not match");

        verify(userRepository, never()).save(any());
    }

    // ---------- deleteUser ----------

    @Test
    void deleteUser_shouldDeleteUser_whenUserExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        userService.deleteUser(1L);

        verify(userRepository).delete(existingUser);
    }

    @Test
    void deleteUser_shouldThrowException_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(RuntimeException.class);

        verify(userRepository, never()).delete(any());
    }
}
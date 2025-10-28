package com.selimhorri.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.impl.UserServiceImpl;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final Integer USER_ID = 1;
    private static final Integer CREDENTIAL_ID = 10;
    private static final String USERNAME = "john.doe";
    private static final String EMAIL = "john.doe@example.com";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User userEntity;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        this.userEntity = buildUserEntity();
        this.userDto = buildUserDto();
    }

    @Test
    void findAll_whenUsersExist_returnsMappedDtos() {
        when(userRepository.findAll()).thenReturn(List.of(userEntity));

        List<UserDto> result = userService.findAll();

        assertEquals(1, result.size());
        assertEquals(EMAIL, result.get(0).getEmail());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void findById_whenUserExists_returnsDto() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));

        UserDto result = userService.findById(USER_ID);

        assertEquals(USER_ID, result.getUserId());
        assertEquals(USERNAME, result.getCredentialDto().getUsername());
    }

    @Test
    void findById_whenUserMissing_throwsException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(UserObjectNotFoundException.class, () -> userService.findById(USER_ID));
    }

    @Test
    void save_whenUserValid_persistsAndReturnsMappedUser() {
        when(userRepository.save(any(User.class))).thenReturn(userEntity);

        UserDto result = userService.save(userDto);

        assertNotNull(result);
        assertEquals(USER_ID, result.getUserId());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedEntity = userCaptor.getValue();
        assertEquals(EMAIL, savedEntity.getEmail());
        assertEquals(USERNAME, savedEntity.getCredential().getUsername());
    }

    @Test
    void update_whenUserValid_persistsAndReturnsMappedUser() {
        when(userRepository.save(any(User.class))).thenReturn(userEntity);

        UserDto result = userService.update(userDto);

        assertEquals(USER_ID, result.getUserId());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateById_whenUserExists_checksPresenceAndSaves() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
        when(userRepository.save(any(User.class))).thenReturn(userEntity);

        UserDto result = userService.update(USER_ID, userDto);

        assertEquals(USER_ID, result.getUserId());
        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void deleteById_always_delegatesToRepository() {
        userService.deleteById(USER_ID);

        verify(userRepository, times(1)).deleteById(USER_ID);
    }

    @Test
    void findByUsername_whenUserExists_returnsDto() {
        when(userRepository.findByCredentialUsername(USERNAME)).thenReturn(Optional.of(userEntity));

        UserDto result = userService.findByUsername(USERNAME);

        assertEquals(USER_ID, result.getUserId());
        assertEquals(EMAIL, result.getEmail());
    }

    @Test
    void findByUsername_whenUserMissing_throwsException() {
        when(userRepository.findByCredentialUsername(USERNAME)).thenReturn(Optional.empty());

        assertThrows(UserObjectNotFoundException.class, () -> userService.findByUsername(USERNAME));
    }

    private User buildUserEntity() {
        Credential credential = Credential.builder()
            .credentialId(CREDENTIAL_ID)
            .username(USERNAME)
            .password("secret")
            .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
            .isEnabled(Boolean.TRUE)
            .isAccountNonExpired(Boolean.TRUE)
            .isAccountNonLocked(Boolean.TRUE)
            .isCredentialsNonExpired(Boolean.TRUE)
            .build();

        User user = User.builder()
            .userId(USER_ID)
            .firstName("John")
            .lastName("Doe")
            .imageUrl("/avatars/john.png")
            .email(EMAIL)
            .phone("555-1234")
            .credential(credential)
            .build();

        credential.setUser(user);
        return user;
    }

    private UserDto buildUserDto() {
        CredentialDto credentialDto = CredentialDto.builder()
            .credentialId(CREDENTIAL_ID)
            .username(USERNAME)
            .password("secret")
            .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
            .isEnabled(Boolean.TRUE)
            .isAccountNonExpired(Boolean.TRUE)
            .isAccountNonLocked(Boolean.TRUE)
            .isCredentialsNonExpired(Boolean.TRUE)
            .build();

        return UserDto.builder()
            .userId(USER_ID)
            .firstName("John")
            .lastName("Doe")
            .imageUrl("/avatars/john.png")
            .email(EMAIL)
            .phone("555-1234")
            .credentialDto(credentialDto)
            .build();
    }
}
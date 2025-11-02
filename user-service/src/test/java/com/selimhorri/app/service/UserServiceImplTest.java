package com.selimhorri.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
            .userId(1)
            .firstName("Ada")
            .lastName("Lovelace")
            .email("ada@example.com")
            .phone("555-1000")
            .credential(Credential.builder()
                .credentialId(7)
                .username("ada")
                .password("encrypted")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build())
            .build();
        sampleUser.getCredential().setUser(sampleUser);
    }

    @Test
    @DisplayName("findAll devuelve la lista enriquecida de usuarios")
    void findAllReturnsUsers() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<UserDto> result = userService.findAll();

        assertThat(result)
            .singleElement()
            .satisfies(dto -> {
                assertThat(dto.getUserId()).isEqualTo(sampleUser.getUserId());
                assertThat(dto.getCredentialDto().getUsername()).isEqualTo("ada");
            });
    }

    @Test
    @DisplayName("findById devuelve el usuario cuando existe")
    void findByIdReturnsUserWhenExists() {
        when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));

        UserDto dto = userService.findById(1);

        assertThat(dto.getFirstName()).isEqualTo("Ada");
        assertThat(dto.getCredentialDto().getRoleBasedAuthority()).isEqualTo(RoleBasedAuthority.ROLE_USER);
    }

    @Test
    @DisplayName("findById lanza excepción cuando el usuario no existe")
    void findByIdThrowsWhenMissing() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99))
            .isInstanceOf(UserObjectNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    @DisplayName("save persiste el usuario y devuelve el dto mapeado")
    void savePersistsUserAndReturnsDto() {
        UserDto payload = UserDto.builder()
            .firstName("Grace")
            .lastName("Hopper")
            .email("grace@example.com")
            .credentialDto(CredentialDto.builder()
                .username("grace")
                .password("secret")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_ADMIN)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build())
            .build();

        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserDto saved = userService.save(payload);

        assertThat(saved.getCredentialDto().getUsername()).isEqualTo("ada");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("findByUsername delega en el repositorio y mapea el resultado")
    void findByUsernameDelegatesToRepository() {
        when(userRepository.findByCredentialUsername(eq("ada"))).thenReturn(Optional.of(sampleUser));

        UserDto dto = userService.findByUsername("ada");

        assertThat(dto.getCredentialDto().getUsername()).isEqualTo("ada");
    }
}

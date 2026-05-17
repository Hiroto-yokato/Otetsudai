package design.hitsuji.otetsudai.service;

import design.hitsuji.otetsudai.entity.User;
import design.hitsuji.otetsudai.enums.Role;
import design.hitsuji.otetsudai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User parentUser;
    private User childUser;

    @BeforeEach
    void setUp() {
        parentUser = new User();
        parentUser.setUserId("parent");
        parentUser.setEmail("parent@example.com");
        parentUser.setPasswordHash("$2a$12$hashedpassword");
        parentUser.setRole(Role.PARENT);

        childUser = new User();
        childUser.setUserId("child");
        childUser.setPasswordHash("$2a$12$hashedpassword2");
        childUser.setRole(Role.CHILD);
    }

    @Test
    void loadUserByUsername_withUserId_returnsUserDetails() {
        // Given
        when(userRepository.findByUserIdOrEmail("parent", "parent"))
                .thenReturn(Optional.of(parentUser));

        // When
        UserDetails details = userDetailsService.loadUserByUsername("parent");

        // Then
        assertThat(details.getUsername()).isEqualTo("parent");
        assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_PARENT"));
    }

    @Test
    void loadUserByUsername_withEmail_returnsUserDetails() {
        // Given
        when(userRepository.findByUserIdOrEmail("parent@example.com", "parent@example.com"))
                .thenReturn(Optional.of(parentUser));

        // When
        UserDetails details = userDetailsService.loadUserByUsername("parent@example.com");

        // Then
        assertThat(details.getUsername()).isEqualTo("parent");
        assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_PARENT"));
    }

    @Test
    void loadUserByUsername_withChildUserId_hasChildRole() {
        // Given
        when(userRepository.findByUserIdOrEmail("child", "child"))
                .thenReturn(Optional.of(childUser));

        // When
        UserDetails details = userDetailsService.loadUserByUsername("child");

        // Then
        assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_CHILD"));
        assertThat(details.getAuthorities()).noneMatch(a -> a.getAuthority().equals("ROLE_PARENT"));
    }

    @Test
    void loadUserByUsername_withUnknownId_throwsUsernameNotFoundException() {
        // Given
        when(userRepository.findByUserIdOrEmail("unknown", "unknown"))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}

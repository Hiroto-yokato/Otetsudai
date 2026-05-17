package design.hitsuji.otetsudai.service;

import design.hitsuji.otetsudai.dto.ParentRegistrationForm;
import design.hitsuji.otetsudai.dto.UserRegistrationForm;
import design.hitsuji.otetsudai.entity.User;
import design.hitsuji.otetsudai.enums.Role;
import design.hitsuji.otetsudai.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User parent;

    @BeforeEach
    void setUp() {
        parent = new User();
        parent.setUserId("parent1");
        parent.setRole(Role.PARENT);
        parent.setFamilyId(1L);
        parent.setPasswordHash("hash");
    }

    @Test
    void registerChild_success() {
        // Given
        when(userRepository.findByUserId("parent1")).thenReturn(Optional.of(parent));
        when(userRepository.existsByUserId("child1")).thenReturn(false);
        when(userRepository.existsByEmail("child@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRegistrationForm form = new UserRegistrationForm();
        form.setUserId("child1");
        form.setPassword("password1");
        form.setPasswordConfirm("password1");
        form.setEmail("child@example.com");
        form.setDisplayName("太郎");

        // When
        User saved = userService.registerChild("parent1", form);

        // Then
        assertThat(saved.getUserId()).isEqualTo("child1");
        assertThat(saved.getRole()).isEqualTo(Role.CHILD);
        assertThat(saved.getFamilyId()).isEqualTo(1L);
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getEmail()).isEqualTo("child@example.com");
        assertThat(saved.getDisplayName()).isEqualTo("太郎");
        verify(passwordEncoder).encode("password1");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerChild_withoutEmailAndDisplayName_savedWithNullFields() {
        // Given
        when(userRepository.findByUserId("parent1")).thenReturn(Optional.of(parent));
        when(userRepository.existsByUserId("child1")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRegistrationForm form = new UserRegistrationForm();
        form.setUserId("child1");
        form.setPassword("password1");
        form.setPasswordConfirm("password1");
        form.setEmail("");
        form.setDisplayName("");

        // When
        User saved = userService.registerChild("parent1", form);

        // Then
        assertThat(saved.getEmail()).isNull();
        assertThat(saved.getDisplayName()).isNull();
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void registerChild_parentNotFound_throwsIllegalStateException() {
        // Given
        when(userRepository.findByUserId("unknown")).thenReturn(Optional.empty());

        UserRegistrationForm form = new UserRegistrationForm();
        form.setUserId("child1");
        form.setPassword("password1");
        form.setPasswordConfirm("password1");

        // When / Then
        assertThatThrownBy(() -> userService.registerChild("unknown", form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ユーザーが見つかりません");
    }

    @Test
    void listChildren_parentNotFound_throwsIllegalStateException() {
        // Given
        when(userRepository.findByUserId("unknown")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> userService.listChildren("unknown"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ユーザーが見つかりません");
    }

    @Test
    void registerChild_duplicateUserId() {
        // Given
        when(userRepository.findByUserId("parent1")).thenReturn(Optional.of(parent));
        when(userRepository.existsByUserId("existing")).thenReturn(true);

        UserRegistrationForm form = new UserRegistrationForm();
        form.setUserId("existing");
        form.setPassword("password1");
        form.setPasswordConfirm("password1");

        // When / Then
        assertThatThrownBy(() -> userService.registerChild("parent1", form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ユーザーIDは既に使用されています");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerChild_passwordMismatch() {
        // Given
        when(userRepository.findByUserId("parent1")).thenReturn(Optional.of(parent));
        when(userRepository.existsByUserId("child1")).thenReturn(false);

        UserRegistrationForm form = new UserRegistrationForm();
        form.setUserId("child1");
        form.setPassword("password1");
        form.setPasswordConfirm("different");

        // When / Then
        assertThatThrownBy(() -> userService.registerChild("parent1", form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("パスワードが一致しません");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerChild_duplicateEmail() {
        // Given
        when(userRepository.findByUserId("parent1")).thenReturn(Optional.of(parent));
        when(userRepository.existsByUserId("child1")).thenReturn(false);
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        UserRegistrationForm form = new UserRegistrationForm();
        form.setUserId("child1");
        form.setPassword("password1");
        form.setPasswordConfirm("password1");
        form.setEmail("dup@example.com");

        // When / Then
        assertThatThrownBy(() -> userService.registerChild("parent1", form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("メールアドレスは既に使用されています");
        verify(userRepository, never()).save(any());
    }

    // ===== registerParent =====

    @Test
    void registerParent_success() {
        // Given
        when(userRepository.existsByUserId("newparent")).thenReturn(false);
        when(userRepository.existsByEmail("newparent@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed");

        User firstSave = new User();
        firstSave.setUserId("newparent");
        firstSave.setRole(Role.PARENT);
        setId(firstSave, 5L);

        User secondSave = new User();
        secondSave.setUserId("newparent");
        secondSave.setRole(Role.PARENT);
        secondSave.setFamilyId(5L);
        setId(secondSave, 5L);

        when(userRepository.save(any(User.class)))
                .thenReturn(firstSave)
                .thenReturn(secondSave);

        ParentRegistrationForm form = new ParentRegistrationForm();
        form.setUserId("newparent");
        form.setPassword("password1");
        form.setPasswordConfirm("password1");
        form.setEmail("newparent@example.com");

        // When
        User saved = userService.registerParent(form);

        // Then
        assertThat(saved.getRole()).isEqualTo(Role.PARENT);
        assertThat(saved.getFamilyId()).isEqualTo(5L);
        verify(passwordEncoder).encode("password1");
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void registerParent_duplicateUserId() {
        // Given
        when(userRepository.existsByUserId("existing")).thenReturn(true);

        ParentRegistrationForm form = new ParentRegistrationForm();
        form.setUserId("existing");
        form.setPassword("password1");
        form.setPasswordConfirm("password1");
        form.setEmail("new@example.com");

        // When / Then
        assertThatThrownBy(() -> userService.registerParent(form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ユーザーIDは既に使用されています");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerParent_passwordMismatch() {
        // Given
        when(userRepository.existsByUserId("newparent")).thenReturn(false);

        ParentRegistrationForm form = new ParentRegistrationForm();
        form.setUserId("newparent");
        form.setPassword("password1");
        form.setPasswordConfirm("different");
        form.setEmail("new@example.com");

        // When / Then
        assertThatThrownBy(() -> userService.registerParent(form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("パスワードが一致しません");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerParent_duplicateEmail() {
        // Given
        when(userRepository.existsByUserId("newparent")).thenReturn(false);
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        ParentRegistrationForm form = new ParentRegistrationForm();
        form.setUserId("newparent");
        form.setPassword("password1");
        form.setPasswordConfirm("password1");
        form.setEmail("dup@example.com");

        // When / Then
        assertThatThrownBy(() -> userService.registerParent(form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("メールアドレスは既に使用されています");
        verify(userRepository, never()).save(any());
    }

    private void setId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void listChildren_returnsChildrenOfSameFamily() {
        // Given
        User child1 = new User();
        child1.setUserId("child1");
        child1.setRole(Role.CHILD);
        child1.setFamilyId(1L);

        User child2 = new User();
        child2.setUserId("child2");
        child2.setRole(Role.CHILD);
        child2.setFamilyId(1L);

        when(userRepository.findByUserId("parent1")).thenReturn(Optional.of(parent));
        when(userRepository.findByRoleAndFamilyId(Role.CHILD, 1L)).thenReturn(List.of(child1, child2));

        // When
        List<User> children = userService.listChildren("parent1");

        // Then
        assertThat(children).hasSize(2);
        assertThat(children).allMatch(u -> u.getRole() == Role.CHILD);
        assertThat(children).allMatch(u -> u.getFamilyId().equals(1L));
    }
}

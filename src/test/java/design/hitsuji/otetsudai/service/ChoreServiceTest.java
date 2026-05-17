package design.hitsuji.otetsudai.service;

import design.hitsuji.otetsudai.entity.Chore;
import design.hitsuji.otetsudai.entity.User;
import design.hitsuji.otetsudai.enums.ChoreStatus;
import design.hitsuji.otetsudai.enums.Role;
import design.hitsuji.otetsudai.repository.ChoreRepository;
import design.hitsuji.otetsudai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChoreServiceTest {

    @Mock
    private ChoreRepository choreRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChoreService choreService;

    private User childUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        childUser = new User();
        childUser.setUserId("child");
        childUser.setPasswordHash("$2a$12$hash");
        childUser.setRole(Role.CHILD);
        setId(childUser, 1L);

        otherUser = new User();
        otherUser.setUserId("other");
        otherUser.setPasswordHash("$2a$12$hash2");
        otherUser.setRole(Role.CHILD);
        setId(otherUser, 2L);
    }

    @Test
    void listChores_returnsChoresForUser() {
        // Given
        Chore chore = buildChore(1L, ChoreStatus.UNPAID);
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));
        when(choreRepository.findByUserIdOrderByChoreDateDesc(1L)).thenReturn(List.of(chore));

        // When
        List<Chore> result = choreService.listChores("child");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    void listUnpaidChores_returnsOnlyUnpaidChores() {
        // Given
        Chore unpaidChore = buildChore(1L, ChoreStatus.UNPAID);
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));
        when(choreRepository.findByUserIdAndStatus(1L, ChoreStatus.UNPAID)).thenReturn(List.of(unpaidChore));

        // When
        List<Chore> result = choreService.listUnpaidChores("child");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ChoreStatus.UNPAID);
        verify(choreRepository).findByUserIdAndStatus(1L, ChoreStatus.UNPAID);
    }

    @Test
    void createChore_savesWithUnpaidStatus() {
        // Given
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));
        Chore saved = new Chore();
        saved.setStatus(ChoreStatus.UNPAID);
        when(choreRepository.save(any(Chore.class))).thenReturn(saved);

        // When
        Chore result = choreService.createChore("child", LocalDate.now(), "お皿洗い");

        // Then
        assertThat(result.getStatus()).isEqualTo(ChoreStatus.UNPAID);
        verify(choreRepository).save(argThat(c ->
                c.getUserId().equals(1L)
                && c.getContent().equals("お皿洗い")
                && c.getStatus() == ChoreStatus.UNPAID
        ));
    }

    @Test
    void deleteChore_unpaidOwnChore_deletesSuccessfully() {
        // Given
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));
        Chore chore = buildChore(1L, ChoreStatus.UNPAID);
        when(choreRepository.findById(10L)).thenReturn(Optional.of(chore));

        // When
        choreService.deleteChore(10L, "child");

        // Then
        verify(choreRepository).delete(chore);
    }

    @ParameterizedTest
    @EnumSource(value = ChoreStatus.class, names = {"PENDING", "APPROVED", "REJECTED"})
    void deleteChore_nonUnpaidChore_throwsException(ChoreStatus status) {
        // Given
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));
        Chore chore = buildChore(1L, status);
        when(choreRepository.findById(10L)).thenReturn(Optional.of(chore));

        // When / Then
        assertThatThrownBy(() -> choreService.deleteChore(10L, "child"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("削除できません");
        verify(choreRepository, never()).delete(any());
    }

    @Test
    void deleteChore_otherUsersChore_throwsException() {
        // Given
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));
        Chore chore = buildChore(2L, ChoreStatus.UNPAID); // userId=2 (otherUser)
        when(choreRepository.findById(10L)).thenReturn(Optional.of(chore));

        // When / Then
        assertThatThrownBy(() -> choreService.deleteChore(10L, "child"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("他のユーザー");
        verify(choreRepository, never()).delete(any());
    }

    private Chore buildChore(Long userId, ChoreStatus status) {
        Chore chore = new Chore();
        chore.setUserId(userId);
        chore.setChoreDate(LocalDate.now());
        chore.setContent("テスト");
        chore.setStatus(status);
        return chore;
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
}

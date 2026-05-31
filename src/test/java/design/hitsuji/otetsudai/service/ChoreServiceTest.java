package design.hitsuji.otetsudai.service;

import design.hitsuji.otetsudai.dto.BulkChoreForm;
import design.hitsuji.otetsudai.dto.ChoreForm;
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
import org.springframework.security.access.AccessDeniedException;

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

    @Mock
    private NotificationService notificationService;

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
        verify(notificationService).notifyParentChoreRegistered(any(User.class), any(Chore.class));
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
                .hasMessageContaining("操作できません");
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
                .isInstanceOf(AccessDeniedException.class);
        verify(choreRepository, never()).delete(any());
    }

    // ===== updateChore =====

    @Test
    void updateChore_success() {
        // Given
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));
        Chore chore = buildChore(1L, ChoreStatus.UNPAID);
        when(choreRepository.findById(10L)).thenReturn(Optional.of(chore));
        when(choreRepository.save(any(Chore.class))).thenAnswer(inv -> inv.getArgument(0));

        ChoreForm form = new ChoreForm();
        form.setChoreDate(LocalDate.of(2026, 5, 20));
        form.setContent("掃除機をかける");

        // When
        Chore updated = choreService.updateChore(10L, "child", form);

        // Then
        assertThat(updated.getChoreDate()).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(updated.getContent()).isEqualTo("掃除機をかける");
        verify(choreRepository).save(chore);
    }

    @ParameterizedTest
    @EnumSource(value = ChoreStatus.class, names = {"PENDING", "APPROVED", "REJECTED"})
    void updateChore_notUnpaid_throwsIllegalStateException(ChoreStatus status) {
        // Given
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));
        Chore chore = buildChore(1L, status);
        when(choreRepository.findById(10L)).thenReturn(Optional.of(chore));

        ChoreForm form = new ChoreForm();
        form.setChoreDate(LocalDate.now());
        form.setContent("テスト");

        // When / Then
        assertThatThrownBy(() -> choreService.updateChore(10L, "child", form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("操作できません");
        verify(choreRepository, never()).save(any());
    }

    @Test
    void updateChore_otherUser_throwsAccessDeniedException() {
        // Given
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));
        Chore chore = buildChore(2L, ChoreStatus.UNPAID); // userId=2 (otherUser)
        when(choreRepository.findById(10L)).thenReturn(Optional.of(chore));

        ChoreForm form = new ChoreForm();
        form.setChoreDate(LocalDate.now());
        form.setContent("テスト");

        // When / Then
        assertThatThrownBy(() -> choreService.updateChore(10L, "child", form))
                .isInstanceOf(AccessDeniedException.class);
        verify(choreRepository, never()).save(any());
    }

    // ===== listFamilyChores =====

    @Test
    void listFamilyChores_returnsAllChildrenChores() {
        // Given
        User parent = new User();
        parent.setUserId("parent1");
        parent.setRole(Role.PARENT);
        parent.setFamilyId(10L);
        setId(parent, 99L);

        User child2 = new User();
        child2.setUserId("child2");
        child2.setRole(Role.CHILD);
        child2.setFamilyId(10L);
        setId(child2, 2L);

        when(userRepository.findByUserId("parent1")).thenReturn(Optional.of(parent));
        when(userRepository.findByRoleAndFamilyId(Role.CHILD, 10L)).thenReturn(List.of(childUser, child2));

        Chore chore1 = buildChore(1L, ChoreStatus.UNPAID);
        Chore chore2 = buildChore(2L, ChoreStatus.APPROVED);
        when(choreRepository.findByUserIdInOrderByChoreDateDesc(List.of(1L, 2L))).thenReturn(List.of(chore1, chore2));

        // When
        List<Chore> result = choreService.listFamilyChores("parent1");

        // Then
        assertThat(result).hasSize(2);
        verify(choreRepository).findByUserIdInOrderByChoreDateDesc(List.of(1L, 2L));
    }

    @Test
    void listFamilyChores_noFamilyId_returnsEmpty() {
        // Given
        User parent = new User();
        parent.setUserId("parent1");
        parent.setRole(Role.PARENT);
        // familyId is null (未設定)

        when(userRepository.findByUserId("parent1")).thenReturn(Optional.of(parent));

        // When
        List<Chore> result = choreService.listFamilyChores("parent1");

        // Then
        assertThat(result).isEmpty();
        verify(choreRepository, never()).findByUserIdInOrderByChoreDateDesc(any());
    }

    @Test
    void listFamilyChores_noChildren_returnsEmpty() {
        // Given
        User parent = new User();
        parent.setUserId("parent1");
        parent.setRole(Role.PARENT);
        parent.setFamilyId(10L);

        when(userRepository.findByUserId("parent1")).thenReturn(Optional.of(parent));
        when(userRepository.findByRoleAndFamilyId(Role.CHILD, 10L)).thenReturn(List.of());

        // When
        List<Chore> result = choreService.listFamilyChores("parent1");

        // Then
        assertThat(result).isEmpty();
        verify(choreRepository, never()).findByUserIdInOrderByChoreDateDesc(any());
    }

    @Test
    void listFamilyChores_calledByChild_throwsAccessDeniedException() {
        // Given
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));

        // When / Then
        assertThatThrownBy(() -> choreService.listFamilyChores("child"))
                .isInstanceOf(AccessDeniedException.class);
        verify(choreRepository, never()).findByUserIdInOrderByChoreDateDesc(any());
    }

    // ===== createChoresBulk =====

    @Test
    void createChoresBulk_success() {
        // Given
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));
        when(choreRepository.save(any(Chore.class))).thenAnswer(inv -> inv.getArgument(0));

        BulkChoreForm form = new BulkChoreForm();
        form.getEntries().add(entry(LocalDate.of(2026, 5, 24), "掃除機をかける"));
        form.getEntries().add(entry(LocalDate.of(2026, 5, 24), "食器を洗う"));

        // When
        List<Chore> saved = choreService.createChoresBulk("child", form);

        // Then
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getContent()).isEqualTo("掃除機をかける");
        assertThat(saved.get(1).getContent()).isEqualTo("食器を洗う");
        assertThat(saved).allMatch(c -> c.getStatus() == ChoreStatus.UNPAID);
        verify(choreRepository, times(2)).save(any(Chore.class));
        verify(notificationService).notifyParentBulkChoreRegistered(any(User.class), anyList());
    }

    @Test
    void createChoresBulk_skipsEmptyEntries() {
        // Given
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));
        when(choreRepository.save(any(Chore.class))).thenAnswer(inv -> inv.getArgument(0));

        BulkChoreForm form = new BulkChoreForm();
        form.getEntries().add(entry(LocalDate.of(2026, 5, 24), "食器を洗う"));
        form.getEntries().add(entry(LocalDate.of(2026, 5, 24), ""));   // 空行
        form.getEntries().add(entry(LocalDate.of(2026, 5, 24), "  ")); // 空白のみ

        // When
        List<Chore> saved = choreService.createChoresBulk("child", form);

        // Then
        assertThat(saved).hasSize(1);
        verify(choreRepository, times(1)).save(any(Chore.class));
    }

    @Test
    void createChoresBulk_allEmpty_throwsIllegalStateException() {
        // Given
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));

        BulkChoreForm form = new BulkChoreForm();
        form.getEntries().add(entry(LocalDate.now(), ""));
        form.getEntries().add(entry(LocalDate.now(), ""));

        // When / Then
        assertThatThrownBy(() -> choreService.createChoresBulk("child", form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1件も入力されていません");
        verify(choreRepository, never()).save(any());
    }

    @Test
    void createChoresBulk_nullDate_usesToday() {
        // Given
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));
        when(choreRepository.save(any(Chore.class))).thenAnswer(inv -> inv.getArgument(0));

        BulkChoreForm form = new BulkChoreForm();
        BulkChoreForm.ChoreEntryForm e = new BulkChoreForm.ChoreEntryForm();
        e.setChoreDate(null);
        e.setContent("ゴミを捨てる");
        form.getEntries().add(e);

        // When
        List<Chore> saved = choreService.createChoresBulk("child", form);

        // Then
        assertThat(saved.get(0).getChoreDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void createChoresBulk_futureDate_throwsIllegalStateException() {
        // Given
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));

        BulkChoreForm form = new BulkChoreForm();
        form.getEntries().add(entry(LocalDate.now().plusDays(1), "お皿洗い"));

        // When / Then
        assertThatThrownBy(() -> choreService.createChoresBulk("child", form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未来の日付");
        verify(choreRepository, never()).save(any());
    }

    @Test
    void createChoresBulk_contentTooLong_throwsIllegalStateException() {
        // Given
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));

        BulkChoreForm form = new BulkChoreForm();
        form.getEntries().add(entry(LocalDate.now(), "あ".repeat(101)));

        // When / Then
        assertThatThrownBy(() -> choreService.createChoresBulk("child", form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("100文字以内");
        verify(choreRepository, never()).save(any());
    }

    @Test
    void createChoresBulk_contentIsTrimmed() {
        // Given
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));
        when(choreRepository.save(any(Chore.class))).thenAnswer(inv -> inv.getArgument(0));

        BulkChoreForm form = new BulkChoreForm();
        form.getEntries().add(entry(LocalDate.now(), "  お皿洗い  "));

        // When
        List<Chore> saved = choreService.createChoresBulk("child", form);

        // Then
        assertThat(saved.get(0).getContent()).isEqualTo("お皿洗い");
    }

    private BulkChoreForm.ChoreEntryForm entry(LocalDate date, String content) {
        BulkChoreForm.ChoreEntryForm e = new BulkChoreForm.ChoreEntryForm();
        e.setChoreDate(date);
        e.setContent(content);
        return e;
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

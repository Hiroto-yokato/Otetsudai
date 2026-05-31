package design.hitsuji.otetsudai.service;

import design.hitsuji.otetsudai.entity.AllowanceRequest;
import design.hitsuji.otetsudai.entity.Chore;
import design.hitsuji.otetsudai.entity.User;
import design.hitsuji.otetsudai.enums.ChoreStatus;
import design.hitsuji.otetsudai.enums.Role;
import design.hitsuji.otetsudai.repository.AllowanceRequestRepository;
import design.hitsuji.otetsudai.repository.ChoreRepository;
import design.hitsuji.otetsudai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllowanceServiceTest {

    @Mock
    private AllowanceRequestRepository allowanceRequestRepository;

    @Mock
    private ChoreRepository choreRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AllowanceService allowanceService;

    private User childUser;
    private final LocalDate today = LocalDate.now();
    private final LocalDate yesterday = today.minusDays(1);

    @BeforeEach
    void setUp() {
        childUser = new User();
        childUser.setUserId("child");
        childUser.setPasswordHash("$2a$12$hash");
        childUser.setRole(Role.CHILD);
        setId(childUser, 1L);
    }

    // ===== calculateAmount =====

    @Test
    void calculateAmount_oneChore_returns20() {
        List<Chore> chores = List.of(buildChore(1L, today));

        int result = allowanceService.calculateAmount(chores);

        assertThat(result).isEqualTo(20);
    }

    @Test
    void calculateAmount_twoChoreSameDay_returns50() {
        List<Chore> chores = List.of(
                buildChore(1L, today),
                buildChore(1L, today)
        );

        int result = allowanceService.calculateAmount(chores);

        assertThat(result).isEqualTo(50);
    }

    @Test
    void calculateAmount_threeChoreSameDay_returns100() {
        List<Chore> chores = List.of(
                buildChore(1L, today),
                buildChore(1L, today),
                buildChore(1L, today)
        );

        int result = allowanceService.calculateAmount(chores);

        assertThat(result).isEqualTo(100);
    }

    @Test
    void calculateAmount_multipleDays_returnsSum() {
        // yesterday: 1 chore → ¥20, today: 2 chores → ¥50, total = ¥70
        List<Chore> chores = List.of(
                buildChore(1L, yesterday),
                buildChore(1L, today),
                buildChore(1L, today)
        );

        int result = allowanceService.calculateAmount(chores);

        assertThat(result).isEqualTo(70);
    }

    // ===== createRequest =====

    @Test
    void createRequest_success_savesAllowanceRequestAsPending() {
        // Given
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));
        Chore chore = buildChore(1L, today);
        chore.setStatus(ChoreStatus.UNPAID);
        when(choreRepository.findByIdInAndUserId(List.of(10L), 1L)).thenReturn(List.of(chore));

        AllowanceRequest saved = new AllowanceRequest();
        saved.setTotalAmount(20);
        setRequestId(saved, 100L);
        when(allowanceRequestRepository.save(any(AllowanceRequest.class))).thenReturn(saved);
        when(choreRepository.save(any(Chore.class))).thenReturn(chore);

        // When
        AllowanceRequest result = allowanceService.createRequest("child", List.of(10L));

        // Then
        assertThat(result.getTotalAmount()).isEqualTo(20);
        verify(allowanceRequestRepository).save(argThat(r ->
                r.getChildUserId().equals(1L) && r.getTotalAmount() == 20
        ));
        verify(notificationService).notifyParentAllowanceRequested(any(User.class), any(AllowanceRequest.class), anyList());
    }

    @Test
    void createRequest_success_choreBecomePendingWithRequestId() {
        // Given
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));
        Chore chore = buildChore(1L, today);
        chore.setStatus(ChoreStatus.UNPAID);
        when(choreRepository.findByIdInAndUserId(List.of(10L), 1L)).thenReturn(List.of(chore));

        AllowanceRequest saved = new AllowanceRequest();
        setRequestId(saved, 100L);
        when(allowanceRequestRepository.save(any(AllowanceRequest.class))).thenReturn(saved);
        when(choreRepository.save(any(Chore.class))).thenReturn(chore);

        // When
        allowanceService.createRequest("child", List.of(10L));

        // Then
        verify(choreRepository).save(argThat(c ->
                c.getStatus() == ChoreStatus.PENDING && Long.valueOf(100L).equals(c.getRequestId())
        ));
    }

    @Test
    void createRequest_emptyList_throwsIllegalStateException() {
        assertThatThrownBy(() -> allowanceService.createRequest("child", Collections.emptyList()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1件以上");
    }

    @Test
    void createRequest_otherUsersChoreIncluded_throwsIllegalStateException() {
        // Given: only 1 of 2 requested IDs belongs to the user (size mismatch)
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));
        Chore chore = buildChore(1L, today);
        chore.setStatus(ChoreStatus.UNPAID);
        when(choreRepository.findByIdInAndUserId(List.of(10L, 99L), 1L)).thenReturn(List.of(chore));

        // When / Then
        assertThatThrownBy(() -> allowanceService.createRequest("child", List.of(10L, 99L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("無効");
    }

    @Test
    void createRequest_pendingChoreIncluded_throwsIllegalStateException() {
        // Given
        when(userRepository.findByUserId("child")).thenReturn(Optional.of(childUser));
        Chore pendingChore = buildChore(1L, today);
        pendingChore.setStatus(ChoreStatus.PENDING);
        when(choreRepository.findByIdInAndUserId(List.of(10L), 1L)).thenReturn(List.of(pendingChore));

        // When / Then
        assertThatThrownBy(() -> allowanceService.createRequest("child", List.of(10L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("申請済み");
        verify(allowanceRequestRepository, never()).save(any());
    }

    @Test
    void calculateAmount_emptyList_returns0() {
        int result = allowanceService.calculateAmount(Collections.emptyList());

        assertThat(result).isEqualTo(0);
    }

    private Chore buildChore(Long userId, LocalDate date) {
        Chore chore = new Chore();
        chore.setUserId(userId);
        chore.setChoreDate(date);
        chore.setContent("テスト");
        chore.setStatus(ChoreStatus.UNPAID);
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

    private void setRequestId(AllowanceRequest request, Long id) {
        try {
            var field = AllowanceRequest.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(request, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

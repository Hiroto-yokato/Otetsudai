package design.hitsuji.otetsudai.service;

import design.hitsuji.otetsudai.entity.AllowanceRequest;
import design.hitsuji.otetsudai.entity.Chore;
import design.hitsuji.otetsudai.enums.ChoreStatus;
import design.hitsuji.otetsudai.enums.RequestStatus;
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
class AllowanceServiceApprovalTest {

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

    private AllowanceRequest pendingRequest;
    private Chore pendingChore;

    @BeforeEach
    void setUp() {
        pendingRequest = new AllowanceRequest();
        pendingRequest.setChildUserId(1L);
        pendingRequest.setRequestDate(LocalDate.now());
        pendingRequest.setTotalAmount(20);
        pendingRequest.setStatus(RequestStatus.PENDING);
        setRequestId(pendingRequest, 10L);

        pendingChore = new Chore();
        pendingChore.setUserId(1L);
        pendingChore.setChoreDate(LocalDate.now());
        pendingChore.setContent("テスト");
        pendingChore.setStatus(ChoreStatus.PENDING);
        pendingChore.setRequestId(10L);
    }

    // ===== approveRequest =====

    @Test
    void approveRequest_success_requestBecomesApproved() {
        // Given
        when(allowanceRequestRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));
        when(allowanceRequestRepository.save(any())).thenReturn(pendingRequest);
        when(choreRepository.findByRequestId(10L)).thenReturn(List.of(pendingChore));
        when(choreRepository.saveAll(anyList())).thenReturn(List.of(pendingChore));

        // When
        allowanceService.approveRequest(10L);

        // Then
        verify(allowanceRequestRepository).save(argThat(r ->
                r.getStatus() == RequestStatus.APPROVED
        ));
    }

    @Test
    void approveRequest_success_approvedAtIsSet() {
        // Given
        when(allowanceRequestRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));
        when(allowanceRequestRepository.save(any())).thenReturn(pendingRequest);
        when(choreRepository.findByRequestId(10L)).thenReturn(List.of(pendingChore));
        when(choreRepository.saveAll(anyList())).thenReturn(List.of(pendingChore));

        // When
        allowanceService.approveRequest(10L);

        // Then
        verify(allowanceRequestRepository).save(argThat(r ->
                r.getApprovedAt() != null
        ));
    }

    @Test
    void approveRequest_success_choreBecomesApproved() {
        // Given
        when(allowanceRequestRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));
        when(allowanceRequestRepository.save(any())).thenReturn(pendingRequest);
        when(choreRepository.findByRequestId(10L)).thenReturn(List.of(pendingChore));
        when(choreRepository.saveAll(anyList())).thenReturn(List.of(pendingChore));

        // When
        allowanceService.approveRequest(10L);

        // Then: pendingChore was mutated to APPROVED before saveAll was called
        assertThat(pendingChore.getStatus()).isEqualTo(ChoreStatus.APPROVED);
        verify(choreRepository).saveAll(anyList());
    }

    @Test
    void approveRequest_notPending_throwsIllegalStateException() {
        // Given
        AllowanceRequest approvedRequest = new AllowanceRequest();
        approvedRequest.setStatus(RequestStatus.APPROVED);
        setRequestId(approvedRequest, 10L);
        when(allowanceRequestRepository.findById(10L)).thenReturn(Optional.of(approvedRequest));

        // When / Then
        assertThatThrownBy(() -> allowanceService.approveRequest(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("承認できない");
        verify(choreRepository, never()).saveAll(any());
    }

    // ===== rejectRequest =====

    @Test
    void rejectRequest_success_requestBecomesRejected() {
        // Given
        when(allowanceRequestRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));
        when(allowanceRequestRepository.save(any())).thenReturn(pendingRequest);
        when(choreRepository.findByRequestId(10L)).thenReturn(List.of(pendingChore));
        when(choreRepository.saveAll(anyList())).thenReturn(List.of(pendingChore));

        // When
        allowanceService.rejectRequest(10L, "お手伝いが不十分です");

        // Then
        verify(allowanceRequestRepository).save(argThat(r ->
                r.getStatus() == RequestStatus.REJECTED
                && "お手伝いが不十分です".equals(r.getRejectionReason())
        ));
    }

    @Test
    void rejectRequest_success_choreBecomesUnpaidWithNullRequestId() {
        // Given
        when(allowanceRequestRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));
        when(allowanceRequestRepository.save(any())).thenReturn(pendingRequest);
        when(choreRepository.findByRequestId(10L)).thenReturn(List.of(pendingChore));
        when(choreRepository.saveAll(anyList())).thenReturn(List.of(pendingChore));

        // When
        allowanceService.rejectRequest(10L, "却下理由");

        // Then: pendingChore was mutated before saveAll was called
        assertThat(pendingChore.getStatus()).isEqualTo(ChoreStatus.UNPAID);
        assertThat(pendingChore.getRequestId()).isNull();
        verify(choreRepository).saveAll(anyList());
    }

    @Test
    void rejectRequest_withNullReason_succeeds() {
        // Given
        when(allowanceRequestRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));
        when(allowanceRequestRepository.save(any())).thenReturn(pendingRequest);
        when(choreRepository.findByRequestId(10L)).thenReturn(List.of(pendingChore));
        when(choreRepository.saveAll(anyList())).thenReturn(List.of(pendingChore));

        // When
        allowanceService.rejectRequest(10L, null);

        // Then
        verify(allowanceRequestRepository).save(argThat(r ->
                r.getStatus() == RequestStatus.REJECTED && r.getRejectionReason() == null
        ));
    }

    @Test
    void rejectRequest_notPending_throwsIllegalStateException() {
        // Given
        AllowanceRequest rejectedRequest = new AllowanceRequest();
        rejectedRequest.setStatus(RequestStatus.REJECTED);
        setRequestId(rejectedRequest, 10L);
        when(allowanceRequestRepository.findById(10L)).thenReturn(Optional.of(rejectedRequest));

        // When / Then
        assertThatThrownBy(() -> allowanceService.rejectRequest(10L, "理由"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("却下できない");
        verify(choreRepository, never()).saveAll(any());
    }

    // ===== getRequest =====

    @Test
    void getRequest_notFound_throwsIllegalStateException() {
        // Given
        when(allowanceRequestRepository.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> allowanceService.getRequest(99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("申請が見つかりません");
    }

    @Test
    void getRequest_alreadyProcessed_throwsIllegalStateException() {
        // Given
        AllowanceRequest approvedRequest = new AllowanceRequest();
        approvedRequest.setStatus(RequestStatus.APPROVED);
        setRequestId(approvedRequest, 10L);
        when(allowanceRequestRepository.findById(10L)).thenReturn(Optional.of(approvedRequest));

        // When / Then
        assertThatThrownBy(() -> allowanceService.getRequest(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("既に処理されています");
    }

    // ===== listPendingRequests / listApprovedHistory =====

    @Test
    void listPendingRequests_returnsPendingOnly() {
        // Given
        when(allowanceRequestRepository.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING))
                .thenReturn(List.of(pendingRequest));

        // When
        List<AllowanceRequest> result = allowanceService.listPendingRequests();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    void listApprovedHistory_returnsApprovedOnly() {
        // Given
        AllowanceRequest approvedRequest = new AllowanceRequest();
        approvedRequest.setStatus(RequestStatus.APPROVED);
        when(allowanceRequestRepository.findByStatusOrderByApprovedAtDesc(RequestStatus.APPROVED))
                .thenReturn(List.of(approvedRequest));

        // When
        List<AllowanceRequest> result = allowanceService.listApprovedHistory();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(RequestStatus.APPROVED);
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

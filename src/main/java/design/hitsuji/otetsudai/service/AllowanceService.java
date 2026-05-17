package design.hitsuji.otetsudai.service;

import design.hitsuji.otetsudai.entity.AllowanceRequest;
import design.hitsuji.otetsudai.entity.Chore;
import design.hitsuji.otetsudai.entity.User;
import design.hitsuji.otetsudai.enums.ChoreStatus;
import design.hitsuji.otetsudai.enums.RequestStatus;
import design.hitsuji.otetsudai.repository.AllowanceRequestRepository;
import design.hitsuji.otetsudai.repository.ChoreRepository;
import design.hitsuji.otetsudai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AllowanceService {

    private static final int SINGLE_CHORE_AMOUNT = 20;
    private static final int DOUBLE_CHORE_AMOUNT = 50;
    private static final int ADDITIONAL_CHORE_AMOUNT = 50;

    private final AllowanceRequestRepository allowanceRequestRepository;
    private final ChoreRepository choreRepository;
    private final UserRepository userRepository;

    public AllowanceService(AllowanceRequestRepository allowanceRequestRepository,
                            ChoreRepository choreRepository,
                            UserRepository userRepository) {
        this.allowanceRequestRepository = allowanceRequestRepository;
        this.choreRepository = choreRepository;
        this.userRepository = userRepository;
    }

    public int calculateAmount(List<Chore> chores) {
        Map<LocalDate, Long> countByDate = chores.stream()
                .collect(Collectors.groupingBy(Chore::getChoreDate, Collectors.counting()));

        int total = 0;
        for (long n : countByDate.values()) {
            if (n == 1) {
                total += SINGLE_CHORE_AMOUNT;
            } else if (n == 2) {
                total += DOUBLE_CHORE_AMOUNT;
            } else {
                total += ADDITIONAL_CHORE_AMOUNT * (n - 1);
            }
        }
        return total;
    }

    @Transactional(readOnly = true)
    public int previewAmount(String loginUserId, List<Long> choreIds) {
        if (choreIds == null || choreIds.isEmpty()) {
            return 0;
        }
        User user = findUserByLoginId(loginUserId);
        List<Chore> chores = choreRepository.findByIdInAndUserId(choreIds, user.getId());
        return calculateAmount(chores);
    }

    public AllowanceRequest createRequest(String loginUserId, List<Long> choreIds) {
        if (choreIds == null || choreIds.isEmpty()) {
            throw new IllegalStateException("お手伝いを1件以上選択してください");
        }

        User user = findUserByLoginId(loginUserId);
        List<Chore> chores = choreRepository.findByIdInAndUserId(choreIds, user.getId());

        if (chores.size() != choreIds.size()) {
            throw new IllegalStateException("選択したお手伝いに無効なものが含まれています");
        }

        for (Chore chore : chores) {
            if (chore.getStatus() != ChoreStatus.UNPAID) {
                throw new IllegalStateException("申請済みのお手伝いが含まれています");
            }
        }

        int amount = calculateAmount(chores);

        AllowanceRequest request = new AllowanceRequest();
        request.setChildUserId(user.getId());
        request.setRequestDate(LocalDate.now());
        request.setTotalAmount(amount);
        AllowanceRequest savedRequest = allowanceRequestRepository.save(request);

        for (Chore chore : chores) {
            chore.setStatus(ChoreStatus.PENDING);
            chore.setRequestId(savedRequest.getId());
            choreRepository.save(chore);
        }

        return savedRequest;
    }

    @Transactional(readOnly = true)
    public List<AllowanceRequest> listPendingRequests() {
        return allowanceRequestRepository.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public AllowanceRequest getRequest(Long requestId) {
        AllowanceRequest request = allowanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("申請が見つかりません"));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("この申請は既に処理されています");
        }
        return request;
    }

    @Transactional(readOnly = true)
    public List<Chore> getChoresForRequest(Long requestId) {
        return choreRepository.findByRequestId(requestId);
    }

    public void approveRequest(Long requestId) {
        AllowanceRequest request = allowanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("申請が見つかりません"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("承認できない状態の申請です");
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setApprovedAt(LocalDateTime.now());
        allowanceRequestRepository.save(request);

        List<Chore> chores = choreRepository.findByRequestId(requestId);
        chores.forEach(chore -> chore.setStatus(ChoreStatus.APPROVED));
        choreRepository.saveAll(chores);
    }

    public void rejectRequest(Long requestId, String reason) {
        AllowanceRequest request = allowanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("申請が見つかりません"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("却下できない状態の申請です");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setRejectionReason(reason);
        allowanceRequestRepository.save(request);

        List<Chore> chores = choreRepository.findByRequestId(requestId);
        chores.forEach(chore -> {
            chore.setStatus(ChoreStatus.UNPAID);
            chore.setRequestId(null);
        });
        choreRepository.saveAll(chores);
    }

    @Transactional(readOnly = true)
    public List<AllowanceRequest> listApprovedHistory() {
        return allowanceRequestRepository.findByStatusOrderByApprovedAtDesc(RequestStatus.APPROVED);
    }

    @Transactional(readOnly = true)
    public String getChildLoginId(Long childUserId) {
        return userRepository.findById(childUserId)
                .map(User::getUserId)
                .orElse("不明");
    }

    private User findUserByLoginId(String loginUserId) {
        return userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"));
    }
}

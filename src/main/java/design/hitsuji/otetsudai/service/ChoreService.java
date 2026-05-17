package design.hitsuji.otetsudai.service;

import design.hitsuji.otetsudai.entity.Chore;
import design.hitsuji.otetsudai.entity.User;
import design.hitsuji.otetsudai.enums.ChoreStatus;
import design.hitsuji.otetsudai.repository.ChoreRepository;
import design.hitsuji.otetsudai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ChoreService {

    private final ChoreRepository choreRepository;
    private final UserRepository userRepository;

    public ChoreService(ChoreRepository choreRepository, UserRepository userRepository) {
        this.choreRepository = choreRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Chore> listChores(String loginUserId) {
        User user = findUserByLoginId(loginUserId);
        return choreRepository.findByUserIdOrderByChoreDateDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public List<Chore> listUnpaidChores(String loginUserId) {
        User user = findUserByLoginId(loginUserId);
        return choreRepository.findByUserIdAndStatus(user.getId(), ChoreStatus.UNPAID);
    }

    public Chore createChore(String loginUserId, LocalDate choreDate, String content) {
        User user = findUserByLoginId(loginUserId);
        Chore chore = new Chore();
        chore.setUserId(user.getId());
        chore.setChoreDate(choreDate);
        chore.setContent(content);
        chore.setStatus(ChoreStatus.UNPAID);
        return choreRepository.save(chore);
    }

    public void deleteChore(Long choreId, String loginUserId) {
        User user = findUserByLoginId(loginUserId);
        Chore chore = choreRepository.findById(choreId)
                .orElseThrow(() -> new IllegalStateException("お手伝いが見つかりません"));

        if (!chore.getUserId().equals(user.getId())) {
            throw new IllegalStateException("他のユーザーのお手伝いは削除できません");
        }
        if (chore.getStatus() != ChoreStatus.UNPAID) {
            throw new IllegalStateException("申請中または承認済みのお手伝いは削除できません");
        }
        choreRepository.delete(chore);
    }

    private User findUserByLoginId(String loginUserId) {
        return userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"));
    }
}

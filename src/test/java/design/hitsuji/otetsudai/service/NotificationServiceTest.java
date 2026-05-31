package design.hitsuji.otetsudai.service;

import design.hitsuji.otetsudai.entity.AllowanceRequest;
import design.hitsuji.otetsudai.entity.Chore;
import design.hitsuji.otetsudai.entity.User;
import design.hitsuji.otetsudai.enums.ChoreStatus;
import design.hitsuji.otetsudai.enums.Role;
import design.hitsuji.otetsudai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User child;
    private User parent;

    @BeforeEach
    void setUp() {
        child = new User();
        child.setUserId("child1");
        child.setRole(Role.CHILD);
        child.setFamilyId(10L);
        child.setDisplayName("たろう");

        parent = new User();
        parent.setUserId("parent1");
        parent.setRole(Role.PARENT);
        parent.setFamilyId(10L);
        parent.setEmail("parent@example.com");
    }

    // ===== notifyParentChoreRegistered =====

    @Test
    void notifyParentChoreRegistered_withParentEmail_sendsEmail() {
        // Given
        when(userRepository.findByRoleAndFamilyId(Role.PARENT, 10L)).thenReturn(List.of(parent));
        Chore chore = buildChore("お皿洗い");

        // When
        notificationService.notifyParentChoreRegistered(child, chore);

        // Then
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("parent@example.com");
        assertThat(sent.getSubject()).contains("お手伝いを登録しました");
        assertThat(sent.getText()).contains("お皿洗い");
    }

    @Test
    void notifyParentChoreRegistered_noParentEmail_skips() {
        // Given
        parent.setEmail(null);
        when(userRepository.findByRoleAndFamilyId(Role.PARENT, 10L)).thenReturn(List.of(parent));
        Chore chore = buildChore("お皿洗い");

        // When
        notificationService.notifyParentChoreRegistered(child, chore);

        // Then
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void notifyParentChoreRegistered_nullFamilyId_skips() {
        // Given
        child.setFamilyId(null);
        Chore chore = buildChore("お皿洗い");

        // When
        notificationService.notifyParentChoreRegistered(child, chore);

        // Then
        verify(userRepository, never()).findByRoleAndFamilyId(any(), any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void notifyParentChoreRegistered_mailException_doesNotPropagate() {
        // Given
        when(userRepository.findByRoleAndFamilyId(Role.PARENT, 10L)).thenReturn(List.of(parent));
        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));
        Chore chore = buildChore("お皿洗い");

        // When / Then — 例外が呼び出し元に伝播しないこと
        assertThatCode(() -> notificationService.notifyParentChoreRegistered(child, chore))
                .doesNotThrowAnyException();
    }

    // ===== notifyParentBulkChoreRegistered =====

    @Test
    void notifyParentBulkChoreRegistered_withParentEmail_sendsEmail() {
        // Given
        when(userRepository.findByRoleAndFamilyId(Role.PARENT, 10L)).thenReturn(List.of(parent));
        List<Chore> chores = List.of(buildChore("掃除機をかける"), buildChore("食器を洗う"));

        // When
        notificationService.notifyParentBulkChoreRegistered(child, chores);

        // Then
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("parent@example.com");
        assertThat(sent.getSubject()).contains("2件まとめて登録しました");
        assertThat(sent.getText()).contains("掃除機をかける");
        assertThat(sent.getText()).contains("食器を洗う");
    }

    @Test
    void notifyParentBulkChoreRegistered_noParentEmail_skips() {
        // Given
        parent.setEmail(null);
        when(userRepository.findByRoleAndFamilyId(Role.PARENT, 10L)).thenReturn(List.of(parent));

        // When
        notificationService.notifyParentBulkChoreRegistered(child, List.of(buildChore("掃除")));

        // Then
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void notifyParentBulkChoreRegistered_mailException_doesNotPropagate() {
        // Given
        when(userRepository.findByRoleAndFamilyId(Role.PARENT, 10L)).thenReturn(List.of(parent));
        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        // When / Then
        assertThatCode(() -> notificationService.notifyParentBulkChoreRegistered(child, List.of(buildChore("掃除"))))
                .doesNotThrowAnyException();
    }

    // ===== notifyParentAllowanceRequested =====

    @Test
    void notifyParentAllowanceRequested_withParentEmail_sendsEmail() {
        // Given
        when(userRepository.findByRoleAndFamilyId(Role.PARENT, 10L)).thenReturn(List.of(parent));
        AllowanceRequest request = new AllowanceRequest();
        request.setTotalAmount(70);
        List<Chore> chores = List.of(buildChore("掃除機をかける"), buildChore("食器を洗う"));

        // When
        notificationService.notifyParentAllowanceRequested(child, request, chores);

        // Then
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("parent@example.com");
        assertThat(sent.getSubject()).contains("¥70");
        assertThat(sent.getText()).contains("¥70");
        assertThat(sent.getText()).contains("2件");
    }

    @Test
    void notifyParentAllowanceRequested_noParentEmail_skips() {
        // Given
        parent.setEmail(null);
        when(userRepository.findByRoleAndFamilyId(Role.PARENT, 10L)).thenReturn(List.of(parent));
        AllowanceRequest request = new AllowanceRequest();
        request.setTotalAmount(20);

        // When
        notificationService.notifyParentAllowanceRequested(child, request, List.of(buildChore("掃除")));

        // Then
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void notifyParentAllowanceRequested_mailException_doesNotPropagate() {
        // Given
        when(userRepository.findByRoleAndFamilyId(Role.PARENT, 10L)).thenReturn(List.of(parent));
        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));
        AllowanceRequest request = new AllowanceRequest();
        request.setTotalAmount(20);

        // When / Then
        assertThatCode(() -> notificationService.notifyParentAllowanceRequested(child, request, List.of(buildChore("掃除"))))
                .doesNotThrowAnyException();
    }

    private Chore buildChore(String content) {
        Chore chore = new Chore();
        chore.setContent(content);
        chore.setChoreDate(LocalDate.now());
        chore.setStatus(ChoreStatus.UNPAID);
        return chore;
    }
}

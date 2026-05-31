package design.hitsuji.otetsudai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * お手伝い単体登録フォームのDTO。
 * {@code POST /chores} で {@code @Valid} によるBean Validationが適用される。
 */
public class ChoreForm {

    @NotNull(message = "日付を入力してください")
    @PastOrPresent(message = "未来の日付は入力できません")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate choreDate;

    @NotBlank(message = "内容を入力してください")
    @Size(min = 1, max = 100, message = "内容は1〜100文字で入力してください")
    private String content;

    public LocalDate getChoreDate() { return choreDate; }
    public void setChoreDate(LocalDate choreDate) { this.choreDate = choreDate; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}

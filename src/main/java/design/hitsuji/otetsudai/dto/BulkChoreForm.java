package design.hitsuji.otetsudai.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * お手伝い一括登録フォームのDTO。
 *
 * <p>各行は {@link ChoreEntryForm} で表される。
 * content が空の行はサービス層でスキップするため、{@code @NotBlank} は付与しない。
 * バリデーション（未来日付・100文字超）はサービス層で実施する。
 */
public class BulkChoreForm {

    private List<ChoreEntryForm> entries = new ArrayList<>();

    public List<ChoreEntryForm> getEntries() { return entries; }
    public void setEntries(List<ChoreEntryForm> entries) { this.entries = entries; }

    /**
     * 一括登録フォームの1行分を表す内部クラス。
     * {@code choreDate} が {@code null} の場合は当日日付がデフォルト適用される。
     */
    public static class ChoreEntryForm {

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate choreDate;

        private String content;

        public LocalDate getChoreDate() { return choreDate; }
        public void setChoreDate(LocalDate choreDate) { this.choreDate = choreDate; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}

package design.hitsuji.otetsudai.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserRegistrationForm {

    @NotBlank(message = "ユーザーIDを入力してください")
    @Size(min = 3, max = 20, message = "ユーザーIDは3〜20文字で入力してください")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "ユーザーIDは英数字・アンダースコアのみ使用できます")
    private String userId;

    @NotBlank(message = "パスワードを入力してください")
    @Size(min = 8, max = 100, message = "パスワードは8文字以上で入力してください")
    private String password;

    @NotBlank(message = "パスワード確認を入力してください")
    private String passwordConfirm;

    @Email(message = "正しいメールアドレスの形式で入力してください")
    @Size(max = 200, message = "メールアドレスは200文字以内で入力してください")
    private String email;

    @Size(max = 50, message = "表示名は50文字以内で入力してください")
    private String displayName;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPasswordConfirm() { return passwordConfirm; }
    public void setPasswordConfirm(String passwordConfirm) { this.passwordConfirm = passwordConfirm; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}

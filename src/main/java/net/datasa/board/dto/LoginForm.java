package net.datasa.board.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** 로그인 입력 폼. (step3의 검증을 로그인에도 그대로 적용한다.) */
@Getter
@Setter
public class LoginForm {

    @NotBlank(message = "아이디를 입력하세요")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력하세요")
    private String password;
}

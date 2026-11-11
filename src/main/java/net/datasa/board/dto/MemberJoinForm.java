package net.datasa.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 회원가입 입력 폼. */
@Getter
@Setter
public class MemberJoinForm {

    @NotBlank(message = "아이디를 입력하세요")
    @Size(min = 3, max = 20, message = "아이디는 3~20자로 입력하세요")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력하세요")
    @Size(min = 4, message = "비밀번호는 4자 이상으로 입력하세요")
    private String password;

    @NotBlank(message = "이름을 입력하세요")
    private String name;
}

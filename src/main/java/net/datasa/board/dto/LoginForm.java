package net.datasa.board.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 로그인 입력 폼.
 */
@Getter
@Setter
public class LoginForm {

    private String loginId;
    private String password;
}

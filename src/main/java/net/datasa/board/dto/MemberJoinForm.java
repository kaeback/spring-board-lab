package net.datasa.board.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 회원가입 입력 폼.
 */
@Getter
@Setter
public class MemberJoinForm {

    private String loginId;
    private String password;
    private String name;
}

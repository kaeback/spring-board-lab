package net.datasa.board.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 회원가입 입력 폼.
 *
 * step2에서는 검증 애너테이션이 없다. (검증은 step3에서 추가한다.)
 */
@Getter
@Setter
public class MemberJoinForm {

    private String loginId;
    private String password;
    private String name;
}

package net.datasa.board.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * 회원 도메인 객체.
 */
@Getter
@Setter
public class Member {

    private String loginId;   // 로그인 아이디 (식별자)
    private String password;  // 비밀번호 (step2는 평문)
    private String name;      // 표시 이름
}

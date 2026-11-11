package net.datasa.board.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * 회원 도메인 객체 (메모리 보관용 POJO).
 *
 * step2에서는 비밀번호를 평문으로 저장한다. (암호화는 시큐리티 단계에서 다룬다.)
 */
@Getter
@Setter
public class Member {

    private String loginId;   // 로그인 아이디 (식별자)
    private String password;  // 비밀번호 (step2는 평문)
    private String name;      // 표시 이름
}

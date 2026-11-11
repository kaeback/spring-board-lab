package net.datasa.board.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

/**
 * 회원 엔티티.
 *
 * step4부터는 JPA로 DB에 저장한다. loginId는 로그인에 사용하는 자연키이지만,
 * 기본키(PK)는 별도의 자동 증가(IDENTITY) {@code id}로 둔다.
 */
@Entity
@Getter
@Setter
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String loginId;   // 로그인 아이디

    private String password;  // 비밀번호 (step9부터 BCrypt 암호화)

    private String name;      // 표시 이름

    private String role;      // 권한 (예: ROLE_USER)
}

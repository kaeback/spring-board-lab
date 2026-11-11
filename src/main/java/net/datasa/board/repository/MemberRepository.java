package net.datasa.board.repository;

import net.datasa.board.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 회원 JPA 저장소.
 *
 * {@code MemberMemoryRepository}를 대체한다. loginId로 조회하는 메서드는
 * 이름 규칙(쿼리 메서드)만으로 Spring Data가 구현체를 만들어 준다.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByLoginId(String loginId);
}

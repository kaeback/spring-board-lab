package net.datasa.board.repository;

import net.datasa.board.domain.Member;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 메모리 기반 회원 저장소. loginId를 키로 보관한다.
 */
@Repository
public class MemberMemoryRepository {

    private final Map<String, Member> store = new ConcurrentHashMap<>();

    public Member save(Member member) {
        store.put(member.getLoginId(), member);
        return member;
    }

    public Optional<Member> findByLoginId(String loginId) {
        return Optional.ofNullable(store.get(loginId));
    }
}

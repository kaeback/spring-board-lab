package net.datasa.board.service;

import lombok.RequiredArgsConstructor;
import net.datasa.board.domain.Member;
import net.datasa.board.dto.MemberJoinForm;
import net.datasa.board.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 회원 가입·로그인 검증 로직.
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public boolean isDuplicateLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId).isPresent();
    }

    public void join(MemberJoinForm form) {
        Member member = new Member();
        member.setLoginId(form.getLoginId());
        member.setPassword(form.getPassword());
        member.setName(form.getName());
        memberRepository.save(member);
    }

    /** 아이디·비밀번호가 맞으면 회원을, 아니면 빈 Optional을 반환한다. */
    public Optional<Member> login(String loginId, String password) {
        return memberRepository.findByLoginId(loginId)
                .filter(member -> member.getPassword().equals(password));
    }
}

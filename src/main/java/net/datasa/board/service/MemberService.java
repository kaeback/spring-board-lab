package net.datasa.board.service;

import lombok.RequiredArgsConstructor;
import net.datasa.board.domain.Member;
import net.datasa.board.dto.MemberJoinForm;
import net.datasa.board.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 회원 가입 로직.
 *
 * step9부터 로그인 검증은 Spring Security가 담당하므로 login() 메서드는 사라졌다.
 * 가입 시 비밀번호는 PasswordEncoder로 암호화해 저장한다.
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean isDuplicateLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId).isPresent();
    }

    public void join(MemberJoinForm form) {
        Member member = new Member();
        member.setLoginId(form.getLoginId());
        member.setPassword(passwordEncoder.encode(form.getPassword()));  // BCrypt 암호화
        member.setName(form.getName());
        member.setRole("ROLE_USER");
        memberRepository.save(member);
    }
}

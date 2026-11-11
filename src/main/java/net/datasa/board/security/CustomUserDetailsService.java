package net.datasa.board.security;

import lombok.RequiredArgsConstructor;
import net.datasa.board.domain.Member;
import net.datasa.board.repository.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 로그인 시 Spring Security가 호출하는 서비스.
 * loginId로 회원을 찾아 UserDetails로 돌려준다.
 * (비밀번호 비교·세션 저장은 Spring Security가 알아서 처리한다.)
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByLoginId(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자: " + username));
        return new CustomUserDetails(member);
    }
}

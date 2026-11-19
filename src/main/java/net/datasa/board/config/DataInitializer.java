package net.datasa.board.config;

import lombok.RequiredArgsConstructor;
import net.datasa.board.dto.MemberJoinForm;
import net.datasa.board.service.MemberService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 앱이 시작될 때 샘플 회원을 미리 넣어 둔다.
 * 메모리 저장소라 재시작하면 사라진다.
 *
 * 로그인 테스트 계정: hong / kim / admin  (비밀번호 모두 1234)
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberService memberService;

    @Override
    public void run(String... args) {
        member("hong", "1234", "홍길동");
        member("kim", "1234", "김철수");
        member("admin", "1234", "관리자");
    }

    private void member(String loginId, String password, String name) {
        MemberJoinForm form = new MemberJoinForm();
        form.setLoginId(loginId);
        form.setPassword(password);
        form.setName(name);
        memberService.join(form);
    }
}

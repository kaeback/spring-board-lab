package net.datasa.board.config;

import lombok.RequiredArgsConstructor;
import net.datasa.board.dto.BoardForm;
import net.datasa.board.dto.MemberJoinForm;
import net.datasa.board.service.BoardService;
import net.datasa.board.service.MemberService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 앱이 시작될 때 샘플 회원과 글을 미리 넣어 둔다.
 * 메모리 저장소라 재시작하면 사라진다.
 *
 * 로그인 테스트 계정: hong / kim / admin  (비밀번호 모두 1234)
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberService memberService;
    private final BoardService boardService;

    @Override
    public void run(String... args) {
        member("hong", "1234", "홍길동");
        member("kim", "1234", "김철수");
        member("admin", "1234", "관리자");

        write("첫 번째 글입니다", "메모리 게시판에 오신 것을 환영합니다.", "admin");
        write("두 번째 글입니다", "로그인 후 글쓰기 버튼으로 새 글을 작성해 보세요.", "hong");
        write("세 번째 글입니다", "본인 글에만 수정·삭제 버튼이 보입니다.", "kim");
    }

    private void member(String loginId, String password, String name) {
        MemberJoinForm form = new MemberJoinForm();
        form.setLoginId(loginId);
        form.setPassword(password);
        form.setName(name);
        memberService.join(form);
    }

    private void write(String title, String content, String writerLoginId) {
        BoardForm form = new BoardForm();
        form.setTitle(title);
        form.setContent(content);
        boardService.write(form, writerLoginId);
    }
}

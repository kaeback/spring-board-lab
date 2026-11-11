package net.datasa.board.config;

import lombok.RequiredArgsConstructor;
import net.datasa.board.dto.BoardForm;
import net.datasa.board.dto.MemberJoinForm;
import net.datasa.board.domain.Board;
import net.datasa.board.repository.BoardRepository;
import net.datasa.board.repository.CommentRepository;
import net.datasa.board.repository.MemberRepository;
import net.datasa.board.service.BoardService;
import net.datasa.board.service.CommentService;
import net.datasa.board.service.MemberService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 앱이 시작될 때 샘플 회원과 글을 미리 넣어 둔다.
 *
 * 이미 데이터가 있으면 다시 넣지 않는다(중복 방지). 글은 페이징 효과를 볼 수 있도록 넉넉히 시드한다.
 *
 * 로그인 테스트 계정: hong / kim / admin  (비밀번호 모두 1234, admin만 ROLE_ADMIN)
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final MemberService memberService;
    private final BoardService boardService;
    private final CommentRepository commentRepository;
    private final CommentService commentService;

    @Override
    public void run(String... args) {
        if (memberRepository.count() == 0) {
            member("hong", "1234", "홍길동");
            member("kim", "1234", "김철수");
            member("admin", "1234", "관리자");
            // 가입은 누구나 ROLE_USER로 되므로, 관리자 계정은 여기서 권한을 바꿔 둔다
            memberRepository.findByLoginId("admin").ifPresent(admin -> {
                admin.setRole("ROLE_ADMIN");
                memberRepository.save(admin);
            });
        }

        if (boardRepository.count() == 0) {
            // 페이징을 확인할 수 있도록 글을 넉넉히 시드한다
            for (int i = 1; i <= 12; i++) {
                String writer = switch (i % 3) {
                    case 0 -> "admin";
                    case 1 -> "hong";
                    default -> "kim";
                };
                write(i + "번째 글입니다", "본문 내용 " + i + " 입니다.", writer);
            }
        }

        // step7: 가장 최근 글에 샘플 댓글 두 개
        if (commentRepository.count() == 0) {
            boardRepository.findTopByOrderByIdDesc().ifPresent((Board board) -> {
                commentService.write(board.getId(), "첫 댓글입니다.", "kim");
                commentService.write(board.getId(), "잘 읽었습니다.", "admin");
            });
        }
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
        boardService.write(form, writerLoginId, null);   // 시드 데이터는 첨부 없음
    }
}

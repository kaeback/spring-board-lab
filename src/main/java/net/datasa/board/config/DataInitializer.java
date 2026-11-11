package net.datasa.board.config;

import lombok.RequiredArgsConstructor;
import net.datasa.board.dto.BoardForm;
import net.datasa.board.service.BoardService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 앱이 시작될 때 샘플 글 몇 개를 미리 넣어 둔다.
 * 메모리 저장소라 재시작하면 사라지므로, 화면을 바로 확인할 수 있도록 돕는 용도다.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BoardService boardService;

    @Override
    public void run(String... args) {
        write("첫 번째 글입니다", "메모리 게시판에 오신 것을 환영합니다.", "관리자");
        write("두 번째 글입니다", "글쓰기 버튼으로 새 글을 작성해 보세요.", "홍길동");
        write("세 번째 글입니다", "상세 화면에서 수정·삭제도 할 수 있습니다.", "김철수");
    }

    private void write(String title, String content, String writer) {
        BoardForm form = new BoardForm();
        form.setTitle(title);
        form.setContent(content);
        form.setWriter(writer);
        boardService.write(form);
    }
}

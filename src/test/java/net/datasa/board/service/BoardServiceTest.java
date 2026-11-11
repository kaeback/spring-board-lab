package net.datasa.board.service;

import net.datasa.board.domain.Board;
import net.datasa.board.domain.Member;
import net.datasa.board.dto.BoardForm;
import net.datasa.board.exception.BoardNotFoundException;
import net.datasa.board.repository.BoardRepository;
import net.datasa.board.repository.CommentRepository;
import net.datasa.board.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 단위 테스트 — 스프링 컨테이너 없이 BoardService 하나만 검증한다.
 *
 * 의존 객체(BoardRepository, MemberRepository, FileService 등)는 Mockito 목(mock)으로 바꿔 끼운다.
 * BoardService가 생성자 주입(@RequiredArgsConstructor)이라 @InjectMocks가 목을 생성자로 주입한다.
 * DB도 톰캣도 띄우지 않으므로 수십 밀리초 안에 끝난다.
 */
@ExtendWith(MockitoExtension.class)   // JUnit(Jupiter)에서 @Mock/@InjectMocks 활성화
class BoardServiceTest {

    @Mock BoardRepository boardRepository;
    @Mock MemberRepository memberRepository;     // 글쓰기 때 loginId로 작성자(회원)를 찾는다
    @Mock FileService fileService;
    @Mock CommentRepository commentRepository;   // step7에서 생긴 의존성
    @InjectMocks BoardService boardService;

    @Test
    @DisplayName("글을 쓰면 작성자·조회수·작성시각이 채워져 저장된다")
    void write() {
        // given
        BoardForm form = new BoardForm();
        form.setTitle("제목");
        form.setContent("내용");
        when(memberRepository.findByLoginId("hong")).thenReturn(Optional.of(member("hong")));
        when(boardRepository.save(any(Board.class))).thenAnswer(inv -> inv.getArgument(0)); // 받은 객체 그대로 반환

        // when
        Board saved = boardService.write(form, "hong", null);   // 첨부파일 없음

        // then
        assertThat(saved.getTitle()).isEqualTo("제목");
        assertThat(saved.getWriter().getLoginId()).isEqualTo("hong");   // 작성자는 회원 엔티티
        assertThat(saved.getViewCount()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.isHasAttachment()).isFalse();
        verify(boardRepository).save(any(Board.class));          // 저장이 실제로 호출됐는지
    }

    @Test
    @DisplayName("상세 조회 시 조회수가 1 증가한다")
    void increaseView() {
        Board board = new Board();
        board.setViewCount(3);
        when(boardRepository.findWithWriterById(1L)).thenReturn(Optional.of(board));

        Board result = boardService.getDetailAndIncreaseView(1L);

        assertThat(result.getViewCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("없는 글을 조회하면 BoardNotFoundException(→ 404)이 발생한다")
    void notFound() {
        when(boardRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.getById(99L))
                .isInstanceOf(BoardNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("작성자 본인인지 확인한다 (없는 글이면 false)")
    void isOwner() {
        // 1번 글의 작성자는 hong. 나머지 조합(남의 글, 없는 글)은 쿼리 결과가 false다.
        when(boardRepository.existsByIdAndWriterLoginId(1L, "hong")).thenReturn(true);
        when(boardRepository.existsByIdAndWriterLoginId(1L, "kim")).thenReturn(false);
        when(boardRepository.existsByIdAndWriterLoginId(2L, "hong")).thenReturn(false);

        assertThat(boardService.isOwner(1L, "hong")).isTrue();
        assertThat(boardService.isOwner(1L, "kim")).isFalse();
        assertThat(boardService.isOwner(2L, "hong")).isFalse();
    }

    @Test
    @DisplayName("없는 글을 삭제하면 파일 삭제는 호출되지 않는다")
    void deleteMissing() {
        when(boardRepository.findById(5L)).thenReturn(Optional.empty());

        boardService.delete(5L);

        verify(fileService, never()).delete(any());
        verify(boardRepository).deleteById(5L);
    }

    private Member member(String loginId) {
        Member m = new Member();
        m.setLoginId(loginId);
        m.setName(loginId + " 이름");
        return m;
    }
}

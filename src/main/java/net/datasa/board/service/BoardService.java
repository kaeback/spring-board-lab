package net.datasa.board.service;

import lombok.RequiredArgsConstructor;
import net.datasa.board.domain.Board;
import net.datasa.board.dto.BoardForm;
import net.datasa.board.repository.BoardMemoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 비즈니스 로직.
 *
 * 컨트롤러와 저장소 사이에서 "글 작성·조회·수정·삭제" 흐름을 담당한다.
 */
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardMemoryRepository boardRepository;

    /** 전체 글 목록 (최신순) */
    public List<Board> getList() {
        return boardRepository.findAll();
    }

    /** 글 작성 — 작성자(writer)는 로그인한 사용자의 loginId를 받는다 */
    public Board write(BoardForm form, String writer) {
        Board board = new Board();
        board.setTitle(form.getTitle());
        board.setContent(form.getContent());
        board.setWriter(writer);
        board.setViewCount(0);
        board.setCreatedAt(LocalDateTime.now());
        return boardRepository.save(board);
    }

    /** 글 단건 조회 (조회수 증가) — 상세 화면용 */
    public Board getDetailAndIncreaseView(Long id) {
        Board board = getById(id);
        board.setViewCount(board.getViewCount() + 1);
        return board;
    }

    /** 글 단건 조회 (조회수 증가 없음) — 수정 화면 등 내부용 */
    public Board getById(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 글입니다. id=" + id));
    }

    /** 글 수정 — 제목·내용만 바꾸고 작성자는 유지한다 */
    public void update(Long id, BoardForm form) {
        Board board = getById(id);
        board.setTitle(form.getTitle());
        board.setContent(form.getContent());
    }

    /** 글 삭제 */
    public void delete(Long id) {
        boardRepository.deleteById(id);
    }

    /** 글의 작성자가 주어진 loginId와 같은지 확인 (수정·삭제 권한 검사용) */
    public boolean isOwner(Long id, String loginId) {
        return boardRepository.findById(id)
                .map(board -> board.getWriter().equals(loginId))
                .orElse(false);
    }
}

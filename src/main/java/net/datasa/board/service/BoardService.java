package net.datasa.board.service;

import lombok.RequiredArgsConstructor;
import net.datasa.board.domain.Board;
import net.datasa.board.domain.Member;
import net.datasa.board.dto.BoardForm;
import net.datasa.board.exception.BoardNotFoundException;
import net.datasa.board.repository.BoardRepository;
import net.datasa.board.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;

    /** 전체 글 목록 (최신순) — 화면에 작성자 이름을 쓰므로 작성자를 함께 조회한다 */
    public List<Board> getList() {
        return boardRepository.findAllByOrderByIdDesc();
    }

    /**
     * 글 작성 — 로그인한 사용자의 loginId로 회원 엔티티를 찾아 작성자로 연결한다.
     * (Board.writer는 문자열이 아니라 Member 참조다.)
     */
    @Transactional
    public Board write(BoardForm form, String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다. loginId=" + loginId));
        Board board = new Board();
        board.setTitle(form.getTitle());
        board.setContent(form.getContent());
        board.setWriter(member);
        board.setViewCount(0);
        board.setCreatedAt(LocalDateTime.now());
        return boardRepository.save(board);
    }

    /**
     * 글 단건 조회 (조회수 증가) — 상세 화면용.
     *
     * JPA는 트랜잭션 안에서만 변경 감지(dirty checking)로 UPDATE를 반영한다.
     * 메모리 저장소였을 때는 객체 참조를 공유해 별도 save 없이도 반영됐지만,
     * JPA에서는 {@code @Transactional}이 없으면 조회수 증가가 DB에 반영되지 않는다.
     * 화면에 작성자 이름을 쓰므로 작성자를 함께 조회한다(findWithWriterById).
     */
    @Transactional
    public Board getDetailAndIncreaseView(Long id) {
        Board board = boardRepository.findWithWriterById(id)
                .orElseThrow(() -> new BoardNotFoundException(id));   // step5: 404로 응답
        board.setViewCount(board.getViewCount() + 1);
        return board;
    }

    /** 글 단건 조회 (조회수 증가 없음) — 수정 화면 등 내부용 (제목·내용만 쓰므로 작성자는 조회하지 않는다) */
    public Board getById(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new BoardNotFoundException(id));   // step5: 404로 응답
    }

    /** 글 수정 — 제목·내용만 바꾸고 작성자는 유지한다 (변경 감지로 UPDATE) */
    @Transactional
    public void update(Long id, BoardForm form) {
        Board board = getById(id);
        board.setTitle(form.getTitle());
        board.setContent(form.getContent());
    }

    /** 글 삭제 */
    public void delete(Long id) {
        boardRepository.deleteById(id);
    }

    /**
     * 글의 작성자가 주어진 loginId와 같은지 확인 (수정·삭제 권한 검사용).
     * 글을 꺼내 board.getWriter()로 LAZY 접근하지 않고, 쿼리 메서드 하나로 바로 확인한다.
     */
    public boolean isOwner(Long id, String loginId) {
        return boardRepository.existsByIdAndWriterLoginId(id, loginId);
    }
}

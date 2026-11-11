package net.datasa.board.service;

import lombok.RequiredArgsConstructor;
import net.datasa.board.domain.Board;
import net.datasa.board.domain.Member;
import net.datasa.board.dto.BoardForm;
import net.datasa.board.exception.BoardNotFoundException;
import net.datasa.board.repository.BoardRepository;
import net.datasa.board.repository.CommentRepository;
import net.datasa.board.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

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
    private final FileService fileService;
    private final CommentRepository commentRepository;   // step7: 글 삭제 시 댓글도 삭제

    /**
     * 페이징된 글 목록.
     *
     * 정렬·페이지 번호·크기는 컨트롤러가 넘겨주는 {@link Pageable}에 실려 온다.
     * step5까지는 글 전체를 한 번에 가져왔지만({@code findAllByOrderByIdDesc()}), 이제는 {@code findAll(Pageable)}이 필요한 페이지만 가져온다.
     * 화면에 작성자 이름을 쓰므로 BoardRepository의 findAll(Pageable)에 @EntityGraph를 붙여 작성자를 함께 조회한다.
     */
    public Page<Board> getList(Pageable pageable) {
        return boardRepository.findAll(pageable);
    }

    /**
     * 글 작성 (첨부파일 포함) — 로그인한 사용자의 loginId로 회원 엔티티를 찾아 작성자로 연결한다.
     * (Board.writer는 문자열이 아니라 Member 참조다.)
     */
    @Transactional
    public Board write(BoardForm form, String loginId, MultipartFile file) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다. loginId=" + loginId));
        Board board = new Board();
        board.setTitle(form.getTitle());
        board.setContent(form.getContent());
        board.setWriter(member);
        board.setViewCount(0);
        board.setCreatedAt(LocalDateTime.now());
        applyFile(board, file);
        return boardRepository.save(board);
    }

    /**
     * 글 단건 조회 (조회수 증가) — 상세 화면용.
     * JPA는 트랜잭션 안에서만 변경 감지(dirty checking)로 UPDATE를 반영하므로 {@code @Transactional}이 필요하다.
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

    /** 글 수정 — 제목·내용, 그리고 새 파일이 올라오면 첨부 교체 (변경 감지로 UPDATE) */
    @Transactional
    public void update(Long id, BoardForm form, MultipartFile file) {
        Board board = getById(id);
        board.setTitle(form.getTitle());
        board.setContent(form.getContent());
        if (file != null && !file.isEmpty()) {
            fileService.delete(board.getStoredFileName());   // 기존 파일 제거
            applyFile(board, file);
        }
    }

    /** 글 삭제 (첨부파일·댓글도 함께 삭제) */
    @Transactional
    public void delete(Long id) {
        boardRepository.findById(id).ifPresent(board -> fileService.delete(board.getStoredFileName()));
        commentRepository.deleteByBoardId(id);      // step7: 댓글이 글을 참조하므로 먼저 지운다
        boardRepository.deleteById(id);
    }

    /**
     * 글의 작성자가 주어진 loginId와 같은지 확인 (수정·삭제 권한 검사용).
     * 글을 꺼내 board.getWriter()로 LAZY 접근하지 않고, 쿼리 메서드 하나로 바로 확인한다.
     */
    public boolean isOwner(Long id, String loginId) {
        return boardRepository.existsByIdAndWriterLoginId(id, loginId);
    }

    /** 업로드 파일을 저장하고 게시글에 파일명 정보를 채운다. */
    private void applyFile(Board board, MultipartFile file) {
        String stored = fileService.store(file);
        if (stored != null) {
            board.setOriginalFileName(file.getOriginalFilename());
            board.setStoredFileName(stored);
        }
    }
}

package net.datasa.board.service;

import lombok.RequiredArgsConstructor;
import net.datasa.board.domain.Board;
import net.datasa.board.dto.BoardForm;
import net.datasa.board.exception.BoardNotFoundException;
import net.datasa.board.repository.BoardRepository;
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
    private final FileService fileService;

    /**
     * 페이징된 글 목록.
     *
     * 정렬·페이지 번호·크기는 컨트롤러가 넘겨주는 {@link Pageable}에 실려 온다.
     * step5까지는 글 전체를 한 번에 가져왔지만({@code findAll(Sort)}), 이제는 {@code findAll(Pageable)}이 필요한 페이지만 가져온다.
     */
    public Page<Board> getList(Pageable pageable) {
        return boardRepository.findAll(pageable);
    }

    /** 글 작성 (첨부파일 포함) — 작성자(writer)는 로그인한 사용자의 loginId를 받는다 */
    public Board write(BoardForm form, String writer, MultipartFile file) {
        Board board = new Board();
        board.setTitle(form.getTitle());
        board.setContent(form.getContent());
        board.setWriter(writer);
        board.setViewCount(0);
        board.setCreatedAt(LocalDateTime.now());
        applyFile(board, file);
        return boardRepository.save(board);
    }

    /**
     * 글 단건 조회 (조회수 증가) — 상세 화면용.
     * JPA는 트랜잭션 안에서만 변경 감지(dirty checking)로 UPDATE를 반영하므로 {@code @Transactional}이 필요하다.
     */
    @Transactional
    public Board getDetailAndIncreaseView(Long id) {
        Board board = getById(id);
        board.setViewCount(board.getViewCount() + 1);
        return board;
    }

    /** 글 단건 조회 (조회수 증가 없음) — 수정 화면 등 내부용 */
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

    /** 글 삭제 (첨부파일도 함께 삭제) */
    public void delete(Long id) {
        boardRepository.findById(id).ifPresent(board -> fileService.delete(board.getStoredFileName()));
        boardRepository.deleteById(id);
    }

    /** 글의 작성자가 주어진 loginId와 같은지 확인 (수정·삭제 권한 검사용) */
    public boolean isOwner(Long id, String loginId) {
        return boardRepository.findById(id)
                .map(board -> board.getWriter().equals(loginId))
                .orElse(false);
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

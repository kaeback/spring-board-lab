package net.datasa.board.service;

import lombok.RequiredArgsConstructor;
import net.datasa.board.domain.Board;
import net.datasa.board.domain.Comment;
import net.datasa.board.domain.Member;
import net.datasa.board.dto.CommentResponse;
import net.datasa.board.exception.CommentNotFoundException;
import net.datasa.board.repository.CommentRepository;
import net.datasa.board.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 댓글 비즈니스 로직.
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardService boardService;
    private final MemberRepository memberRepository;

    /** 글의 댓글 목록 (글이 없으면 BoardNotFoundException → 404) */
    public List<CommentResponse> getList(Long boardId) {
        boardService.getById(boardId);
        return commentRepository.findByBoardIdOrderByIdAsc(boardId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    /** 댓글 등록 — 로그인한 사용자의 loginId로 회원 엔티티를 찾아 작성자로 연결한다 */
    @Transactional
    public CommentResponse write(Long boardId, String content, String loginId) {
        Board board = boardService.getById(boardId);
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다. loginId=" + loginId));
        Comment comment = new Comment();
        comment.setBoard(board);
        comment.setContent(content);
        comment.setWriter(member);
        comment.setCreatedAt(LocalDateTime.now());
        return CommentResponse.from(commentRepository.save(comment));
    }

    /**
     * 댓글 작성자가 주어진 loginId와 같은지 (없는 댓글이면 CommentNotFoundException → 404).
     * comment.getWriter()로 LAZY 접근하지 않고 쿼리 메서드로 확인한다.
     */
    public boolean isOwner(Long commentId, String loginId) {
        if (!commentRepository.existsById(commentId)) {
            throw new CommentNotFoundException(commentId);
        }
        return commentRepository.existsByIdAndWriterLoginId(commentId, loginId);
    }

    /** 댓글 삭제 */
    public void delete(Long commentId) {
        commentRepository.deleteById(commentId);
    }
}

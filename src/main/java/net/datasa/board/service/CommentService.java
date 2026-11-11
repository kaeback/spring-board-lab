package net.datasa.board.service;

import lombok.RequiredArgsConstructor;
import net.datasa.board.domain.Board;
import net.datasa.board.domain.Comment;
import net.datasa.board.dto.CommentResponse;
import net.datasa.board.exception.CommentNotFoundException;
import net.datasa.board.repository.CommentRepository;
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

    /** 글의 댓글 목록 (글이 없으면 BoardNotFoundException → 404) */
    public List<CommentResponse> getList(Long boardId) {
        boardService.getById(boardId);
        return commentRepository.findByBoardIdOrderByIdAsc(boardId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    /** 댓글 등록 */
    @Transactional
    public CommentResponse write(Long boardId, String content, String writer) {
        Board board = boardService.getById(boardId);
        Comment comment = new Comment();
        comment.setBoard(board);
        comment.setContent(content);
        comment.setWriter(writer);
        comment.setCreatedAt(LocalDateTime.now());
        return CommentResponse.from(commentRepository.save(comment));
    }

    /** 댓글 작성자가 주어진 loginId와 같은지 (없는 댓글이면 CommentNotFoundException → 404) */
    public boolean isOwner(Long commentId, String loginId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        return comment.getWriter().equals(loginId);
    }

    /** 댓글 삭제 */
    public void delete(Long commentId) {
        commentRepository.deleteById(commentId);
    }
}

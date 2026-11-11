package net.datasa.board.repository;

import net.datasa.board.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 댓글 JPA 저장소.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** 글 하나의 댓글을 작성 순서대로 */
    List<Comment> findByBoardIdOrderByIdAsc(Long boardId);

    /** 글을 지울 때 그 글의 댓글도 함께 지운다 */
    void deleteByBoardId(Long boardId);
}

package net.datasa.board.repository;

import net.datasa.board.domain.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 댓글 JPA 저장소.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 글 하나의 댓글을 작성 순서대로.
     * 응답 DTO가 작성자 이름(writer.name)을 쓰므로 @EntityGraph로 작성자를 함께 조회한다.
     * (없으면 댓글마다 회원 SELECT가 한 번씩 더 나가거나, 트랜잭션 밖이면 LazyInitializationException)
     */
    @EntityGraph(attributePaths = "writer")
    List<Comment> findByBoardIdOrderByIdAsc(Long boardId);

    /** 작성자 확인 — 댓글 id와 작성자 loginId가 모두 맞는 댓글이 있는지 (LAZY 접근 없음) */
    boolean existsByIdAndWriterLoginId(Long id, String loginId);

    /** 글을 지울 때 그 글의 댓글도 함께 지운다 */
    void deleteByBoardId(Long boardId);
}

package net.datasa.board.dto;

import net.datasa.board.domain.Comment;

import java.time.format.DateTimeFormatter;

/**
 * 댓글 응답(JSON). 엔티티 대신 화면에 필요한 값만 내보낸다.
 * <pre>{"id":1,"writer":"hong","writerName":"홍길동","content":"좋은 글이네요","createdAt":"2026-11-11 10:30"}</pre>
 * writer는 작성자 loginId(화면 JS가 본인 댓글인지 판별할 때 사용), writerName은 화면에 보여 줄 이름이다.
 */
public record CommentResponse(Long id, String writer, String writerName, String content, String createdAt) {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(),
                comment.getWriter().getLoginId(), comment.getWriter().getName(), comment.getContent(),
                comment.getCreatedAt().format(FORMAT));
    }
}

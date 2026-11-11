package net.datasa.board.dto;

import net.datasa.board.domain.Comment;

import java.time.format.DateTimeFormatter;

/**
 * 댓글 응답(JSON). 엔티티 대신 화면에 필요한 값만 내보낸다.
 * <pre>{"id":1,"writer":"hong","content":"좋은 글이네요","createdAt":"2026-11-11 10:30"}</pre>
 */
public record CommentResponse(Long id, String writer, String content, String createdAt) {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getWriter(), comment.getContent(),
                comment.getCreatedAt().format(FORMAT));
    }
}

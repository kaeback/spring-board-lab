package net.datasa.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 댓글 등록 요청 본문(JSON).
 * <pre>{"content": "좋은 글이네요"}</pre>
 * record라서 getter 대신 {@code content()}로 꺼낸다. JSON → record 변환은 Jackson이 해 준다.
 */
public record CommentRequest(
        @NotBlank(message = "댓글 내용을 입력하세요.")
        @Size(max = 300, message = "댓글은 300자 이하로 입력하세요.")
        String content) {
}

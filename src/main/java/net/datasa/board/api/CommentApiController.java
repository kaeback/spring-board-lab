package net.datasa.board.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.datasa.board.dto.CommentRequest;
import net.datasa.board.dto.CommentResponse;
import net.datasa.board.dto.ErrorResult;
import net.datasa.board.security.CustomUserDetails;
import net.datasa.board.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * 댓글 REST API. 화면(HTML) 대신 JSON을 주고받는다.
 *
 * <pre>
 * GET    /api/boards/{boardId}/comments   댓글 목록          200
 * POST   /api/boards/{boardId}/comments   댓글 등록 (로그인)  201 / 400 / 401(시큐리티)
 * DELETE /api/comments/{commentId}        댓글 삭제 (본인)    204 / 401(시큐리티) / 403 / 404
 * </pre>
 *
 * step9: 로그인 확인은 Spring Security가 한다. 로그인하지 않고 등록·삭제를 부르면
 * SecurityConfig의 인증 진입점이 401 JSON으로 응답하므로, 여기서는 로그인한 사용자만 들어온다.
 */
@RestController
@RequiredArgsConstructor
public class CommentApiController {

    private final CommentService commentService;

    @GetMapping("/api/boards/{boardId}/comments")
    public List<CommentResponse> list(@PathVariable Long boardId) {
        return commentService.getList(boardId);                 // List → JSON 배열
    }

    @PostMapping("/api/boards/{boardId}/comments")
    public ResponseEntity<?> write(@PathVariable Long boardId,
                                   @Valid @RequestBody CommentRequest request,   // JSON 본문 → 객체
                                   @AuthenticationPrincipal CustomUserDetails user) {      // 로그인한 사용자
        CommentResponse saved = commentService.write(boardId, request.content(), user.getUsername());
        // 201 Created + Location 헤더(새로 만든 자원의 주소)
        return ResponseEntity.created(URI.create("/api/comments/" + saved.id())).body(saved);
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<?> delete(@PathVariable Long commentId,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        if (!commentService.isOwner(commentId, user.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResult("FORBIDDEN", "본인이 쓴 댓글만 삭제할 수 있습니다."));
        }
        commentService.delete(commentId);
        return ResponseEntity.noContent().build();               // 204 No Content (본문 없음)
    }
}

package net.datasa.board.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.datasa.board.domain.Member;
import net.datasa.board.dto.CommentRequest;
import net.datasa.board.dto.CommentResponse;
import net.datasa.board.dto.ErrorResult;
import net.datasa.board.service.CommentService;
import net.datasa.board.web.SessionConst;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.net.URI;
import java.util.List;

/**
 * 댓글 REST API. 화면(HTML) 대신 JSON을 주고받는다.
 *
 * <pre>
 * GET    /api/boards/{boardId}/comments   댓글 목록          200
 * POST   /api/boards/{boardId}/comments   댓글 등록 (로그인)  201 / 400 / 401
 * DELETE /api/comments/{commentId}        댓글 삭제 (본인)    204 / 401 / 403 / 404
 * </pre>
 *
 * 로그인 확인은 인터셉터가 아니라 여기서 한다. 인터셉터는 로그인 화면으로 redirect하는데,
 * API를 부르는 자바스크립트에게 필요한 것은 HTML 로그인 화면이 아니라 401 상태 코드다.
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
                                   @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) Member login) {
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResult("UNAUTHORIZED", "로그인이 필요합니다."));
        }
        CommentResponse saved = commentService.write(boardId, request.content(), login.getLoginId());
        // 201 Created + Location 헤더(새로 만든 자원의 주소)
        return ResponseEntity.created(URI.create("/api/comments/" + saved.id())).body(saved);
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<?> delete(@PathVariable Long commentId,
                                    @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) Member login) {
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResult("UNAUTHORIZED", "로그인이 필요합니다."));
        }
        if (!commentService.isOwner(commentId, login.getLoginId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResult("FORBIDDEN", "본인이 쓴 댓글만 삭제할 수 있습니다."));
        }
        commentService.delete(commentId);
        return ResponseEntity.noContent().build();               // 204 No Content (본문 없음)
    }
}

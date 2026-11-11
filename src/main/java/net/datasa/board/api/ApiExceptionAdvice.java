package net.datasa.board.api;

import net.datasa.board.dto.ErrorResult;
import net.datasa.board.exception.BoardNotFoundException;
import net.datasa.board.exception.CommentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * API(JSON) 전용 예외 처리. {@code api} 패키지의 컨트롤러에만 적용된다.
 * 화면 컨트롤러의 예외는 그대로 오류 페이지(error/404.html 등)로 간다.
 */
@RestControllerAdvice(basePackages = "net.datasa.board.api")
public class ApiExceptionAdvice {

    /** 없는 글·댓글 → 404 */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({BoardNotFoundException.class, CommentNotFoundException.class})
    public ErrorResult notFound(RuntimeException e) {
        return new ErrorResult("NOT_FOUND", e.getMessage());
    }

    /** @Valid 검증 실패 → 400 (첫 번째 오류 메시지만) */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResult invalid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        return new ErrorResult("VALIDATION", message);
    }
}

package net.datasa.board.exception;

/**
 * 요청한 댓글이 없을 때 던지는 예외. API 예외 처리기({@code ApiExceptionAdvice})가 404 JSON으로 바꾼다.
 */
public class CommentNotFoundException extends RuntimeException {

    public CommentNotFoundException(Long id) {
        super("존재하지 않는 댓글입니다. id=" + id);
    }
}

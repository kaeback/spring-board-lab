package net.datasa.board.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 요청한 글이 없을 때 던지는 예외.
 *
 * {@code @ResponseStatus(NOT_FOUND)}가 붙어 있으므로 컨트롤러 밖으로 던져지면
 * 스프링의 {@code ResponseStatusExceptionResolver}가 <b>404</b> 응답으로 바꾼다.
 * 그러면 스프링 부트의 {@code BasicErrorController}가 {@code templates/error/404.html}을 보여 준다.
 * (예전처럼 IllegalArgumentException을 그대로 던지면 사용자는 500 오류 화면을 보게 된다.)
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class BoardNotFoundException extends RuntimeException {

    public BoardNotFoundException(Long id) {
        super("존재하지 않는 글입니다. id=" + id);
    }
}

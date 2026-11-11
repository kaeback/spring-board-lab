package net.datasa.board.dto;

/**
 * API 오류 응답(JSON).
 * <pre>{"code":"UNAUTHORIZED","message":"로그인이 필요합니다."}</pre>
 */
public record ErrorResult(String code, String message) {
}

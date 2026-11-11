package net.datasa.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 글쓰기·수정 화면 입력 폼.
 *
 * step2부터는 작성자를 폼에서 받지 않는다. (로그인한 세션 사용자가 곧 작성자다.)
 * step3부터 검증 애너테이션이 붙는다.
 */
@Getter
@Setter
public class BoardForm {

    @NotBlank(message = "제목을 입력하세요")
    @Size(max = 100, message = "제목은 100자 이하로 입력하세요")
    private String title;

    @NotBlank(message = "내용을 입력하세요")
    private String content;
}

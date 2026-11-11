package net.datasa.board.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 글쓰기·수정 화면 입력 폼.
 *
 * step2부터는 작성자를 폼에서 받지 않는다. (로그인한 세션 사용자가 곧 작성자다.)
 * 검증 애너테이션은 아직 없다. (검증은 step3에서 추가한다.)
 */
@Getter
@Setter
public class BoardForm {

    private String title;
    private String content;
}

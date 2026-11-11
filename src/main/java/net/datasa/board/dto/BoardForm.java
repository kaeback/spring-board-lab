package net.datasa.board.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 글쓰기·수정 화면에서 넘어오는 입력 값을 담는 폼 객체.
 *
 * step1에서는 검증 애너테이션이 없다. (검증은 step3에서 추가한다.)
 */
@Getter
@Setter
public class BoardForm {

    private String title;
    private String content;
    private String writer;
}

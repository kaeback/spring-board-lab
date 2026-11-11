package net.datasa.board.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 게시글 도메인 객체.
 */
@Getter
@Setter
public class Board {

    private Long id;             // 저장소가 부여하는 식별자
    private String title;        // 제목
    private String content;      // 내용
    private String writer;       // 작성자 (로그인한 회원의 loginId — 컨트롤러가 세션에서 꺼내 넘긴다)
    private int viewCount;       // 조회수
    private LocalDateTime createdAt;  // 작성 시각
}

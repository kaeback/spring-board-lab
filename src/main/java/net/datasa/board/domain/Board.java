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
    private String writer;       // 작성자 (step1은 로그인이 없어 폼에서 직접 입력받는다)
    private int viewCount;       // 조회수
    private LocalDateTime createdAt;  // 작성 시각
}

package net.datasa.board.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 게시글 도메인 객체.
 *
 * step1에서는 DB(JPA 엔티티)가 아니라 메모리에 보관하는 평범한 자바 객체(POJO)다.
 * @Entity 같은 애너테이션이 전혀 없다는 점에 주목하자.
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

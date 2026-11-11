package net.datasa.board.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 댓글 엔티티. 글 하나에 댓글 여러 개 (N:1).
 *
 * 화면이 아니라 REST API(JSON)로 주고받으므로, 이 엔티티를 그대로 응답하지 않고
 * {@code CommentResponse} DTO로 바꿔서 내보낸다.
 */
@Entity
@Getter
@Setter
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)      // 댓글 → 글 (연관관계의 주인)
    @JoinColumn(name = "board_id")
    private Board board;

    private String writer;                  // 작성자 (로그인한 회원의 loginId)

    @Column(length = 300)                   // CommentRequest의 @Size(max = 300)와 맞춘다 (기본 255)
    private String content;                 // 내용

    private LocalDateTime createdAt;        // 작성 시각
}

package net.datasa.board.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 게시글 엔티티.
 *
 * step4부터는 메모리 대신 JPA로 DB(PostgreSQL)에 저장한다.
 * {@code @Entity}가 붙으면 이 클래스가 테이블과 매핑된다.
 *
 * 작성자는 loginId 문자열이 아니라 {@link Member} 엔티티를 참조한다(다대일, 단방향).
 * 글 여러 개가 회원 한 명을 가리키므로 {@code @ManyToOne}이고, board 테이블에 외래 키 writer_id가 생긴다.
 * LAZY라서 작성자는 실제로 접근할 때 조회된다. (화면용 조회는 BoardRepository의 @EntityGraph 참고)
 */
@Entity
@Getter
@Setter
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;             // 저장소가 부여하는 식별자 (DB IDENTITY 컬럼이 자동 증가)

    private String title;        // 제목

    @Column(columnDefinition = "TEXT")
    private String content;      // 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", nullable = false)
    private Member writer;       // 작성자 (회원 엔티티 참조 — 외래 키 writer_id)

    private int viewCount;       // 조회수

    private LocalDateTime createdAt;  // 작성 시각

    // step6: 첨부파일 (1개)
    private String originalFileName;  // 사용자가 올린 원래 파일명 (화면 표시용)
    private String storedFileName;    // 서버에 저장된 실제 파일명 (UUID, 중복 방지)

    /** 첨부파일이 있는지 여부 (Thymeleaf의 board.hasAttachment). DB 컬럼이 아니므로 @Transient. */
    @Transient
    public boolean isHasAttachment() {
        return storedFileName != null;
    }
}

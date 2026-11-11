package net.datasa.board.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 게시글 엔티티.
 *
 * step4부터는 메모리 대신 JPA로 DB(PostgreSQL)에 저장한다.
 * {@code @Entity}가 붙으면 이 클래스가 테이블과 매핑된다.
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

    private String writer;       // 작성자 (로그인한 회원의 loginId)

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

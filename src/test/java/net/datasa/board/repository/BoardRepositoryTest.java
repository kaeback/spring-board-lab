package net.datasa.board.repository;

import net.datasa.board.domain.Board;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;   // Boot 4 패키지
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 슬라이스 테스트 — JPA 관련 빈(엔티티·리포지토리·EntityManager)만 띄운다.
 *
 * {@code @DataJpaTest}는 기본적으로 실제 DB 대신 <b>내장 DB(H2)</b>로 바꿔 실행하고,
 * 테스트마다 트랜잭션을 롤백한다. 그래서 PostgreSQL이 꺼져 있어도 돌고, 데이터가 남지 않는다.
 * (H2는 build.gradle에 testRuntimeOnly로만 추가했다 — 운영 코드에는 들어가지 않는다)
 */
@DataJpaTest
class BoardRepositoryTest {

    @Autowired BoardRepository boardRepository;

    @Test
    @DisplayName("Pageable로 최신순 5건씩 페이징 조회한다")
    void paging() {
        for (int i = 1; i <= 12; i++) {
            boardRepository.save(board("글 " + i));
        }

        Page<Board> page = boardRepository.findAll(
                PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "id")));   // 두 번째 페이지(0부터)

        assertThat(page.getTotalElements()).isEqualTo(12);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(5)
                .extracting(Board::getTitle)
                .containsExactly("글 7", "글 6", "글 5", "글 4", "글 3");
    }

    @Test
    @DisplayName("저장하면 IDENTITY 전략으로 id가 부여된다")
    void identity() {
        Board saved = boardRepository.save(board("새 글"));

        assertThat(saved.getId()).isNotNull();
        assertThat(boardRepository.findById(saved.getId())).isPresent();
    }

    private Board board(String title) {
        Board b = new Board();
        b.setTitle(title);
        b.setContent("내용");
        b.setWriter("hong");
        b.setCreatedAt(LocalDateTime.now());
        return b;
    }
}

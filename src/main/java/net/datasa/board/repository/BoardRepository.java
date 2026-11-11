package net.datasa.board.repository;

import net.datasa.board.domain.Board;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 게시글 JPA 저장소.
 *
 * step1~3에서 쓰던 {@code BoardMemoryRepository}를 대체한다.
 * save/findById/findAll/deleteById 등 기본 CRUD는 {@link JpaRepository}가 이미 제공한다.
 *
 * Board.writer는 LAZY(지연 로딩)라 처음엔 프록시만 들어 있다. 그런데 open-in-view: false라서
 * 트랜잭션이 끝난 뒤(화면 렌더링 중) {@code board.writer.name}에 접근하면 LazyInitializationException이 난다.
 * 그래서 화면에 작성자 이름을 보여 주는 목록·상세 조회는 {@code @EntityGraph}로 작성자(Member)를 join해 함께 가져온다.
 */
public interface BoardRepository extends JpaRepository<Board, Long> {

    /** 목록 — 최신순(id 내림차순), 작성자를 함께 조회 */
    @EntityGraph(attributePaths = "writer")
    List<Board> findAllByOrderByIdDesc();

    /** 상세 — 작성자를 함께 조회 */
    @EntityGraph(attributePaths = "writer")
    Optional<Board> findWithWriterById(Long id);

    /** 작성자 확인 — 글 id와 작성자 loginId가 모두 맞는 글이 있는지 (쿼리 한 번, LAZY 접근 없음) */
    boolean existsByIdAndWriterLoginId(Long id, String loginId);
}

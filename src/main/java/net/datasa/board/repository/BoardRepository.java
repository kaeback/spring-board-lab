package net.datasa.board.repository;

import net.datasa.board.domain.Board;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 게시글 JPA 저장소.
 *
 * step1~3에서 쓰던 {@code BoardMemoryRepository}를 대체한다.
 * save/findById/findAll/deleteById 등 기본 CRUD는 {@link JpaRepository}가 이미 제공한다.
 */
public interface BoardRepository extends JpaRepository<Board, Long> {
}

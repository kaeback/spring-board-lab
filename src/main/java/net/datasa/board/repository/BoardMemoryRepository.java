package net.datasa.board.repository;

import net.datasa.board.domain.Board;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 메모리 기반 게시글 저장소.
 *
 * DB 대신 {@link Map}에 보관한다. 애플리케이션을 재시작하면 데이터가 사라진다.
 * 다음 단계(JPA)로 넘어갈 때 이 클래스만 바꾸면 되도록, 저장소 역할을 한 곳에 모았다.
 */
@Repository
public class BoardMemoryRepository {

    private final Map<Long, Board> store = new ConcurrentHashMap<>();  // id → 게시글
    private final AtomicLong sequence = new AtomicLong(0);             // id 자동 증가용

    /** 새 글이면 id를 부여해 저장하고, 기존 글이면 그대로 덮어쓴다. */
    public Board save(Board board) {
        if (board.getId() == null) {
            board.setId(sequence.incrementAndGet());
        }
        store.put(board.getId(), board);
        return board;
    }

    public Optional<Board> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    /** 최신 글(id가 큰 글)이 먼저 오도록 정렬해 반환한다. */
    public List<Board> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(Board::getId).reversed())
                .toList();
    }

    public void deleteById(Long id) {
        store.remove(id);
    }
}

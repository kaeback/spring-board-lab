# 게시판 단계별 실습 — step7: 댓글 REST API

스프링 게시판을 **한 단계에 한 기능씩** 쌓아 올리며 만드는 실습입니다.
각 단계는 별도 브랜치로 제공되며, 이전 단계 위에 기능을 누적합니다.

## 단계(브랜치) 로드맵

| 브랜치 | 추가되는 기능 | 핵심 학습 |
|---|---|---|
| step0-setup | 프로젝트 생성 | 의존성, `application.yml`, 첫 화면 |
| step1-memory | 메모리 기반 게시판 CRUD + Thymeleaf | 계층 구조·DI, 요청 매핑·PRG, 템플릿·레이아웃 |
| step2-session | 세션 로그인 | `HttpSession`, 로그인 상태 |
| step3-validation | 입력값 검증 | `@Valid`, `BindingResult`, `th:errors` |
| step4-jpa | JPA + PostgreSQL 연동 | `@Entity`, `JpaRepository`, 트랜잭션 |
| step5-filter-interceptor | 필터·인터셉터·예외 처리 | 공통 전처리, 로그인 체크, 오류 페이지 |
| step6-file-paging | 파일 업/다운로드 + 페이징 | 파일 처리, `Pageable` |
| **step7-rest** ← 현재 | 댓글 REST API | `@RestController`, JSON, `fetch` |
| step8-test | 테스트 코드 | JUnit 6, Mockito, `@DataJpaTest`, `@WebMvcTest`, `@SpringBootTest` |
| step9-security | Spring Security로 교체 | `SecurityFilterChain`, 인증/인가, `@WithMockUser` |

> **기술 스택**: Spring Boot 4.1 (Spring Framework 7 · Spring Security 7) · Java 21 · Gradle 9 · Thymeleaf · Spring Data JPA (Hibernate 7) · PostgreSQL 17

> AI 요약 기능까지 들어간 완성본은 `master` 브랜치에 있습니다.

## step7에서 하는 것

- **댓글 REST API** — 화면(HTML)이 아니라 JSON을 주고받는 `@RestController` (`api/CommentApiController`)

  | 메서드 | 주소 | 하는 일 | 응답 |
  |---|---|---|---|
  | GET | `/api/boards/{boardId}/comments` | 댓글 목록 | 200 + JSON 배열 |
  | POST | `/api/boards/{boardId}/comments` | 댓글 등록 (로그인 필요) | 201 / 400 / 401 |
  | DELETE | `/api/comments/{commentId}` | 댓글 삭제 (본인만) | 204 / 401 / 403 / 404 |

- 요청 본문은 `@RequestBody CommentRequest`(record + `@Valid`), 응답은 엔티티 대신 `CommentResponse` DTO
- 상태 코드는 `ResponseEntity`로 직접 정한다 (201 Created + `Location`, 204 No Content, 401, 403)
- 오류는 `ApiExceptionAdvice`(`@RestControllerAdvice(basePackages = "net.datasa.board.api")`)가 `{"code","message"}` JSON으로 바꾼다 — 화면 쪽 오류 페이지에는 영향 없음
- 상세 화면(`board/detail.html`)은 자바스크립트 `fetch()`로 API를 불러 댓글을 그리고, 등록·삭제 후 목록만 다시 그린다 (페이지 새로고침 없음)
- 글을 지우면 그 글의 댓글도 함께 지운다 (`CommentRepository.deleteByBoardId`)

### 아직 없는 것 (다음 단계에서 추가)
테스트 · 시큐리티 · AI

## 실행 전 준비 — 로컬 PostgreSQL

PostgreSQL 17에 `boarddb` 데이터베이스를 만든다. (관리자 계정 `postgres`로 접속해 실행)

```sql
CREATE DATABASE boarddb ENCODING 'UTF8';
```

Docker를 쓴다면 아래 한 줄로 DB까지 준비된다.

```bash
docker run -d --name board-pg -e POSTGRES_PASSWORD=1234 -e POSTGRES_DB=boarddb -p 5432:5432 postgres:17
```

기본 접속 정보는 `localhost:5432/boarddb`, `postgres` / `1234`다. 환경이 다르면 `application.yml`을 고치거나
환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`로 덮어쓴다.
(예: `DB_URL=jdbc:postgresql://localhost:5433/boarddb ./gradlew bootRun`)

## 실행 방법

```bash
./gradlew bootRun
```

브라우저에서 <http://localhost:8080> 접속 → 게시판 목록으로 이동합니다.

## 패키지 구조

```
net.datasa.board
├─ SpringBoardApplication      애플리케이션 진입점
├─ controller
│   ├─ BoardController         게시판 요청 처리(CRUD) + 페이징 + 파일 업/다운로드
│   ├─ MemberController        회원가입·로그인·로그아웃 + 검증
│   └─ HomeController          "/" → 게시판으로 이동
├─ api
│   ├─ CommentApiController    댓글 REST API (JSON)
│   └─ ApiExceptionAdvice      API 전용 예외 → JSON 오류 응답
├─ service
│   ├─ BoardService            게시판 비즈니스 로직 (Pageable, 파일 첨부, 트랜잭션)
│   ├─ MemberService           회원 가입·로그인 검증
│   └─ FileService              첨부파일 저장/조회/삭제 (디스크)
├─ repository
│   ├─ BoardRepository          JPA 게시글 저장소
│   └─ MemberRepository         JPA 회원 저장소
├─ domain
│   ├─ Board                   게시글 엔티티 (@Entity, 첨부파일 필드 포함)
│   └─ Member                  회원 엔티티 (@Entity)
├─ exception
│   └─ BoardNotFoundException   없는 글 요청 → 404 (@ResponseStatus)
├─ dto
│   ├─ BoardForm                글쓰기/수정 입력 폼 (검증 애너테이션 포함)
│   ├─ LoginForm                로그인 입력 폼 (검증 애너테이션 포함)
│   └─ MemberJoinForm           회원가입 입력 폼 (검증 애너테이션 포함)
├─ web
│   ├─ SessionConst             세션 속성 키 상수
│   ├─ filter
│   │   └─ LogFilter            모든 요청 로깅 필터
│   └─ interceptor
│       └─ LoginCheckInterceptor  로그인 체크 인터셉터
└─ config
    ├─ WebConfig                필터·인터셉터 등록
    └─ DataInitializer          샘플 회원·데이터 주입 (중복 방지, 페이징용 12건 시드)
```

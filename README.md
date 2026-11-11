# 게시판 단계별 실습 — step6: 첨부파일 업/다운로드 + 페이징

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
| **step6-file-paging** ← 현재 | 파일 업/다운로드 + 페이징 | 파일 처리, `Pageable` |
| step7-rest | 댓글 REST API | `@RestController`, JSON, `fetch` |
| step8-test | 테스트 코드 | JUnit 6, Mockito, `@DataJpaTest`, `@WebMvcTest`, `@SpringBootTest` |
| step9-security | Spring Security로 교체 | `SecurityFilterChain`, 인증/인가, `@WithMockUser` |

> **기술 스택**: Spring Boot 4.1 (Spring Framework 7 · Spring Security 7) · Java 21 · Gradle 9 · Thymeleaf · Spring Data JPA (Hibernate 7) · PostgreSQL 17

> AI 요약 기능까지 들어간 완성본은 `master` 브랜치에 있습니다.

## step6에서 하는 것

- **첨부파일 업/다운로드** — 글쓰기·수정 화면에서 파일 1개를 첨부할 수 있고, 상세 화면에서 다운로드할 수 있다. 파일 실체는 디스크(`file.upload-dir`)에, DB에는 원본 파일명/저장 파일명 두 컬럼만 저장한다 (`FileService`)
- **페이징** — 목록 조회를 직접 계산하던 방식(subList 등) 대신 **Spring Data JPA의 `Pageable`/`Page<Board>`**를 사용한다. `BoardController.list()`가 `@PageableDefault(size = 5, sort = "id", direction = DESC) Pageable`을 받아 그대로 `boardRepository.findAll(pageable)`에 넘긴다
- `Page<T>`는 `content`/`number`/`size`/`totalElements`/`totalPages`/`first`/`last`를 그대로 제공하므로, 페이지네이션 화면(`fragments/layout.html`의 `pagination` 프래그먼트)은 별도 DTO 없이 그대로 재사용된다
- `DataInitializer`가 페이징 효과를 볼 수 있도록 샘플 글 12개를 시드한다 (이미 데이터가 있으면 건너뜀)

### 아직 없는 것 (다음 단계에서 추가)
댓글 API · 테스트 · 시큐리티 · AI

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

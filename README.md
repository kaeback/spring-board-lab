# 게시판 단계별 실습 — step8: 테스트 코드

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
| step7-rest | 댓글 REST API | `@RestController`, JSON, `fetch` |
| **step8-test** ← 현재 | 테스트 코드 | JUnit 6, Mockito, `@DataJpaTest`, `@WebMvcTest`, `@SpringBootTest` |
| step9-security | Spring Security로 교체 | `SecurityFilterChain`, 인증/인가, `@WithMockUser` |

> **기술 스택**: Spring Boot 4.1 (Spring Framework 7 · Spring Security 7) · Java 21 · Gradle 9 · Thymeleaf · Spring Data JPA (Hibernate 7) · PostgreSQL 17

> AI 요약 기능까지 들어간 완성본은 `master` 브랜치에 있습니다.

## step8에서 하는 것

기능은 step7과 같고, **테스트 코드**를 더한다. 교안 [테스트] 장의 네 가지 테스트 방식을 이 게시판에 적용한다.

| 테스트 | 방식 | 무엇을 검증하나 |
|---|---|---|
| `BoardServiceTest` | **단위 테스트** — `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks` | 스프링 없이 `BoardService` 로직만: 글쓰기 값 채우기, 조회수 증가, 없는 글 → `BoardNotFoundException`, 작성자 확인 |
| `BoardRepositoryTest` | **JPA 슬라이스** — `@DataJpaTest` (내장 H2, 테스트마다 롤백) | `Pageable` 페이징·정렬, IDENTITY id 부여 |
| `CommentApiControllerTest` | **웹 슬라이스(API)** — `@WebMvcTest` + `jsonPath` | 댓글 목록 JSON, 401·201·400·403·204 상태 코드 |
| `BoardControllerTest` | **웹 슬라이스** — `@WebMvcTest` + `MockMvc` + `@MockitoBean` | 목록 200, 없는 글 404, 비로그인 글쓰기 → 인터셉터 리다이렉트, 검증 실패 시 폼 재표시 |
| `BoardFlowIntegrationTest` | **통합 테스트** — `@SpringBootTest` + `@AutoConfigureMockMvc` + `@AutoConfigureTestDatabase` | 로그인 → 글쓰기 → 상세까지 실제 빈과 DB로 |

- **Boot 4 포인트**
    - 테스트 스타터는 기술별: `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`
    - `@WebMvcTest`, `@DataJpaTest`, `@AutoConfigureMockMvc`, `@AutoConfigureTestDatabase`의 **패키지가 바뀌었다** (테스트 코드의 import 참고)
    - `@MockBean`은 제거 → **`@MockitoBean`**
    - `@SpringBootTest`만으로는 `MockMvc`가 생기지 않는다 → **`@AutoConfigureMockMvc`**
- 테스트는 **PostgreSQL 없이** 돈다. `testRuntimeOnly 'com.h2database:h2'`를 추가해, `@DataJpaTest`·`@AutoConfigureTestDatabase`가 내장 H2로 바꿔 실행한다 (H2는 테스트 클래스패스에만 들어간다)
- `build.gradle`에서 Mockito를 **JVM 에이전트**(`-javaagent`)로 등록한다. JDK 21+에서 "Mockito is currently self-attaching…" / "A Java agent has been loaded dynamically" 경고가 사라진다

### 테스트 실행

```bash
./gradlew test
```

결과 보고서: `build/reports/tests/test/index.html`

### 아직 없는 것 (다음 단계에서 추가)
시큐리티 · AI

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

### 테스트 코드 (`src/test/java`)

```
net.datasa.board
├─ BoardFlowIntegrationTest     @SpringBootTest 통합 테스트 (로그인 → 글쓰기 → 상세)
├─ controller
│   └─ BoardControllerTest      @WebMvcTest + MockMvc + @MockitoBean
├─ repository
│   └─ BoardRepositoryTest      @DataJpaTest (H2, 페이징)
└─ service
    └─ BoardServiceTest         Mockito 단위 테스트
```

# 게시판 단계별 실습 — step9: Spring Security로 교체

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
| step8-test | 테스트 코드 | JUnit 6, Mockito, `@DataJpaTest`, `@WebMvcTest`, `@SpringBootTest` |
| **step9-security** ← 현재 | Spring Security로 교체 | `SecurityFilterChain`, 인증/인가, `@WithMockUser` |

> **기술 스택**: Spring Boot 4.1 (Spring Framework 7 · Spring Security 7) · Java 21 · Gradle 9 · Thymeleaf · Spring Data JPA (Hibernate 7) · PostgreSQL 17

> AI 요약 기능까지 들어간 완성본은 `master` 브랜치에 있습니다.

## step9에서 하는 것

- 직접 만든 세션 로그인·`LoginCheckInterceptor`를 걷어내고 **Spring Security**로 교체
- `SecurityConfig`의 `authorizeHttpRequests`로 인가 규칙(작성/수정/삭제는 인증 필요, 목록/상세/다운로드는 공개)을 선언적으로 정의
- 정적 리소스는 `PathRequest.toStaticResources().atCommonLocations()`로, 오류 화면 경로 `/error`는 `permitAll()`로 열어 둔다 (막으면 비로그인 사용자가 404 대신 로그인 페이지로 튕긴다)
- `formLogin`으로 우리가 만든 `/login` 화면을 그대로 사용하되, 인증 처리(POST `/login`)와 로그아웃(POST `/logout`)은 Security가 담당
- `CustomUserDetailsService`가 `MemberRepository.findByLoginId`로 회원을 찾아 `UserDetails`(`CustomUserDetails`)로 변환
- 비밀번호는 가입 시 `PasswordEncoder`(BCrypt)로 암호화해 저장하고, 로그인 시 비교도 Security가 대신한다 (`MemberService.login()` 메서드는 제거)
- 컨트롤러는 `@AuthenticationPrincipal CustomUserDetails`로 로그인 사용자를 받는다 — `HttpSession`/`SessionConst`는 더 이상 필요 없어 삭제
- 화면은 `sec:authorize`, `sec:authentication`, `#authentication.name` 등 `thymeleaf-extras-springsecurity6`의 `sec:` 네임스페이스로 로그인 상태를 판단
- **댓글 API(step7)** — 로그인 확인을 Security에 맡긴다. 댓글 목록 GET만 `permitAll()`이고, 로그인하지 않고 등록·삭제를 부르면 로그인 화면으로 redirect하는 대신 `authenticationEntryPoint`가 **401 JSON**으로 응답한다(`/api/**`만). 컨트롤러는 `@AuthenticationPrincipal`로 작성자를 받는다
- **로그인/작성자 본인 확인**처럼 애플리케이션 도메인 규칙(`isOwner`)은 Security가 대신할 수 없어 여전히 컨트롤러/서비스에 남는다

### 테스트 (step8에서 이어짐)

시큐리티가 들어오면서 step8의 테스트가 그대로는 깨진다. 무엇을 바꿨는지가 이 단계의 학습 포인트다.

- `@WebMvcTest`는 우리 `SecurityConfig`를 읽지 않아 **기본 보안(전부 인증 필요)** 이 걸린다 → `@Import(SecurityConfig.class)`
- 세션에 회원을 넣던 방식 대신 **인증된 사용자를 요청에 붙인다**. 컨트롤러가 `@AuthenticationPrincipal CustomUserDetails`를 받으므로 `@WithMockUser`가 아닌 `SecurityMockMvcRequestPostProcessors.user(new CustomUserDetails(member))`를 쓴다
- 통합 테스트는 `formLogin("/login").user("loginId", "hong").password("password", "1234")`로 실제 로그인 흐름(BCrypt 검증 포함)을 검증한다
- `CommentApiControllerTest`도 같은 방식으로 바꾸고, 비로그인 요청에는 시큐리티가 401 JSON을 주는지 확인한다
- 시큐리티 테스트 도구는 `spring-boot-starter-security-test`에 들어 있다 (Boot 4)

```bash
./gradlew test
```

### 아직 없는 것 (다음 단계에서 추가)
AI 요약

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
로그인 테스트 계정: `hong` / `kim` / `admin` (비밀번호 모두 `1234`)

## 패키지 구조

```
net.datasa.board
├─ SpringBoardApplication      애플리케이션 진입점
├─ controller
│   ├─ BoardController         게시판 요청 처리(CRUD) + 페이징 + 파일 업/다운로드
│   ├─ MemberController        회원가입 + 로그인 화면 표시 (인증 처리는 Security)
│   └─ HomeController          "/" → 게시판으로 이동
├─ service
│   ├─ BoardService            게시판 비즈니스 로직 (Pageable, 파일 첨부, 트랜잭션)
│   ├─ MemberService           회원 가입 (비밀번호 암호화)
│   └─ FileService              첨부파일 저장/조회/삭제 (디스크)
├─ repository
│   ├─ BoardRepository          JPA 게시글 저장소
│   └─ MemberRepository         JPA 회원 저장소
├─ security
│   ├─ CustomUserDetails         Member를 UserDetails로 감싸는 어댑터
│   └─ CustomUserDetailsService  loginId로 회원 조회 → UserDetails 반환
├─ domain
│   ├─ Board                   게시글 엔티티 (@Entity, 첨부파일 필드 포함)
│   └─ Member                  회원 엔티티 (@Entity, role 포함)
├─ exception
│   └─ BoardNotFoundException   없는 글 요청 → 404 (@ResponseStatus)
├─ dto
│   ├─ BoardForm                글쓰기/수정 입력 폼 (검증 애너테이션 포함)
│   └─ MemberJoinForm           회원가입 입력 폼 (검증 애너테이션 포함)
├─ web
│   └─ filter
│       └─ LogFilter            모든 요청 로깅 필터
└─ config
    ├─ SecurityConfig            Spring Security 설정 (인가 규칙, 폼 로그인, 로그아웃)
    ├─ WebConfig                 필터 등록 (인터셉터는 Security로 대체되어 제거)
    └─ DataInitializer           샘플 회원·데이터 주입 (중복 방지, 페이징용 12건 시드)
```

# 게시판 단계별 실습 — step2: 세션 로그인

스프링 게시판을 **한 단계에 한 기능씩** 쌓아 올리며 만드는 실습입니다.
각 단계는 별도 브랜치로 제공되며, 이전 단계 위에 기능을 누적합니다.

## 단계(브랜치) 로드맵

| 브랜치 | 추가되는 기능 | 핵심 학습 |
|---|---|---|
| step0-setup | 프로젝트 생성 | 의존성, `application.yml`, 첫 화면 |
| step1-memory | 메모리 기반 게시판 CRUD + Thymeleaf | 계층 구조·DI, 요청 매핑·PRG, 템플릿·레이아웃 |
| **step2-session** ← 현재 | 세션 로그인 | `HttpSession`, 로그인 상태 |
| step3-validation | 입력값 검증 | `@Valid`, `BindingResult`, `th:errors` |
| step4-jpa | JPA + PostgreSQL 연동 | `@Entity`, `JpaRepository`, 트랜잭션 |
| step5-filter-interceptor | 필터·인터셉터·예외 처리 | 공통 전처리, 로그인 체크, 오류 페이지 |
| step6-file-paging | 파일 업/다운로드 + 페이징 | 파일 처리, `Pageable` |
| step7-rest | 댓글 REST API | `@RestController`, JSON, `fetch` |
| step8-test | 테스트 코드 | JUnit 6, Mockito, `@DataJpaTest`, `@WebMvcTest`, `@SpringBootTest` |
| step9-security | Spring Security로 교체 | `SecurityFilterChain`, 인증/인가, `@WithMockUser` |

> **기술 스택**: Spring Boot 4.1 (Spring Framework 7 · Spring Security 7) · Java 21 · Gradle 9 · Thymeleaf · Spring Data JPA (Hibernate 7) · PostgreSQL 17

> AI 요약 기능까지 들어간 완성본은 `master` 브랜치에 있습니다.

## step2에서 하는 것

- **회원가입 / 로그인 / 로그아웃** — `HttpSession`으로 로그인 상태를 직접 관리
- 글쓰기·수정·삭제는 **로그인한 사용자만** 가능 (각 컨트롤러 메서드에서 세션을 직접 확인)
- **작성자는 더 이상 폼에서 입력받지 않는다** — 로그인한 세션 사용자의 loginId를 그대로 사용
- 수정·삭제 버튼은 **작성자 본인에게만** 노출 (`isOwner` 권한 검사)
- 저장은 여전히 **메모리(`Map`)** — 재시작하면 데이터가 사라짐
- 로그인 테스트 계정: `hong` / `kim` / `admin` (비밀번호 모두 `1234`)

### 아직 없는 것 (다음 단계에서 추가)
입력값 검증 · JPA/DB 연동 · 필터/인터셉터 · 파일 첨부 · 페이징 · 댓글 API · 테스트 · 시큐리티 · AI

## 실행 방법

```bash
./gradlew bootRun
```

브라우저에서 <http://localhost:8080> 접속 → 게시판 목록으로 이동합니다.
(앱이 뜰 때 샘플 회원 3명과 글 3개가 자동으로 들어갑니다.)

## 패키지 구조

```
net.datasa.board
├─ SpringBoardApplication      애플리케이션 진입점
├─ controller
│   ├─ BoardController         게시판 요청 처리(CRUD) + 세션 로그인 확인
│   ├─ MemberController        회원가입·로그인·로그아웃
│   └─ HomeController          "/" → 게시판으로 이동
├─ service
│   ├─ BoardService            게시판 비즈니스 로직
│   └─ MemberService           회원 가입·로그인 검증
├─ repository
│   ├─ BoardMemoryRepository   메모리 저장소(Map)
│   └─ MemberMemoryRepository  회원 메모리 저장소(Map)
├─ domain
│   ├─ Board                   게시글 객체(POJO)
│   └─ Member                  회원 객체(POJO)
├─ dto
│   ├─ BoardForm                글쓰기/수정 입력 폼
│   ├─ LoginForm                로그인 입력 폼
│   └─ MemberJoinForm           회원가입 입력 폼
├─ web
│   └─ SessionConst             세션 속성 키 상수
└─ config
    └─ DataInitializer          샘플 회원·데이터 주입
```

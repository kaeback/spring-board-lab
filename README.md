# 게시판 실습 과제 — step4: 게시글 기능 직접 만들기 + JPA로 DB 연결하기

step2(세션 로그인)까지 만든 게시판은 글과 회원을 메모리(`Map`)에 보관하므로, 앱을 재시작하면 데이터가 모두 사라집니다.
이번 과제 프로젝트에는 입력값 검증(`@Valid`)이 들어 있지 않습니다. 검증은 아직 배우지 않았으므로 이 과제에서는 다루지 않습니다.
이번 과제에서는 **Spring Data JPA로 PostgreSQL에 데이터를 저장하도록** 저장소 계층을 직접 바꿉니다.

이 프로젝트에는 **회원가입과 로그인만 완성되어 있습니다.** 게시글 목록·읽기·등록·수정·삭제는 `BoardController`와
`BoardService`에 메서드 선언만 있고, 본문은 `UnsupportedOperationException`을 던지도록 비워 두었습니다.
저장소를 JPA로 바꾸면서 이 메서드들의 본문을 직접 작성해 게시판을 완성합니다.

## 과제 목표

- `build.gradle`에 JPA·PostgreSQL 의존성을 추가하고, `application.yml`에 DB 접속 정보와 JPA 설정을 써서 각 설정이 무슨 일을 하는지 설명할 수 있다.
- `@Entity`, `@Id`, `@GeneratedValue`, `@Column`으로 자바 클래스를 테이블에 매핑할 수 있다.
- `JpaRepository`를 상속하는 것만으로 기본 CRUD가 만들어지는 원리를 이해하고, 쿼리 메서드(`findByLoginId`)를 선언할 수 있다.
- 서비스 계층에 `@Transactional`을 적용하고, 변경 감지(dirty checking)로 UPDATE가 실행되는 과정을 설명할 수 있다.
- 게시글 작성자를 문자열 대신 `Member` 엔티티 참조(`@ManyToOne(fetch = LAZY)`)로 매핑하고, OSIV를 끈 환경에서 `@EntityGraph`로 작성자를 함께 조회해 `LazyInitializationException`을 피할 수 있다.
- 컨트롤러와 서비스에 게시글 목록·읽기·등록·수정·삭제를 직접 구현하고, 로그인·작성자 확인 흐름을 설명할 수 있다.
- 재시작 후에도 데이터가 남는 환경에서 초기 데이터를 어떻게 넣어야 하는지 판단할 수 있다.

## 시작 상태

받은 프로젝트에는 JPA·PostgreSQL 의존성과 DB 설정이 아직 없습니다. 그래서 그대로 실행하면 DB 없이 메모리 저장소로 동작합니다.
회원가입·로그인은 바로 쓸 수 있고, 게시판 화면(`/boards`)은 과제 5를 마칠 때까지 오류 화면이 나옵니다.

## 사전 준비 — PostgreSQL에 계정과 board_db 만들기

PostgreSQL 17에 관리자 계정 `postgres`로 접속해, 앱에서 사용할 계정 `boarduser`와 데이터베이스 `board_db`를 만듭니다.

```sql
CREATE USER boarduser WITH PASSWORD 'board1234';
CREATE DATABASE board_db OWNER boarduser;
```

`psql`을 쓴다면 `psql -U postgres`로 접속해 위 두 문장을 실행합니다.
`board_db`의 소유자가 `boarduser`이므로, 이 계정으로 테이블을 만들 권한이 따로 필요하지 않습니다.

Docker를 쓴다면 아래 명령 하나로 계정과 DB가 함께 준비됩니다.

```bash
docker run -d --name board-pg -e POSTGRES_USER=boarduser -e POSTGRES_PASSWORD=board1234 -e POSTGRES_DB=board_db -p 5432:5432 postgres:17
```

접속 정보는 `localhost:5432/board_db`, `boarduser` / `board1234`입니다. 과제 1에서 이 값을 `application.yml`에 씁니다.

## 과제 목록

과제 2~8은 서로 연결되어 있으므로, 중간에 컴파일 오류가 나더라도 8번까지 마친 뒤 실행해 보세요.

### 과제 1. 의존성 추가와 DB 연결 설정 — `build.gradle`, `src/main/resources/application.yml`

**요구사항**
- `build.gradle`의 `dependencies`에 아래 세 의존성을 추가한다.

  | 의존성 | 구성 | 용도 |
  |---|---|---|
  | `org.springframework.boot:spring-boot-starter-data-jpa` | `implementation` | Spring Data JPA와 Hibernate |
  | `org.postgresql:postgresql` | `runtimeOnly` | PostgreSQL JDBC 드라이버 (실행할 때만 필요) |
  | `org.springframework.boot:spring-boot-starter-data-jpa-test` | `testImplementation` | JPA 테스트용 스타터 |

- `spring.datasource`에 PostgreSQL 접속 정보를 쓴다.
  - URL: `jdbc:postgresql://localhost:5432/board_db`
  - 계정: `boarduser`, 비밀번호: `board1234`
- `spring.jpa`에 아래 설정을 쓴다.
  - 엔티티를 기준으로 테이블을 만들고, 이미 있으면 그대로 둔다(`ddl-auto: update`).
  - 실행되는 SQL을 콘솔에 보기 좋게 출력한다.
  - OSIV(Open Session In View)를 끈다. 영속성 컨텍스트가 서비스의 트랜잭션 안에서만 열려 있게 하기 위해서다.
- 설정을 마치면 앱이 시작되어야 한다. 아직 엔티티가 없으므로 데이터는 여전히 메모리에 저장된다.
  회원가입·로그인은 동작하지만, 게시판 화면(`/boards`)은 과제 5를 마칠 때까지 오류 화면이 나온다.

**힌트**
- 버전은 쓰지 않는다. 스프링 부트가 맞는 버전을 정해 준다. 의존성을 추가한 뒤에는 IntelliJ의 Gradle 새로고침을 실행한다.
- JPA 의존성을 추가하면 스프링 부트가 시작할 때 DB에 연결하려고 한다. 이때 `application.yml`에 접속 정보가 없으면
  `Failed to configure a DataSource` 오류로 앱이 시작되지 않는다. 의존성과 접속 정보를 함께 넣자.
- 설정 키: `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`,
  `spring.jpa.hibernate.ddl-auto`, `spring.jpa.show-sql`, `spring.jpa.properties.hibernate.format_sql`, `spring.jpa.open-in-view`
- JDBC URL 형식은 `jdbc:postgresql://호스트:포트/데이터베이스`다. 드라이버 클래스는 URL을 보고 자동으로 정해지므로 쓰지 않아도 된다.
- YAML은 들여쓰기로 계층을 나타낸다. 이미 있는 `spring:` 아래에 `datasource:`, `jpa:`를 같은 깊이로 추가한다.
- DB가 다른 포트에 있다면 `${DB_URL:jdbc:postgresql://localhost:5432/board_db}`처럼 써 두고 환경변수로 바꿀 수 있다.
  (예: `DB_URL=jdbc:postgresql://localhost:5433/board_db ./gradlew bootRun`)

### 과제 2. 엔티티 매핑 — `domain/Board.java`, `domain/Member.java`

**요구사항**
- `Board`
  - 클래스를 JPA 엔티티로 지정한다.
  - `id`를 기본키로 지정하고, DB가 자동 증가 값을 부여하도록 한다(IDENTITY 전략).
  - `content`는 긴 글을 담을 수 있도록 컬럼 타입을 `TEXT`로 지정한다.
  - `writer`의 타입을 `String`(loginId)에서 `Member`로 바꾸고 **다대일 연관관계**로 매핑한다. 필드 이름은 `writer` 그대로 둔다.
    - 지연 로딩(LAZY), 외래 키 컬럼 이름 `writer_id`, not null
    - `Member` 쪽에는 컬렉션을 추가하지 않는다(단방향).
- `Member`
  - 클래스를 JPA 엔티티로 지정한다.
  - 기본키로 쓸 `Long id` 필드를 새로 추가하고 IDENTITY 전략으로 자동 증가시킨다.
  - `loginId`는 기본키가 아니라 일반 컬럼으로 두되, 중복될 수 없고(unique) 비어 있을 수 없게(not null) 한다.

**힌트**
- 필요한 애너테이션은 모두 `jakarta.persistence` 패키지에 있다: `@Entity`, `@Id`, `@GeneratedValue`, `@Column`
- `@GeneratedValue`의 `strategy` 속성과 `GenerationType` 열거형을 살펴보자.
- `@Column`에는 `columnDefinition`, `unique`, `nullable` 속성이 있다.
- 애너테이션이 없는 필드도 같은 이름(카멜 표기는 스네이크 표기로 바뀜)의 컬럼으로 매핑된다. 예: `viewCount` → `view_count`
- 연관관계는 `@ManyToOne`(`fetch` 속성과 `FetchType`)과 `@JoinColumn`(`name`, `nullable` 속성)으로 매핑한다. 글 여러 개가 회원 한 명을 가리키므로 "다대일"이다.

### 과제 3. JPA 리포지토리 만들기 — 새 파일 2개

`src/main/java/net/datasa/board/repository/` 폴더에 아래 두 인터페이스를 **새로** 만든다.

| 파일 | 패키지 | 선언 |
|---|---|---|
| `BoardRepository.java` | `net.datasa.board.repository` | `public interface BoardRepository extends JpaRepository<Board, Long>` |
| `MemberRepository.java` | `net.datasa.board.repository` | `public interface MemberRepository extends JpaRepository<Member, Long>` |

**요구사항**
- `BoardRepository`에는 아래 메서드 세 개를 선언한다. (`save`, `findById`, `deleteById`, `count` 등은 `JpaRepository`가 제공한다.)

  | 메서드 | 쓰는 곳 | 하는 일 |
  |---|---|---|
  | `List<Board> findAllByOrderByIdDesc()` | 목록 | id 내림차순(최신 글 먼저), **작성자를 함께 조회** |
  | `Optional<Board> findWithWriterById(Long id)` | 상세 | 글 하나, **작성자를 함께 조회** |
  | `boolean existsByIdAndWriterLoginId(Long id, String loginId)` | 작성자 확인 | 글 id와 작성자의 loginId가 모두 맞는 글이 있는지 |

- `MemberRepository`에는 아래 메서드 하나를 선언한다.

  ```java
  Optional<Member> findByLoginId(String loginId);
  ```

**힌트**
- `JpaRepository<엔티티 타입, 기본키 타입>` 형태다. `Member`의 기본키는 과제 2에서 추가한 `Long id`다.
- 인터페이스만 선언하면 Spring Data JPA가 실행 시점에 구현 객체를 만들어 빈으로 등록한다. `@Repository`를 붙이지 않아도 된다.
- `findBy` + 필드 이름 규칙으로 선언한 메서드는 이름을 분석해 조회 쿼리가 만들어진다(쿼리 메서드).
  `OrderByIdDesc`는 정렬, `existsBy...`는 존재 여부(boolean), `WriterLoginId`는 `writer.loginId`처럼 연관 엔티티의 필드를 따라간다.
  `findWithWriterById`의 `With...` 부분은 이름 분석에서 무시되는 설명용 이름이다(`findById`와 같은 조건).
- `writer`가 LAZY이므로 그냥 조회하면 작성자 자리에는 프록시만 들어 있다. `open-in-view: false`라서 트랜잭션이 끝난 뒤 화면에서
  `board.writer.name`을 읽으면 `LazyInitializationException`이 난다. 목록·상세 메서드에 `@EntityGraph(attributePaths = "writer")`를 붙여
  작성자를 join으로 함께 가져오자. (`org.springframework.data.jpa.repository.EntityGraph`)

### 과제 4. 게시글 서비스 구현 — `service/BoardService.java`, `service/MemberService.java`

**요구사항**
- `MemberService`의 필드 타입을 `MemberMemoryRepository`에서 과제 3에서 만든 `MemberRepository`로 바꾼다. 필드 이름(`memberRepository`)은 그대로 둔다.
- `BoardService`의 필드 타입을 `BoardMemoryRepository`에서 `BoardRepository`로 바꾸고, 작성자를 찾을 `MemberRepository`도 주입받는다(필드 이름 `memberRepository`).
- `BoardService`의 메서드 본문을 아래 표대로 작성한다. 메서드 이름·매개변수 타입·반환 타입은 바꾸지 않는다(컨트롤러가 이 메서드들을 호출한다).
  `write`의 두 번째 매개변수 이름은 `writer` 대신 `loginId`로 바꿔도 좋다.

  | 메서드 | 하는 일 | 조건 |
  |---|---|---|
  | `List<Board> getList()` | 전체 글을 최신 글부터(id 내림차순) 돌려준다. 작성자도 함께 조회한다 | `findAllByOrderByIdDesc()` |
  | `Board write(BoardForm form, String loginId)` | `loginId`로 회원을 찾아(없으면 `IllegalArgumentException`) 작성자로 연결하고, 제목·내용, 조회수 0, 작성 시각(현재 시각)을 넣어 저장한 뒤 저장된 글을 돌려준다 | `@Transactional` |
  | `Board getDetailAndIncreaseView(Long id)` | 글을 작성자와 함께 조회하고(없으면 `IllegalArgumentException`) 조회수를 1 올린 뒤 돌려준다 | `@Transactional`, `findWithWriterById(id)` |
  | `Board getById(Long id)` | 글 하나를 조회수 변경 없이 돌려준다(없으면 `IllegalArgumentException`). 수정 화면용 | `findById(id)` |
  | `void update(Long id, BoardForm form)` | 제목과 내용만 바꾼다. 작성자는 그대로 둔다 | `@Transactional` |
  | `void delete(Long id)` | 글을 삭제한다 | |
  | `boolean isOwner(Long id, String loginId)` | 글 `id`의 작성자가 `loginId`인지 확인한다 | `existsByIdAndWriterLoginId(id, loginId)` |

**힌트**
- `Optional`에서 값을 꺼낼 때는 `orElseThrow(() -> new IllegalArgumentException("존재하지 않는 글입니다. id=" + id))`처럼 쓴다.
- `JpaRepository.findAll()`은 순서를 보장하지 않는다. 그래서 정렬과 작성자 조회가 들어간 `findAllByOrderByIdDesc()`를 쓴다.
- `isOwner`에서 글을 꺼내 `board.getWriter().getLoginId()`를 비교하면, 트랜잭션 밖에서 LAZY 작성자에 접근하게 된다. 쿼리 메서드 하나로 끝내면 그럴 일이 없다.
- `@Transactional`은 `org.springframework.transaction.annotation` 패키지의 것을 쓴다.
- JPA에서는 트랜잭션 안에서 조회한 엔티티의 값이 바뀌면, 트랜잭션이 끝날 때 변경 감지로 UPDATE SQL이 실행된다. 트랜잭션이 없으면 조회수 증가나 글 수정이 DB에 반영되지 않는다.
- `update`에 `save` 호출을 넣지 않아도 된다. 변경 감지로 처리되는지 콘솔의 SQL 로그로 확인해 보자.

### 과제 5. 게시글 컨트롤러 구현 — `controller/BoardController.java`

**요구사항**
- 각 핸들러 메서드의 본문을 아래 표대로 작성한다. 매핑 애너테이션과 메서드 선언은 바꾸지 않는다.

  | 메서드 | 요청 | 하는 일 |
  |---|---|---|
  | `list` | `GET /boards` | 누구나. 글 목록을 모델 `boards`에 담아 `board/list` 화면을 보여 준다 |
  | `writeForm` | `GET /boards/write` | 로그인하지 않았으면 `redirect:/login?redirectURL=/boards/write`. 빈 `BoardForm`을 모델 `boardForm`에 담아 `board/write` 화면을 보여 준다 |
  | `write` | `POST /boards/write` | 로그인하지 않았으면 `redirect:/login`. 로그인 회원의 `loginId`로 글을 저장하고 `redirect:/boards/{새 글 id}` |
  | `detail` | `GET /boards/{id}` | 누구나. 조회수를 올린 글을 모델 `board`에 담아 `board/detail` 화면을 보여 준다 |
  | `editForm` | `GET /boards/{id}/edit` | 로그인하지 않았으면 `redirect:/login`, 작성자 본인이 아니면 `redirect:/boards/{id}`. 기존 제목·내용을 담은 `BoardForm`을 모델 `boardForm`에, 글 id를 모델 `boardId`에 담아 `board/edit` 화면을 보여 준다 |
  | `edit` | `POST /boards/{id}/edit` | 로그인하지 않았으면 `redirect:/login`, 본인이 아니면 `redirect:/boards/{id}`. 글을 수정하고 `redirect:/boards/{id}` |
  | `delete` | `POST /boards/{id}/delete` | 로그인하지 않았으면 `redirect:/login`. 본인일 때만 삭제하고 `redirect:/boards` |

**힌트**
- 로그인 회원은 클래스에 있는 `loginMember(session)`으로 꺼낸다. 로그인하지 않았으면 `null`이다.
- 모델 속성 이름은 화면 템플릿(`templates/board/*.html`)이 쓰는 이름과 같아야 한다. 템플릿을 열어 `${boards}`, `${board}`, `*{title}`, `${boardId}` 같은 표현을 확인하자.
- 수정·삭제 버튼은 화면에서 본인에게만 보이지만, 주소를 직접 입력하거나 요청을 보내면 누구나 호출할 수 있다. 그래서 컨트롤러에서도 `boardService.isOwner(...)`로 확인한다.
- 목록 → 글쓰기 → 상세 → 수정 → 삭제 순서로 하나씩 만들고, 만들 때마다 브라우저에서 확인하자.

### 과제 6. 초기 데이터(중복 방지와 샘플 글) — `config/DataInitializer.java`

**요구사항**
- `MemberRepository`와 `BoardRepository`를 생성자로 주입받는다.
- 회원 테이블이 비어 있을 때만 샘플 회원 3명을 넣는다.
- 지금은 샘플 회원만 넣는다. 과제 4에서 `write`를 구현했으면, 게시글 테이블이 비어 있을 때만 `BoardService.write`로 샘플 글 3개를 넣는 코드를 추가한다.

  | 제목 | 내용 | 작성자 |
  |---|---|---|
  | 첫 번째 글입니다 | DB(JPA)에 저장되는 게시판에 오신 것을 환영합니다. | `admin` |
  | 두 번째 글입니다 | 로그인 후 글쓰기 버튼으로 새 글을 작성해 보세요. | `hong` |
  | 세 번째 글입니다 | 본인 글에만 수정·삭제 버튼이 보입니다. | `kim` |
- 클래스 주석의 "재시작하면 사라진다" 설명을 DB 저장 방식에 맞게 고친다.

**힌트**
- `JpaRepository`가 제공하는 `count()`를 사용한다.
- 이 처리가 없으면 두 번째 기동부터 `loginId`의 unique 제약 때문에 회원 저장이 실패하고 앱이 뜨지 않는다.
- 글의 작성자는 회원을 가리키는 외래 키(`writer_id`)이므로, 회원을 먼저 넣고 글을 나중에 넣는다.

### 과제 7. 메모리 저장소 삭제 — `repository/BoardMemoryRepository.java`, `repository/MemberMemoryRepository.java`

**요구사항**
- 두 파일을 삭제한다.
- 삭제한 뒤에도 `./gradlew build`가 성공해야 한다. 아직 두 클래스를 참조하는 코드가 남아 있다면 과제 4를 다시 확인한다.

### 과제 8. 화면에서 작성자 표시 — `templates/board/list.html`, `templates/board/detail.html`

**요구사항**
- 목록과 상세 화면의 작성자 자리에 로그인 아이디 대신 **작성자 이름**을 표시한다.
- 상세 화면에서 수정·삭제 버튼을 보여 줄지 정하는 본인 확인은 **로그인 아이디끼리** 비교한다.

**힌트**
- `board.writer`는 이제 `Member` 객체다. Thymeleaf에서는 `${board.writer.name}`, `${board.writer.loginId}`처럼 점(.)으로 필드를 따라간다.
- 세션의 로그인 회원은 `session.loginMember`이고, 비교할 값은 `session.loginMember.loginId`다.
- 과제 3에서 `@EntityGraph`를 빠뜨리면 이 화면에서 `LazyInitializationException`이 난다.

## 완료 확인 체크리스트

- [ ] `./gradlew build`가 성공한다.
- [ ] `application.yml`에 DB·JPA 설정을 넣은 뒤 앱이 오류 없이 시작된다.
- [ ] `./gradlew bootRun`으로 처음 기동할 때 콘솔에 `create table board`, `create table member` SQL이 출력된다.
- [ ] `board` 테이블에 `writer_id` 컬럼과 `member`를 참조하는 외래 키(`foreign key (writer_id) references member`)가 만들어진다.
- [ ] 게시판 목록에 샘플 글 3개가 최신 글부터 보이고, 작성자 칸에 이름(`관리자`, `홍길동`, `김철수`)이 보인다.
- [ ] 목록·상세 화면을 열 때 콘솔의 조회 SQL에 `join member`가 보이고, `LazyInitializationException`이 나지 않는다.
- [ ] 회원가입 후 새 계정으로 로그인할 수 있다. 이미 있는 아이디로 가입하면 중복 오류가 표시된다.
- [ ] 로그인 후 글을 쓰면 목록 맨 위에 나타나고, 콘솔에 `insert into board` SQL이 출력된다.
- [ ] 상세 화면에 들어갈 때마다 조회수가 올라가고, 콘솔에 `update board` SQL이 출력된다.
- [ ] 본인 글을 수정하면 바뀐 내용이 보이고, 삭제하면 목록에서 사라진다.
- [ ] 다른 사람의 글 상세 화면에는 수정·삭제 버튼이 보이지 않는다.
- [ ] 앱을 종료했다가 다시 기동해도 직접 쓴 글, 가입한 회원, 조회수가 그대로 남아 있다.
- [ ] 다시 기동할 때 오류 없이 뜨고, 샘플 회원·글이 중복으로 추가되지 않는다.
- [ ] (선택) `psql`이나 DB 도구로 `SELECT * FROM board;`, `SELECT * FROM member;`를 실행해 데이터를 직접 확인한다.

로그인 테스트 계정: `hong` / `kim` / `admin` (비밀번호 모두 `1234`)

## 정답

정답 코드는 `step4-jpa` 브랜치에 있습니다. 과제를 마친 뒤 아래 명령으로 자신의 코드와 비교해 보세요.

```bash
git diff step4-jpa -- src
```

`step4-jpa`에는 입력값 검증(step3) 코드도 들어 있습니다. 그래서 위 비교 결과에는 입력 폼(`BoardForm`·`LoginForm`·`MemberJoinForm`)의 검증 애너테이션,
컨트롤러의 `@Valid`·`BindingResult`, 화면의 오류 메시지 표시도 차이로 나옵니다. 이 부분은 과제 범위가 아니므로 무시하고,
`application.yml`·엔티티·리포지토리·서비스·컨트롤러·`DataInitializer`·`list.html`·`detail.html`의 작성자 표시만 비교하면 됩니다.

## 패키지 구조 (과제 완료 후)

```
net.datasa.board
├─ SpringBoardApplication      애플리케이션 진입점
├─ controller
│   ├─ BoardController         게시판 요청 처리(CRUD) + 세션 로그인 확인  ← 과제 5
│   ├─ MemberController        회원가입·로그인·로그아웃
│   └─ HomeController          "/" → 게시판으로 이동
├─ service
│   ├─ BoardService            게시판 비즈니스 로직 (트랜잭션 경계 포함)      ← 과제 4
│   └─ MemberService           회원 가입·로그인 검증                        ← 과제 4
├─ repository
│   ├─ BoardRepository         JPA 게시글 저장소 (새 파일)                  ← 과제 3
│   └─ MemberRepository        JPA 회원 저장소 (새 파일)                    ← 과제 3
├─ domain
│   ├─ Board                   게시글 엔티티 (@Entity, 작성자 → Member)     ← 과제 2
│   └─ Member                  회원 엔티티 (@Entity)                        ← 과제 2
├─ dto
│   ├─ BoardForm               글쓰기/수정 입력 폼
│   ├─ LoginForm               로그인 입력 폼
│   └─ MemberJoinForm          회원가입 입력 폼
├─ web
│   └─ SessionConst            세션 속성 키 상수
└─ config
    └─ DataInitializer         샘플 회원·데이터 주입 (중복 방지)             ← 과제 6
```

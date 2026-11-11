package net.datasa.board.controller;

import net.datasa.board.config.SecurityConfig;
import net.datasa.board.domain.Board;
import net.datasa.board.domain.Member;
import net.datasa.board.exception.BoardNotFoundException;
import net.datasa.board.security.CustomUserDetails;
import net.datasa.board.service.BoardService;
import net.datasa.board.service.FileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;   // Boot 4 패키지
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 컨트롤러 슬라이스 테스트 (step9: Spring Security 적용 후).
 *
 * step8과 달라진 점:
 * <ul>
 *   <li>{@code @WebMvcTest}는 우리가 만든 {@link SecurityConfig}(@Configuration)를 <b>읽지 않는다.</b>
 *       그대로 두면 스프링 부트 기본 보안(모든 요청 인증 필요, HTTP Basic)이 적용돼 목록 조회조차 401이 난다.
 *       그래서 {@code @Import(SecurityConfig.class)}로 실제 인가 규칙을 가져온다.</li>
 *   <li>세션에 회원을 넣던 방식 대신, 인증된 사용자를 요청에 붙인다.
 *       컨트롤러가 {@code @AuthenticationPrincipal CustomUserDetails}를 받으므로
 *       {@code @WithMockUser}(스프링의 기본 User 객체를 만든다)로는 null이 들어온다 →
 *       {@code user(new CustomUserDetails(...))}로 우리 타입의 principal을 넣는다.</li>
 * </ul>
 * {@code spring-boot-starter-security-test}가 있어야 security 테스트 도구를 쓸 수 있다.
 */
@WebMvcTest(BoardController.class)
@Import(SecurityConfig.class)
class BoardControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean BoardService boardService;
    @MockitoBean FileService fileService;

    @Test
    @DisplayName("목록은 로그인 없이 볼 수 있다 (permitAll)")
    void list() throws Exception {
        when(boardService.getList(any())).thenReturn(new PageImpl<>(List.of(board(1L)), PageRequest.of(0, 5), 1));

        mockMvc.perform(get("/boards"))
                .andExpect(status().isOk())
                .andExpect(view().name("board/list"))
                .andExpect(model().attributeExists("page"));
    }

    @Test
    @DisplayName("없는 글은 404")
    void notFound() throws Exception {
        when(boardService.getDetailAndIncreaseView(99L)).thenThrow(new BoardNotFoundException(99L));

        mockMvc.perform(get("/boards/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("비로그인 사용자가 글쓰기 화면에 가면 Security가 로그인 페이지로 보낸다")
    void writeFormRequiresLogin() throws Exception {
        mockMvc.perform(get("/boards/write"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("제목이 비어 있으면 검증에 걸려 글쓰기 화면으로 돌아온다")
    void writeValidation() throws Exception {
        mockMvc.perform(post("/boards/write")
                        .with(loginAs("hong"))
                        .param("title", "")
                        .param("content", "내용"))
                .andExpect(status().isOk())
                .andExpect(view().name("board/write"))
                .andExpect(model().attributeHasFieldErrors("boardForm", "title"));

        verify(boardService, never()).write(any(), anyString(), any());
    }

    @Test
    @DisplayName("로그인한 사용자가 글을 쓰면 작성자가 그 사용자로 저장된다")
    void write() throws Exception {
        when(boardService.write(any(), eq("hong"), any())).thenReturn(board(7L));

        mockMvc.perform(post("/boards/write")
                        .with(loginAs("hong"))
                        .param("title", "제목")
                        .param("content", "내용"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/boards/7"));

        verify(boardService).write(any(), eq("hong"), any());   // principal의 loginId가 작성자로 넘어갔는지
    }

    @Test
    @DisplayName("작성자가 아니면 수정 화면 대신 상세로 돌려보낸다")
    void editFormNotOwner() throws Exception {
        when(boardService.isOwner(1L, "kim")).thenReturn(false);

        mockMvc.perform(get("/boards/1/edit").with(loginAs("kim")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/boards/1"));
    }

    /** 우리 UserDetails 구현체(CustomUserDetails)로 인증된 요청을 만든다 */
    private RequestPostProcessor loginAs(String loginId) {
        Member member = new Member();
        member.setLoginId(loginId);
        member.setName(loginId);
        member.setPassword("{noop}1234");
        member.setRole("ROLE_USER");
        return SecurityMockMvcRequestPostProcessors.user(new CustomUserDetails(member));
    }

    private Board board(Long id) {
        Board b = new Board();
        b.setId(id);
        b.setTitle("제목" + id);
        b.setContent("내용");
        b.setWriter("hong");
        b.setCreatedAt(LocalDateTime.now());
        return b;
    }
}

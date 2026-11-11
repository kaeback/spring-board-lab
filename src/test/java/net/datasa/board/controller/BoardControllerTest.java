package net.datasa.board.controller;

import net.datasa.board.domain.Board;
import net.datasa.board.domain.Member;
import net.datasa.board.exception.BoardNotFoundException;
import net.datasa.board.service.BoardService;
import net.datasa.board.service.FileService;
import net.datasa.board.web.SessionConst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;   // Boot 4 패키지
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
 * 컨트롤러 슬라이스 테스트 — 웹 계층(컨트롤러·인터셉터·필터·Thymeleaf)만 띄운다.
 *
 * 서비스는 {@code @MockitoBean}으로 목을 넣는다. (Boot 4에서 옛 {@code @MockBean}은 제거됐다)
 * {@code MockMvc}는 톰캣 없이 가짜 HTTP 요청을 보내 응답 상태·뷰 이름·모델을 검증한다.
 * WebConfig(WebMvcConfigurer)도 함께 뜨므로 LoginCheckInterceptor 동작까지 확인할 수 있다.
 */
@WebMvcTest(BoardController.class)
class BoardControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean BoardService boardService;
    @MockitoBean FileService fileService;

    @Test
    @DisplayName("목록은 로그인 없이 볼 수 있다")
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
    @DisplayName("로그인하지 않고 글쓰기 화면에 가면 인터셉터가 로그인 페이지로 보낸다")
    void writeFormRequiresLogin() throws Exception {
        mockMvc.perform(get("/boards/write"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?redirectURL=/boards/write"));
    }

    @Test
    @DisplayName("제목이 비어 있으면 검증에 걸려 글쓰기 화면으로 돌아온다")
    void writeValidation() throws Exception {
        mockMvc.perform(post("/boards/write")
                        .session(loginSession())
                        .param("title", "")
                        .param("content", "내용"))
                .andExpect(status().isOk())
                .andExpect(view().name("board/write"))
                .andExpect(model().attributeHasFieldErrors("boardForm", "title"));

        verify(boardService, never()).write(any(), anyString(), any());   // 저장은 호출되지 않아야 한다
    }

    @Test
    @DisplayName("로그인한 사용자가 글을 쓰면 상세 화면으로 리다이렉트된다")
    void write() throws Exception {
        when(boardService.write(any(), anyString(), any())).thenReturn(board(7L));

        mockMvc.perform(post("/boards/write")
                        .session(loginSession())
                        .param("title", "제목")
                        .param("content", "내용"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/boards/7"));
    }

    /** 세션에 로그인 회원을 넣어 둔 가짜 세션 (step2~7의 수동 세션 로그인 방식) */
    private MockHttpSession loginSession() {
        Member member = new Member();
        member.setLoginId("hong");
        member.setName("홍길동");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_MEMBER, member);
        return session;
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

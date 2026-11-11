package net.datasa.board.api;

import net.datasa.board.domain.Member;
import net.datasa.board.dto.CommentResponse;
import net.datasa.board.exception.BoardNotFoundException;
import net.datasa.board.service.CommentService;
import net.datasa.board.web.SessionConst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;   // Boot 4 패키지
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 댓글 REST API 웹 슬라이스 테스트 — 화면 대신 JSON 응답과 상태 코드를 검증한다.
 *
 * {@code jsonPath("$.code")}처럼 JSON 안의 값을 경로로 꺼내 비교한다.
 */
@WebMvcTest(CommentApiController.class)
class CommentApiControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean CommentService commentService;

    @Test
    @DisplayName("댓글 목록은 JSON 배열로 응답한다")
    void list() throws Exception {
        when(commentService.getList(1L)).thenReturn(List.of(
                new CommentResponse(10L, "kim", "첫 댓글", "2026-11-11 10:00")));

        mockMvc.perform(get("/api/boards/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].writer").value("kim"));
    }

    @Test
    @DisplayName("없는 글의 댓글 목록 → 404 JSON")
    void listOfMissingBoard() throws Exception {
        when(commentService.getList(99L)).thenThrow(new BoardNotFoundException(99L));

        mockMvc.perform(get("/api/boards/99/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("로그인하지 않으면 댓글을 쓸 수 없다 → 401")
    void writeWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/boards/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"안녕\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verify(commentService, never()).write(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("로그인하면 댓글이 등록된다 → 201 + Location")
    void write() throws Exception {
        when(commentService.write(1L, "안녕", "hong"))
                .thenReturn(new CommentResponse(5L, "hong", "안녕", "2026-11-11 10:00"));

        mockMvc.perform(post("/api/boards/1/comments")
                        .session(loginSession("hong"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"안녕\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/comments/5"))
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @DisplayName("빈 댓글 → 400 VALIDATION")
    void writeBlank() throws Exception {
        mockMvc.perform(post("/api/boards/1/comments")
                        .session(loginSession("hong"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    @DisplayName("남의 댓글은 지울 수 없다 → 403")
    void deleteOthers() throws Exception {
        when(commentService.isOwner(7L, "hong")).thenReturn(false);

        mockMvc.perform(delete("/api/comments/7").session(loginSession("hong")))
                .andExpect(status().isForbidden());

        verify(commentService, never()).delete(any());
    }

    @Test
    @DisplayName("내 댓글은 지울 수 있다 → 204")
    void deleteMine() throws Exception {
        when(commentService.isOwner(7L, "hong")).thenReturn(true);

        mockMvc.perform(delete("/api/comments/7").session(loginSession("hong")))
                .andExpect(status().isNoContent());

        verify(commentService).delete(7L);
    }

    private MockHttpSession loginSession(String loginId) {
        Member member = new Member();
        member.setLoginId(loginId);
        member.setName(loginId);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_MEMBER, member);
        return session;
    }
}

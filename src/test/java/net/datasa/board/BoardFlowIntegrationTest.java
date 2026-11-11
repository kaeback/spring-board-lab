package net.datasa.board;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;   // Boot 4 패키지
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;      // Boot 4 패키지
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 통합 테스트 — 애플리케이션 전체(컨트롤러~서비스~JPA~DB)를 띄워 "로그인 → 글쓰기 → 상세"를 검증한다.
 *
 * <ul>
 *   <li>{@code @AutoConfigureMockMvc}: Boot 4의 @SpringBootTest는 MockMvc를 자동으로 만들지 않으므로 직접 켠다.</li>
 *   <li>{@code @AutoConfigureTestDatabase}: application.yml의 PostgreSQL 대신 내장 H2를 쓴다.
 *       (실제 PostgreSQL로 검증하고 싶다면 Testcontainers를 쓰는 방법이 있다)</li>
 *   <li>DataInitializer가 그대로 실행되므로 샘플 회원 hong/1234로 로그인할 수 있다.</li>
 * </ul>
 */
@SpringBootTest(properties = "file.upload-dir=build/test-uploads")
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
class BoardFlowIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("로그인 → 글쓰기 → 상세 조회까지 실제 빈과 DB로 동작한다")
    void loginWriteDetail() throws Exception {
        // 로그인 (세션에 회원 저장)
        MvcResult login = mockMvc.perform(post("/login")
                        .param("loginId", "hong")
                        .param("password", "1234"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // 글쓰기 → /boards/{id} 로 리다이렉트
        MvcResult write = mockMvc.perform(post("/boards/write")
                        .session(session)
                        .param("title", "통합 테스트 글")
                        .param("content", "통합 테스트 본문"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String location = write.getResponse().getRedirectedUrl();
        assertThat(location).matches("/boards/\\d+");

        // 상세 화면에 제목과 작성자가 보인다
        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("통합 테스트 글")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("hong")));
    }

    @Test
    @DisplayName("틀린 비밀번호로는 로그인되지 않는다")
    void loginFail() throws Exception {
        mockMvc.perform(post("/login")
                        .param("loginId", "hong")
                        .param("password", "wrong"))
                .andExpect(status().isOk())   // 로그인 화면을 다시 보여 준다
                .andExpect(content().string(org.hamcrest.Matchers.containsString("올바르지 않습니다")));
    }
}

package net.datasa.board;

import net.datasa.board.repository.MemberRepository;
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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 통합 테스트 (step9): 실제 SecurityConfig·CustomUserDetailsService·BCrypt로
 * "폼 로그인 → 글쓰기 → 상세"를 검증한다.
 *
 * {@code formLogin("/login").user("loginId", ...)}는 Security 테스트 도구가 제공하는 로그인 요청 빌더다.
 * 우리 폼의 아이디 필드명이 username이 아니라 loginId이므로 파라미터 이름을 함께 지정한다.
 */
@SpringBootTest(properties = "file.upload-dir=build/test-uploads")
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
class BoardFlowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;

    @Test
    @DisplayName("폼 로그인 → 글쓰기 → 상세 조회")
    void loginWriteDetail() throws Exception {
        MvcResult login = mockMvc.perform(formLogin("/login").user("loginId", "hong").password("password", "1234"))
                .andExpect(authenticated().withUsername("hong"))
                .andExpect(redirectedUrl("/boards"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        MvcResult write = mockMvc.perform(post("/boards/write")
                        .session(session)
                        .param("title", "통합 테스트 글")
                        .param("content", "통합 테스트 본문"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String location = write.getResponse().getRedirectedUrl();
        assertThat(location).matches("/boards/\\d+");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("통합 테스트 글")))
                .andExpect(content().string(containsString("hong")));
    }

    @Test
    @DisplayName("틀린 비밀번호 → 인증 실패, /login?error 로 이동")
    void loginFail() throws Exception {
        mockMvc.perform(formLogin("/login").user("loginId", "hong").password("password", "wrong"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("비밀번호는 BCrypt로 암호화되어 저장된다")
    void passwordIsEncoded() {
        String stored = memberRepository.findByLoginId("hong").orElseThrow().getPassword();
        assertThat(stored).startsWith("$2a$").isNotEqualTo("1234");
    }

    @Test
    @DisplayName("없는 글은 비로그인 사용자에게도 404")
    void notFoundPageForAnonymous() throws Exception {
        // MockMvc는 /error 로 포워드하지 않으므로 상태 코드만 확인한다 (/error permitAll 효과는 실제 서버에서 확인)
        mockMvc.perform(get("/boards/999999"))
                .andExpect(status().isNotFound());
    }
}

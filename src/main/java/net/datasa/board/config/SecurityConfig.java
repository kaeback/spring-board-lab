package net.datasa.board.config;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * Spring Security 설정.
 *
 * step8까지는 세션 로그인과 로그인 체크 인터셉터를 직접 만들었지만,
 * step9에서는 그 일을 모두 Spring Security에 맡긴다.
 * (로그인 처리·세션 관리·인가 검사·비밀번호 검증을 프레임워크가 대신 한다.)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 비밀번호 암호화기 (가입 시 인코딩, 로그인 시 비교에 사용) */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        LoginUrlAuthenticationEntryPoint loginPage = new LoginUrlAuthenticationEntryPoint("/login");
        http
                // CSRF: 학습 편의를 위해 끈다. 이 단계의 핵심은 '인증/인가'다.
                // 실무에서는 켠 채로 두고, 폼에 CSRF 토큰을 함께 보내야 한다(Thymeleaf가 자동 주입).
                .csrf(AbstractHttpConfigurer::disable)
                // 인가 규칙: 위에서부터 먼저 매칭되는 규칙이 적용된다
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스(/css/**, /js/**, /images/**, favicon 등)는 누구나
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        // /error: 예외가 나면 스프링 부트가 이 경로로 오류 화면을 그린다.
                        // 막아 두면 비로그인 사용자는 404/500 화면 대신 로그인 페이지로 이동한다.
                        .requestMatchers("/", "/join", "/login", "/error").permitAll()
                        // 작성·수정·삭제는 로그인 필요 (아래 GET 허용보다 먼저 둔다)
                        .requestMatchers("/boards/write", "/boards/*/edit", "/boards/*/delete").authenticated()
                        // 목록·상세·다운로드 조회는 누구나
                        .requestMatchers(HttpMethod.GET, "/boards", "/boards/*", "/boards/*/download").permitAll()
                        // 댓글 목록 조회(API)도 누구나. 등록·삭제는 아래 anyRequest().authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/boards/*/comments").permitAll()
                        .anyRequest().authenticated()
                )
                // 폼 로그인: 우리가 만든 /login 페이지 사용
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")     // 이 URL로 POST되면 Security가 인증 처리
                        .usernameParameter("loginId")     // 폼의 아이디 필드명
                        .passwordParameter("password")    // 폼의 비밀번호 필드명
                        .successHandler(loginSuccessHandler())   // 로그인 후 이동할 곳 (아래 메서드)
                        .permitAll()
                )
                // 로그아웃
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")    // 로그인 화면에 "로그아웃 되었습니다" 표시
                )
                // 로그인하지 않고 API(/api/**)를 부르면 로그인 화면으로 redirect하지 않고 401 JSON으로 응답한다.
                // (자바스크립트 fetch에게 HTML 로그인 화면은 쓸모가 없다)
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"로그인이 필요합니다.\"}");
                    } else {
                        loginPage.commence(request, response, authException);   // 화면은 원래대로 로그인 페이지로
                    }
                }));
        return http.build();
    }

    /**
     * 로그인 성공 후 이동할 곳.
     * <ol>
     *   <li>로그인 폼이 redirectURL(우리 사이트 안의 경로)을 보냈으면 그곳으로 — 상세 화면의 "로그인" 링크처럼
     *       돌아갈 곳을 직접 넘긴 경우</li>
     *   <li>아니면 Security 기본 동작 — 로그인이 필요한 주소에서 막혀 왔다면 그 주소로, 처음부터 로그인 화면에 왔다면 /boards로</li>
     * </ol>
     * redirectURL은 "/"로 시작하는 경로만 받는다. "https://다른사이트"로 보내는 공격(오픈 리다이렉트)을 막기 위해서다.
     */
    private AuthenticationSuccessHandler loginSuccessHandler() {
        SavedRequestAwareAuthenticationSuccessHandler savedRequest = new SavedRequestAwareAuthenticationSuccessHandler();
        savedRequest.setDefaultTargetUrl("/boards");
        return (request, response, authentication) -> {
            String redirectURL = request.getParameter("redirectURL");
            if (redirectURL != null && redirectURL.startsWith("/") && !redirectURL.startsWith("//")) {
                response.sendRedirect(redirectURL);
            } else {
                savedRequest.onAuthenticationSuccess(request, response, authentication);
            }
        };
    }
}

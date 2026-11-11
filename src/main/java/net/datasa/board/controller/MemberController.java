package net.datasa.board.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import net.datasa.board.domain.Member;
import net.datasa.board.dto.LoginForm;
import net.datasa.board.dto.MemberJoinForm;
import net.datasa.board.service.MemberService;
import net.datasa.board.web.SessionConst;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * 회원가입 · 로그인 · 로그아웃 컨트롤러.
 *
 * step2는 Spring Security 없이 <b>세션(HttpSession)</b>으로 로그인 상태를 직접 관리한다.
 * 입력값 검증은 아직 없다. (검증은 step3에서 추가한다.)
 */
@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // ---------- 회원가입 ----------

    @GetMapping("/join")
    public String joinForm(Model model) {
        model.addAttribute("joinForm", new MemberJoinForm());
        return "member/join";
    }

    @PostMapping("/join")
    public String join(@ModelAttribute("joinForm") MemberJoinForm joinForm, Model model) {
        if (memberService.isDuplicateLoginId(joinForm.getLoginId())) {
            model.addAttribute("error", "이미 사용 중인 아이디입니다");
            return "member/join";
        }
        memberService.join(joinForm);
        return "redirect:/login";
    }

    // ---------- 로그인 / 로그아웃 ----------

    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "member/login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute("loginForm") LoginForm loginForm,
                        @RequestParam(defaultValue = "/boards") String redirectURL,
                        HttpServletRequest request,
                        Model model) {
        Optional<Member> loginMember = memberService.login(loginForm.getLoginId(), loginForm.getPassword());
        if (loginMember.isEmpty()) {
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다");
            return "member/login";
        }

        // 로그인 성공 → 세션에 회원 정보 보관
        // 세션 고정(session fixation) 공격 방지: 로그인 전 세션 ID를 버리고 새 ID를 발급받는다.
        // (step9에서 Spring Security로 바꾸면 이 처리를 프레임워크가 자동으로 해 준다)
        HttpSession session = request.getSession();
        request.changeSessionId();
        session.setAttribute(SessionConst.LOGIN_MEMBER, loginMember.get());
        return "redirect:" + safeRedirect(redirectURL);
    }

    /**
     * 로그인 후 돌아갈 주소 검증 — 오픈 리다이렉트(open redirect) 방지.
     * redirectURL은 사용자가 마음대로 바꿀 수 있는 값이라, 그대로 쓰면
     * {@code /login?redirectURL=https://evil.com} 같은 링크로 피싱 사이트에 보낼 수 있다.
     * 우리 사이트 내부 경로("/"로 시작, "//"·"/\"는 제외)만 허용한다.
     */
    private String safeRedirect(String redirectURL) {
        if (redirectURL == null || !redirectURL.startsWith("/")
                || redirectURL.startsWith("//") || redirectURL.startsWith("/\\")) {
            return "/boards";
        }
        return redirectURL;
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();   // 세션 폐기 → 로그아웃
        }
        return "redirect:/boards";
    }
}

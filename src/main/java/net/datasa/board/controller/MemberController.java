package net.datasa.board.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.datasa.board.dto.MemberJoinForm;
import net.datasa.board.service.MemberService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 회원가입 · 로그인 페이지 컨트롤러.
 *
 * step9: 로그인(POST)·로그아웃은 Spring Security가 처리하므로 여기서는
 * <b>로그인 화면 표시(GET)</b>와 <b>회원가입</b>만 담당한다.
 */
@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /** 로그인 화면 (실제 인증 처리는 Security가 POST /login에서 수행) */
    @GetMapping("/login")
    public String loginForm() {
        return "member/login";
    }

    @GetMapping("/join")
    public String joinForm(Model model) {
        model.addAttribute("joinForm", new MemberJoinForm());
        return "member/join";
    }

    @PostMapping("/join")
    public String join(@Valid @ModelAttribute("joinForm") MemberJoinForm joinForm,
                       BindingResult bindingResult) {
        if (memberService.isDuplicateLoginId(joinForm.getLoginId())) {
            bindingResult.rejectValue("loginId", "duplicate", "이미 사용 중인 아이디입니다");
        }
        if (bindingResult.hasErrors()) {
            return "member/join";
        }
        memberService.join(joinForm);
        return "redirect:/login";
    }
}

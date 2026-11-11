package net.datasa.board.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.datasa.board.domain.Board;
import net.datasa.board.domain.Member;
import net.datasa.board.dto.BoardForm;
import net.datasa.board.service.BoardService;
import net.datasa.board.web.SessionConst;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * 게시판 화면 컨트롤러.
 *
 * step2: 작성·수정·삭제는 로그인이 필요하다. 아직 인터셉터가 없으므로
 * 각 메서드에서 세션을 <b>직접 확인</b>한다. (step5에서 이 반복을 인터셉터로 뽑아낸다.)
 * step3부터 입력값은 @Valid + BindingResult로 검증한다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/boards")
public class BoardController {

    private final BoardService boardService;

    /** 세션에서 로그인 회원을 꺼낸다. 없으면 null. */
    private Member loginMember(HttpSession session) {
        return (Member) session.getAttribute(SessionConst.LOGIN_MEMBER);
    }

    /** 글 목록 (로그인 없이 누구나) */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("boards", boardService.getList());
        return "board/list";
    }

    /** 글쓰기 화면 (로그인 필요) */
    @GetMapping("/write")
    public String writeForm(HttpSession session, Model model) {
        if (loginMember(session) == null) {
            return "redirect:/login?redirectURL=/boards/write";
        }
        model.addAttribute("boardForm", new BoardForm());
        return "board/write";
    }

    /** 글쓰기 처리 (로그인 필요) */
    @PostMapping("/write")
    public String write(@Valid @ModelAttribute("boardForm") BoardForm form,
                        BindingResult bindingResult,
                        HttpSession session) {
        Member login = loginMember(session);
        if (login == null) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            return "board/write";
        }
        Board board = boardService.write(form, login.getLoginId());
        return "redirect:/boards/" + board.getId();
    }

    /** 글 상세 (조회수 증가) */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("board", boardService.getDetailAndIncreaseView(id));
        return "board/detail";
    }

    /** 글 수정 화면 (작성자 본인만) */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        Member login = loginMember(session);
        if (login == null) {
            return "redirect:/login";
        }
        if (!boardService.isOwner(id, login.getLoginId())) {
            return "redirect:/boards/" + id;   // 권한 없으면 상세로 되돌림
        }
        Board board = boardService.getById(id);
        BoardForm form = new BoardForm();
        form.setTitle(board.getTitle());
        form.setContent(board.getContent());
        model.addAttribute("boardForm", form);
        model.addAttribute("boardId", id);
        return "board/edit";
    }

    /** 글 수정 처리 (작성자 본인만) */
    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("boardForm") BoardForm form,
                       BindingResult bindingResult,
                       HttpSession session,
                       Model model) {
        Member login = loginMember(session);
        if (login == null) {
            return "redirect:/login";
        }
        if (!boardService.isOwner(id, login.getLoginId())) {
            return "redirect:/boards/" + id;
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("boardId", id);
            return "board/edit";
        }
        boardService.update(id, form);
        return "redirect:/boards/" + id;
    }

    /** 글 삭제 (작성자 본인만) */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session) {
        Member login = loginMember(session);
        if (login == null) {
            return "redirect:/login";
        }
        if (boardService.isOwner(id, login.getLoginId())) {
            boardService.delete(id);
        }
        return "redirect:/boards";
    }
}

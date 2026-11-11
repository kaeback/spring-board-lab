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
 * step5: 로그인 여부 확인은 {@code LoginCheckInterceptor}가 담당한다.
 * 따라서 컨트롤러에는 "로그인 안 했으면 redirect" 코드가 더 이상 없다.
 * (단, "작성자 본인인지"를 따지는 권한 검사는 인터셉터가 못 하므로 여기 남는다.)
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/boards")
public class BoardController {

    private final BoardService boardService;

    /** 세션에서 로그인 회원을 꺼낸다. (보호 경로에서는 인터셉터가 보장하므로 null이 아니다.) */
    private Member loginMember(HttpSession session) {
        return (Member) session.getAttribute(SessionConst.LOGIN_MEMBER);
    }

    /** 글 목록 (누구나) */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("boards", boardService.getList());
        return "board/list";
    }

    /** 글쓰기 화면 (인터셉터가 로그인 보장) */
    @GetMapping("/write")
    public String writeForm(Model model) {
        model.addAttribute("boardForm", new BoardForm());
        return "board/write";
    }

    /** 글쓰기 처리 */
    @PostMapping("/write")
    public String write(@Valid @ModelAttribute("boardForm") BoardForm form,
                        BindingResult bindingResult,
                        HttpSession session) {
        if (bindingResult.hasErrors()) {
            return "board/write";
        }
        Board board = boardService.write(form, loginMember(session).getLoginId());
        return "redirect:/boards/" + board.getId();
    }

    /** 글 상세 (조회수 증가, 누구나) */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("board", boardService.getDetailAndIncreaseView(id));
        return "board/detail";
    }

    /** 글 수정 화면 (작성자 본인만) */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        if (!boardService.isOwner(id, loginMember(session).getLoginId())) {
            return "redirect:/boards/" + id;
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
        if (!boardService.isOwner(id, loginMember(session).getLoginId())) {
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
        if (boardService.isOwner(id, loginMember(session).getLoginId())) {
            boardService.delete(id);
        }
        return "redirect:/boards";
    }
}

package net.datasa.board.controller;

import lombok.RequiredArgsConstructor;
import net.datasa.board.domain.Board;
import net.datasa.board.dto.BoardForm;
import net.datasa.board.service.BoardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * 게시판 화면 컨트롤러.
 *
 * 요청 URL을 받아 서비스를 호출하고, 결과를 Model에 담아 Thymeleaf 뷰 이름을 반환한다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/boards")
public class BoardController {

    private final BoardService boardService;

    /** 글 목록 */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("boards", boardService.getList());
        return "board/list";
    }

    /** 글쓰기 화면 */
    @GetMapping("/write")
    public String writeForm(Model model) {
        model.addAttribute("boardForm", new BoardForm());
        return "board/write";
    }

    /** 글쓰기 처리 → 작성된 글 상세로 이동 */
    @PostMapping("/write")
    public String write(@ModelAttribute("boardForm") BoardForm form) {
        Board board = boardService.write(form);
        return "redirect:/boards/" + board.getId();
    }

    /** 글 상세 (조회수 증가) */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("board", boardService.getDetailAndIncreaseView(id));
        return "board/detail";
    }

    /** 글 수정 화면 — 기존 값을 폼에 채워서 보여준다 */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Board board = boardService.getById(id);
        BoardForm form = new BoardForm();
        form.setTitle(board.getTitle());
        form.setContent(board.getContent());
        form.setWriter(board.getWriter());
        model.addAttribute("boardForm", form);
        model.addAttribute("boardId", id);
        return "board/edit";
    }

    /** 글 수정 처리 */
    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute("boardForm") BoardForm form) {
        boardService.update(id, form);
        return "redirect:/boards/" + id;
    }

    /** 글 삭제 */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        boardService.delete(id);
        return "redirect:/boards";
    }
}

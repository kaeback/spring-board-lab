package net.datasa.board.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.datasa.board.domain.Board;
import net.datasa.board.domain.Member;
import net.datasa.board.dto.BoardForm;
import net.datasa.board.service.BoardService;
import net.datasa.board.service.FileService;
import net.datasa.board.web.SessionConst;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 게시판 화면 컨트롤러.
 *
 * step6: 목록 페이징(Pageable), 첨부파일 업로드/다운로드를 추가했다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/boards")
public class BoardController {

    private final BoardService boardService;
    private final FileService fileService;

    /** 세션에서 로그인 회원을 꺼낸다. (보호 경로에서는 인터셉터가 보장하므로 null이 아니다.) */
    private Member loginMember(HttpSession session) {
        return (Member) session.getAttribute(SessionConst.LOGIN_MEMBER);
    }

    /** 글 목록 + 페이징 (누구나). 기본 5건씩, id 내림차순(최신순). */
    @GetMapping
    public String list(@PageableDefault(size = 5, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                       Model model) {
        model.addAttribute("page", boardService.getList(pageable));
        return "board/list";
    }

    /** 글쓰기 화면 (인터셉터가 로그인 보장) */
    @GetMapping("/write")
    public String writeForm(Model model) {
        model.addAttribute("boardForm", new BoardForm());
        return "board/write";
    }

    /** 글쓰기 처리 (첨부파일 포함) */
    @PostMapping("/write")
    public String write(@Valid @ModelAttribute("boardForm") BoardForm form,
                        BindingResult bindingResult,
                        @RequestParam(value = "file", required = false) MultipartFile file,
                        HttpSession session) {
        if (bindingResult.hasErrors()) {
            return "board/write";
        }
        Board board = boardService.write(form, loginMember(session).getLoginId(), file);
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
        model.addAttribute("board", board);   // 현재 첨부파일명 표시용
        return "board/edit";
    }

    /** 글 수정 처리 (작성자 본인만) */
    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("boardForm") BoardForm form,
                       BindingResult bindingResult,
                       @RequestParam(value = "file", required = false) MultipartFile file,
                       HttpSession session,
                       Model model) {
        if (!boardService.isOwner(id, loginMember(session).getLoginId())) {
            return "redirect:/boards/" + id;
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("board", boardService.getById(id));
            return "board/edit";
        }
        boardService.update(id, form, file);
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

    /** 첨부파일 다운로드 (누구나) */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Board board = boardService.getById(id);
        if (!board.isHasAttachment()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = fileService.loadAsResource(board.getStoredFileName());
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        // 한글 파일명이 깨지지 않도록 인코딩
        String encodedName = URLEncoder.encode(board.getOriginalFileName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedName + "\"; filename*=UTF-8''" + encodedName)
                .body(resource);
    }
}

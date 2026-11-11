package net.datasa.board.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.datasa.board.domain.Board;
import net.datasa.board.dto.BoardForm;
import net.datasa.board.security.CustomUserDetails;
import net.datasa.board.service.BoardService;
import net.datasa.board.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * step9: 로그인 여부는 Spring Security가 보장한다.
 * 로그인 사용자 정보는 {@code @AuthenticationPrincipal}로 주입받는다.
 * (작성자 본인 여부 검사는 도메인 규칙이라 여전히 컨트롤러/서비스에 있다.)
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/boards")
public class BoardController {

    private final BoardService boardService;
    private final FileService fileService;

    /** 글 목록 + 페이징 (누구나). 기본 5건씩, id 내림차순(최신순). */
    @GetMapping
    public String list(@PageableDefault(size = 5, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                       Model model) {
        model.addAttribute("page", boardService.getList(pageable));
        return "board/list";
    }

    /** 글쓰기 화면 (Security가 로그인 보장) */
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
                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (bindingResult.hasErrors()) {
            return "board/write";
        }
        Board board = boardService.write(form, userDetails.getUsername(), file);
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
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal CustomUserDetails userDetails,
                           Model model) {
        if (!boardService.isOwner(id, userDetails.getUsername())) {
            return "redirect:/boards/" + id;
        }
        Board board = boardService.getById(id);
        BoardForm form = new BoardForm();
        form.setTitle(board.getTitle());
        form.setContent(board.getContent());
        model.addAttribute("boardForm", form);
        model.addAttribute("board", board);
        return "board/edit";
    }

    /** 글 수정 처리 (작성자 본인만) */
    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("boardForm") BoardForm form,
                       BindingResult bindingResult,
                       @RequestParam(value = "file", required = false) MultipartFile file,
                       @AuthenticationPrincipal CustomUserDetails userDetails,
                       Model model) {
        if (!boardService.isOwner(id, userDetails.getUsername())) {
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
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (boardService.isOwner(id, userDetails.getUsername())) {
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
        String encodedName = URLEncoder.encode(board.getOriginalFileName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedName + "\"; filename*=UTF-8''" + encodedName)
                .body(resource);
    }
}

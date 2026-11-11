package net.datasa.board.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 루트("/") 진입 시 게시판 목록으로 보내는 컨트롤러.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/boards";
    }
}

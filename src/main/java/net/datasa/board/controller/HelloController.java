package net.datasa.board.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 프로젝트가 제대로 만들어졌는지 확인하는 첫 컨트롤러.
 * http://localhost:8080/hello 로 접속하면 templates/hello.html 이 보인다.
 */
@Controller
public class HelloController {

    @GetMapping("hello")
    public String hello(Model model) {
        model.addAttribute("data", "hello");
        return "hello";   // templates/hello.html 을 렌더링
    }
}

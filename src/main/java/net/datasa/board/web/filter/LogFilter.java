package net.datasa.board.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.UUID;

/**
 * 모든 요청의 시작/끝을 로그로 남기는 필터.
 *
 * 필터는 <b>서블릿 앞단(디스패처서블릿 이전)</b>에서 동작한다.
 * 요청마다 짧은 식별자(uuid)를 부여해 한 요청의 흐름을 추적할 수 있다.
 */
@Slf4j
public class LogFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        try {
            log.info("[{}] REQUEST  {} {}", uuid, httpRequest.getMethod(), uri);
            chain.doFilter(request, response);   // 다음 필터 또는 서블릿으로 진행
        } finally {
            log.info("[{}] RESPONSE {}", uuid, uri);
        }
    }
}

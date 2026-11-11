package net.datasa.board.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import net.datasa.board.web.SessionConst;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 로그인이 필요한 요청을 막는 인터셉터.
 *
 * 인터셉터는 <b>디스패처서블릿과 컨트롤러 사이</b>에서 동작한다.
 * 로그인하지 않았으면 컨트롤러를 호출하지 않고 로그인 페이지로 돌려보낸다.
 *
 * step2~4에서 컨트롤러마다 반복하던 "세션 확인 후 redirect" 코드를 이 한 곳으로 모았다.
 */
@Slf4j
public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(SessionConst.LOGIN_MEMBER) == null) {
            String requestURI = request.getRequestURI();
            log.info("로그인 필요 → 차단: {}", requestURI);
            // 로그인 후 원래 가려던 곳으로 돌아오도록 redirectURL을 함께 넘긴다
            response.sendRedirect("/login?redirectURL=" + requestURI);
            return false;   // false면 컨트롤러를 실행하지 않는다
        }
        return true;
    }
}

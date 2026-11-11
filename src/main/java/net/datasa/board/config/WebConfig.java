package net.datasa.board.config;

import net.datasa.board.web.filter.LogFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 웹 공통 설정.
 *
 * step9부터 로그인 체크는 Spring Security가 담당하므로 LoginCheckInterceptor는 제거했다.
 * 로그 필터는 보안과 무관한 공통 관심사라 그대로 둔다.
 */
@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<LogFilter> logFilter() {
        FilterRegistrationBean<LogFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new LogFilter());
        registrationBean.setOrder(1);
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}

package net.datasa.board.config;

import net.datasa.board.web.filter.LogFilter;
import net.datasa.board.web.interceptor.LoginCheckInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 필터·인터셉터 등록 설정.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 로그인 체크 인터셉터: 작성·수정·삭제 경로에만 적용 (목록·상세는 누구나 열람) */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginCheckInterceptor())
                .order(1)
                .addPathPatterns("/boards/write", "/boards/*/edit", "/boards/*/delete");
    }

    /** 로그 필터: 모든 요청에 적용 */
    @Bean
    public FilterRegistrationBean<LogFilter> logFilter() {
        FilterRegistrationBean<LogFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new LogFilter());
        registrationBean.setOrder(1);
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}

package org.dynamcorp.handsaiv2.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FilterConfig {

    private final TokenAuthFilter tokenAuthFilter;
    private final AdminSessionFilter adminSessionFilter;

    /**
     * AdminSessionFilter runs first (order 1) — protects/admin/** with session
     * cookie.
     * TokenAuthFilter runs second (order 2) — protects MCP/API endpoints with PAT
     * token.
     */
    @Bean
    public FilterRegistrationBean<AdminSessionFilter> adminSessionFilterRegistration() {
        FilterRegistrationBean<AdminSessionFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(adminSessionFilter);
        bean.addUrlPatterns("/admin/*");
        bean.setOrder(1);
        bean.setName("adminSessionFilter");
        return bean;
    }

    @Bean
    public FilterRegistrationBean<TokenAuthFilter> tokenAuthFilterRegistration() {
        FilterRegistrationBean<TokenAuthFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(tokenAuthFilter);
        bean.addUrlPatterns("/mcp/*");
        bean.setOrder(2);
        bean.setName("tokenAuthFilter");
        return bean;
    }
}

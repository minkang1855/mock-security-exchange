package edu.cnu.swacademy.security.common;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

  @Bean
  public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilter(JwtAuthenticationFilter jwtAuthenticationFilter) {
    FilterRegistrationBean<JwtAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(jwtAuthenticationFilter);
    
    // JWT 인증이 필요한 경로들
    registrationBean.addUrlPatterns("/api/v1/cash-wallet/*");
    registrationBean.addUrlPatterns("/api/v1/stock-wallet/*");
    registrationBean.addUrlPatterns("/api/v1/order/*");
    registrationBean.addUrlPatterns("/api/v1/orderbook/*");
    
    registrationBean.setOrder(1);
    return registrationBean;
  }
}

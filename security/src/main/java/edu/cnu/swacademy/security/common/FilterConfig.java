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
    // Cash Wallet API
    registrationBean.addUrlPatterns("/api/v1/cash-wallet");
    registrationBean.addUrlPatterns("/api/v1/cash-wallet/deposit");
    registrationBean.addUrlPatterns("/api/v1/cash-wallet/withdrawal");
    registrationBean.addUrlPatterns("/api/v1/cash-wallet/balance");
    registrationBean.addUrlPatterns("/api/v1/cash-wallet/histories");

    registrationBean.addUrlPatterns("/api/v1/stock-wallet");
    registrationBean.addUrlPatterns("/api/v1/stock-wallet/balance/*");

    registrationBean.addUrlPatterns("/api/v1/order/*");
    registrationBean.addUrlPatterns("/api/v1/orderbook/*");

    registrationBean.addUrlPatterns("/api/v1/match/*");
    
    registrationBean.setOrder(1);
    return registrationBean;
  }
}

package top.lvpi.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册Sa-Token拦截器
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/**") // 拦截所有请求
                .excludePathPatterns("/user/login", "/user/register"); // 排除登录和注册接口
    }
} 
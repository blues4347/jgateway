package me.lijf.jgateway.config;

import me.lijf.jgateway.exception.JExceptionHandler;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.List;

//@Configuration
//@EnableConfigurationProperties({ServerProperties.class, WebProperties.Resources.class})
public class JErrorHandlerConfig {
    private final ServerProperties serverProperties;
    private final WebProperties.Resources resources;
    private final ApplicationContext applicationContext;
    private final ServerCodecConfigurer codecConfigurer;
    private final List<ViewResolver> viewResolvers;

    public JErrorHandlerConfig(ServerProperties serverProperties,
                               WebProperties.Resources resources,
                               ApplicationContext applicationContext,
                               ServerCodecConfigurer codecConfigurer,
                               List<ViewResolver> viewResolvers) {
        this.serverProperties = serverProperties;
        this.resources = resources;
        this.applicationContext = applicationContext;
        this.codecConfigurer = codecConfigurer;
        this.viewResolvers = viewResolvers;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ErrorWebExceptionHandler errorWebExceptionHandler(ErrorAttributes errorAttributes){
        JExceptionHandler handler=new JExceptionHandler(
                errorAttributes,this.resources,this.serverProperties.getError(),this.applicationContext);
        handler.setViewResolvers(this.viewResolvers);
        handler.setMessageWriters(this.codecConfigurer.getWriters());
        handler.setMessageReaders(this.codecConfigurer.getReaders());
        return handler;
    }

}

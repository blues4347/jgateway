package me.lijf.jgateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

//@Component
public class JGlobalLoggingFilter implements GlobalFilter, Ordered {
    private Logger logger= LoggerFactory.getLogger(JGlobalLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String appkey=exchange.getRequest().getQueryParams().getFirst("appkey");
        if(StringUtils.isEmpty(appkey)){
            logger.warn("Annoymous@{} attempts to access {}",exchange.getRequest().getRemoteAddress().getHostString(),exchange.getRequest().getPath());
            ServerHttpResponse response=exchange.getResponse();
            response.setStatusCode(HttpStatus.NON_AUTHORITATIVE_INFORMATION);
            String text="No appkey provided.";
            DataBuffer msg=response.bufferFactory().wrap(text.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(msg));
        }
        logger.info("{}@{} attempts to access {}",appkey,exchange.getRequest().getRemoteAddress().getHostString(),exchange.getRequest().getPath());
        //继续执行其他filter
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}

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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

//@Component
public class JGlobalAuthFilter implements GlobalFilter, Ordered {
    private Logger logger= LoggerFactory.getLogger(JGlobalLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String appkey=exchange.getRequest().getQueryParams().getFirst("appkey");
        String secret=exchange.getRequest().getQueryParams().getFirst("secret");
        if("lijf".equals(appkey) && "1q2w3e4r".equals(secret)) {
            logger.info("{} request accepted.",appkey);
            return chain.filter(exchange);
        }
        logger.warn("Appkey [{}] mismatches secret provided.",appkey);
        ServerHttpResponse response=exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        String text="Appkey and secret mismatched";
        DataBuffer msg=response.bufferFactory().wrap(text.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(msg));
    }

    @Override
    public int getOrder() {
        return 1;
    }
}

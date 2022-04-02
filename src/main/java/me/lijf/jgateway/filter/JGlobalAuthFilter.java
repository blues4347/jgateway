package me.lijf.jgateway.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.lijf.jgateway.entity.JAppAccount;
import me.lijf.jgateway.entity.JRouteDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import sun.misc.BASE64Decoder;

import javax.annotation.PostConstruct;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class JGlobalAuthFilter implements GlobalFilter, Ordered {
    @Autowired
    ObjectMapper mapper;

    private String AUTH_METHOD="Basic";
    private List<JAppAccount> apps=new ArrayList<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders headers=exchange.getRequest().getHeaders();
        try{
            if(!headers.containsKey("Authorization")) throw new RuntimeException("No authorization provided.");
            String encrypted=headers.getFirst("Authorization").replace(AUTH_METHOD,"").trim();
            BASE64Decoder decoder=new BASE64Decoder();
            String authStr=new String(decoder.decodeBuffer(encrypted),"UTF-8");
            String[] authentication=authStr.split(":");
            String appkey=authentication[0],secret=authentication[1];
            if(!validate(appkey,secret)) throw new RuntimeException("Authorisation failed.");

            ServerHttpRequestDecorator requestDecorator = new ServerHttpRequestDecorator(exchange.getRequest()){
                //HttpHeader染色
                @Override
                public HttpHeaders getHeaders(){
                    HttpHeaders httpHeaders=new HttpHeaders();
                    httpHeaders.putAll(exchange.getRequest().getHeaders());
                    httpHeaders.set("5i5j-appkey",appkey);
                    return httpHeaders;
                }
            };
            return chain.filter(exchange.mutate().request(requestDecorator).build());
        }catch (RuntimeException e){
            return this.exceptionHandler(exchange,HttpStatus.UNAUTHORIZED,e);
        }catch (Exception e){
            return this.exceptionHandler(exchange,HttpStatus.INSUFFICIENT_STORAGE,e);
        }
    }

    private boolean validate(String appkey,String secret){
        return apps.stream().filter(x-> x.getAppId().equals(appkey) && x.getSecretKey().equals(secret)).findAny().isPresent();
    }

    private Mono<Void> exceptionHandler(ServerWebExchange exchange,HttpStatus status,Exception e){
        ServerHttpResponse response=exchange.getResponse();
        response.setStatusCode(status);
        DataBuffer msg=response.bufferFactory().wrap(e.getLocalizedMessage().getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(msg));
    }

    @Override
    public int getOrder() {
        return -10000;
    }

    @PostConstruct
    public void init() throws IOException {
        log.info("开始加载应用账号...");
        this.loadFromFile();
        log.info("应用账号加载完毕，加载数量：{}",apps.size());
    }

    private void loadFromFile() throws IOException {
        ClassPathResource resource=new ClassPathResource("apps.json");
        FileReader reader=new FileReader(resource.getFile());
        apps= mapper.readValue(reader,new TypeReference<List<JAppAccount>>() { });
    }
}

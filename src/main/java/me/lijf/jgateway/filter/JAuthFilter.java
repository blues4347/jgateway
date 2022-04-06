package me.lijf.jgateway.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.lijf.jgateway.entity.JConsumer;
import me.lijf.jgateway.entity.JProvider;
import me.lijf.jgateway.service.JDynamicRouteServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import sun.misc.BASE64Decoder;

import javax.annotation.PostConstruct;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class JAuthFilter implements GlobalFilter, Ordered {
    @Autowired
    ObjectMapper mapper;
    @Autowired
    JDynamicRouteServiceImpl routeService;

    private String AUTH_METHOD="Basic";
    private List<JConsumer> apps=new ArrayList<>();
    private List<JProvider> providers=new ArrayList<>();
    private Map<String,String> tokens=new HashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request= exchange.getRequest();
        HttpHeaders headers=request.getHeaders();
        String path=request.getPath().value();
        try{
            if(!headers.containsKey("Authorization")) throw new RuntimeException("No authorization provided.");
            String encrypted=headers.getFirst("Authorization").replace(AUTH_METHOD,"").trim();
            BASE64Decoder decoder=new BASE64Decoder();
            String authStr=new String(decoder.decodeBuffer(encrypted),"UTF-8");
            String[] authentication=authStr.split(":");
            String appkey=authentication[0],secret=authentication[1];
            if(!validate(appkey,secret)) throw new RuntimeException("Authorisation failed.");

            Optional<Map.Entry<String,String>> entry=routeService.getPatterns().entrySet().stream().filter(pattern->path.startsWith(pattern.getValue().replace("/**",""))).findFirst();
            if(!entry.isPresent()) throw new RuntimeException("请求的路径"+path+"没有对应的微服务");
            String pattern=entry.get().getValue();

            ServerHttpRequestDecorator requestDecorator = new ServerHttpRequestDecorator(exchange.getRequest()){
                //HttpHeader染色
                @Override
                public HttpHeaders getHeaders(){
                    HttpHeaders httpHeaders=new HttpHeaders();
                    httpHeaders.putAll(exchange.getRequest().getHeaders());
                    httpHeaders.set("appkey",appkey);
                    httpHeaders.set("token",tokens.get(pattern));
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
        this.loadProviderFromFile();
        this.loadRequestMapping();
        this.loadConsumerFromFile();
    }

    private void loadProviderFromFile() throws IOException {
        log.info("开始注册微服务...");
        ClassPathResource resource=new ClassPathResource("providers.json");
        FileReader reader=new FileReader(resource.getFile());
        providers= mapper.readValue(reader,new TypeReference<List<JProvider>>() { });

        //校验微服务的route是否存在
        List<Route> routeList=(List<Route>) routeService.getRoutes().get("路由");
        Stream<String> routes=routeList.stream().map(route -> route.getId());
        Stream<String> routesInUse=providers.stream().map(provider->provider.getRoute());
        if(!outOfSet(routesInUse,routes)) {
            log.error("微服务使用了不存在的路由。");
            System.exit(1);
        }
        log.info("微服务注册完毕，注册数量：{}",providers.size());
    }

    private boolean outOfSet(Stream<String> inUses,Stream<String> data){
        List<String> exceptions=inUses.filter(inUse->!data.collect(Collectors.toList()).contains(inUse)).collect(Collectors.toList());
        if(!exceptions.isEmpty())
            exceptions.stream().forEach(e->{log.error("非法数据：{}",e);});
        return exceptions.isEmpty();
    }

    private void loadRequestMapping(){
        Map<String,String> patterns=routeService.getPatterns();
        providers.stream().filter(provider->patterns.containsKey(provider.getRoute())).forEach(provider->{
            String route=provider.getRoute();
            tokens.put(patterns.get(route),provider.getToken());
        });
    }

    private void loadConsumerFromFile() throws IOException {
        Stream<String> allProviders=providers.stream().map(provider-> provider.getName());
        log.info("开始加载应用账号...");
        ClassPathResource resource=new ClassPathResource("apps.json");
        FileReader reader=new FileReader(resource.getFile());
        apps= mapper.readValue(reader,new TypeReference<List<JConsumer>>() { });
        //校验权限数据中引用的微服务是否存在
        apps.stream().forEach(app->{
            if(!outOfSet(app.getAuthorities().stream(),allProviders)){
                log.error("应用账号使用了不存在的微服务。");
                System.exit(1);
            };
        });
        log.info("应用账号加载完毕，加载数量：{}",apps.size());
    }
}

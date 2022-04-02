package me.lijf.jgateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.lijf.jgateway.entity.JFilterDefinition;
import me.lijf.jgateway.entity.JPredicateDefinition;
import me.lijf.jgateway.entity.JRouteDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.*;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.*;

@Slf4j
@Service
public class JDynamicRouteServiceImpl implements ApplicationEventPublisherAware {
    @Autowired
    ObjectMapper mapper;
    @Autowired
    private RouteDefinitionRepository repository;
    @Autowired
    private RouteDefinitionWriter writer;

    private ApplicationEventPublisher publisher;

    @Autowired
    private RouteLocator locator;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher=applicationEventPublisher;
    }

    public Map<String,Object> getRoutes(){
        Map<String,Object> ret=new LinkedHashMap<>();
        ret.put("路由定义", this.getAllRouteDefinitions());
        ret.put("路由",this.getAllRoutes());
        return ret;
    }

    private List<RouteDefinition> getAllRouteDefinitions(){
        List<RouteDefinition> rtn=new ArrayList<>();
        repository.getRouteDefinitions().subscribe(result->{
            rtn.add(result);
        });
        return rtn;
    }

    private List<Route> getAllRoutes(){
        List<Route> rtn=new ArrayList<>();
        locator.getRoutes().subscribe(result->{
            rtn.add(result);
        });
        return rtn;
    }

    public void add(JRouteDefinition definition){
        //不加subscribe时，无法执行后面的publish。
        writer.save(Mono.just(this.assembleRouteDefinition(definition))).subscribe();
        publisher.publishEvent(new RefreshRoutesEvent(this));
    }

    private RouteDefinition assembleRouteDefinition(JRouteDefinition definition) {
        RouteDefinition ret = new RouteDefinition();
        ret.setId(definition.getId());
        ret.setOrder(definition.getOrder());

        //设置断言
        List<PredicateDefinition> pdList=new ArrayList<>();
        List<JPredicateDefinition> predicateDefinitions=definition.getPredicates();
        for (JPredicateDefinition pd: predicateDefinitions) {
            PredicateDefinition predicate = new PredicateDefinition();
            predicate.setArgs(pd.getArgs());
            predicate.setName(pd.getName());
            pdList.add(predicate);
        }
        ret.setPredicates(pdList);

        //设置过滤器
        List<FilterDefinition> fdList = new ArrayList();
        List<JFilterDefinition> gatewayFilters = definition.getFilters();
        for(JFilterDefinition filterDefinition : gatewayFilters){
            FilterDefinition filter = new FilterDefinition();
            filter.setName(filterDefinition.getName());
            filter.setArgs(filterDefinition.getArgs());
            fdList.add(filter);
        }
        ret.setFilters(fdList);

        URI uri = null;
        if(definition.getUri().startsWith("http")){
            uri = UriComponentsBuilder.fromHttpUrl(definition.getUri()).build().toUri();
        }else{
            // uri为 lb://consumer-service 时使用下面的方法
            uri = URI.create(definition.getUri());
        }
        ret.setUri(uri);
        return ret;
    }

    public String update(JRouteDefinition definition) {
        try {
            this.delete(definition.getId());
        } catch (Exception e) {
            return "update fail,not find route  routeId: "+definition.getId();
        }
        try {
            this.add(definition);
            return "success";
        } catch (Exception e) {
            return "update route  fail";
        }
    }

    public Mono<ResponseEntity<Object>> delete(String id) {
        return this.writer.delete(Mono.just(id)).then(Mono.defer(() -> {
            return Mono.just(ResponseEntity.ok().build());
        })).onErrorResume((t) -> {
            return t instanceof NotFoundException;
        }, (t) -> {
            return Mono.just(ResponseEntity.notFound().build());
        });
    }

    @PostConstruct
    public void init() throws IOException {
        log.info("开始加载路由...");
        this.loadFromFile();
        log.info("路由加载完毕，加载数量：{}",this.getAllRoutes().size());
    }

    private void loadFromFile() throws IOException {
        ClassPathResource resource=new ClassPathResource("routes.json");
        FileReader reader=new FileReader(resource.getFile());
        JRouteDefinition definition= mapper.readValue(reader,JRouteDefinition.class);
        this.add(definition);
    }
}

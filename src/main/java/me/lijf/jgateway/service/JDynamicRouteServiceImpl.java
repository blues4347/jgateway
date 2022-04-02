package me.lijf.jgateway.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
import org.springframework.cloud.gateway.route.*;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class JDynamicRouteServiceImpl implements ApplicationEventPublisherAware {
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

    public void add(RouteDefinition definition){
        //不加subscribe时，无法执行后面的publish。
        writer.save(Mono.just(definition)).subscribe();
        publisher.publishEvent(new RefreshRoutesEvent(this));
    }

    public String update(RouteDefinition definition) {
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
}

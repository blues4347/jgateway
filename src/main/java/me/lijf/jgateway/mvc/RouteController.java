package me.lijf.jgateway.mvc;

import me.lijf.jgateway.entity.JFilterDefinition;
import me.lijf.jgateway.entity.JPredicateDefinition;
import me.lijf.jgateway.entity.JRouteDefinition;
import me.lijf.jgateway.service.JDynamicRouteServiceImpl;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class RouteController {
    @Resource
    JDynamicRouteServiceImpl service;

    @RequestMapping("/")
    public Map<String,Object> echo(){
        return this.getRoutes();
    }

    @RequestMapping("/routes")
    public Map<String,Object> getRoutes(){
        return service.getRoutes();
    }

    @PostMapping("/route")
    public Map<String,Object> addRd(@RequestBody JRouteDefinition definition) {
        RouteDefinition rd = assembleRouteDefinition(definition);
        this.service.add(rd);
        return this.getRoutes();
    }

    @DeleteMapping("/route/{id}")
    public Map<String,Object> deleteRd(@PathVariable("id") String id){
        service.delete(id);
        return this.getRoutes();
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


}

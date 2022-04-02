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
        this.service.add(definition);
        return this.getRoutes();
    }

    @DeleteMapping("/route/{id}")
    public Map<String,Object> deleteRd(@PathVariable("id") String id){
        service.delete(id);
        return this.getRoutes();
    }
}

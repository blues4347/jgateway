package me.lijf.jgateway.entity;

import java.util.ArrayList;
import java.util.List;

public class JRouteDefinition {
        //路由的Id
        private String id;
        //路由断言集合配置
        private List<JPredicateDefinition> predicates = new ArrayList<>();
        //路由过滤器集合配置
        private List<JFilterDefinition> filters = new ArrayList<>();
        //路由规则转发的目标uri
        private String uri;
        //路由执行的顺序
        private int order = 0;
        //此处省略get和set方法


        public String getId() {
                return id;
        }

        public void setId(String id) {
                this.id = id;
        }

        public List<JPredicateDefinition> getPredicates() {
                return predicates;
        }

        public void setPredicates(List<JPredicateDefinition> predicates) {
                this.predicates = predicates;
        }

        public List<JFilterDefinition> getFilters() {
                return filters;
        }

        public void setFilters(List<JFilterDefinition> filters) {
                this.filters = filters;
        }

        public String getUri() {
                return uri;
        }

        public void setUri(String uri) {
                this.uri = uri;
        }

        public int getOrder() {
                return order;
        }

        public void setOrder(int order) {
                this.order = order;
        }
}

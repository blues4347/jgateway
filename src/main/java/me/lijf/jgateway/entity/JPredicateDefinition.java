package me.lijf.jgateway.entity;

import java.util.LinkedHashMap;
import java.util.Map;

public class JPredicateDefinition {
    //断言对应的Name
    private String name;
    //配置的断言规则,key包括Path、Query、Method、Before/After/Between、RemoteAddr、Header。Query、Header的value为逗号分隔的字符串，前面是判断条件的key，后面是判断条件的value；其他Value接收一到多个参数，多个参数用逗号分隔，为或的关系。
    private Map<String, String> args = new LinkedHashMap<>();
    //此处省略Get和Set方法


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getArgs() {
        return args;
    }

    public void setArgs(Map<String, String> args) {
        this.args = args;
    }
}

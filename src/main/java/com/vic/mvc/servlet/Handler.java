package com.vic.mvc.servlet;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author vic
 * @date 2021/12/21 10:01 下午
 **/
public class Handler {

    private Object controller;

    private Method method;

    private Pattern uri;

    private Map<String, Integer> parameterIndexMap;

    public Handler(Object controller, Method method, Pattern uri, Map<String, Integer> parameterIndexMap) {
        this.controller = controller;
        this.method = method;
        this.uri = uri;
        this.parameterIndexMap = parameterIndexMap;
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Pattern getUri() {
        return uri;
    }

    public void setUri(Pattern uri) {
        this.uri = uri;
    }

    public Map<String, Integer> getParameterIndexMap() {
        return parameterIndexMap;
    }

    public void setParameterIndexMap(Map<String, Integer> parameterIndexMap) {
        this.parameterIndexMap = parameterIndexMap;
    }
}

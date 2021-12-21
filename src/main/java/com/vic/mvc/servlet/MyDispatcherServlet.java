package com.vic.mvc.servlet;

import cn.hutool.core.util.ClassUtil;
import com.vic.mvc.annotation.MyAutowired;
import com.vic.mvc.annotation.MyController;
import com.vic.mvc.annotation.MyRequestMapping;
import com.vic.mvc.annotation.MyService;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author vic
 * @date 2021/12/20 11:51 下午
 **/
public class MyDispatcherServlet extends HttpServlet {

    public static final char A = 'A';

    public static final char Z = 'Z';

    private Properties properties = new Properties();

    private Set<Class<?>> classes = new HashSet<>();

    private Map<String, Object> BEANS = new HashMap<>();

    private List<String> filedAlreadyProcessed = new ArrayList<>();

    // private Map<String, Handler> handlerMapping = new HashMap<>();
    private List<Handler> handlerMapping = new ArrayList<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            // 读取servlet参数
            String contextConfigLocation = config.getInitParameter("contextConfigLocation");
            // 解析配置文件
            doLoadConfig(contextConfigLocation);
            // 扫描注解
            doScan(properties.getProperty("scanPackage"));
            // 初始化对象
            doInstance();
            // 依赖注入
            doAutowired();
            // 初始化HandlerMapping
            initHandlerMapping();
            // 输出初始化完成
            System.out.println("MyDispatcherServlet初始化完成");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initHandlerMapping() {
        for (Map.Entry<String, Object> entry : BEANS.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(MyController.class)) {
                continue;
            }

            String baseUri = "";
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                baseUri = clazz.getAnnotation(MyRequestMapping.class).value();
            }

            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }

                String fullUri = baseUri + method.getAnnotation(MyRequestMapping.class).value();

                Handler handler = new Handler(entry.getValue(), method, Pattern.compile(fullUri), new HashMap<>());
                Parameter[] parameters = method.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    if (parameter.getType() == HttpServletRequest.class || parameter.getType() == HttpServletResponse.class) {
                        handler.getParameterIndexMap().put(parameter.getType().getSimpleName(), i);
                    } else {
                        handler.getParameterIndexMap().put(parameter.getName(), i);
                    }
                }
                handlerMapping.add(handler);
            }
        }

    }

    private void doAutowired() throws Exception {
        if (BEANS.isEmpty()) {
            return;
        }
        for (Object obj : BEANS.values()) {
            doDependency(obj);
        }
    }

    private void doDependency(Object obj) throws Exception {
        Field[] declaredFields = obj.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            if (!field.isAnnotationPresent(MyAutowired.class)) {
                continue;
            }

            // 变量已注入，跳过
            if (filedAlreadyProcessed.contains(obj.getClass().getName() + "." + field.getName())) {
                continue;
            }

            Object dependency = BEANS.get(field.getType().getName());

            // 如果为null尝试类名首字母小写为key获取
            if (dependency == null) {
                dependency = BEANS.get(lowerFirst(field.getType().getSimpleName()));
            }

            if (dependency == null) {
                throw new RuntimeException(field.getType().getName() + "未实例");
            }

            // 添加标记，表示变量已注入
            filedAlreadyProcessed.add(obj.getClass().getName() + "." + field.getName());

            // 递归
            doDependency(dependency);

            field.setAccessible(true);
            field.set(obj, dependency);
        }
    }

    private void doInstance() throws InstantiationException, IllegalAccessException {
        if (classes.isEmpty()) {
            return;
        }
        for (Class<?> clazz : classes) {
            BEANS.put(lowerFirst(clazz.getSimpleName()), clazz.newInstance());

            Class<?>[] interfaces = clazz.getInterfaces();
            if (interfaces.length != 0) {
                for (Class<?> clazzInterface : interfaces) {
                    BEANS.put(lowerFirst(clazzInterface.getSimpleName()), clazz.newInstance());
                }
            }
        }
    }

    private static String lowerFirst(String str) {
        char[] chars = str.toCharArray();
        if (A <= chars[0] && chars[0] <= Z) {
            chars[0] += 32;
        }
        return String.valueOf(chars);
    }

    private void doScan(String scanPackage) {
        classes.addAll(ClassUtil.scanPackageByAnnotation(scanPackage, MyController.class));
        classes.addAll(ClassUtil.scanPackageByAnnotation(scanPackage, MyService.class));
    }

    private void doLoadConfig(String contextConfigLocation) throws IOException {
        properties.load(this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 请求处理
        Handler handler = getHandler(req);

        if (handler == null) {
            resp.getWriter().write("404 not found");
            return;
        }

        Parameter[] parameters = handler.getMethod().getParameters();

        Object[] parameterValues = new Object[parameters.length];
        req.getParameterMap().forEach((parameterKey, values) -> {
            String value = StringUtils.join(values, ",");

            if (!handler.getParameterIndexMap().containsKey(parameterKey)) {
                return;
            }
            parameterValues[handler.getParameterIndexMap().get(parameterKey)] = value;
        });

        parameterValues[handler.getParameterIndexMap().get(HttpServletRequest.class.getSimpleName())] = req;
        parameterValues[handler.getParameterIndexMap().get(HttpServletResponse.class.getSimpleName())] = resp;

        Object result = null;
        try {
            result = handler.getMethod().invoke(handler.getController(), parameterValues);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(result);
    }

    private Handler getHandler(HttpServletRequest req) {
        if (handlerMapping.isEmpty()) {
            return null;
        }

        for (Handler handler : handlerMapping) {
            Matcher matcher = handler.getUri().matcher(req.getRequestURI());
            if (!matcher.matches()) {
                continue;
            }
            return handler;
        }
        return null;

    }

}

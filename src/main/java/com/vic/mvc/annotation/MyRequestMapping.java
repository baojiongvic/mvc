package com.vic.mvc.annotation;

import java.lang.annotation.*;

/**
 * @author vic
 * @date 2021/12/20 11:54 下午
 **/
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MyRequestMapping {
    String value() default "";
}

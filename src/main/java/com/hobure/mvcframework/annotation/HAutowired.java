package com.hobure.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * 2019-03-26
 * hobure
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HAutowired {
    String value() default "";
}

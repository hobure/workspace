package com.hobure.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * 2019-03-26
 * hobure
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HRequestMapping {
    String value() default "";
}

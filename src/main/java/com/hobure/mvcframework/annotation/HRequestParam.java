package com.hobure.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * 2019-03-26
 * hobure
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HRequestParam {
    String value() default "";
}

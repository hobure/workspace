package com.hobure.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * 2019-03-26
 * hobure
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HController {
    String value() default "";
}

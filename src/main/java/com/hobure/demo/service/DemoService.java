package com.hobure.demo.service;

import com.hobure.mvcframework.annotation.HService;

/**
 * 2019-03-26
 * hobure
 */
@HService
public class DemoService {

    private String name;

    public String getProductName(){
        return name;
    }

    public void setProductName(String name){
        this.name = name;
    }
}

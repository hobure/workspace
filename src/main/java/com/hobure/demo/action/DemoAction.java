package com.hobure.demo.action;

import com.hobure.demo.service.DemoService;
import com.hobure.mvcframework.annotation.HAutowired;
import com.hobure.mvcframework.annotation.HController;
import com.hobure.mvcframework.annotation.HRequestMapping;
import com.hobure.mvcframework.annotation.HRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 2019-03-26
 * hobure
 */
@HController
@HRequestMapping("/demo")
public class DemoAction {

    @HAutowired
    DemoService service;

    @HRequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response, @HRequestParam("name") String name){
        try {
            service.setProductName(name);
            response.getWriter().write(service.getProductName());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}

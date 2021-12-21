package com.vic.mvc.controller;

import com.vic.mvc.annotation.MyAutowired;
import com.vic.mvc.annotation.MyController;
import com.vic.mvc.annotation.MyRequestMapping;
import com.vic.mvc.serivce.IDemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author vic
 * @date 2021/12/21 9:29 下午
 **/
@MyController
@MyRequestMapping("/demo")
public class DemoController {

    @MyAutowired
    private IDemoService demoService;

    @MyRequestMapping("/query")
    public String query(HttpServletRequest request, HttpServletResponse response, String name) {
        return demoService.get(name);
    }


}

package com.vic.mvc.serivce.impl;

import com.vic.mvc.annotation.MyService;
import com.vic.mvc.serivce.IDemoService;

/**
 * @author vic
 * @date 2021/12/21 9:30 δΈε
 **/
@MyService
public class DemoServiceImpl implements IDemoService {

    @Override
    public String get(String name) {
        System.out.println("service ε₯ε" + name);
        return name;
    }
}

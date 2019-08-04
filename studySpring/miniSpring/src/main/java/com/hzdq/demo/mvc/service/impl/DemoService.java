package com.hzdq.demo.mvc.service.impl;


import com.hzdq.demo.annotation.DqService;
import com.hzdq.demo.mvc.service.IDemoService;

/**
 * 核心业务逻辑
 */
@DqService
public class DemoService implements IDemoService {

	public String get(String name) {
		return "my name is " + name;
	}

}

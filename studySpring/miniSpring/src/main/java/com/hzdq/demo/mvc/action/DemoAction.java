package com.hzdq.demo.mvc.action;

import com.hzdq.demo.annotation.DqAutowired;
import com.hzdq.demo.annotation.DqController;
import com.hzdq.demo.annotation.DqRequestMapping;
import com.hzdq.demo.annotation.DqRequestParam;
import com.hzdq.demo.mvc.service.IDemoService;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@DqController
@DqRequestMapping("/demo")
public class DemoAction {

  	@DqAutowired
	private IDemoService demoService;

	@DqRequestMapping("/query")
	public void query(HttpServletRequest req, HttpServletResponse resp,
					  @DqRequestParam("name") String name){
		String result = demoService.get(name);
//		String result = "My name is " + name;
		try {
			resp.getWriter().write(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@DqRequestMapping("/add")
	public void add(HttpServletRequest req, HttpServletResponse resp,
					@DqRequestParam("a") Integer a, @DqRequestParam("b") Integer b){
		try {
			resp.getWriter().write(a + "+" + b + "=" + (a + b));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@DqRequestMapping("/remove")
	public void remove(HttpServletRequest req,HttpServletResponse resp,
					   @DqRequestParam("id") Integer id){
	}

}

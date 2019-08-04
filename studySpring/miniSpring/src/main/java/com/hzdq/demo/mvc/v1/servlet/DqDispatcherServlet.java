package com.hzdq.demo.mvc.v1.servlet;

import com.hzdq.demo.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;

/**
 * dingqiang
 * 2019-07-30
 */
public class DqDispatcherServlet extends HttpServlet {
    private Properties properties=new Properties();
    private List<String> classNames=new ArrayList<>();
    private Map<String,Object> ioc = new HashMap<>();
//    private Map<String, Method> handlerMapping = new HashMap<>();
    private List<HandlerMapping> handlerMappings = new ArrayList<>();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDisPath(req, resp);
    }

    private void doDisPath(HttpServletRequest req, HttpServletResponse resp) {
        String url=req.getRequestURI();
//        Method method= handlerMapping.get(url);
        HandlerMapping handler = null;
        Method method=null;
        for (HandlerMapping handlerMapping : handlerMappings) {
            if(handlerMapping.url.equals(url)){
                handler=handlerMapping;
                break;
            }
        }
        if(handler==null){
            try {
                resp.getWriter().print("404");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        method = handler.method;
        if(method!=null){
            Class<?>[] parameterTypes=method.getParameterTypes();
            Parameter[] parameters=method.getParameters();
            Map<String,String[]> map=req.getParameterMap();
            Object[] objects = new Object[parameterTypes.length];
//            if(parameterTypes.length>0){
//                for (int i = 0; i < parameterTypes.length; i++){
//                    if(parameterTypes[i].equals(HttpServletRequest.class)){
//                        objects[i]=req;
//                        continue;
//                    }else if(parameterTypes[i].equals(HttpServletResponse.class)){
//                        objects[i]=resp;
//                        continue;
//                    }
//                    String paramName=parameters[i].getName();
//                    if(parameters[i].isAnnotationPresent(DqRequestParam.class)){
//                        String name=parameters[i].getAnnotation(DqRequestParam.class).value().trim();
//                        paramName = "".equals(name)?paramName:name;
//                    }
//                        objects[i]=conver(map.get(paramName),parameterTypes[i]);
//                }
//            }
//            Class<?> clazz = method.getDeclaringClass();
//            String clzName=firstLowerCase(clazz.getSimpleName());
//            try {
//                method.invoke(ioc.get(clzName),objects);
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            } catch (InvocationTargetException e) {
//                e.printStackTrace();
//            }
            Map<String,Integer> m = handler.parametersI;
            for (Map.Entry<String, String[]> entry : map.entrySet()) {
                String name= entry.getKey();
                objects[m.get(name)]=conver(parameterTypes[m.get(name)],entry.getValue());
            }
            if(m.containsKey(HttpServletRequest.class.getName())){
                objects[m.get(HttpServletRequest.class.getName())]=req;
            }
            if(m.containsKey(HttpServletResponse.class.getName())){
                objects[m.get(HttpServletResponse.class.getName())]=resp;
            }
            try {
                method.invoke(handler.controller,objects);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private Object conver(Class<?> parameterType,String[] strings) {
        if(parameterType.equals(String.class)){
            return strings[0];
        }else if(parameterType.equals(Integer.class)){
            return Integer.parseInt(strings[0]);
        }else{
            return strings[0];
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        InputStream inputStream =this.getClass().getClassLoader().getResourceAsStream(config.getInitParameter("contextConfigLocation"));
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //扫描获取所有类
        setClassList(properties.getProperty("scanPackage"));
        //初始化所有注解对应的类实例
        doInstance();
        //注解依赖注入
        initAutoWire();
        //初始化url与对应的方法handlerMapping
        initHandlerMapping();
        System.out.println("初始化完毕");
    }

    private void initHandlerMapping() {
        if(ioc.size()>0){
            for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                Class<?> clazz = entry.getValue().getClass();
                Method[] methods=clazz.getMethods();
                if(methods.length>0){
                    for (Method method : methods) {
                        if(method.isAnnotationPresent(DqRequestMapping.class)){
                            String url1=clazz.getAnnotation(DqRequestMapping.class).value().trim();
                            String url2=method.getAnnotation(DqRequestMapping.class).value().trim();
//                            handlerMapping.put((url1+"/"+url2).replaceAll("/+","/"),method);
                            handlerMappings.add(new HandlerMapping((url1+"/"+url2).replaceAll("/+","/"),entry.getValue(),method));
                        }
                    }
                }
            }
        }
    }

    //初始化所有依赖注入的值
    private void initAutoWire() {
        if(ioc.size()>0){
            for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                Object o =entry.getValue();
                Field[] fields=o.getClass().getDeclaredFields();
                if(fields.length>0){
                    for (Field field : fields) {
                        if(field.isAnnotationPresent(DqAutowired.class)){
                            DqAutowired d = field.getAnnotation(DqAutowired.class);
                            String name=d.value().trim();
                            name= "".equals(name)?firstLowerCase(field.getName()):firstLowerCase(name);
                            try {
                                field.setAccessible(true);
                                field.set(entry.getValue(),ioc.get(name));
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 初始化所有类
     */
    private void doInstance() {
        if(classNames.size()>0){
            for (String className : classNames) {
                try {
                    Class<?> clazz=Class.forName(className);
                        String clzName=clazz.getSimpleName();
                        clzName=firstLowerCase(clzName);
                        if(clazz.getAnnotation(DqController.class)!=null){
                            putToIoc(clzName,clazz);
                        }else if(clazz.getAnnotation(DqService.class)!=null){
                            Annotation annotation=clazz.getAnnotation(DqService.class);
                            String value=((DqService) annotation).value().trim();
                            clzName="".equals(value)?clzName:value;
                            putToIoc(clzName,clazz);
                        }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void putToIoc(String clzName, Class<?> clazz) {
        try {
            ioc.put(clzName,clazz.newInstance());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private String firstLowerCase(String clzName) {
        char[] chars=clzName.toCharArray();
        if(chars[0]<97){
            chars[0]+=32;
        }
        return String.valueOf(chars);
    }

    /**
     * 获取所有class文件
     * @param scanPackage
     */
    private void setClassList(String scanPackage) {
        String url = scanPackage.replaceAll("\\.","/");
        URL url1=this.getClass().getClassLoader().getResource("/"+url);
        File file = new File(url1.getFile());
        File[] files=file.listFiles();
        for (File file1 : files) {
            if(file1.isDirectory()){
                setClassList(scanPackage+"."+file1.getName());
            }else if(file1.getName().endsWith(".class")){
                String clzUrl = scanPackage+"."+file1.getName().replace(".class","");
                classNames.add(clzUrl);
            }
        }
    }
    class HandlerMapping{
        private String url;
        private Object controller;
        private Method method;
        private Map<String,Integer> parametersI = new HashMap<>();

        public HandlerMapping(String url, Object controller, Method method) {
            this.url = url;
            this.controller = controller;
            this.method = method;
            initParameterIndex();
        }
        public void initParameterIndex(){
            Annotation[][] annotations=method.getParameterAnnotations();
            Parameter[] parameters=method.getParameters();
            if(annotations!=null&&annotations.length>0){
                for (int i = 0; i < annotations.length; i++) {
                    Annotation[] annotations1=annotations[i];
                    for (int j = 0; j < annotations1.length; j++) {
                        if(annotations1[j] instanceof DqRequestParam){
                            String value=((DqRequestParam)annotations1[j]).value();
                            if (value != null&&value!="") {
                                parametersI.put(value,i);
                            }
                        }
                    }
                }
            }
            Class<?>[] classes=method.getParameterTypes();
            Parameter[] paramete=method.getParameters();
            for (int i = 0; i < classes.length; i++) {
                Class<?> c = classes[i];
                if(c == HttpServletRequest.class||c==HttpServletResponse.class){
                    parametersI.put(c.getName(),i);
                }else if(!parametersI.containsValue(i)){
                    parametersI.put(paramete[i].getName(),i);
                }
            }
        }
    }

}

package com.hobure.mvcframework;

import com.hobure.mvcframework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;

/**
 * 2019-03-26
 * hobure
 *
 * 1、加载配置文件，得到包路径
 * 2、扫描包下的所有类文件
 * 3、缓存url和方法的映射，缓存Service的bean对象
 * 4、浏览器访问时， 去缓存匹配到url, 然后从缓存取到bean对象并注入到controller类的变量中。（ 当前是调用时才注入bean对象，后续加到初始化时注入 )
 * 5、执行url所对应的方法
 *
 */
public class HDispatcherServlet extends HttpServlet {

    private static List<String> clazzList = new ArrayList<String>();
    private static Map<String, ExecuteTarget> mapping = new HashMap<String, ExecuteTarget>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try{
            this.doPost(req, resp);
        }catch (Exception e){
            resp.getWriter().write("500! " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //获取uri访问地址，
        String uri = req.getRequestURI() == null ? "" : req.getRequestURI();
        uri = uri.replaceAll(req.getContextPath(),"").replaceAll("/+","/");

        //遍历uriMapping, 匹配到客户端访问的地址，得到要执行的类和方法
        try {
            for (Map.Entry<String, ExecuteTarget> executeTargetEntry : mapping.entrySet()) {

                //如果访问的地址和缓存的地址匹配上了，则执行对应的方法
                if(uri.equals(executeTargetEntry.getKey())){
                    ExecuteTarget executeTarget = executeTargetEntry.getValue();

                    //实例contrller类
                    Object clazzInstance = executeTarget.getClazz().newInstance();

                    //获取对应的方法，并映射参数后执行方法
                    Method method = executeTarget.getMethod();
                    Parameter[] parameters = method.getParameters();
                    Object[] obj = new Object[parameters.length];
                    int i = 0;
                    for(Parameter p : parameters){
                       if(HttpServletRequest.class == p.getType()){
                           obj[i++] = req;
                       }else if(HttpServletResponse.class == p.getType()){
                           obj[i++] = resp;
                       }else{
                           if(p.isAnnotationPresent(HRequestParam.class)){
                               HRequestParam hRequestParam = p.getAnnotation(HRequestParam.class);
                               obj[i++] = req.getParameter(hRequestParam.value());
                           }
                       }
                    }
                    //注入bean对象，
                    Field[] fields = executeTarget.getClazz().getDeclaredFields();
                    for(int j=0; j<fields.length; j++){
                        Field field = fields[j];
                        if(field.isAnnotationPresent(HAutowired.class)){
                            ExecuteTarget beanTarget = mapping.get(field.getType().getName());
                            if (beanTarget != null && beanTarget.getObject() != null) {
                                field.setAccessible(true);
                                field.set(clazzInstance, beanTarget.getObject());
                            }
                        }
                    }
                    //执行方法
                    method.invoke(clazzInstance,obj);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        String paramFilePath = config.getInitParameter("contextConfigLocation");
        Properties property = new Properties();
        try {
            //加载配置文件
            property.load(this.getClass().getClassLoader().getResourceAsStream(paramFilePath));
            String scanPackage = property.getProperty("scanPackage");
            scanPackage = scanPackage.replaceAll("\\.", File.separator);

            //扫描文件
            scanFiles(scanPackage);

            //缓存url+方法,缓存bean
            cacheClazzAndMethod();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 扫描指定包下的所有类
     * @param scanPackage
     */
    public void scanFiles(String scanPackage){
        //如果是本项目工程作为jar包被使用，则使用getResource()会存在获取路径错误的问题，暂时先不处理
        URL url = this.getClass().getClassLoader().getResource(File.separator + scanPackage);
        File file = new File(url.getFile());
        for(File f : file.listFiles()){
            if(f.isDirectory()){
                scanFiles(scanPackage + File.separator + f.getName());
            }else if(f.getName().endsWith(".class")){
                clazzList.add(String.format("%s.%s",scanPackage.replaceAll("/","."), f.getName().replaceAll(".class","")));
            }
        }
    }

    /**
     * 遍历指定包下的所有类，缓存方法,缓存bean
     */
    private void cacheClazzAndMethod() {
        try {
            for(int i=0; i<clazzList.size(); i++){
                String clazzName = clazzList.get(i);
                if(!clazzName.contains(".")){
                    continue;
                }
                StringBuffer uri = new StringBuffer();
                Class<?> clazz = Class.forName(clazzName);
                if(clazz.isAnnotationPresent(HController.class) && clazz.isAnnotationPresent(HRequestMapping.class)){
                    HRequestMapping hRequestMapping =  clazz.getAnnotation(HRequestMapping.class);
                    uri.append(hRequestMapping.value());
                    //缓存访问地址
                    Method[] methods = clazz.getMethods();
                    for(int j=0; j<methods.length; j++){
                        Method method = methods[j];
                        if(method.isAnnotationPresent(HRequestMapping.class)){
                            HRequestMapping methodHRequestMapping =  method.getAnnotation(HRequestMapping.class);
                            uri.append("/").append(methodHRequestMapping.value());
                            mapping.put(uri.toString().replaceAll("/+", "/"),new ExecuteTarget(clazz,method));
                        }
                    }

                }else if(clazz.isAnnotationPresent(HService.class)){
                    //缓存bean
                    HService hService = clazz.getAnnotation(HService.class);
                    String beanKeyName = hService.value();
                    beanKeyName = "".equals(beanKeyName) ? clazz.getName() : beanKeyName;
                    mapping.put(beanKeyName,new ExecuteTarget(clazz.newInstance()));
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private class ExecuteTarget {
        private Class clazz;
        private Method method;
        private Object object;

        public ExecuteTarget(Class clazz, Method method){
            this.clazz = clazz;
            this.method = method;
        }

        public ExecuteTarget(Object object){
            this.clazz = clazz;
            this.method = method;
            this.object = object;
        }

        public Class getClazz() {
            return clazz;
        }

        public void setClazz(Class clazz) {
            this.clazz = clazz;
        }

        public Method getMethod() {
            return method;
        }

        public void setMethod(Method method) {
            this.method = method;
        }

        public Object getObject() {
            return object;
        }

        public void setObject(Object object) {
            this.object = object;
        }
    }
}
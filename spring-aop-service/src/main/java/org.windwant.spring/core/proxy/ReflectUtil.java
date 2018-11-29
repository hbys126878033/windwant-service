package org.windwant.spring.core.proxy;

import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.windwant.spring.core.hibernate.TBaseDao;
import org.windwant.spring.web.service.BookService;
import org.windwant.spring.web.service.Performer;
import org.windwant.spring.web.service.impl.APerformer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * ���乤����
 * Created by windwant on 2016/9/18.
 */
public class ReflectUtil {

    public static void main(String[] args) {
//        invokeClassMethod("org.windwant.spring.core.proxy.JavassistUtil", "getClassInstance", new Class[]{String.class, Map.class},
//                new ArrayList() {{add("org.windwant.TestReflect");add(new HashMap(){{
//                    put("name", "java.lang.String");
//                    put("age", "int");
//                    put("sex", "int");
//                }});}});

        Performer cglib = (Performer) getCglibProxy(APerformer.class, new MethodInterceptor() {
            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                System.out.println("cglib before action");
                Object result = methodProxy.invokeSuper(o, objects);
                System.out.println("cglib after action");
                return result;
            }
        });
        System.out.println(cglib.perform("1"));

//        APerformer jdk = (APerformer) getJDKProxy(APerformer.class, new InvocationHandler() {
//            @Override
//            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//                System.out.println("proxy: before action");
//                Object resutl = method.invoke(proxy, args);
//                System.out.println("proxy: after action");
//                return resutl;
//            }
//        });
//        System.out.println(jdk.perform("111"));
    }


    /**
     * �����෽��
     * @param clazz
     * @param methodName ��������
     * @param paramTypes ��������
     * @param params ����ֵ
     * @return
     */
    public static Object invokeClassMethod(String clazz, String methodName, Class[] paramTypes, List params){
        try {
            Object cls = Class.forName(clazz).newInstance();
            Method m = cls.getClass().getDeclaredMethod(methodName, paramTypes);
            if(m != null){
                return m.invoke(cls, params.toArray());
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * CGLIB ������
     * @param clazz ����߽ӿ�
     * @param callback ͨ��ʵ�� MethodInterceptor
     * @return
     */
    public static Object getCglibProxy(Class clazz, Callback callback){
        if(callback == null) return null;

        //��̬�������࣬��ʵ�ַ���interception һ������࣬��̬�ŷ�ʹ��
        Enhancer enhancer = new Enhancer();
        if(!clazz.isInterface()) {
            //������Ҫ���ɴ���������� ��final ���캯���ɷ���
            enhancer.setSuperclass(clazz);
        }else {
            enhancer.setInterfaces(new Class[]{clazz});
        }
        enhancer.setCallback(callback);
        return enhancer.create();
    }

    /**
     * CGLIB ������ �ӿ�ʵ��
     * @param clazzs �ӿ�
     * @param callback ͨ��ʵ�� MethodInterceptor
     * @return
     */
    public static Object getCglibProxy(Class[] clazzs, Callback callback){
        if(callback == null) return null;

        for (Class aClass : clazzs) {
            if(!aClass.isInterface()){
                return null;
            }
        }

        //��̬�������࣬��ʵ�ַ���interception һ������࣬��̬�ŷ�ʹ��
        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(clazzs);
        enhancer.setCallback(callback);
        return enhancer.create();
    }


    public static Object getJDKProxy(Object targetObject, InvocationHandler handler){
        return Proxy.newProxyInstance(targetObject.getClass().getClassLoader(), targetObject.getClass().getInterfaces(), handler);
    }


}

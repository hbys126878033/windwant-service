package org.windwant.spring.core.proxy;

import javassist.*;

import javax.el.MethodNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * javassist opt
 * Created by windwant on 2016/9/18.
 */
public class JavassistUtil {

    /**
     * ���������
     * @param clazz ��ȫ�޶���
     * @param pClazz ����ȫ�޶���
     * @param methodName ��������
     * @param methodBefore ����ǰ����
     * @param methodAfter ���������
     * @param src ���ļ�����·���� Ĭ�ϵ�ǰ��Ŀ��·��
     * @return
     */
    public static Object getProxyInstance(String clazz, String pClazz, String methodName, String methodBefore, String methodAfter, String src){
        if(clazz == null || clazz == "") return null;
        //CtClass��
        ClassPool cp = ClassPool.getDefault();
        CtClass ct;
        try {
            ct = cp.getOrNull(clazz);
            //�������򴴽�
            if(ct == null){
                ct = cp.makeClass(clazz);
            }
            //�ӿ��ж�
            if(ct.isInterface()) return null;

            if(pClazz != null && cp.find(pClazz) != null){
                CtClass superClass = cp.get(pClazz);
                if(!superClass.isInterface()) {
                    ct.setSuperclass(cp.get(pClazz));
                }

            }
            //д���ļ���ct�Ͻ�������������
            if(src != null && src != "") {
                ct.writeFile(src);
            }else {
                ct.writeFile();
            }
            //Defrosts ����ʹ��ct���������޸�
            ct.defrost();

            if(methodName != null && methodName != "") {

                CtMethod m;
                try {
                    m = ct.getDeclaredMethod(methodName);
                } catch (MethodNotFoundException e) {
                    m = CtMethod.make(methodName, ct);
                }
                //���������������
                if (m != null) {
                    ct.addMethod(m);
                }
                //�ڷ���ǰ��Ӵ����
                if (methodBefore != null) {
                    m.insertBefore(methodBefore);
                }
                //�ڷ�������Ӵ����
                if (methodAfter != null) {
                    m.insertAfter(methodAfter);
                }
            }
            return ct.toClass().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getProxyInstance(String clazz, String pClazz, String methodName, String methodBefore, String methodAfter) {
        return getProxyInstance(clazz, pClazz, methodName, methodBefore, methodAfter, null);
    }

    public static Object getProxyInstance(String clazz, String pClazz, String methodName) {
        return getProxyInstance(clazz, pClazz, methodName, null, null, null);
    }

    public static Object getProxyInstance(String clazz, String pClazz) {
        return getProxyInstance(clazz, pClazz, null, null, null, null);
    }

    public static void main(String[] args) {
//        System.out.println(getProxyInstance("org.windwant.spring.web.service.BookService", null, null, null, null));
        String methodBefore = "{ System.out.println(\"method before...:\"); }";
        String methodAfter = "{ System.out.println(\"method after...:\"); }";
        System.out.println(getProxyInstance("org.windwant.TestProxyClass", "org.windwant.spring.core.proxy.Hello", null, null, null));
//        Class test = (Class) getClassOrInstance("org.windwant.TestClass", new HashMap() {{
//            put("name", "java.lang.String");
//            put("age", "int");
//            put("sex", "int");
//        }}, true);
//        try {
//            Constructor c = test.getDeclaredConstructor(new Class[]{String.class, int.class, int.class});
//            Object testObject = c.newInstance("lilei", 20, 1);
//            System.out.println(test);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        } catch (InstantiationException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
    }


    /**
     * ���������
     * @param clazz ��ȫ�޶���
     * @param fields �ֶ�����
     * @param extraConstructor �Ƿ���Ҫ�������Ĺ��캯��
     * @return
     */
    public static Object getClassOrInstance(String clazz, Map<String, String> fields, boolean extraConstructor, boolean instance){
        // ������ ClassPool CtClass ������CtClass�����ɴ�������ã�ClassPool�洢���д�����CtClass���������ڱ������޸ĵ�һ���ԣ����������ķѺܶ��ڴ棬��ˣ��Ƽ������Σ����������ؽ�ClassPool
        ClassPool pool = ClassPool.getDefault();
        //�����ȫ�޶�������������Ѿ���ͬ�����࣬�򸲸�
        CtClass cls = pool.makeClass(clazz);

        List<CtClass> cs = new ArrayList();
        StringBuilder conBody = null;

        if(extraConstructor) {
            new StringBuilder();
            conBody.append("{\r\n");
        }

        // ���� getter��setter����
        final int[] i = {1};
        fields.entrySet().stream().forEach(item -> {
            CtField param = null; //name����
            try {
                param = new CtField(pool.get(item.getValue()), item.getKey(), cls);
                param.setModifiers(Modifier.PRIVATE); //���ʿ���

                //CtNewMethod CtMethod��������
                String firstUpper = String.valueOf(item.getKey().charAt(0)).toUpperCase() + item.getKey().substring(1);
                cls.addMethod(CtNewMethod.setter("set" + firstUpper, param));
                cls.addMethod(CtNewMethod.getter("get" + firstUpper, param));
                cls.addField(param);

                if (extraConstructor) {
                    cs.add(pool.get(item.getValue()));
                    conBody.append("$0." + item.getKey() + " = $" + i[0] + ";"); //$num ���� $0ָ��this
                }
                i[0]++;
            } catch (CannotCompileException e) {
                e.printStackTrace();
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
        });

        //���캯��
        CtConstructor cons = new CtConstructor(new CtClass[] {}, cls);
        try {
            cons.setBody(null);
            cls.addConstructor(cons);

            // ����вεĹ�����
            if(extraConstructor) {
                conBody.append("\r\n}");
                cons = new CtConstructor(cs.toArray(new CtClass[0]), cls);
                cons.setBody(conBody.toString());
                cls.addConstructor(cons);
            }

            //�����ɵ��ౣ�浽�ļ�
            cls.writeFile();
            if(instance){
                return cls.toClass().newInstance();
            }
            return cls.toClass();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ���������
     * @param clazz
     * @param fields
     * @return
     */
    public static Object getClassOrInstance(String clazz, Map<String, String> fields){
        return getClassOrInstance(clazz, fields, false, false);
    }

    /**
     * ���������
     * @param clazz
     * @param fields
     * @return
     */
    public static Object getClassOrInstance(String clazz, Map<String, String> fields, boolean extraConstructor){
        return getClassOrInstance(clazz, fields, extraConstructor, false);
    }
}

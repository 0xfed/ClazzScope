package net.babyphd.clazzscope;


public class JavassistClassLoader extends ClassLoader {
    public JavassistClassLoader(){
        super(Thread.currentThread().getContextClassLoader());
    }
}
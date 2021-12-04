package burp;

import javassist.*;
import net.babyphd.clazzscope.ClazzScope;
import sun.misc.URLClassPath;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Analyzer {

    public static String Analyze(Set<String> found, Map<String, String> library) {
        StringBuilder sb = new StringBuilder();

        // Get All jar found
        TreeSet<String> a = new TreeSet<>();
        sb.append("Detect Library Versions:\n");
        for (String s : found) {
            String lib = s;
            String cls = lib.substring(lib.lastIndexOf(",")+1);
            if (a.contains(cls)) {
            }
            else  {
                a.add(cls); sb.append( cls+ "\n");}
        }

        return sb.toString();
    }

    public static String getUniqueList(String[] args) throws Exception{
        ClassPool pool = ClassPool.getDefault();
        TreeMap<String, String> dedupList = new TreeMap<>();
        TreeMap<String, String> uniqList = new TreeMap<>();
        int count = 0;
        for (final File fileEntry : new File(args[0]).listFiles()) {
            if (fileEntry.isFile() && fileEntry.getName().endsWith(".jar")) {
                count += 1;
                ClassPath cp = pool.insertClassPath(fileEntry.getPath());
                for (String a: getClassFromJar(fileEntry.getPath())) {
                    CtClass cc = pool.get(a);
                    try{
                        for (CtClass ccInterface : cc.getInterfaces()) {
                            if(ccInterface.getName().equals(Serializable.class.getName())){
                                URLClassLoader loader = new URLClassLoader(new URL[] {fileEntry.toURL()}, Thread.currentThread().getContextClassLoader());
                                Class cls = loader.loadClass(cc.getName());
                                long serialVersionUID = ObjectStreamClass.lookup(cls).getSerialVersionUID();
                                if (checkSerializeAndDeserialize(cc, serialVersionUID, loader)){
                                    String iden = cc.getName()+","+serialVersionUID;
                                    String lib = fileEntry.getName();
                                    if (dedupList.get(iden) == null) {
                                        dedupList.put(iden, lib);
                                    }
                                    else
                                        dedupList.put(iden, dedupList.get(iden) + "|" + lib);
                                    uniqList.put(iden, lib);
                                }
                                break;
                            }
                        }
                    } catch (NotFoundException | NoClassDefFoundError e){
                    }
                }
                pool.removeClassPath(cp);
            }
        }
        long max = -1;
        String common = "";
        for (String iden :
                dedupList.keySet()) {
            if  (max < dedupList.get(iden).split("\\|").length){
                    max = dedupList.get(iden).split("\\|").length;
                    common = iden;
                }
            if ( dedupList.get(iden).split("\\|").length < count){
                System.out.println(iden + "," + dedupList.get(iden));
            }
        }
//        System.out.println(common+ "," + dedupList.get(common));




        return "";
    }
    private static ArrayList<String> getClassFromJar(String path) throws Exception{
        JarInputStream jarFile = new JarInputStream(new FileInputStream(path));
        JarEntry jarEntry;
        ArrayList<String> classList = new ArrayList<String>();
        while (true){
            jarEntry = jarFile.getNextJarEntry();
            if (jarEntry == null) break;

            if (jarEntry.getName().endsWith(".class")){
                String className = jarEntry.getName().replaceAll("/", "\\.").replaceAll("\\.class$", "");
                classList.add(className);
            }
        }
        return classList;
    }

    private static boolean checkSerializeAndDeserialize(CtClass clazz, Long serialVersionUID, URLClassLoader loader){
        try {
            String fakeClass = genString(clazz.getName().length());
            Object fakeClassObjc = ClazzScope.getOrGenerateClass(fakeClass, serialVersionUID.toString()).newInstance();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteArrayOutputStream);
            out.writeObject(fakeClassObjc);
            // Method for deserialization of object
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toString().replaceAll(Pattern.quote(fakeClass), Matcher.quoteReplacement(clazz.getName())).getBytes())){

                    @Override
                    public Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                    try {
                        return  loader.loadClass(desc.getName());
                    } catch (Exception e) { }

                    // Fall back (e.g. for primClasses)
                    return super.resolveClass(desc);
                }

            };
            String a = (String)in.readObject();
        } catch (IOException | ClassNotFoundException | InstantiationException| IllegalAccessError| IllegalAccessException | NoClassDefFoundError | UnsupportedOperationException | NullPointerException e) {
            return false;
        } catch (java.lang.ClassCastException e){

        }

        return true;
    }
    public static String genString(int targetStringLength) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();
        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return generatedString;
    }

    public static void addToClasspath(File file) {
        try {
            URL url = file.toURI().toURL();

            URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(classLoader, url);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    public static void removeFromClasspath(File file) {
        try{
            URL url = file.toURI().toURL();
            URLClassLoader urlClassLoader = (URLClassLoader)
                    ClassLoader.getSystemClassLoader();
            Class<?> urlClass = URLClassLoader.class;
            Field ucpField = urlClass.getDeclaredField("ucp");
            ucpField.setAccessible(true);
            URLClassPath ucp = (URLClassPath) ucpField.get(urlClassLoader);
            Class<?> ucpClass = URLClassPath.class;
            Field urlsField = ucpClass.getDeclaredField("urls");
            urlsField.setAccessible(true);
            Stack urls = (Stack) urlsField.get(ucp);
            urls.remove(url);
            System.out.println("remove class");
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }


    public static String[] getVersion(String s){
        int i = s.lastIndexOf('-');
        return new String[] {s.substring(0, i), s.substring(i+1).replaceAll("\\.jar$", "")};
    }
}

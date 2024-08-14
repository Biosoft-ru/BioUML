package biouml.workbench._test;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.ParameterAnnotationEntry;
import org.apache.bcel.classfile.RuntimeVisibleAnnotations;
import org.apache.bcel.classfile.RuntimeVisibleParameterAnnotations;
import org.apache.bcel.generic.Type;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.Repository;
import org.apache.bcel.util.SyntheticRepository;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DepDumper
{
    private static final Logger log = Logger.getLogger(DepDumper.class.getName());
    private String[] jars;
    private Set<String> usedClasses = new TreeSet<>();
    private Set<String> classPathClasses = new TreeSet<>();

    public DepDumper(String[] jars)
    {
        this.jars = jars;
    }
    
    public static void main(String... args) throws Exception
    {
        if(args.length == 0)
        {
            System.err.println("Use DepDumper <jar> [<jar>...]");
            return;
        }
        (new DepDumper(args)).run();
        //(new DepDumper(new String[] {"C:\\projects\\java\\BioUML\\plugins\\com.developmentontheedge.util_0.9.6\\dote.jar"})).run();
    }
    
    public void run() throws Exception
    {
        getDependencies(null);
        for(String clazz : usedClasses)
        {
            System.out.println(clazz);
        }
    }

    public Set<String> getDependencies(Collection<String> classes) throws Exception
    {
        Repository repository = createRepository();
        for(String jar: jars)
        {
            processJar(repository, jar);
        }
        Collection<String> curClasses = classes == null ? classPathClasses : classes;
        Collection<String> checkedClasses = new TreeSet<>();
        while(!curClasses.isEmpty())
        {
            for(String clazz : curClasses)
            {
                processClass(repository, clazz);
            }
            checkedClasses.addAll(curClasses);
            curClasses = new TreeSet<>(usedClasses);
            curClasses.retainAll(classPathClasses);
            curClasses.removeAll(checkedClasses);
        }
        usedClasses.removeAll(classPathClasses);
        return Collections.unmodifiableSet(usedClasses);
    }

    private void processJar(Repository repository, String jar) throws Exception
    {
        JarFile jarFile;
        try
        {
            jarFile = new JarFile(jar);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Unable to open "+jar+"; continue");
            return;
        }
        try
        {
            Enumeration<JarEntry> entries = jarFile.entries();
            while( entries.hasMoreElements() )
            {
                String entry = entries.nextElement().getName();
                if( entry.endsWith(".class") )
                {
                    String className = entry.substring(0, entry.length() - ".class".length());
                    className = className.replace('/', '.');
                    classPathClasses.add(className);
                }
            }
        }
        finally
        {
            jarFile.close();
        }
    }

    private Repository createRepository()
    {
        StringBuilder sb = new StringBuilder();
        for(String jar: jars)
        {
            if(sb.length() > 0)
                sb.append(File.pathSeparatorChar);
            sb.append(jar);
        }
        Repository repository = SyntheticRepository.getInstance(new ClassPath(sb.toString()));
        return repository;
    }
    
    private void processClass(Repository repository, String className) throws Exception
    {
        JavaClass javaClass = repository.loadClass(className);
        ConstantPool constantPool = javaClass.getConstantPool();
        for(Constant constant : constantPool.getConstantPool())
        {
            if(constant instanceof ConstantClass)
            {
                processUsedClass(((ConstantClass)constant).getBytes(constantPool));
            } else if(constant instanceof ConstantMethodref)
            {
                Constant nameType = constantPool.getConstant(( (ConstantMethodref)constant ).getNameAndTypeIndex());
                if(nameType instanceof ConstantNameAndType)
                {
                    Constant signature = constantPool.getConstant(( (ConstantNameAndType)nameType ).getSignatureIndex());
                    if(signature instanceof ConstantUtf8)
                    {
                        String signatureString = ((ConstantUtf8)signature).getBytes();
                        processUsedClass(Type.getReturnType(signatureString));
                        for(Type argument : Type.getArgumentTypes(signatureString))
                        {
                            processUsedClass(argument);
                        }
                    }
                }
            }
        }
        processAnnotations(javaClass.getAttributes());
        for(Method method: javaClass.getMethods())
        {
            processAnnotations(method.getAttributes());
            for(Type argument : method.getArgumentTypes())
            {
                processUsedClass(argument);
            }
            processUsedClass(method.getReturnType());
        }
    }

    private void processAnnotations(Attribute[] attributes)
    {
        for(Attribute attribute : attributes)
        {
            if( attribute instanceof RuntimeVisibleAnnotations )
            {
                for( AnnotationEntry entry : ( (RuntimeVisibleAnnotations)attribute ).getAnnotationEntries() )
                    processUsedClass(entry.getAnnotationType());
            }
            if( attribute instanceof RuntimeVisibleParameterAnnotations )
            {
                for(ParameterAnnotationEntry parameterEntry : ( (RuntimeVisibleParameterAnnotations)attribute ).getParameterAnnotationEntries())
                {
                    for( AnnotationEntry entry : parameterEntry.getAnnotationEntries())
                        processUsedClass(entry.getAnnotationType());
                }
            }
        }
    }

    protected void processUsedClass(Type type)
    {
        processUsedClass(type.getSignature());
    }

    protected void processUsedClass(String usedClass)
    {
        if(usedClass.isEmpty())
            return;
        if(usedClass.length() == 1 && "IDFCVZLBSJ".contains(usedClass))
            return;
        usedClass = usedClass.replace('/', '.');
        usedClass = usedClass.replaceFirst("^\\[*L(.+);$", "$1");
        if(usedClasses.contains(usedClass))
            return;
        try
        {
            if( usedClass.equals("org.rosuda.JRI.Rengine") )
                //Rengine.<clinit> causes System.exit when jri library not found
                return;
            if ( usedClass.startsWith("java.") || usedClass.startsWith("javax.") )
                return;
            Class<?> clazz = Class.forName(usedClass);
            if(clazz.getClassLoader() == null)
                return;
        }
        catch(Throwable e)
        {
        }
        usedClasses.add(usedClass);
    }

}

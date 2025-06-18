package biouml.workbench._test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import one.util.streamex.StreamEx;
import ru.biosoft.access.OsgiManifestParser;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.TextUtil2;

import com.developmentontheedge.application.ApplicationUtils;

public class Plugin
{
    private final String name;
    private final File pluginFile;
    private final List<String> dependencies = new ArrayList<>();
    private final Set<String> optionalDependencies = new HashSet<>();
    private final Set<String> exportedPackages = new HashSet<>();
    private final Set<String> unseenPackages = new HashSet<>();
    private final List<String> classPath = new ArrayList<>();
    private Map<String, Set<String>> classesOnClasspath;
    private List<String> exportedClasses = null;
    private List<String> requiredClasses = null;

    // Classes required directly by BioUML classes
    private List<String> bioumlRequiredClasses;

    public Plugin(File pluginFile) throws IOException
    {
        this.pluginFile = pluginFile;

        Manifest manifest = pluginFile.isDirectory() ? new Manifest(ApplicationUtils.readAsStream(new File(pluginFile,
                "META-INF/MANIFEST.MF"))) : new JarFile( pluginFile ).getManifest();
        Attributes attrs = manifest.getMainAttributes();

        name = TextUtil2.split( attrs.getValue("Bundle-SymbolicName"), ';' )[0];

        String value = attrs.getValue("Require-Bundle");
        if( value != null )
        {
            value = value.replaceAll("\"[^\"]*\"", "");
            for( String dep : TextUtil2.split( value, ',' ) )
            {
                String[] tokens = TextUtil2.split( dep, ';' );
                dependencies.add(tokens[0]);
                if( Arrays.asList(tokens).contains("resolution:=optional") )
                    optionalDependencies.add(tokens[0]);
            }
        }
        Collections.sort( dependencies );

        value = attrs.getValue("Export-Package");
        if( value != null )
        {
            try
            {
                exportedPackages.addAll(OsgiManifestParser.getStrings( value ));
            }
            catch( OsgiManifestParser.ParseException e )
            {
                throw new RuntimeException( "Error parsing Exported-Packages for "+name+":\n"+e.getMessage() );
            }
        }

        value = attrs.getValue("Bundle-ClassPath");
        if( value != null )
            for( String classPathEntry : TextUtil2.split( value, ',' ) )
            {
                classPathEntry = classPathEntry.trim();
                File classPathFile;
                if(pluginFile.isDirectory())
                    classPathFile = new File(pluginFile, classPathEntry);
                else
                {
                    classPathFile = new File(pluginFile.getParentFile(), classPathEntry);
                    if(!classPathFile.exists())
                    {
                        JarFile mainJar = new JarFile( pluginFile );
                        JarEntry nestedJarFile = mainJar.getJarEntry( classPathEntry );
                        if(nestedJarFile != null)
                            classPathFile = TempFiles.file( ".jar", mainJar.getInputStream( nestedJarFile ) );
                    }
                }
                classPath.add( classPathFile.getAbsolutePath() );
            }
        if( pluginFile.isFile() )
            classPath.add(pluginFile.getAbsolutePath());
        unseenPackages.addAll( exportedPackages );
    }


    public String getName()
    {
        return name;
    }

    public File getPluginFile()
    {
        return pluginFile;
    }

    public List<String> getDependencies()
    {
        return Collections.unmodifiableList(dependencies);
    }

    public boolean isOptionalDependency(String pluginName)
    {
        return optionalDependencies.contains(pluginName);
    }

    public Set<String> getExportedPackages()
    {
        return Collections.unmodifiableSet(exportedPackages);
    }

    public List<String> getClassPath()
    {
        return Collections.unmodifiableList(classPath);
    }

    public synchronized List<String> getExportedClasses() throws IOException
    {
        if( exportedClasses == null )
        {
            exportedClasses = new ArrayList<>();
            for( String classPathEntry : classPath )
            {
                File file = new File(classPathEntry);
                if( file.isFile() )
                {
                    JarFile jarFile = new JarFile(file);
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while( entries.hasMoreElements() )
                    {
                        String entry = entries.nextElement().getName();
                        int idx = entry.lastIndexOf('/');
                        String packageName = idx > 0 ? entry.substring(0, idx).replace('/', '.') : "";
                        if( entry.endsWith(".class") )
                        {
                            String className = entry.substring(0, entry.length() - ".class".length());
                            className = className.replace('/', '.');
                            if( exportedPackages.contains(packageName) )
                            {
                                exportedClasses.add(className);
                            }
                        }
                        unseenPackages.remove( packageName );
                    }
                }
            }
        }
        return exportedClasses;
    }

    public Set<String> getUnseenPackages() throws IOException
    {
        getExportedClasses();
        return unseenPackages;
    }

    public synchronized Map<String, Set<String>> getClassesOnClasspath() throws IOException
    {
        if( classesOnClasspath == null )
        {
            classesOnClasspath = new HashMap<>();
            for( String classPathEntry : classPath )
            {
                classesOnClasspath.put(classPathEntry, new HashSet<String>());
                File file = new File(classPathEntry);
                if( file.isFile() )
                {
                    JarFile jarFile = new JarFile(file);
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while( entries.hasMoreElements() )
                    {
                        String entry = entries.nextElement().getName();
                        if( entry.endsWith(".class") )
                        {
                            String className = entry.substring(0, entry.length() - ".class".length());
                            className = className.replace('/', '.');
                            classesOnClasspath.get(classPathEntry).add(className);
                        }
                    }
                }
            }
        }
        return classesOnClasspath;
    }

    private List<String> getRequiredClassesInternal(boolean checkOnlyBioUMLClasses) throws Exception
    {
        List<String> result = new ArrayList<>();

        Set<String> myClasses = StreamEx.ofValues( getClassesOnClasspath() ).flatMap( Collection::stream )
                .filter( clazz -> !checkOnlyBioUMLClasses || isBioUMLPrefix( clazz ) ).toSet();
        if( myClasses.isEmpty() )
            return result;

        result.addAll( new DepDumper( classPath.toArray( new String[classPath.size()] ) ).getDependencies( myClasses ) );
        return result;
    }

    public synchronized List<String> getBioUMLRequiredClasses() throws Exception
    {
        if( bioumlRequiredClasses == null )
            bioumlRequiredClasses = getRequiredClassesInternal( true );
        return bioumlRequiredClasses;
    }

    public synchronized List<String> getRequiredClasses() throws Exception
    {
        if( requiredClasses == null )
            requiredClasses = getRequiredClassesInternal( false );
        return requiredClasses;
    }

    public List<String> findExtraDependencies(Map<String, Plugin> allPlugins) throws Exception
    {
        List<String> result = new ArrayList<>();

        Set<String> requiredClasses = new HashSet<>( getRequiredClasses() );
        for( String dep : getDependencies() )
        {
            if( dep.equals( "com.mysql.cj" ) )
                //Mysql driver loads by Class.forName
                continue;
            if( dep.equals( "org.eclipse.osgi" ))
                //org.eclipse.osgi is necessary in every plugin for Netbeans integration
                continue;
            if( !allPlugins.containsKey( dep ) )
                continue;
            boolean extraDependency = true;
            for( String providedClass : allPlugins.get( dep ).getExportedClasses() )
                if( requiredClasses.contains( providedClass ) )
                {
                    extraDependency = false;
                    break;
                }
            if( extraDependency )
                result.add(dep);
        }

        return result;
    }

    public static boolean isBioUMLPrefix(String className)
    {
        return ( className.startsWith( "biouml" ) && !className.startsWith( "biouml.plugins.beakerx" ) )
                || ( className.startsWith( "com.developmentontheedge" ) && !className.startsWith( "com.developmentontheedge.beans" ) )
                || ( className.startsWith( "ru.biosoft" ) && !className.startsWith( "ru.biosoft.graphics.editor" )
                        && !className.startsWith( "ru.biosoft.graphics.core" ) && !className.startsWith( "ru.biosoft.jobcontrol" )
                        && !className.startsWith("ru.biosoft.exception") && !className.startsWith("ru.biosoft.physicell") && !className.startsWith("ru.biosoft.rtree"));
    }


    @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof Plugin && ( (Plugin)obj ).getName().equals(name);
    }

    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    public static List<Plugin> loadPlugins(File baseDir)
    {
        List<Plugin> result = new ArrayList<>();
        File[] files = baseDir.listFiles();
        Arrays.sort( files, (a,b)->a.getName().compareTo( b.getName() ) );
        for( File plugin : files )
            if( plugin.isDirectory() || plugin.getName().endsWith(".jar") )
                try
                {
                    result.add(new Plugin(plugin));
                }
                catch( Exception e )
                {
                    System.err.println("Can not load plugin from " + plugin.getAbsolutePath());
                    e.printStackTrace();
                }
        return result;
    }

}

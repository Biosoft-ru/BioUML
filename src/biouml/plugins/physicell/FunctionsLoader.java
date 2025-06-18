package biouml.plugins.physicell;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Logger;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.util.TempFiles;


public class FunctionsLoader
{
    private static String LIBRARY_NAME = "ru.biosoft.physicell_2025.2.jar";

    public static <T> T load(DataElementPath dep, Class<T> c, Logger log) throws Exception
    {
        return load( dep, c, log, null );
    }

    public static <T> T load(DataElementPath dep, Class<T> c, Logger log, Model model) throws Exception
    {
        ScriptDataElement element = dep.getDataElement( ScriptDataElement.class );
        String content = element.getContent();
        String name = element.getName();
        String fileName;
        String className;
        if( name.endsWith( ".java" ) )
        {
            fileName = name;
            className = name.substring( 0, name.lastIndexOf( "." ) );
        }
        else if( name.contains( "." ) )
        {
            className = name.substring( 0, name.lastIndexOf( "." ) );
            fileName = name + ".java";
        }
        else
        {
            fileName = name + ".java";
            className = name;
        }
        String out = TempFiles.dir( "JPhysicell" ).getAbsolutePath();
        File temp = new File( out, fileName );

        ApplicationUtils.writeString( temp, content );
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        File jarFile = new File( FunctionsLoader.class.getProtectionDomain().getCodeSource().getLocation().getFile() );
        String libPath = new File( jarFile.getParentFile(), LIBRARY_NAME ).getAbsolutePath();

        StringStream outputStream = new StringStream();
        compiler.run( null, null, outputStream, "-d", out, "-classpath", libPath, out + "/" + fileName );
        String error = outputStream.toString();
        if( !error.isEmpty() )
            log.info( "\nCompilation error in java class " + dep.getName() + ":\n" + error );

        File resultFile = new File( out );
        URL[] url = new URL[] {resultFile.toURI().toURL()};
        try (URLClassLoader cl = new URLClassLoader( url, FunctionsLoader.class.getClassLoader() ))
        {
            try
            {
                return cl.loadClass( className ).asSubclass( c ).getDeclaredConstructor( Model.class ).newInstance( model );
            }
            catch( Exception ex )
            {
                return cl.loadClass( className ).asSubclass( c ).getDeclaredConstructor().newInstance();
            }
        }
    }

    public static class StringStream extends OutputStream
    {
        private StringBuilder string = new StringBuilder();

        @Override
        public void write(int b) throws IOException
        {
            this.string.append( (char)b );
        }

        @Override
        public String toString()
        {
            return this.string.toString();
        }
    }

}
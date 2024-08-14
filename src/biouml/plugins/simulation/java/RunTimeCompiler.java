package biouml.plugins.simulation.java;

import java.io.ByteArrayOutputStream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;

/**
 * Class to compile generated Java models.
 */
@CodePrivilege ( CodePrivilegeType.REFLECTION )
public class RunTimeCompiler
{
    protected static final Logger log = Logger.getLogger( RunTimeCompiler.class.getName() );
    protected String classpath = ".";
    protected String srcDir;
    protected String outDir;
    protected String[] files;
    protected ErrorOutputStream outStream = new ErrorOutputStream();

    public RunTimeCompiler(String classpath, String srcDir, String[] files)
    {
        this.classpath = classpath;
        this.srcDir = srcDir;
        this.outDir = srcDir;
        this.files = files;
    }

    /**
     * Use introspection to not include javac into build classpath.
     */
    public boolean execute()
    {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        for( int i = 0; i < files.length; i++ )
        {
            int results = compiler.run( null, null, outStream, "-d", outDir, "-classpath", classpath, files[i] );
            if( results != 0 )
            {
                log.log( Level.SEVERE, "can not compile model" );
                return false;
            }
        }
        return true;
    }

    public String getMessages()
    {
        return outStream.toString();
    }

    public static class ErrorOutputStream extends ByteArrayOutputStream
    {
        public void clear()
        {
            try
            {
                flush();
            }
            catch( Throwable t )
            {
            }

            buf = new byte[32];
            count = 0;
        }
    }
}

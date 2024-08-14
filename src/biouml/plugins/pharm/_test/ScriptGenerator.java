package biouml.plugins.pharm._test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates R script which inits all BioUML plugin jars
 * @author Ilya
 *
 */
public class ScriptGenerator
{

    String folderPath = "C:/projects/java/BioUML/plugins";

    public String generate()
    {
        StringBuilder builder = new StringBuilder("");
        
        builder.append( "library(rJava)" );
        builder.append( ".jinit()" );
        List<File> files = getFiles( new File( folderPath ) );

        for (File f: files)
        {
            builder.append( ".jadd(\"" );
            builder.append(f.getAbsolutePath());
            builder.append("\")");
        }
        String result = builder.toString();
        System.out.println(result);
        return result;
    }



    public List<File> getFiles(File f)
    {
        List<File> result = new ArrayList<>();

        if( f.isDirectory() )
        {
            for( File innerF : f.listFiles() )
            {
                if( innerF.isDirectory() )
                    result.addAll( getFiles( innerF ) );
                else if( innerF.getName().endsWith( ".jar" ) )
                    result.add( innerF );
            }
        }
        return result;
    }
}

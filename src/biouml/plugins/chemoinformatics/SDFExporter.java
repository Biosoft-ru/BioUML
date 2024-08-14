package biouml.plugins.chemoinformatics;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import biouml.standard.type.Structure;

import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.FunctionJobControl;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.DataElementExporterRegistry;

public class SDFExporter implements DataElementExporter
{
    @Override
    public int accept(DataElement de)
    {
        if( ( de instanceof DataCollection ) && ( Structure.class.isAssignableFrom( ( (DataCollection)de ).getDataElementType()) ) )
            return DataElementExporter.ACCEPT_HIGH_PRIORITY;
        return DataElementExporter.ACCEPT_UNSUPPORTED;
    }
    
    @Override
    public List<Class<? extends DataElement>> getSupportedTypes()
    {
        return Arrays.asList( ru.biosoft.access.core.DataCollection.class );
    }
    

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file) throws Exception
    {
        doExport(de, file, null);
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file, FunctionJobControl jobControl) throws Exception
    {
        if( jobControl != null )
        {
            jobControl.functionStarted();
        }
        exportStructures((DataCollection)de, file);
        if( jobControl != null )
        {
            jobControl.functionFinished();
        }
    }

    @Override
    public boolean init(Properties properties)
    {
        if( properties.getProperty(DataElementExporterRegistry.SUFFIX).equals("txt") )
            return true;
        return false;
    }

    /**
     * Export structure collection in SDF format
     */
    public static void exportStructures(DataCollection structures, File file) throws Exception
    {
        if( Structure.class.isAssignableFrom( ( structures ).getDataElementType()) )
        {
            try (PrintWriter pw = new PrintWriter( file ))
            {
                Iterator<Structure> iter = structures.iterator();
                while( iter.hasNext() )
                {
                    Structure structure = iter.next();
                    pw.println();
                    pw.print( structure.getData() );

                    DynamicPropertySet dps = structure.getAttributes();
                    Iterator<String> pIter = dps.nameIterator();
                    while( pIter.hasNext() )
                    {
                        String pName = pIter.next();
                        Object pValue = dps.getValue( pName );
                        pw.println( ">  <" + pName + ">" );
                        pw.println( pValue );
                    }

                    pw.print( "$$$$" );
                }
            }
        }
    }
}

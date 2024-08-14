package biouml.model.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.DiagramImporter;
import biouml.model.Module;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.security.Permission;
import ru.biosoft.jobcontrol.FunctionJobControl;

/**
 * Import diagram from BioUML format (.dml)
 */
public class DiagramDMLImporter extends DiagramImporter
{
    protected static final Logger log = Logger.getLogger( DiagramDMLImporter.class.getName() );

    @Override
    public int accept(File file)
    {
        try
        {
            String header = ApplicationUtils.readAsString(file, 1000);

            int iXml = header.indexOf("<?xml");
            if( iXml == -1 ) return ACCEPT_UNSUPPORTED;

            if( !header.substring(iXml, iXml + 100 > header.length() ? header.length() : iXml + 100).matches(
                    "(\\s)*<\\?xml(\\s)*version(\\s)*=(.|\\s)*") )
            {
                return ACCEPT_UNSUPPORTED;
            }

            if( header.indexOf("<dml") == -1 )
                return ACCEPT_UNSUPPORTED;

            return ACCEPT_HIGH_PRIORITY;
        }
        catch( Throwable t )
        {
            log.log( Level.SEVERE, "accept error :", t );
        }

        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public DataElement doImport(Module module, File file, String diagramName) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File file, String diagramName, FunctionJobControl jobControl, Logger log) throws Exception
    {
        if( jobControl != null )
            jobControl.functionStarted();
        try
        {
            parent = DataCollectionUtils.fetchPrimaryCollection(parent, Permission.WRITE);
            DataCollection<?> actualParent = null;
            Module module = Module.optModule( parent );
            if( parent instanceof FolderCollection && parent.isAcceptable( Diagram.class ) )
            {
                actualParent = parent;
            }
            else
            {                
                actualParent = module == null ? parent : module.getDiagrams();
                if( actualParent == null )
                    actualParent = parent;
            }
            if( diagramName == null || diagramName.trim().isEmpty() )
                throw new IllegalArgumentException( "Incorrect diagram name" );

            Diagram diagram =  DiagramXmlReader.readDiagram(diagramName, new FileInputStream( file ), null, actualParent, module, null, newPaths);
            ( (DataCollection)actualParent ).put( diagram );
            if(jobControl != null) jobControl.functionFinished();
            return diagram;
        }
        catch( Exception e )
        {
            if( jobControl != null )
                jobControl.functionTerminatedByError( e );
            throw e;
        }
    }

    @Override
    public int accept(DataCollection parent, File file)
    {
        if( DataCollectionUtils.isAcceptable(parent, Diagram.class) )
            return file == null ? ACCEPT_HIGH_PRIORITY : accept(file);
        return super.accept(parent, file);
    }
}
package biouml.plugins.antimony;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.DiagramImporter;
import biouml.model.Module;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.standard.type.DiagramInfo;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.jobcontrol.FunctionJobControl;

public class AntimonyImporter extends DiagramImporter
{
    @Override
    public int accept(File file)
    {
        if( !file.canRead() )
            return ACCEPT_UNSUPPORTED;
        return detectAntimony( file );
    }

    private int detectAntimony(File file)
    {
        try (BufferedReader br = ApplicationUtils.utfReader( file ))
        {
            String line = br.readLine();
            while( line != null )
            {
                line = line.trim();
                if( line.isEmpty() || line.startsWith( "#" ) || line.startsWith( "//" ) )
                {
                    //skip comment
                }
                else if( line.startsWith( "/*" ) )
                {
                    while( !line.endsWith( "*/" ) )//look for multiple line comment end
                        line = br.readLine();
                }
                else
                {
                    if( line.startsWith( "model " ) )
                        return ACCEPT_HIGH_PRIORITY;
                    else
                        return ACCEPT_UNSUPPORTED;
                }
                line = br.readLine();
            }
        }
        catch( Exception ex )
        {

        }
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public DataElement doImport(Module module, File file, String diagramName) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File file, String diagramName, FunctionJobControl jobControl,
            Logger log) throws Exception
    {
        if( jobControl != null )
            jobControl.functionStarted();
        try (FileInputStream in = new FileInputStream(file); InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
        {
            Diagram prototypeDiagram = new SbgnDiagramType().createDiagram(parent, diagramName, new DiagramInfo(diagramName));

            Diagram diagram = new Antimony(prototypeDiagram).generateDiagram(reader);
            if( diagram == null )
                throw new IllegalArgumentException("Can't import diagram");

            AntimonyUtility.setAntimonyAttribute( diagram, "2.0", AntimonyConstants.ANTIMONY_VERSION_ATTR );
            AntimonyUtility.setAntimonyAttribute(diagram, ApplicationUtils.readAsString(file), AntimonyConstants.ANTIMONY_TEXT_ATTR);

            if( jobControl != null )
                jobControl.functionFinished();
            CollectionFactoryUtils.save(diagram);
            return diagram;
        }
        catch( Exception e )
        {
            if( jobControl != null )
                jobControl.functionTerminatedByError(e);
            throw e;
        }
    }

    @Override
    public int accept(DataCollection parent, File file)
    {
        if( parent.isAcceptable(Diagram.class) )
            return file == null ? ACCEPT_HIGH_PRIORITY : accept(file);
        return super.accept(parent, file);
    }
}

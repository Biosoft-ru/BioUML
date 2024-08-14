package biouml.plugins.bionetgen.diagram;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.DiagramImporter;
import biouml.model.Module;
import biouml.plugins.bionetgen.bnglparser.BNGStart;
import biouml.plugins.bionetgen.bnglparser.BionetgenParser;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * Import diagram from BioNetGen language format (.bngl) and deploy it
 * to get full reactions network and all possible species if necessary
 */
public class BionetgenImporter extends DiagramImporter
{
    protected static final Logger log = Logger.getLogger( BionetgenImporter.class.getName() );
    private BNGImporterOptions properties;

    @Override
    public int accept(File file)
    {
        if( !file.canRead() )
            return ACCEPT_UNSUPPORTED;
        try (FileInputStream is = new FileInputStream(file); InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8))
        {
            BionetgenParser parser = new BionetgenParser();
            parser.parse("acceptTest", reader);
            if( parser.getStatus() != BionetgenParser.STATUS_OK )
            {
                return ACCEPT_UNSUPPORTED;
            }
            return ACCEPT_HIGH_PRIORITY;
        }
        catch( Throwable t )
        {
            return ACCEPT_UNSUPPORTED;
        }
    }

    @Override
    public int accept(DataCollection parent, File file)
    {
        if( parent instanceof FolderCollection && parent.isAcceptable(Diagram.class) )
            return file == null ? ACCEPT_HIGH_PRIORITY : accept(file);
        return super.accept(parent, file);
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

        boolean needDeploy = false;
        boolean needLayout = true;
        if( properties != null )
        {
            if( !properties.getDiagramName().isEmpty() )
                diagramName = properties.getDiagramName();
            needDeploy = properties.isDeploy();
            needLayout = properties.isLayout();
        }

        try (FileInputStream is = new FileInputStream(file); InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8))
        {
            BionetgenParser parser = new BionetgenParser();
            BNGStart start = parser.parse(diagramName, reader);
            if( jobControl != null )
                jobControl.setPreparedness(25);
            Diagram diagram = BionetgenDiagramGenerator.generateDiagram( start, parent, diagramName, needDeploy ? false : needLayout );
            if( diagram.getName() == null )
                throw new IllegalArgumentException("Incorrect diagram name");
            if( needDeploy )
            {
                if( jobControl != null )
                    jobControl.setPreparedness(40);
                diagram = BionetgenDiagramDeployer.deployBNGDiagram(diagram, needLayout);
                if( jobControl != null )
                    jobControl.setPreparedness(85);
            }
            else if( jobControl != null )
            {
                jobControl.setPreparedness(50);
                jobControl.functionFinished();
            }
            CollectionFactoryUtils.save(diagram);
            if( jobControl != null )
            {
                jobControl.setPreparedness(100);
                jobControl.functionFinished();
            }
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
    public Object getProperties(DataCollection parent, File file, String elementName)
    {
        properties = new BNGImporterOptions( elementName );
        return properties;
    }

    @SuppressWarnings ( "serial" )
    @PropertyName ( "Importer options" )
    @PropertyDescription ( "Importer options." )
    public static class BNGImporterOptions extends OptionEx
    {
        public BNGImporterOptions(String diagramName)
        {
            this.diagramName = diagramName == null ? "" : diagramName;
        }

        String diagramName;
        boolean deploy = false;
        boolean layout = true;
        @PropertyName ( "Diagram name" )
        @PropertyDescription ( "Name of imported diagram." )
        public String getDiagramName()
        {
            return diagramName;
        }
        public void setDiagramName(String diagramName)
        {
            this.diagramName = diagramName;
        }
        @PropertyName ( "Need deploy" )
        @PropertyDescription ( "Flag to show if diagram should be deployed." )
        public boolean isDeploy()
        {
            return deploy;
        }
        public void setDeploy(boolean deploy)
        {
            this.deploy = deploy;
        }
        @PropertyName ( "Need layout" )
        @PropertyDescription ( "Flag to show if diagram should be layouted." )
        public boolean isLayout()
        {
            return layout;
        }
        public void setLayout(boolean layout)
        {
            this.layout = layout;
        }
    }

    public static class BNGImporterOptionsBeanInfo extends BeanInfoEx2<BNGImporterOptions>
    {
        public BNGImporterOptionsBeanInfo()
        {
            super(BNGImporterOptions.class);
        }
        @Override
        public void initProperties() throws Exception
        {
            add("diagramName");
            add("deploy");
            add("layout");
        }
    }

}

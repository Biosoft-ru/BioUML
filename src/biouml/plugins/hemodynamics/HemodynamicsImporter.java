package biouml.plugins.hemodynamics;

import java.io.File;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Diagram;
import biouml.model.DiagramImporter;
import biouml.model.Module;

import com.developmentontheedge.beans.Option;
import ru.biosoft.jobcontrol.FunctionJobControl;

public class HemodynamicsImporter extends DiagramImporter
{
    private HemodynamicsImporterOptions properties;

    @Override
    public int accept(File file)
    {
        if( !file.canRead() )
            return ACCEPT_UNSUPPORTED;
        return ACCEPT_LOW_PRIORITY;
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

        try
        {
            HemodynamicsDiagramGenerator generator = new HemodynamicsDiagramGenerator( properties.getDiagramName() );
            generator.setAddBranches( properties.addBranches );
            generator.setExtentAreaFactor( properties.extentAreaFactor );
            generator.setGeneratePorts( properties.generatePorts );
            generator.setOutletAreaFactor( properties.outletAreaFactor );
            Diagram diagram = generator.createDiagram( parent, file );

            if( jobControl != null )
                jobControl.functionFinished();

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
        if( parent.isAcceptable( Diagram.class ) )
            return file == null ? ACCEPT_HIGH_PRIORITY : accept( file );
        return super.accept( parent, file );
    }

    @Override
    public Object getProperties(DataCollection parent, File file, String elementName)
    {
        return properties = new HemodynamicsImporterOptions( elementName );
    }

    public static class HemodynamicsImporterOptions extends Option
    {
        private String diagramName;
        private boolean addBranches = false;
        private boolean generatePorts = true;
        private double outletAreaFactor = 0.1;
        private double extentAreaFactor = 0.9;

        public HemodynamicsImporterOptions(String name)
        {
            this.diagramName = name;
        }

        public boolean isNotAddBranches()
        {
            return !isAddBranches();
        }

        @PropertyName ( "Additional branches" )
        @PropertyDescription ( "Add branches for each vessel." )
        public boolean isAddBranches()
        {
            return addBranches;
        }
        public void setAddBranches(boolean addBranches)
        {
            this.addBranches = addBranches;
        }

        @PropertyName ( "Generate ports" )
        @PropertyDescription ( "Generate ports." )
        public boolean isGeneratePorts()
        {
            return generatePorts;
        }
        public void setGeneratePorts(boolean generatePorts)
        {
            this.generatePorts = generatePorts;
        }

        @PropertyName ( "Additional branches area" )
        @PropertyDescription ( "Additional branches area." )
        public double getOutletAreaFactor()
        {
            return outletAreaFactor;
        }
        public void setOutletAreaFactor(double outletAreaFactor)
        {
            this.outletAreaFactor = outletAreaFactor;
        }

        @PropertyName ( "Main branches area" )
        @PropertyDescription ( "Main branches area." )
        public double getExtentAreaFactor()
        {
            return extentAreaFactor;
        }
        public void setExtentAreaFactor(double extentAreaFactor)
        {
            this.extentAreaFactor = extentAreaFactor;
        }

        @PropertyName ( "Diagram name" )
        @PropertyDescription ( "Diagram name." )
        public String getDiagramName()
        {
            return diagramName;
        }

        public void setDiagramName(String diagramName)
        {
            this.diagramName = diagramName;
        }
    }

    public static class HemodynamicsImporterOptionsBeanInfo extends BeanInfoEx2<HemodynamicsImporterOptions>
    {
        public HemodynamicsImporterOptionsBeanInfo()
        {
            super( HemodynamicsImporterOptions.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            add( "diagramName" );
            add( "addBranches" );
            add( "generatePorts" );
            add( "outletAreaFactor");
            add( "extentAreaFactor");
        }
    }
}

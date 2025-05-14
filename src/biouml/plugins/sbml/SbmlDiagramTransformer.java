package biouml.plugins.sbml;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Diagram;
import biouml.model.DiagramViewOptions;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbml.converters.SBGNConverterNew;
import biouml.standard.type.DiagramInfo;
import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.file.FileTypePriority;
import ru.biosoft.access.generic.PriorityTransformer;
import ru.biosoft.graph.PathLayouterWrapper;
import ru.biosoft.util.bean.StaticDescriptor;

public class SbmlDiagramTransformer extends AbstractFileTransformer<Diagram> implements PriorityTransformer
{
    public static final String BASE_DIAGRAM_TYPE = "baseDiagramType";
    protected static final PropertyDescriptor BASE_DIAGRAM_TYPE_PD = StaticDescriptor.createReadOnly(BASE_DIAGRAM_TYPE);

    /**
     * Return class of output data element.
     * Output data element stored in transformed data collection.
     * @return Class of output data element.
     */
    @Override
    public Class<Diagram> getOutputType()
    {
        return Diagram.class;
    }

    public Diagram getDiagramToWrite(Diagram diagram) throws Exception
    {
        if( diagram.getType() instanceof SbgnDiagramType )
        {
            Diagram sbmlDiagram = SBGNConverterNew.restore(diagram);
            sbmlDiagram.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME, Diagram.class, diagram));
            return sbmlDiagram;
        }
        return diagram;
    }

    @Override
    public int getInputPriority(Class<? extends DataElement> inputClass, DataElement de)
    {
        if( FileDataElement.class.isAssignableFrom(inputClass) && ( de instanceof Diagram ) )
        {
            if( ( (Diagram)de ).getType() instanceof SbgnDiagramType )
                return 12;
        }
        return 0;
    }

    @Override
    public Diagram load(File input, String name, DataCollection<Diagram> origin) throws Exception
    {
        Diagram diagram = SbmlModelFactory.readDiagram(input, name, origin, null);
        Object sbgnDiagramObj = diagram.getAttributes().getValue(SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME);
        if( ! ( sbgnDiagramObj instanceof Diagram ) )
            sbgnDiagramObj = SBGNConverterNew.convert(diagram); //convert anyway
        Diagram sbgnDiagram = (Diagram)sbgnDiagramObj;
        copyProperties(diagram, sbgnDiagram);
        sbgnDiagram.getAttributes().add(new DynamicProperty(BASE_DIAGRAM_TYPE_PD, String.class, diagram.getType().getClass().getName()));
        return sbgnDiagram;
    }

    private void copyProperties(Diagram sbmlDiagram, Diagram sbgnDiagram)
    {
        ( (DiagramInfo)sbgnDiagram.getKernel() ).setDatabaseReferences( ( (DiagramInfo)sbmlDiagram.getKernel() ).getDatabaseReferences());
        ( (DiagramInfo)sbgnDiagram.getKernel() )
                .setLiteratureReferences( ( (DiagramInfo)sbmlDiagram.getKernel() ).getLiteratureReferences());

        DiagramViewOptions viewOptions = sbgnDiagram.getViewOptions();
        boolean notificationEnabled = viewOptions.isNotificationEnabled();
        viewOptions.setNotificationEnabled(false);
        viewOptions.setDependencyEdges(sbmlDiagram.getViewOptions().isDependencyEdges());
        viewOptions.setAutoLayout(sbmlDiagram.getViewOptions().isAutoLayout());
        if( sbmlDiagram.getViewOptions().isAutoLayout() && sbmlDiagram.getViewOptions().getPathLayouter() != null )
        {
            viewOptions.setPathLayouterWrapper(new PathLayouterWrapper(sbmlDiagram.getViewOptions().getPathLayouter()));
        }
        viewOptions.setNotificationEnabled(notificationEnabled);

        //TODO: handle without explicit string
        DynamicProperty dp = sbmlDiagram.getAttributes().getProperty("metaIds");
        if( dp != null )
            sbgnDiagram.getAttributes().add(dp);
        dp = sbmlDiagram.getAttributes().getProperty("Antimony text");
        if( dp != null )
            sbgnDiagram.getAttributes().add(dp);
        dp = sbmlDiagram.getAttributes().getProperty("Antimony version");
        if( dp != null )
            sbgnDiagram.getAttributes().add(dp);
        //TODO: handle without explicit string
        dp = sbmlDiagram.getAttributes().getProperty("pfUser");
        if( dp != null )
            sbgnDiagram.getAttributes().add(dp);
        //TODO: handle without explicit string
        dp = sbmlDiagram.getAttributes().getProperty("autoscale");
        if( dp != null )
            sbgnDiagram.getAttributes().add(dp);
        dp = sbmlDiagram.getAttributes().getProperty(SbmlConstants.BIOUML_BIOHUB_ATTR);
        if( dp != null )
            sbgnDiagram.getAttributes().add(dp);
        dp = sbmlDiagram.getAttributes().getProperty(SbmlConstants.BIOUML_REFERENCE_TYPE_ATTR);
        if( dp != null )
            sbgnDiagram.getAttributes().add(dp);
        dp = sbmlDiagram.getAttributes().getProperty(SbmlConstants.BIOUML_CONVERTER_ATTR);
        if( dp != null )
            sbgnDiagram.getAttributes().add(dp);
        dp = sbmlDiagram.getAttributes().getProperty(SbmlConstants.BIOUML_SPECIES_ATTR);
        if( dp != null )
            sbgnDiagram.getAttributes().add(dp);
    }

    @Override
    public void save(File output, Diagram element) throws Exception
    {
        SbmlModelFactory.writeDiagram(output, getDiagramToWrite(element));
    }

    @Override
    public int getOutputPriority(String name)
    {
        return name.endsWith(".sbml") ? 2 : 0;
    }
}

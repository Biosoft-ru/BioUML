package biouml.standard.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.editor.ViewEditorPane;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.DiagramViewBuilder;
import biouml.model.InitialElementPropertiesSupport;
import biouml.model.ModelDefinition;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.SubDiagram;
import one.util.streamex.StreamEx;

@SuppressWarnings("serial")
@PropertyName("Subdiagram properties")
@PropertyDescription("Subdiagram properties.")
public class SubDiagramProperties extends InitialElementPropertiesSupport
{
    private static final Logger log = Logger.getLogger(SubDiagramProperties.class.getName());
    public static final String NOT_SELECTED = "Not selected";

    private final Module module;
    private final Diagram upperDiagram;
    private final String[] availableModelDefs;

    private DataElementPath diagramPath; //inner diagram path
    private String name = "Subdiagram"; //subdiagram element name
    private boolean external = true;
    private String modelDefinitionName = NOT_SELECTED;

    public SubDiagramProperties(Diagram diagram)
    {
        this.module = Module.getModule(diagram);
        this.upperDiagram = diagram;

        availableModelDefs = Util.getModelDefinitionNames(diagram).toArray(String[]::new);
        if( availableModelDefs.length > 0 )
            modelDefinitionName = availableModelDefs[0];
    }

    //TODO: more explicit error printing
    public void setDiagramPath(DataElementPath diagramPath)
    {
        DataElement obj = diagramPath.getDataElement();
        if( ! ( obj instanceof Diagram ) )
        {
            log.log(Level.SEVERE, "Only diagram may be selected for subidgram");
            return;
        }
        if( !Module.getModule(obj).equals(module) )
        {
            log.log(Level.SEVERE, "Please select diagram from module " + module.getName());
            return;
        }

        this.diagramPath = diagramPath;
        setName(diagramPath.getName());
    }

    @Override
    public DiagramElementGroup doCreateElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        if( name.isEmpty() )
        {
            log.log(Level.SEVERE, "Please choose name for subdiagram");
            return null;
        }
        Diagram diagram = null;
        if( isExternal() )
        {
            diagram = (Diagram)diagramPath.getDataElement();
        }
        else
        {
            if (modelDefinitionName.equals(NOT_SELECTED))
            {
                log.log(Level.SEVERE, "Please choose model definition");
                return null;
            }
            DiagramElement de = upperDiagram.get(modelDefinitionName);
            diagram = de.cast( ModelDefinition.class ).getDiagram();
        }
        SubDiagram de = new SubDiagram(compartment, diagram, DefaultSemanticController.generateUniqueNodeName(compartment, name));
        arrangeView(de);
        return new DiagramElementGroup( de );
    }

    @PropertyName("External diagram")
    @PropertyDescription("Type of associated diagram.")
    public boolean isExternal()
    {
        return external;
    }
    public void setExternal(boolean external)
    {
        this.external = external;
        this.firePropertyChange("*", null, null);
    }

    @PropertyName("Name")
    @PropertyDescription("Subdiagram name.")
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = validateName(name);
    }

    @PropertyName("Diagram")
    @PropertyDescription("Diagram associated with subdiagram.")
    public DataElementPath getDiagramPath()
    {
        return diagramPath;
    }

    @PropertyName("Model definition")
    @PropertyDescription("Model definition name.")
    public String getModelDefinitionName()
    {
        return modelDefinitionName;
    }
    public void setModelDefinitionName(String modelDefinitionName)
    {
        this.modelDefinitionName = modelDefinitionName;
    }

    public String[] getAvailableModelDefinitions()
    {
        return availableModelDefs;
    }

    public boolean isInternal()
    {
        return !isExternal();
    }

    protected String validateName(String name)
    {
        return name;
    }

    private void arrangeView(Compartment c) throws Exception
    {
        SemanticController controller = Diagram.getDiagram(c).getType().getSemanticController();
        DiagramViewBuilder viewBuilder = Diagram.getDiagram(c).getType().getDiagramViewBuilder();
        viewBuilder.createCompartmentView(c, Diagram.getDiagram(c).getViewOptions(), ApplicationUtils.getGraphics());

        int width = c.getShapeSize().width;
        int height = c.getNodes().length * 50;
        c.setShapeSize(new Dimension(width, height));

        int x = 10;


        for (String name: StreamEx.of(c.getNameList()).sorted())
        {
            Node node = c.findNode(name);
            Dimension dim = new Dimension(width, x);
            controller.move(node, c, dim , node.getView().getBounds());
            x+=node.getView().getBounds().height;
        }

        height = x - c.getLocation().x + 10;
        c.setShapeSize(new Dimension(width, height));
    }
}

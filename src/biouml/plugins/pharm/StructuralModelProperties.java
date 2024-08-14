package biouml.plugins.pharm;

import java.awt.Point;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.Option;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.DPSUtils;

import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Module;

public class StructuralModelProperties extends Option implements InitialElementProperties
{
    private static final Logger log = Logger.getLogger(StructuralModelProperties.class.getName());

    private Module module;
    private DataElementPath diagramPath;
    private String name = "Subdiagram";

    public static final String NOT_SELECTED = "Not selected";

    public StructuralModelProperties(Diagram diagram)
    {
        this.module = Module.getModule(diagram);
    }

    public Module getModule()
    {
        return module;
    }

    @PropertyName ( "Name" )
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @PropertyName ( "Diagram path" )
    public DataElementPath getDiagramPath()
    {
        return diagramPath;
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
        this.name = diagramPath.getName();

    }

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        if( name.isEmpty() )
            throw new Exception("Please choose name for subdiagram");

        if( diagramPath.isEmpty() )
            throw new Exception("Please choose path for diagram");

        Diagram diagram = (Diagram)diagramPath.getDataElement();

        StructuralModel de = new StructuralModel(compartment, diagram, DefaultSemanticController.generateUniqueNodeName(compartment, name));
        de.getAttributes().add(DPSUtils.createHiddenReadOnly("DiagramPath", DataElementPath.class, DataElementPath.create(diagram)));

        boolean isNotificationEnabled = compartment.isNotificationEnabled();
        compartment.setNotificationEnabled(true);
        viewPane.add(de, location);
        compartment.setNotificationEnabled(isNotificationEnabled);
        return new DiagramElementGroup( de );
    }


}

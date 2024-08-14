package biouml.plugins.sbgn;

import java.awt.Point;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementPropertiesSupport;
import biouml.model.SemanticController;
import biouml.plugins.sbml.SbmlSupport;
import biouml.standard.type.Specie;
import ru.biosoft.access.exception.BiosoftCustomException;
import ru.biosoft.graphics.editor.ViewEditorPane;

@PropertyName("SBGN element")
public class SbgnElementProperties extends InitialElementPropertiesSupport
{
    private String name;
    private String title;
    private String type;
    private DynamicPropertySet properties;

    public SbgnElementProperties(String type, String name)
    {
        this.properties = SbgnSemanticController.getDPSByType(type);
        this.name = name;
        this.title = name;
        this.type = type;
    }

    @PropertyName ( "name" )
    @PropertyDescription ( "name" )
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        String oldName = this.name;
        this.name = validateName(name);
        if( title.equals( oldName ) )
            title = name;
    }
    
    @PropertyName ( "title" )
    @PropertyDescription ( "title" )
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }

    private String validateName(String name)
    {
        return SbmlSupport.castStringToSId(name);
    }

    @PropertyName ( "properties" )
    @PropertyDescription ( "properties" )
    public DynamicPropertySet getProperties()
    {
        return properties;
    }
    public void setProperties(DynamicPropertySet dps)
    {
        this.properties = dps;
    }

    @Override
    public DiagramElementGroup doCreateElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        String kernelType = properties.getValueAsString(SBGNPropertyConstants.SBGN_ENTITY_TYPE);
        if( kernelType != null )
            type = kernelType;

        name = DefaultSemanticController.generateUniqueNodeName( compartment, name );
        DiagramElement de = SbgnSemanticController.createDiagramElement( type, name, compartment );
        SemanticController controller = Diagram.getDiagram( compartment ).getType().getSemanticController();
        
        if( de.getKernel() instanceof Specie )
            title = SbgnComplexStructureManager.validateTitle( title );
        de.setTitle( title );

        for( DynamicProperty dp : properties )
        {
            if( !dp.getName().equals( SBGNPropertyConstants.SBGN_ENTITY_TYPE ) )
                de.getAttributes().add( dp );
        }

        if( de.getKernel() instanceof Specie ) //TODO: move creation of species to separate class 
            SbgnComplexStructureManager.annotateSpecies( (Compartment)de );

        if( !controller.canAccept( compartment, de ) )
            throw new BiosoftCustomException( null, "Can't accept node '" + name + "' to compartment '" + compartment.getName() + "'" );
        return new DiagramElementGroup( de );
    }
}
package biouml.plugins.sbol;

import java.awt.Dimension;
import java.awt.Point;
import java.net.URI;

import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.Identified;
import org.sbolstandard.core2.SBOLDocument;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class Backbone extends SbolBase implements InitialElementProperties
{
    public static String[] strandTypes = new String[] {"Single-stranded", "Double-stranded"};
    public static String[] topologyTypes = new String[] {"Linear", "Circular"};
    public static String[] types = new String[] {"DNA", "RNA"};
    public static String[] roles = new String[] {"Sequence feature", "RNA"};

    public Backbone(String name)
    {
        super( null );
        this.name = name;
        this.title = name;
    }

    public Backbone(Identified so)
    {
        super( so );
        setName(so.getDisplayId());
    }

    private boolean isCreated = false;;
    private String strandType = "Single-stranded";
    private String topologyType = "Linear";
    private String type = "DNA";
    private String role = "Sequence feature";
    private String name = "Backbone";
    private String title = "Backbone";

    @Override
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    @PropertyName ( "Strand type" )
    public String getStrandType()
    {
        return strandType;
    }

    public void setStrandType(String strandType)
    {
        this.strandType = strandType;
    }

    @PropertyName ( "Topology type" )
    public String getTopologyType()
    {
        return topologyType;
    }

    public void setTopologyType(String topologyType)
    {
        this.topologyType = topologyType;
    }

    @PropertyName ( "Type" )
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }


    @PropertyName ( "Role" )
    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        Diagram diagram = Diagram.getDiagram( compartment );
        setName( DefaultSemanticController.generateUniqueName( diagram, name ) );
        Object doc = diagram.getAttributes().getValue( SbolUtil.SBOL_DOCUMENT_PROPERTY );
        if( doc != null && doc instanceof SBOLDocument )
        {
            URI type = ( getType().equals( "DNA" ) ) ? ComponentDefinition.DNA_REGION : ComponentDefinition.RNA_REGION;
            ComponentDefinition cd = ( (SBOLDocument)doc ).createComponentDefinition( getName(), "1", type );
            this.setSbolObject( cd );
        }
        else
        {
            return DiagramElementGroup.EMPTY_EG;
        }

        this.isCreated = true;
        Compartment result = new Compartment( compartment, this );
        result.getAttributes().add(new DynamicProperty("isCircular", Boolean.class, getTopologyType().equals("Circular")));
        result.getAttributes().add(new DynamicProperty("isWithChromLocus", Boolean.class, false));

        result.setShapeSize(new Dimension(45 + 10, 45 + 20));
        result.setLocation( location );
        compartment.put( result );

        if( viewPane != null )
            viewPane.completeTransaction();

        return new DiagramElementGroup( result );
    }

    public boolean isCreated()
    {
        return isCreated;
    }

    //    http://www.biopax.org/release/biopax-level3.owl#DnaRegion
    //    http://identifiers.org/so/SO:0000110

}

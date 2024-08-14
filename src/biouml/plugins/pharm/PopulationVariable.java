package biouml.plugins.pharm;

import java.awt.Point;
import java.beans.PropertyChangeListener;
import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.MutableDataElement;
import ru.biosoft.graphics.editor.ViewEditorPane;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SemanticController;
import biouml.standard.type.Stub;

public class PopulationVariable implements Role, MutableDataElement, InitialElementProperties
{
    public static final String DISTRIBUTION_NORMAL ="Normal";
    public static final String DISTRIBUTION_NONE = "-";
    public static final String TRANSFORMATION_LOG = "log";
    public static final String TRANSFORMATION_NONE = "-";

    DiagramElement de;

    public PopulationVariable(String name)
    {
        this(name, true);
    }

    public PopulationVariable(String name, boolean isCreated)
    {
        this.name = name;
        this.isCreated = isCreated;
    }

    private String name;
    private double initialValue = 0.0;
    private String distribution = DISTRIBUTION_NORMAL;
    private String type = Type.TYPE_STOCHASTIC;
    private String comment;
    private String transformation = TRANSFORMATION_LOG;

    @PropertyName ( "Distribution" )
    public String getDistribution()
    {
        return distribution;
    }

    public void setDistribution(String distribution)
    {
        this.distribution = distribution;
    }

    @Override
    public DiagramElement getDiagramElement()
    {
        return de;
    }

    public void setDiagramElement(DiagramElement de)
    {
        this.de = de;
    }


    @Override
    public Role clone(DiagramElement de)
    {
        PopulationVariable result = new PopulationVariable(name);
        result.de = de;
        result.type = type;
        result.distribution = distribution;
        result.initialValue = initialValue;
        result.isCreated = true;
        result.comment = comment;
        // TODO Auto-generated method stub
        return result;
    }

    @Override
    @PropertyName("Name")
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @PropertyName("Initial value")
    public double getInitialValue()
    {
        return initialValue;
    }

    public void setInitialValue(double initialValue)
    {
        this.initialValue = initialValue;
    }

    @PropertyName("Type")
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        if (!Type.TYPE_STOCHASTIC.equals( type ))
        {
            this.setDistribution( DISTRIBUTION_NONE );
        }
        this.type = type;
    }

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        if( name.isEmpty() )
            throw new IllegalArgumentException( "Please specify node name" );

        name = DefaultSemanticController.generateUniqueNodeName( compartment, name, false );
        DiagramElement node = new Node( compartment, new Stub( null, name, Type.TYPE_VARIABLE ) );
        node.setRole( this );
        this.de = node;
        this.isCreated = true;

        DynamicProperty dp = new DynamicProperty( "populationVariable", PopulationVariable.class, this );
        dp.setHidden( true );
        dp.setReadOnly( true );
        node.getAttributes().add(dp );

        SemanticController semanticController = Diagram.getDiagram( compartment ).getType().getSemanticController();
        if( semanticController.canAccept(compartment, node) )
        {
            viewPane.add(node, location);
        }

        return new DiagramElementGroup( node );
    }

    @PropertyName("Comment")
    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public boolean isNotRandom()
    {
        return !Type.TYPE_STOCHASTIC.equals(type);
    }

    private boolean isCreated = true;
    public boolean isCreated()
    {
        return isCreated;
    }

    @Override
    public DataCollection getOrigin()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener pcl)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener pcl)
    {
        // TODO Auto-generated method stub

    }

    @PropertyName("Transformation")
    public String getTransformation()
    {
        return transformation;
    }

    public void setTransformation(String transformation)
    {
        this.transformation = transformation;
    }

}

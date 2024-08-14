package biouml.plugins.brain.diagram;

import java.awt.Point;
import java.beans.PropertyChangeListener;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyDescription;
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
import biouml.plugins.brain.model.BrainModelProperties;
import biouml.plugins.brain.model.regional.EpileptorRegionalModelProperties;
import biouml.plugins.brain.model.regional.RosslerRegionalModelProperties;
import biouml.plugins.simulation.Options;
import biouml.standard.type.Stub;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.MutableDataElement;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class BrainRegionalModel extends Options implements Role, MutableDataElement, InitialElementProperties 
{
    private String name;
    private String comment;
    DiagramElement de;
    
    static final String[] availableRegionalModels = new String[] 
    {
        BrainType.TYPE_REGIONAL_ROSSLER, 
        BrainType.TYPE_REGIONAL_EPILEPTOR
    };
    private String regionalModelType = BrainType.TYPE_REGIONAL_ROSSLER;
    private BrainModelProperties regionalModelProperties = new RosslerRegionalModelProperties();
    
    public BrainRegionalModel()
    {
    }

    public BrainRegionalModel(String name)
    {
        this(name, true);
    }

    public BrainRegionalModel(String name, boolean isCreated)
    {
        this.name = name;
        this.isCreated = isCreated;
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
    	BrainRegionalModel result = new BrainRegionalModel(name);
        result.comment = comment;
        result.de = de;
        result.regionalModelType = regionalModelType;
        result.regionalModelProperties = regionalModelProperties;
        result.isCreated = true;
        return result;
    }

    @PropertyName("Name")
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    @PropertyName ("Regional model type")
    @PropertyDescription ("Type of regional model which will be used")
    public String getRegionalModelType()
    {
        return regionalModelType;
    }
    public void setRegionalModelType(String regionalModelType)
    {
        Object oldValue = this.regionalModelType;
        this.regionalModelType = regionalModelType;
        firePropertyChange("regionalModelType", oldValue, regionalModelType);
        firePropertyChange("*", null, null);
        switch(regionalModelType)
        {
            case BrainType.TYPE_REGIONAL_ROSSLER:
                this.setRegionalModelProperties(new RosslerRegionalModelProperties());
                break;
            case BrainType.TYPE_REGIONAL_EPILEPTOR:
                this.setRegionalModelProperties(new EpileptorRegionalModelProperties());
                break;
            default:
                break;
        }
    }

    @PropertyName("Regional model parameters")
    @PropertyDescription("Parameters of regional model which will be used")
    public BrainModelProperties getRegionalModelProperties()
    {
        return regionalModelProperties;
    }
    public void setRegionalModelProperties(BrainModelProperties regionalModelProperties)
    {
        Object oldValue = this.regionalModelProperties;
        this.regionalModelProperties = regionalModelProperties;
        firePropertyChange("regionalModelProperties", oldValue, regionalModelProperties);
        firePropertyChange("*", null, null);
    }

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {  
        if (name.isEmpty())
        {
        	throw new IllegalArgumentException("Please specify node name");
        }

        name = DefaultSemanticController.generateUniqueNodeName(compartment, name, false);
        Node node = new Node(compartment, new Stub(null, name, BrainType.TYPE_REGIONAL_MODEL));
        node.setRole(this);
        this.de = node;
        this.isCreated = true;

        DynamicProperty dp = new DynamicProperty("regionalModel", BrainRegionalModel.class, this);
        dp.setHidden(true);
        dp.setReadOnly(true);
        node.getAttributes().add(dp);

        SemanticController semanticController = Diagram.getDiagram(compartment).getType().getSemanticController();
        if (semanticController.canAccept(compartment, node))
        {
            viewPane.add(node, location);
        }

        return new DiagramElementGroup(node);
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

    private boolean isCreated = true;
    public boolean isCreated()
    {
        return isCreated;
    }

    @Override
    public DataCollection getOrigin()
    {
    	return de.getOrigin();
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener pcl)
    {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener pcl)
    {
    }
}

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
import biouml.plugins.brain.model.receptor.AmpaReceptorModelProperties;
import biouml.plugins.simulation.Options;
import biouml.standard.type.Stub;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.MutableDataElement;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class BrainReceptorModel extends Options implements Role, MutableDataElement, InitialElementProperties 
{
    private String name;
    private String comment;
    DiagramElement de;
    
    static final String[] availableReceptorModels = new String[] 
    {
    	BrainType.TYPE_RECEPTOR_AMPA
    };
    private String receptorModelType = BrainType.TYPE_RECEPTOR_AMPA;
    private BrainModelProperties receptorModelProperties = new AmpaReceptorModelProperties();
    
    public BrainReceptorModel()
    {
    }

    public BrainReceptorModel(String name)
    {
        this(name, true);
    }

    public BrainReceptorModel(String name, boolean isCreated)
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
    	BrainReceptorModel result = new BrainReceptorModel(name);
        result.comment = comment;
        result.de = de;
        result.receptorModelType = receptorModelType;
        result.receptorModelProperties = receptorModelProperties;
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

    @PropertyName("Receptor model type")
    @PropertyDescription("Type of receptor model which will be used")
    public String getReceptorModelType()
    {
        return receptorModelType;
    }
    public void setReceptorModelType(String receptorModelType)
    {
        Object oldValue = this.receptorModelType;
        this.receptorModelType = receptorModelType;
        firePropertyChange("receptorModelType", oldValue, receptorModelType);
        firePropertyChange("*", null, null);
        switch(receptorModelType)
        {
            case BrainType.TYPE_RECEPTOR_AMPA:
                setReceptorModelProperties(new AmpaReceptorModelProperties());
                break;
            default:
                break;
        }
    }

    @PropertyName("Receptor model parameters")
    @PropertyDescription("Parameters of receptor model which will be used")
    public BrainModelProperties getReceptorModelProperties()
    {
        return receptorModelProperties;
    }
    public void setReceptorModelProperties(BrainModelProperties receptorModelProperties)
    {
        Object oldValue = this.receptorModelProperties;
        this.receptorModelProperties = receptorModelProperties;
        firePropertyChange("receptorModelProperties", oldValue, receptorModelProperties);
        firePropertyChange("*", null, null);
    }

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {  
        if (name.isEmpty())
            throw new IllegalArgumentException("Please specify node name");

        name = DefaultSemanticController.generateUniqueNodeName(compartment, name, false);
        Node node = new Node(compartment, new Stub(null, name, BrainType.TYPE_RECEPTOR_MODEL));
        node.setRole(this);
        this.de = node;
        this.isCreated = true;

        DynamicProperty dp = new DynamicProperty("receptorModel", BrainCellularModel.class, this);
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

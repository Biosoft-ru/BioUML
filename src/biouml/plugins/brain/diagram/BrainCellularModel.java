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
import biouml.plugins.brain.model.cellular.Epileptor2CellularModelProperties;
import biouml.plugins.brain.model.cellular.Epileptor2OxygenCellularModelProperties;
import biouml.plugins.brain.model.cellular.MinimalCellularModelProperties;
import biouml.plugins.brain.model.cellular.OxygenCellularModelProperties;
import biouml.plugins.simulation.Options;
import biouml.standard.type.Stub;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.MutableDataElement;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class BrainCellularModel extends Options implements Role, MutableDataElement, InitialElementProperties 
{
    private String name;
    private String comment;
    DiagramElement de;
    
    static final String[] availableCellularModels = new String[] 
    {
        BrainType.TYPE_CELLULAR_EPILEPTOR2, 
    	BrainType.TYPE_CELLULAR_OXYGEN, 
    	BrainType.TYPE_CELLULAR_MINIMAL, 
    	BrainType.TYPE_CELLULAR_EPILEPTOR2_WITH_OXYGEN,
    };
    private String cellularModelType = BrainType.TYPE_CELLULAR_EPILEPTOR2;
    private BrainModelProperties cellularModelProperties = new Epileptor2CellularModelProperties();
    
    public BrainCellularModel()
    {
    }

    public BrainCellularModel(String name)
    {
        this(name, true);
    }

    public BrainCellularModel(String name, boolean isCreated)
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
    	BrainCellularModel result = new BrainCellularModel(name);
        result.comment = comment;
        result.de = de;
        result.cellularModelType = cellularModelType;
        result.cellularModelProperties = cellularModelProperties;
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

    @PropertyName("Cellular model type")
    @PropertyDescription("Type of cellular model which will be used")
    public String getCellularModelType()
    {
        return cellularModelType;
    }
    public void setCellularModelType(String cellularModelType)
    {
        Object oldValue = this.cellularModelType;
        this.cellularModelType = cellularModelType;
        firePropertyChange("cellularModelType", oldValue, cellularModelType);
        firePropertyChange("*", null, null);
        switch(cellularModelType)
        {
            case BrainType.TYPE_CELLULAR_EPILEPTOR2:
                setCellularModelProperties(new Epileptor2CellularModelProperties());
                break;
            case BrainType.TYPE_CELLULAR_OXYGEN:
                setCellularModelProperties(new OxygenCellularModelProperties());
                break;
            case BrainType.TYPE_CELLULAR_MINIMAL:
                setCellularModelProperties(new MinimalCellularModelProperties());
                break;
            case BrainType.TYPE_CELLULAR_EPILEPTOR2_WITH_OXYGEN:
                setCellularModelProperties(new Epileptor2OxygenCellularModelProperties());
                break;
            default:
                break;
        }
    }

    @PropertyName("Cellular model parameters")
    @PropertyDescription("Parameters of cellular model which will be used")
    public BrainModelProperties getCellularModelProperties()
    {
        return cellularModelProperties;
    }
    public void setCellularModelProperties(BrainModelProperties cellularModelProperties)
    {
        Object oldValue = this.cellularModelProperties;
        this.cellularModelProperties = cellularModelProperties;
        firePropertyChange("cellularModelProperties", oldValue, cellularModelProperties);
        firePropertyChange("*", null, null);
    }

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {  
        if (name.isEmpty())
            throw new IllegalArgumentException("Please specify node name");

        name = DefaultSemanticController.generateUniqueNodeName(compartment, name, false);
        Node node = new Node(compartment, new Stub(null, name, BrainType.TYPE_CELLULAR_MODEL));
        node.setRole(this);
        this.de = node;
        this.isCreated = true;

        DynamicProperty dp = new DynamicProperty("cellularModel", BrainCellularModel.class, this);
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

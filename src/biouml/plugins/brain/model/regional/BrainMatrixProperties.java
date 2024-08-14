package biouml.plugins.brain.model.regional;

import java.awt.Point;

import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.dynamics.TableElement;
import biouml.plugins.brain.diagram.BrainType;
import biouml.standard.type.Stub;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.table.TableDataCollection;

public class BrainMatrixProperties implements InitialElementProperties
{
    public static final String FORMULA_ATTR = "Formula";
    public BrainMatrixProperties(String name, String type)
    {
        this.name = name;
        this.type = type;
    }
    
    private String name = "Unknown brain matrix";
    private String type = null;
    private DataElementPath tablePath;
    private TableDataCollection table;
    private String formula;

    @Override
    public DiagramElementGroup createElements(Compartment c, Point location, ViewEditorPane viewPane) throws Exception
    {   	
        if (name.isEmpty()) 
        {
        	throw new IllegalArgumentException("Please specify node name");
        }
    	
        Node node;
        if (type == BrainType.TYPE_CONNECTIVITY_MATRIX)
        {
        	node = new Node(c, new Stub(null, name, BrainType.TYPE_CONNECTIVITY_MATRIX));
        }
        else if  (type == BrainType.TYPE_DELAY_MATRIX)
        {
        	node = new Node(c, new Stub(null, name, BrainType.TYPE_DELAY_MATRIX));
        }
        else
        {
        	throw new IllegalArgumentException("Please specify brain matrix type");
        }
        TableElement role = new TableElement(node);
        role.setFormula(formula);
        role.setTablePath(tablePath);
        //TableDataCollection table = (TableDataCollection)CollectionFactory.getDataCollection(tablePath.toString());
        TableDataCollection table = (TableDataCollection)tablePath.getDataElement();
        role.setTable(table);
        node.setRole(role);

        SemanticController semanticController = Diagram.getDiagram(c).getType().getSemanticController();
        if (semanticController.canAccept(c, node))
        {
            viewPane.add(node, location);
        }
        return new DiagramElementGroup(node);
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

    @PropertyName("Table path")
    public DataElementPath getTablePath()
    {
        return tablePath;
    }
    public void setTablePath(DataElementPath tablePath)
    {
        if (tablePath != null)
        {
            DataElement de = tablePath.optDataElement();
            if (de != null && de instanceof TableDataCollection)
            {
                this.tablePath = tablePath;
            }
        }
    }
    
    @PropertyName("Table Data collection")
    public TableDataCollection getTable()
    {
        return table;
    }
    public void setTable(TableDataCollection table)
    {
        if (table != null)
        {
        	this.table = table;
        }
    }

    @PropertyName("Formula")
    public String getFormula()
    {
        return formula;
    }
    public void setFormula(String formula)
    {
        this.formula = formula;
    }

}

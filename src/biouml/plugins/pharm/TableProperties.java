package biouml.plugins.pharm;

import java.awt.Point;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.table.TableDataCollection;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.dynamics.TableElement;
import biouml.standard.type.Stub;

public class TableProperties implements InitialElementProperties
{
    public static final String FORMULA_ATTR = "Formula";
    public TableProperties(String name)
    {
        this.name = name;
    }

    private String name = "table";
    private DataElementPath path;
    private String formula;

    @Override
    public DiagramElementGroup createElements(Compartment c, Point location, ViewEditorPane viewPane) throws Exception
    {
        if( name.isEmpty() )
            throw new IllegalArgumentException( "Please specify node name" );

        Node node = new Node( c, new Stub( null, name, Type.TYPE_TABLE_DATA ) );
        TableElement role = new TableElement( node );
        role.setFormula(formula);
        role.setTablePath( path );
        node.setRole( role );

        SemanticController semanticController = Diagram.getDiagram( c ).getType().getSemanticController();
        if( semanticController.canAccept(c, node) )
        {
            viewPane.add(node, location);
        }
        return new DiagramElementGroup( node );
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

    @PropertyName("Table")
    public DataElementPath getTablePath()
    {
        return path;
    }

    public void setTablePath(DataElementPath path)
    {
        if( path != null )
        {
            DataElement de = path.optDataElement();
            if( de != null && de instanceof TableDataCollection )
            {
                this.path = path;
            }
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

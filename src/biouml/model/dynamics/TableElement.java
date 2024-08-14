package biouml.model.dynamics;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.DiagramElement;
import biouml.model.Role;

import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName("Table element")
@PropertyDescription("Element associated with table.")
public class TableElement extends EModelRoleSupport implements ExpressionOwner
{
    private TableDataCollection table;
    private DataElementPath path;
    private Variable[] variables;
    private String formula;
    private boolean cycled = false;
    private String splineType = SplineType.toString(SplineType.CUBIC);
  
    public TableElement(DiagramElement de)
    {
        super(de);
    }
      
    @PropertyName("Variables")
    @PropertyDescription("Variables associated with table columns.")
    public Variable[] getVariables()
    {
        return variables;
    }

    public void setVariables(Variable[] variable)
    {
        this.variables = variable;
    }

    public TableDataCollection getTable()
    {
        if (table == null && path != null)
            table = path.optDataElement(TableDataCollection.class);
        return table;
    }
    
    public void setTable(TableDataCollection table)
    {
        TableDataCollection oldValue = this.table;
        this.table = table;
        path = table.getCompletePath();
        variables = table.columns().map( column -> new Variable("", column.getName(), this) ).toArray( Variable[]::new );
        firePropertyChange( "table", oldValue, table );
    }
    
    @PropertyName("Table path")
    @PropertyDescription("Path to associated table.")
    public DataElementPath getTablePath()
    {
        return path;
    }

    public void setTablePath(DataElementPath path)
    {
        if( path != null )
        {
            DataElement de = path.optDataElement();
            if(de instanceof TableDataCollection)
                setTable((TableDataCollection)de);
            this.path = path;
        }
    }

    @Override
    public Role clone(DiagramElement de)
    {
        TableElement te = new TableElement( de );
        if( table != null )
        {
            te.setTable( table );
            te.setVariables( variables.clone() );
            te.setFormula( formula );
            te.setCycled( cycled );
            te.setSplineType( splineType );
        }
        return te;
    }
    
    @Override
    public boolean isExpression(String propertyName)
    {
        return "variables".equals( propertyName );
    }

    @Override
    public String[] getExpressions()
    {
        return StreamEx.of(variables).map( Variable::getName ).toArray( String[]::new );
    }

    @Override
    public void setExpressions(String[] exps)
    {
        for (int i=0; i< variables.length; i++)
            variables[i].setName( exps[i] );
    }

    @Override
    public Role getRole()
    {
        return this;
    }
    
    @PropertyName("Variable")
    @PropertyDescription("Variable associated with table column.")
    public static class Variable extends Option implements ExpressionOwner
    {
        private String name;
        private String columnName;

        Variable(String name, String columnName, Option parent)
        {
            super( parent );
            this.name = name;
            this.columnName = columnName;
        }
        
        @PropertyName("Name")
        @PropertyDescription("Variable name.")
        public String getName()
        {
            return name;
        }
        public void setName(String name)
        {
            String oldValue = this.name;
            this.name = name;
            this.firePropertyChange( "name", oldValue, name );
        }
        
        @PropertyName("Column name")
        @PropertyDescription("Name of the correspondent column in table.")
        public String getColumnName()
        {
            return columnName;
        }
        public void setColumnName(String columnName)
        {
            String oldValue = this.columnName;
            this.columnName = columnName;
            this.firePropertyChange( "columnName", oldValue, columnName );
        }

        @Override
        public boolean isExpression(String propertyName)
        {
            return "name".equals(propertyName);
        }

        @Override
        public String[] getExpressions()
        {
            return new String[]{name};
        }

        @Override
        public void setExpressions(String[] exps)
        {
           this.name = exps[0];
        }

        @Override
        public Role getRole()
        {
            return (Role)getParent();
        }
    }
    
    public static class VariableBeanInfo extends BeanInfoEx2<Variable>
    {
        public VariableBeanInfo()
        {
            super( Variable.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            property( "name" ).add();
            property( "columnName" ).readOnly().add();
        }
    }

    @PropertyName("Formula")
    @PropertyDescription("Dependency between variables of type \"func ~ arg\".")
    public String getFormula()
    {
        return formula;
    }

    public void setFormula(String formula)
    {
        String oldValue = this.formula;
        this.formula = formula;
        this.firePropertyChange( "formula", oldValue, formula );
    }

    @PropertyName("Cycled")
    @PropertyDescription("If true then table data considered as cycled i.e. after last argument point it will use value from first point and so on.")
    public boolean isCycled()
    {
        return cycled;
    }

    public void setCycled(boolean cycled)
    {
        boolean oldValue = this.cycled;
        this.cycled = cycled;
        this.firePropertyChange( "cycled", oldValue, cycled );
    }

    @PropertyName("Spline type")
    @PropertyDescription("Spline type")
    public String getSplineType()
    {
        return splineType;
    }

    public void setSplineType(String splineType)
    {
        String oldValue = this.splineType;
        this.splineType = splineType;
        this.firePropertyChange( "splineType", oldValue, splineType );
    }

    public static enum SplineType
    {
        LINEAR, CUBIC;

        public static String toString(SplineType splineType)
        {
            switch( splineType )
            {
                case LINEAR:
                    return "linear";

                case CUBIC:
                    return "cubic";

                default:
                    return "";
            }
        }

        public static List<String> getSplineTypes()
        {
            List<String> list = new ArrayList<>();
            for( SplineType type : values() )
            {
                list.add(toString(type));
            }
			return list;
        }
    }
}

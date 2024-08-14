package biouml.model.dynamics;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramElement;
import biouml.model.Role;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

@PropertyName ( "Table element" )
@PropertyDescription ( "Element associated with table." )
public class SimpleTableElement extends EModelRoleSupport implements ExpressionOwner
{
    //    public static String TYPE_SPLINE = "Spline";
    public static String TYPE_PIECEWISE = "Piecewise";

    private TableDataCollection table;
    private DataElementPath path;
    private VarColumn argColumn = new VarColumn();
    private VarColumn[] columns = new VarColumn[1];
    private String[] availableColumns = new String[0];
    private String type = TYPE_PIECEWISE;

    private VarColumn prototype;

    public SimpleTableElement(DiagramElement de)
    {
        super(de);
        columns[0] = new VarColumn();
        argColumn.setParent(de);
        columns[0].setParent(de);
    }

    public String[] getAvailableColumns()//StreamEx<String> getAvailableColumns()
    {
        return availableColumns;//StreamEx.of( availableColumns);
    }

    @PropertyName ( "Columns" )
    @PropertyDescription ( "Table columns." )
    public VarColumn[] getColumns()
    {
        return columns;
    }
    public void setColumns(VarColumn[] argVariable)
    {
        VarColumn[] oldValue = this.columns;
        this.columns = argVariable;
        for( VarColumn col : argVariable )
        {
            col.setParent(getParent());
            col.availableColumns = this.availableColumns;
        }
        this.firePropertyChange("columns", oldValue, argVariable);
    }

    @PropertyName ( "Argument Column" )
    @PropertyDescription ( "Column for table associated with argument variable." )
    public VarColumn getArgColumn()
    {
        return argColumn;
    }
    public void setArgColumn(VarColumn argColumn)
    {
        VarColumn oldValue = this.argColumn;
        this.argColumn = argColumn;
        this.argColumn.setParent(getParent());
        this.firePropertyChange("argColumn", oldValue, argColumn);
    }

    public TableDataCollection getTable()
    {
        if( table == null && path != null )
            table = path.optDataElement(TableDataCollection.class);
        return table;
    }

    public void setTable(TableDataCollection table)
    {
        TableDataCollection oldValue = this.table;
        this.table = table;
        path = table.getCompletePath();
        prototype = new VarColumn(table);
        availableColumns = TableDataCollectionUtils.getColumnNames(table);
        for( VarColumn col : this.getColumns() )
        {
            col.availableColumns = this.availableColumns;
            validateColumn(col);
        }

        this.argColumn.availableColumns = this.availableColumns;
        validateColumn(this.argColumn);

        firePropertyChange("table", oldValue, table);
    }

    private void validateColumn(VarColumn col)
    {
        boolean exist = false;

        for( String availableColumn : this.availableColumns )
            if( availableColumn.equals(col.getColumn()) )
                exist = true;

        if( !exist )
            col.setColumn("");
    }

    @PropertyName ( "Table path" )
    @PropertyDescription ( "Path to associated table." )
    public DataElementPath getTablePath()
    {
        return path;
    }

    public void setTablePath(DataElementPath path)
    {
        if( path != null )
        {
            DataElement de = path.optDataElement();
            if( de instanceof TableDataCollection )
                setTable((TableDataCollection)de);
            this.path = path;
            //            this.setColumn( "" );
            //            this.setArgColumn( "" );
        }
    }

    @Override
    public Role clone(DiagramElement de)
    {
        SimpleTableElement te = new SimpleTableElement(de);
        if( table != null )
        {
            te.setTable(table);
            te.setColumns(columns);
            te.setArgColumn(argColumn);
        }
        return te;
    }

    @Override
    public boolean isExpression(String propertyName)
    {
        return "variables".equals(propertyName);
    }

    @Override
    public String[] getExpressions()
    {
        return StreamEx.of(columns).map(c -> c.getVariable()).toArray(String[]::new);
    }

    @Override
    public void setExpressions(String[] exps)
    {
        for( int i = 0; i < columns.length; i++ )
            columns[i].setVariable(exps[i]);
    }

    @Override
    public Role getRole()
    {
        return this;
    }

    public static class VarColumn extends Option
    {
        private String variable = "";
        private String column = "";
        private TableDataCollection table;
        private String[] availableColumns = new String[0];

        public VarColumn()
        {
        }

        public VarColumn(TableDataCollection table)
        {
            this.table = table;
            this.availableColumns = TableDataCollectionUtils.getColumnNames(table);
        }

        public StreamEx<String> getAvailableColumns()
        {
            return StreamEx.of(availableColumns);
        }

        @PropertyName ( "Column" )
        @PropertyDescription ( "Column for table associated with calculated variable." )
        public String getColumn()
        {
            return column;
        }
        public void setColumn(String column)
        {
            String oldValue = this.column;
            this.column = column;
            this.firePropertyChange("name", oldValue, column);
        }

        @PropertyName ( "Variable" )
        @PropertyDescription ( "Variable calculated by tabular data." )
        public String getVariable()
        {
            return variable;
        }
        public void setVariable(String variable)
        {
            String oldValue = this.variable;
            this.variable = variable;
            this.firePropertyChange("variable", oldValue, variable);
        }

        public VarColumn clone()
        {
            VarColumn result = new VarColumn(table);
            result.column = column;
            result.variable = variable;
            return result;
        }
    }

    public static class VarColumnBeanInfo extends BeanInfoEx2<VarColumn>
    {
        public VarColumnBeanInfo()
        {
            super(VarColumn.class);

        }

        @Override
        public void initProperties() throws Exception
        {
            add("variable");
            //            add(new PropertyDescriptorEx("column", beanClass), VarColumnEditor.class);
            //            property("column").editor( VarColumnEditor.class );
            property("column").tags(bean -> bean.getAvailableColumns()).add();
        }
    }
}

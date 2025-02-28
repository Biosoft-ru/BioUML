package ru.biosoft.table.document.editors;

import java.util.Iterator;
import java.util.logging.Level;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Option;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.gui.Document;
import ru.biosoft.table.Sample;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.datatype.DataType;

//table element bean
public class TableElement extends Option implements DataElement
{
    protected TableDataCollection me;
    protected int row;
    protected boolean reallyVisible;
    protected Document document;
    public TableElement(TableDataCollection me, int row)
    {
        this(me, row, null);
    }
    public TableElement(TableDataCollection me, int row, Document document)
    {
        this.me = me;
        this.row = row;
        this.document = document;
    }

    public boolean isReallyVisible()
    {
        if( row == -1 )
        {
            return true;
        }

        reallyVisible = !me.getColumnModel().getColumn(row).isHidden();
        return reallyVisible;
    }

    public void setReallyVisible(boolean isVisible)
    {
        if( row != -1 )
        {
            reallyVisible = isVisible;
            me.getColumnModel().getColumn(row).setHidden( !reallyVisible);
        }
    }

    public int getRow()
    {
        return row;
    }

    public int getVisibleRow()
    {
        return row+1;
    }

    public String getColumnName()
    {
        if( row == -1 )
        {
            return "id";
        }
        return me.getColumnModel().getColumn(row).getName();
    }

    public void setColumnName(String columnName)
    {
        if( row != -1 )
        {
            me.getColumnModel().getColumn(row).setName(columnName);
            if( document != null )
                document.update();
        }
    }

    public String getDescription()
    {
        if( row == -1 )
        {
            return "";
        }
        return me.getColumnModel().getColumn(row).getShortDescription();
    }

    public void setDescription(String description)
    {
        if( row != -1 )
        {
            me.getColumnModel().getColumn(row).setShortDescription(description);
        }
    }

    public String getType()
    {
        if( row == -1 )
        {
            return DataType.Text.toString();
        }
        return me.getColumnModel().getColumn(row).getType().toString();
    }

    public void setType(String typeName)
    {
        if( row != -1 )
        {
            DataType newType = DataType.fromString(typeName);
            me.getColumnModel().getColumn(row).setType(newType);
            if( document != null )
                document.update();
        }
    }

    public String getNature()
    {
        if( row != -1 )
        {
            return me.getColumnModel().getColumn(row).getNature().toString();
        }

        return TableColumn.Nature.NONE.toString();
    }

    public void setNature(String strNewNature)
    {
        if( row != -1 )
        {
            TableColumn.Nature newNature = TableColumn.Nature.valueOf(strNewNature);

            TableColumn col = me.getColumnModel().getColumn(row);
            if( newNature != TableColumn.Nature.SAMPLE && col.getNature() == TableColumn.Nature.SAMPLE )
            {
                try
                {
                    me.getSamples().remove(col.getName());
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                }
            }

            if( newNature == TableColumn.Nature.SAMPLE )
            {
                Sample sample = new Sample(me.getSamples(), col.getName());
                fillNewSampleAttributes(sample);
                col.setSample(sample);
                try
                {
                    me.getSamples().put(sample);
                }
                catch( Throwable t )
                {
                    ColumnsViewPane.log.log(Level.SEVERE, "can't add sample", t);
                }
            }

            col.setNature(newNature);
        }
    }

    public String getExpression()
    {
        if( row != -1 )
        {
            TableColumn column = me.getColumnModel().getColumn(row);
            Object value = column.getExpression();
            return value == null ? "" : value.toString();
        }
        return "";
    }

    public void setExpression(String expression)
    {
        if( row != -1 )
        {
            TableColumn column = me.getColumnModel().getColumn(row);
            //boolean isNotificationEnabled = me.isNotificationEnabled();
            //me.setNotificationEnabled(false);
            column.setExpression(expression);
            //me.setNotificationEnabled(isNotificationEnabled);
        }
    }

    public Boolean isReadOnly()
    {
        return row == -1;
    }

    public boolean isExpressionDisabled()
    {
        return row == -1 || me.getColumnModel().getColumn(row).isExpressionLocked();
    }

    private void fillNewSampleAttributes(Sample sample)
    {
        if( me.getSamples().getSize() > 0 )
        {
            Sample template = me.getSamples().iterator().next();
            Iterator<String> iter = template.getAttributes().nameIterator();
            while( iter.hasNext() )
            {
                String pName = iter.next();
                try
                {
                    sample.getAttributes().add(new DynamicProperty(pName, String.class));
                }
                catch( Throwable t )
                {
                    ColumnsViewPane.log.log(Level.SEVERE, "can't get sample property", t);
                }
            }
        }
    }
    public TableDataCollection getTable()
    {
        return me;
    }

    @Override
    public String getName()
    {
        return String.valueOf( row );
    }

    @Override
    public DataCollection getOrigin()
    {
        return me;
    }
}
package biouml.plugins.chemoinformatics.access;

import java.awt.Dimension;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.StringSet;
import biouml.standard.type.CDKRenderer;
import biouml.standard.type.Structure;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.application.ApplicationUtils;

public class TableStructureWrapper extends StandardTableDataCollection
{
    protected DataCollection<Structure> structures;
    protected List<String> nameList = new ArrayList<>();
    public TableStructureWrapper(DataCollection<Structure> structures)
    {
        super(null, "dpsTable");

        this.structures = structures;
        this.nameList = structures.getNameList();

        try
        {
            columnModel.addColumn("Structure", CompositeView.class);
            for( String rowName : this.nameList )
            {
                Structure structure = structures.get(rowName);
                Iterator<DynamicProperty> iter = structure.getAttributes().propertyIterator();
                while( iter.hasNext() )
                {
                    DynamicProperty dp = iter.next();
                    if( !columnModel.hasColumn(dp.getName()) )
                    {
                        Class<?> type;
                        if( ( dp.getType() == Integer.TYPE ) || ( Integer.class.isAssignableFrom(dp.getType()) ) )
                        {
                            type = Integer.class;
                        }
                        else if( ( dp.getType() == Double.TYPE ) || ( Double.class.isAssignableFrom(dp.getType()) ) )
                        {
                            type = Double.class;
                        }
                        else if(StringSet.class.isAssignableFrom(dp.getType()))
                        {
                            type = StringSet.class;
                        }
                        else
                        {
                            type = String.class;
                        }

                        columnModel.addColumn(dp.getName(), dp.getDisplayName(), dp.getShortDescription(), type, null);
                    }
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Create column structure error", e);
        }
    }

    @Override
    public int getSize()
    {
        return nameList.size();
    }

    @Override
    public String getName(int index)
    {
        return nameList.get(index);
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        return nameList;
    }

    @Override
    protected RowDataElement doGet(String name) throws Exception
    {
        RowDataElement rde = new RowDataElement(name, this);
        try
        {
            Structure structure = structures.get(name);
            DynamicPropertySet dps = structure.getAttributes();
            Object[] result = new Object[columnModel.getColumnCount()];
            try
            {
                result[0] = getStructureView(structure);
            }
            catch( Throwable t )
            {
                result[0] = new CompositeView();
            }
            for( int i = 1; i < columnModel.getColumnCount(); i++ )
            {
                result[i] = dps.getValue(columnModel.getColumn(i).getName());
            }
            rde.setValues(result);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not get element: " + name, e);
        }
        return rde;
    }

    @Override
    public RowDataElement getAt(int index)
    {
        try
        {
            String name = getName(index);
            return doGet(name);
        }
        catch( Exception e )
        {
        }
        return null;
    }

    @Override
    public Object getValueAt(int rowIdx, int columnIdx)
    {
        try
        {
            String name = getName(rowIdx);
            Structure structure = structures.get(name);
            if( columnIdx == 0 )
            {
                try
                {
                    return getStructureView(structure);
                }
                catch( Throwable t )
                {
                    return new CompositeView();
                }
            }
            if( columnIdx < columnModel.getColumnCount() )
            {
                DynamicPropertySet dps = structure.getAttributes();
                return dps.getValue(columnModel.getColumn(columnIdx).getName());
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can't get element at (" + rowIdx + "," + columnIdx + ")", e);
        }
        return null;
    }

    protected CompositeView getStructureView(Structure structure) throws Exception
    {
        CompositeView structureView = CDKRenderer.createStructureView(structure, new Dimension(100, 100), ApplicationUtils.getGraphics());
        return structureView;
    }
}

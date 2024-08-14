package ru.biosoft.table.columnbeans;

import java.beans.IntrospectionException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

public class ColumnNameSelector extends GenericComboBoxEditor
{
    public static final String COLLECTION_PROPERTY = "CollectionProperty";
    public static final String NUMERIC_ONLY_PROPERTY = "NumericOnly";
    public static final String TEXT_ONLY_PROPERTY = "TextOnly";
    public static final String ADD_ID_COLUMN = "AddIdColumn";
    public static final @Nonnull String NONE_COLUMN = "(none)";
    public static final @Nonnull String ID_COLUMN_SHORT = "ID";
    public static final @Nonnull String ID_COLUMN_FULL = "ID (row ID, first column)";

    public static String getNumericColumn(DataElementPath tablePath, String currentColumn)
    {
        TableDataCollection table = tablePath.optDataElement(TableDataCollection.class);
        if(table == null) return currentColumn;
        if(table.getColumnModel().hasColumn(currentColumn) &&
                table.getColumnModel().getColumn(currentColumn).getType().isNumeric())
            return currentColumn;
        return table.columns().findFirst(col -> col.getType().isNumeric()).map(TableColumn::getName).orElse(NONE_COLUMN);
    }

    public static String getColumn(DataElementPath tablePath, String currentColumn)
    {
        TableDataCollection table = tablePath.optDataElement(TableDataCollection.class);
        if(table == null) return currentColumn;
        if(table.getColumnModel().hasColumn(currentColumn))
            return currentColumn;
        return table.columns().findFirst().map(TableColumn::getName).orElse(NONE_COLUMN);
    }

    @Override
    public String[] getAvailableValues()
    {
        Object propertyName = getDescriptor().getValue(COLLECTION_PROPERTY);
        DataElementPath path = (DataElementPath)ComponentFactory.getModel(getBean()).findProperty(propertyName.toString()).getValue();
        if(path == null)
            return new String[] {NONE_COLUMN};
        TableDataCollection collection = path.optDataElement(TableDataCollection.class);
        if( collection == null )
            return new String[] {NONE_COLUMN};
        boolean canBeNull = BeanUtil.getBooleanValue(this, BeanInfoConstants.CAN_BE_NULL);
        boolean numericOnly = BeanUtil.getBooleanValue(this, NUMERIC_ONLY_PROPERTY);
        boolean addIdColumn = BeanUtil.getBooleanValue( this, ADD_ID_COLUMN );
        boolean textOnly = BeanUtil.getBooleanValue( this, TEXT_ONLY_PROPERTY );
        List<String> values = new ArrayList<>();
        if(canBeNull)
        {
            values.add(NONE_COLUMN);
        }
        if( addIdColumn )
        {
            if( collection.getColumnModel().hasColumn( ID_COLUMN_SHORT ) )
                values.add( ID_COLUMN_FULL );
            else
                values.add( ID_COLUMN_SHORT );
        }
        for( TableColumn column : collection.getColumnModel() )
        {
            if( isAcceptable( column, numericOnly, textOnly ) )
                values.add( column.getName() );
        }
        return values.toArray(new String[values.size()]);
    }

    private boolean isAcceptable(TableColumn tc, boolean numericOnly, boolean textOnly)
    {
        DataType type = tc.getType();
        if( numericOnly && !type.isNumeric() )
            return false;
        if( textOnly && ! ( String.class.equals( type.getType() ) || StringSet.class.equals( type.getType() ) ) )
            return false;
        return true;
    }

    public static PropertyDescriptorEx registerSelector(PropertyDescriptorEx pde, String collectionProperty)
    {
        return registerSelector(pde, collectionProperty, true);
    }

    public static PropertyDescriptorEx registerSelector(PropertyDescriptorEx pde, String collectionProperty, boolean canBeNull)
    {
        pde.setPropertyEditorClass(ColumnNameSelector.class);
        pde.setSimple(true);
        pde.setCanBeNull(canBeNull);
        pde.setValue(COLLECTION_PROPERTY, collectionProperty);
        return pde;
    }

    public static PropertyDescriptorEx registerSelector(String property, Class<?> beanClass, String collectionProperty) throws IntrospectionException
    {
        return registerSelector(new PropertyDescriptorEx(property, beanClass), collectionProperty);
    }

    public static PropertyDescriptorEx registerSelector(String property, Class<?> beanClass, String collectionProperty, boolean canBeNull) throws IntrospectionException
    {
        return registerSelector(new PropertyDescriptorEx(property, beanClass), collectionProperty, canBeNull);
    }

    public static PropertyDescriptorEx registerTextOrSetSelector(String property, Class<?> beanClass, String collectionProperty,
            boolean canBeNull, boolean addIdColumn) throws IntrospectionException
    {
        PropertyDescriptorEx pde = registerSelector( property, beanClass, collectionProperty, canBeNull );
        pde.setValue( ADD_ID_COLUMN, addIdColumn );
        pde.setValue( TEXT_ONLY_PROPERTY, true );
        return pde;
    }

    public static PropertyDescriptorEx registerNumericSelector(PropertyDescriptorEx pde, String collectionProperty)
    {
        return registerNumericSelector(pde, collectionProperty, true);
    }

    public static PropertyDescriptorEx registerNumericSelector(PropertyDescriptorEx pde, String collectionProperty, boolean canBeNull)
    {
        pde.setPropertyEditorClass(ColumnNameSelector.class);
        pde.setSimple(true);
        pde.setCanBeNull(canBeNull);
        pde.setValue(COLLECTION_PROPERTY, collectionProperty);
        pde.setValue(NUMERIC_ONLY_PROPERTY, true);
        return pde;
    }

    public static PropertyDescriptorEx registerNumericSelector(String property, Class<?> beanClass, String collectionProperty) throws IntrospectionException
    {
        return registerNumericSelector(new PropertyDescriptorEx(property, beanClass), collectionProperty);
    }

    public static PropertyDescriptorEx registerNumericSelector(String property, Class<?> beanClass, String collectionProperty, boolean canBeNull) throws IntrospectionException
    {
        return registerNumericSelector(new PropertyDescriptorEx(property, beanClass), collectionProperty, canBeNull);
    }

    public static void updateColumns(Object bean)
    {
        ComponentModel model = ComponentFactory.getModel( bean );
        for(int i=0; i<model.getPropertyCount(); i++)
        {
            try
            {
                Property property = model.getPropertyAt( i );
                if(property.getPropertyEditorClass() != ColumnNameSelector.class)
                    continue;
                String collectionPropertyName = (String)property.getDescriptor().getValue( COLLECTION_PROPERTY );
                if(collectionPropertyName == null)
                    continue;
                Property collectionProperty = model.findProperty( collectionPropertyName );
                if(collectionProperty == null)
                    continue;
                Object collectionValue = collectionProperty.getValue();
                if(!(collectionValue instanceof DataElementPath))
                    continue;
                String columnValue = (String)property.getValue();
                if(columnValue.equals( NONE_COLUMN ) && property.getBooleanAttribute( BeanInfoConstants.CAN_BE_NULL ))
                    continue;
                TableDataCollection table = ((DataElementPath)collectionValue).optDataElement( TableDataCollection.class );
                if(table == null)
                {
                    property.setValue( NONE_COLUMN );
                    continue;
                }
                if(table.getColumnModel().hasColumn( columnValue ))
                    continue;
                if(property.getBooleanAttribute( BeanInfoConstants.CAN_BE_NULL ))
                {
                    property.setValue( NONE_COLUMN );
                    continue;
                }
                Predicate<TableColumn> filter = property.getBooleanAttribute( NUMERIC_ONLY_PROPERTY ) ? c -> c.getType().isNumeric() : c -> true;
                property.setValue( table.columns().findFirst( filter ).map( TableColumn::getName ).orElse( NONE_COLUMN ) );
            }
            catch( NoSuchMethodException e )
            {
                throw ExceptionRegistry.translateException( e );
            }
        }
    }
}
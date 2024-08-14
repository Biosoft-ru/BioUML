package biouml.plugins.riboseq.db.editors;

import java.beans.IntrospectionException;
import java.util.Comparator;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import biouml.plugins.riboseq.db.DatabaseCollections;
import biouml.plugins.riboseq.db.model.Condition;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class ConditionMultiSelector extends GenericMultiSelectEditor
{
    @Override
    protected Condition[] getAvailableValues()
    {
        DataElement experiment = (DataElement)getBean();
        DatabaseCollections dbCollections = DatabaseCollections.getInstanceForExperimentCollection( experiment.getOrigin() );
        DataCollection<Condition> conditionCollection = dbCollections.getConditionCollection();
        return conditionCollection.stream().sorted( Comparator.comparing( Condition::toString ) ).toArray( Condition[]::new );
    }

    public static PropertyDescriptorEx registerSelector(PropertyDescriptorEx pde)
    {
        pde.setPropertyEditorClass( ConditionMultiSelector.class );
        pde.setSimple( true );
        return pde;
    }

    public static PropertyDescriptorEx registerSelector(String property, Class<?> beanClass) throws IntrospectionException
    {
        return registerSelector( new PropertyDescriptorEx( property, beanClass ) );
    }
}

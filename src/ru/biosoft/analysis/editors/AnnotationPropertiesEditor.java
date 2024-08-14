package ru.biosoft.analysis.editors;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.analysis.AnnotateParameters;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.PropertyInfo;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class AnnotationPropertiesEditor extends GenericMultiSelectEditor
{
    @Override
    protected PropertyInfo[] getAvailableValues()
    {
        try
        {
            DataCollection<?> dataCollection = ((AnnotateParameters)getBean()).getAnnotationCollection();
            if( dataCollection == null || dataCollection.getSize() == 0 )
                return new PropertyInfo[] {};
            DataElement dataElement = dataCollection.iterator().next();
            return BeanUtil.getRecursivePropertiesList(dataElement);
        }
        catch( Exception ex )
        {
            ExceptionRegistry.log( ex );
            return new PropertyInfo[] {};
        }
    }
}

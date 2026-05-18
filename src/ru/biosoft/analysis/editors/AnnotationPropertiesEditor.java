package ru.biosoft.analysis.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.analysis.AnnotateParameters;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.PropertyInfo;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class AnnotationPropertiesEditor extends GenericMultiSelectEditor
{

    private static final String MINIMAL_ANNOTATION_CHECK = "minimalAnnotationCheck";
    @Override
    protected PropertyInfo[] getAvailableValues()
    {
        try
        {
            DataCollection<?> dataCollection = ((AnnotateParameters)getBean()).getAnnotationCollection();
            return getProperties( dataCollection );
        }
        catch( Exception ex )
        {
            ExceptionRegistry.log( ex );
            return new PropertyInfo[] {};
        }
    }

    public static PropertyInfo[] getProperties(DataCollection<?> dataCollection)
    {
        if( dataCollection == null || dataCollection.getSize() == 0 )
            return new PropertyInfo[] {};
        int maxTries = 1;
        if( dataCollection.getInfo().getProperty( MINIMAL_ANNOTATION_CHECK ) != null )
        {
            maxTries = Integer.valueOf( dataCollection.getInfo().getProperty( MINIMAL_ANNOTATION_CHECK ) );
        }
        DataElement dataElement = null;
        int numTries = 0;

        Set<PropertyInfo> curInfo = new HashSet<>();
        Iterator iterator = dataCollection.iterator();
        while ( numTries < maxTries )
        {
            try
            {
                dataElement = (DataElement) iterator.next();
            }
            catch (Exception e)
            {
            }
            numTries++;
            PropertyInfo[] pi;
            if( dataElement == null )
                pi = new PropertyInfo[] {};
            else
                pi = BeanUtil.getRecursivePropertiesList( dataElement );
            Collections.addAll( curInfo, pi );

        }
        List<PropertyInfo> pl = new ArrayList<PropertyInfo>( curInfo );
        Collections.sort( pl );
        return pl.toArray( new PropertyInfo[] {} );
    }
}

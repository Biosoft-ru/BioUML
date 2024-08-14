package ru.biosoft.analysis;

import java.lang.reflect.Method;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;


public class CopyDataElement extends AnalysisMethodSupport<CopyDataElement.Parameters>
{

    public CopyDataElement(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths( parameters.getInputNames(), null );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataElement src = parameters.getSrc().getDataElement();
        Method cloneMethod;
        try
        {
            cloneMethod = src.getClass().getMethod( "clone", DataCollection.class, String.class );
        }
        catch( NoSuchMethodException e )
        {
            throw new Exception( "Element doesn't support copying", e );
        }

        DataElementPath dstPath = parameters.getDst();
        DataCollectionUtils.createFoldersForPath( dstPath );
        DataCollection<?> parent = dstPath.getParentCollection();
        String name = dstPath.getName();
        DataElement result;
        try
        {
            result = (DataElement)cloneMethod.invoke( src, parent, name );
        }
        catch( Exception e )
        {
            throw new Exception( "Copying error", e );
        }

        //Ilya: dirty hack. Sometimes for some reason new element does not replace old one (example - Dynamic modeling workflow)
        DataCollection<?> parentCollection = dstPath.getParentCollection();
        if (parentCollection.contains(result.getName()))
            parentCollection.remove(result.getName());
        
        dstPath.save( result );
        return result;
    }

    @SuppressWarnings ( "serial" )
    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath src, dst;

        @PropertyName("Source")
        @PropertyDescription("Source path")
        public DataElementPath getSrc()
        {
            return src;
        }

        public void setSrc(DataElementPath src)
        {
            this.src = src;
        }

        @PropertyName("Destination")
        @PropertyDescription("Destination path")
        public DataElementPath getDst()
        {
            return dst;
        }

        public void setDst(DataElementPath dst)
        {
            this.dst = dst;
        }
    }

    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "src" ).inputElement( ru.biosoft.access.core.DataElement.class ).add();
            property( "dst" ).outputElement( ru.biosoft.access.core.DataElement.class ).add();
        }
    }
}

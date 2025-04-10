package ru.biosoft.access;

import java.util.LinkedHashMap;
import java.util.Map;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.EntryStream;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.Transformer;
import ru.biosoft.access.file.FileDataCollection;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.file.GenericFileDataCollection;
import ru.biosoft.access.generic.TransformerRegistry;
import ru.biosoft.access.generic.TransformerRegistry.TransformerInfo;
import ru.biosoft.util.BeanAsMapUtil;


public class FDCBeanProvider implements BeanProvider
{

    @Override
    public Object getBean(String path)
    {
        DataElementPath dePath = DataElementPath.create( path );
        DataCollection<?> parent = dePath.optParentCollection();
        if( parent == null || (!(parent instanceof FileDataCollection) && !(parent instanceof GenericFileDataCollection)) )
            return null;
        
        String transformerName = FileInfo.NO_TRANSFORMER;
        
        Map<String, Object> fileInfo = parent instanceof GenericFileDataCollection ? ((GenericFileDataCollection) parent).getFileInfo( dePath.getName() )
                : ((FileDataCollection) parent).getFileInfo( dePath.getName() );
        String transformerClass = (String)fileInfo.get("transformer");
        if(transformerClass != null)
        {
            Class<? extends Transformer> clazz = (Class<? extends Transformer>)ClassLoading.loadClass( transformerClass );
            TransformerInfo ti = TransformerRegistry.getTransformerInfo( clazz );
            transformerName = ti.getName();
        }
        FileInfo fi = new FileInfo();
        fi.setTransformer( transformerName );
        Map<String, Object> properties = null;
        if( parent instanceof GenericFileDataCollection )
        {
            properties = ((GenericFileDataCollection) parent).getChildProperties( dePath.getName(), transformerClass );
        }
        else if( fileInfo != null )
            properties = (Map<String, Object>) fileInfo.get( "properties" );
        if(properties != null)
        {
            DynamicPropertySet dps = fi.getElementProperties();
            properties.forEach( (k,v)->{
                dps.add( new DynamicProperty( k, v.getClass(), v ) );
            } );
        }
        
        return fi;
    }
    
    @Override
    public void saveBean(String path, Object bean) throws Exception
    {
        FileInfo fi = (FileInfo)bean;
        
        DataElementPath dePath = DataElementPath.create( path );
        DataCollection<?> parent = dePath.optParentCollection();
        if( parent == null || (!(parent instanceof FileDataCollection) && !(parent instanceof GenericFileDataCollection)) )
            return;
        
        Map<String, Object> yaml = new LinkedHashMap<>();
        yaml.put( "name", dePath.getName() );

        if(!fi.transformer.equals( FileInfo.NO_TRANSFORMER ))
        {
            TransformerInfo ti = TransformerRegistry.getTransformerInfo( fi.transformer );
            yaml.put( "transformer", ti.getTransformerClass().getName() );
        }

        if(fi.transformerOptions != null)
        {
            Map<String, Object> tOpts = BeanAsMapUtil.convertBeanToMap( fi.transformerOptions );
            yaml.put( "transformerParameters", tOpts );
        }
        
        if(fi.elementProperties != null && !fi.elementProperties.isEmpty())
        {
            Map<String, Object> props = EntryStream.of( fi.elementProperties.asMap() ).filter( e -> {
                return e.getValue() != null && !(e.getValue().toString().isEmpty());
            } ).toMap();
            if( !props.isEmpty() )
                yaml.put( "properties", props );
        }
        
        if( parent instanceof GenericFileDataCollection )
            ((GenericFileDataCollection) parent).setFileInfo( yaml );
        else
            ((FileDataCollection) parent).setFileInfo( yaml );
        FileInfo fiNew = (FileInfo) getBean( path );
        fi.setElementProperties( fiNew.getElementProperties() );
    }
    
    
    public static class FileInfo
    {
        public static final String NO_TRANSFORMER = "(none)";
        private String transformer = NO_TRANSFORMER;
        private Object transformerOptions;
        private DynamicPropertySet elementProperties = new DynamicPropertySetSupport();

        @PropertyName("File type")
        public String getTransformer()
        {
            return transformer;
        }
        public void setTransformer(String transformer)
        {
            this.transformer = transformer;
            // TransformerInfo ti = TransformerRegistry.getTransformerInfo( transformer );
            // Transformer t = ti.getTransformerClass().newInstance();
        }
        public Object getTransformerOptions()
        {
            return transformerOptions;
        }
        public void setTransformerOptions(Object transformerOptions)
        {
            this.transformerOptions = transformerOptions;
        }

        @PropertyName("Element options")
        public DynamicPropertySet getElementProperties()
        {
            return elementProperties;
        }
        public void setElementProperties(DynamicPropertySet elementProperties)
        {
            this.elementProperties = elementProperties;
        }

        public boolean hideTransformerOptions()
        {
            return transformerOptions == null;
        }
        
    }
    
    public static class FileInfoBeanInfo extends BeanInfoEx
    {
        public FileInfoBeanInfo()
        {
            super( FileInfo.class );
        }
        @Override
        protected void initProperties() throws Exception
        {
            String[] transformers = TransformerRegistry.getSupportedTransformers( FileDataElement.class ).prepend( FileInfo.NO_TRANSFORMER ).toArray( String[]::new );
            property( "transformer" ).tags(transformers).add();
            addHidden( "transformerOptions", "hideTransformerOptions" );
            add("elementProperties");
        }
    }
    
    
}

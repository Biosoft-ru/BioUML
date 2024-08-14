package ru.biosoft.access;

import java.util.LinkedHashMap;
import java.util.Map;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.Transformer;
import ru.biosoft.access.file.FileDataCollection;
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
        if(parent == null || !(parent instanceof FileDataCollection))
            return null;
        FileDataCollection fdc = (FileDataCollection)parent;
        
        String transformerName = FileInfo.NO_TRANSFORMER;
        
        Map<String, Object> fileInfo = fdc.getFileInfo(dePath.getName());
        String transformerClass = (String)fileInfo.get("transformer");
        if(transformerClass != null)
        {
            Class<? extends Transformer> clazz = (Class<? extends Transformer>)ClassLoading.loadClass( transformerClass );
            TransformerInfo ti = TransformerRegistry.getTransformerInfo( clazz );
            transformerName = ti.getName();
        }
        FileInfo fi = new FileInfo();
        fi.setTransformer( transformerName );
        
        Map<String, Object> properties = (Map<String, Object>)fileInfo.get( "properties" );
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
        if(parent == null || !(parent instanceof FileDataCollection))
            return;
        FileDataCollection fdc = (FileDataCollection)parent;
        
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
            yaml.put( "properties", fi.elementProperties.asMap());
        }
        
        fdc.setFileInfo( yaml );
    }
    
    
    public static class FileInfo
    {
        public static final String NO_TRANSFORMER = "(none)";
        private String transformer = NO_TRANSFORMER;
        private Object transformerOptions;
        private DynamicPropertySet elementProperties = new DynamicPropertySetSupport();
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
        public DynamicPropertySet getElementProperties()
        {
            return elementProperties;
        }
        public void setElementProperties(DynamicPropertySet elementProperties)
        {
            this.elementProperties = elementProperties;
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
            add("transformerOptions");
            add("elementProperties");
        }
    }
    
    
}

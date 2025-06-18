package ru.biosoft.access;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.EntryStream;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataCollection;
import ru.biosoft.access.file.FileType;
import ru.biosoft.access.file.FileTypeRegistry;
import ru.biosoft.access.file.GenericFileDataCollection;
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
        
        String fileType = FileInfo.AUTO_FILE_TYPE;
        
        Map<String, Object> fileInfo = parent instanceof GenericFileDataCollection ? ((GenericFileDataCollection) parent).getFileInfo( dePath.getName() )
                : ((FileDataCollection) parent).getFileInfo( dePath.getName() );
        String transformerClass = null;
        if( fileInfo.get( "type" ) != null )
        {
            fileType = (String) fileInfo.get( "type" );
            FileType ft = FileTypeRegistry.getFileType( fileType );
            if( ft != null )
                transformerClass = ft.getTransformerClassName();
        }
        else if( fileInfo.get( "transformer" ) != null )
        {
            transformerClass = (String) fileInfo.get( "transformer" );
            FileType ft = FileTypeRegistry.getFileTypeByTransformer( transformerClass );
            if( ft != null )
                fileType = ft.getName();
        }
        FileInfo fi = new FileInfo();
        fi.setFileType( fileType );

        Map<String, Object> properties = null;
        if( parent instanceof GenericFileDataCollection )
        {
            try
            {
                properties = ((GenericFileDataCollection) parent).getChildProperties( dePath.getName(), transformerClass );
            }
            catch (Exception e)
            {
                // can not read 
                e.printStackTrace();
            }
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

        if( !fi.fileType.equals( FileInfo.AUTO_FILE_TYPE ) )
        {
            String fileTypeName = fi.fileType;
            FileType fileType = FileTypeRegistry.getFileType( fileTypeName );
            if( fileType != null )
            {
                yaml.put( "type", fileType.getName() );
                if( parent instanceof FileDataCollection )
                    yaml.put( "transformer", fileType.getTransformerClassName() );
            }
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
        public static final String AUTO_FILE_TYPE = "(auto)";
        private String fileType = AUTO_FILE_TYPE;
        private Object transformerOptions;
        private DynamicPropertySet elementProperties = new DynamicPropertySetSupport();

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
        
        @PropertyName("File type")
        public String getFileType()
        {
            return fileType;
        }

        public void setFileType(String fileType)
        {
            this.fileType = fileType;
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
            String[] fileTypes = Stream.concat( Stream.of( FileInfo.AUTO_FILE_TYPE ), FileTypeRegistry.fileTypes().map( FileType::getName ).sorted() ).toArray( String[]::new );
            property( "fileType" ).tags( fileTypes ).add();
            addHidden( "transformerOptions", "hideTransformerOptions" );
            add("elementProperties");
        }
    }
    
    
}

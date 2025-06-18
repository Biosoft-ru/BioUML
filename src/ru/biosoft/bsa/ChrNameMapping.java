package ru.biosoft.bsa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.BeanInfoConstants;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.Environment;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class ChrNameMapping extends DataElementSupport
{
    public static final DataElementPath DEFAULT_PATH = DataElementPath.create("databases/Utils/ChrMapping");
    public static final String PROP_CHR_MAPPING = "ChrMapping";
    public static final String PROP_CHR_MAPPING_PATH = "ChrMappingPath";
    public static final @Nonnull String NONE_MAPPING = "(none)";
    
    public ChrNameMapping(String name, DataCollection<?> origin)
    {
        super( name, origin );
    }

    public Map<String, String> srcToDst = new HashMap<>();
    public Map<String, String> dstToSrc = new HashMap<>();
    
    public String srcToDst(String src) { return srcToDst.get( src ); }
    public String dstToSrc(String dst) { return dstToSrc.get( dst ); }
    
    //name can be complete path String or only name of mapping that should be taken from DEFAULT_PATH or Environment.getValue( PROP_CHR_MAPPING_PATH ) repository
    public static ChrNameMapping getMapping(String name)
    {
        if( name == null || name.isEmpty() || name.equals( NONE_MAPPING ) )
            return null;
        if(!name.endsWith( ".txt" ))
            name += ".txt";

        DataElementPath namePath = DataElementPath.create( name );
        if( !namePath.exists() )
        {
            DataElementPath parentPath = Environment.getValue( PROP_CHR_MAPPING_PATH ) != null ? DataElementPath.create( (String) Environment.getValue( PROP_CHR_MAPPING_PATH ) )
                    : DEFAULT_PATH;
            namePath = parentPath.getChildPath( name );
        }
        try
        {
            return namePath.getDataElement( ChrNameMapping.class );
        }
        catch (Exception e)
        {
            return null;
        }
    }
    
    public static ChrNameMapping getMapping(Properties props)
    {
        String chrMappingStr = props.getProperty( PROP_CHR_MAPPING );
        if(chrMappingStr != null)
            return getMapping( chrMappingStr );
        return null;
    }
    
    public static class ChrMappingSelector extends GenericComboBoxEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            if( DEFAULT_PATH.exists() )
            {
                List<String> names = DEFAULT_PATH.getDataCollection().getNameList();
                boolean canBeNull = BeanUtil.getBooleanValue( this, BeanInfoConstants.CAN_BE_NULL );
                if( canBeNull )
                    names.add( NONE_MAPPING );
                return names.toArray( new String[0] );
            }
            return new String[] {NONE_MAPPING};
        }
    }
}


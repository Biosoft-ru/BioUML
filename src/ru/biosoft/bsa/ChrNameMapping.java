package ru.biosoft.bsa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.BeanInfoConstants;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class ChrNameMapping extends DataElementSupport
{
    public static final DataElementPath DEFAULT_PATH = DataElementPath.create("databases/Utils/ChrMapping");
    public static final String PROP_CHR_MAPPING = "ChrMapping";
    public static final @Nonnull String NONE_MAPPING = "(none)";
    
    public ChrNameMapping(String name, DataCollection<?> origin)
    {
        super( name, origin );
    }

    private Map<String, String> srcToDst = new HashMap<>();
    private Map<String, String> dstToSrc = new HashMap<>();
    
    public String srcToDst(String src) { return srcToDst.get( src ); }
    public String dstToSrc(String dst) { return dstToSrc.get( dst ); }
    
    public static ChrNameMapping getMapping(String name)
    {
        if( name == null || name.isEmpty() || name.equals( NONE_MAPPING ) )
            return null;
        if(!name.endsWith( ".txt" ))
            name += ".txt";
        return  DEFAULT_PATH.getChildPath( name ).getDataElement( ChrNameMapping.class );
    }
    
    public static ChrNameMapping getMapping(Properties props)
    {
        String chrMappingStr = props.getProperty( PROP_CHR_MAPPING );
        if(chrMappingStr != null)
            return getMapping( chrMappingStr );
        return null;
    }
    
    public static class Transformer extends AbstractFileTransformer<ChrNameMapping>
    {
        @Override
        public Class<? extends ChrNameMapping> getOutputType()
        {
            return ChrNameMapping.class;
        }

        @Override
        public ChrNameMapping load(File file, String name, DataCollection<ChrNameMapping> origin) throws Exception
        {
            ChrNameMapping result = new ChrNameMapping( name, origin );
            try (BufferedReader reader = new BufferedReader( new FileReader( file ) ))
            {
                String line;
                while( ( line = reader.readLine() ) != null )
                {
                    String[] parts = line.split( "\t", 2 );
                    result.srcToDst.put( parts[0], parts[1] );
                }
            }
            result.srcToDst.forEach( (k, v) -> result.dstToSrc.put( v, k ) );
            return result;
        }

        @Override
        public void save(File output, ChrNameMapping element) throws Exception
        {
            throw new UnsupportedOperationException();
        }
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


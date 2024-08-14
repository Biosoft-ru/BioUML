package ru.biosoft.workbench.editors;

import java.util.HashMap;
import java.util.Map;

import ru.biosoft.access.core.DataCollection;

/**
 * @author lan
 * @deprecated Currently to use GenericMultiSelectEditor or GenericComboBoxEditor, you must subclass these classes and override getAvailableValues. GenericEditorData should not be used anymore.
 * It's especially forbidden to register non-constant set of values under the same name as this may cause really weird bugs
 */
@Deprecated
public class GenericEditorData
{
    private static Map<String,Object> values = new HashMap<>();
    
    protected static Object[] getAvailableValues(String key)
    {
        Object vals = values.get(key);
        if(vals == null) return null;
        Object[] result = ( vals instanceof StringsProvider ) ? ( (StringsProvider)vals ).getStrings() : (Object[])vals;
        return result == null?new Object[]{}:result;
    }

    /**
     * Register list of strings which can appear in specific control instance identified by key
     */
    public static void registerValues(String key, Object[] values)
    {
        GenericEditorData.values.put(key, values);
    }
    
    /**
     * Register names provider which will provide names list on demand
     * Useful for lists that can change (DataCollection names, etc)
     */
    public static void registerValues(String key, StringsProvider provider)
    {
        GenericEditorData.values.put(key, provider);
    }
    
    public interface StringsProvider
    {
        public Object[] getStrings();
    }
    
    public static class DataCollectionNamesProvider implements StringsProvider
    {
        private DataCollection<?> dc;
        
        public DataCollectionNamesProvider(DataCollection<?> dc)
        {
            this.dc = dc;
        }

        @Override
        public String[] getStrings()
        {
            if(dc == null) return null;
            return dc.names().toArray( String[]::new );
        }
    }
}

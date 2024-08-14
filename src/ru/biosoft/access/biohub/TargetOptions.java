package ru.biosoft.access.biohub;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.support.SerializableAsText;
import ru.biosoft.workbench.editors.GenericEditorData;
import ru.biosoft.workbench.editors.GenericMultiSelectItem;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

/**
 * Target collection preferences
 */
public class TargetOptions
{
    protected CollectionRecord[] collections;
    
    public TargetOptions(CollectionRecord... collections)
    {
        this.collections = collections;
    }

    public TargetOptions(String... collections)
    {
        this.collections = Arrays.stream( collections ).map( c -> new CollectionRecord( c, false ) ).toArray( CollectionRecord[]::new );
    }
    
    public TargetOptions(String[] collections, boolean use)
    {
        this(collections);
        setUseForAll(use);
    }
    
    public TargetOptions(DataElementPath path)
    {
        this(new String[] {path.toString()}, true);
    }

    /**
     * Set use flag for all collections
     */
    public void setUseForAll(boolean use)
    {
        for( CollectionRecord cr : collections )
        {
            cr.setUse(use);
        }
    }

    /**
     * Get collection records.
     * Necessary for PropertyInspector
     */
    public CollectionRecord[] getCollections()
    {
        return collections;
    }
    
    public StreamEx<CollectionRecord> collections()
    {
        return StreamEx.of(collections);
    }

    /**
     * Set collection records.
     * Necessary for PropertyInspector
     */
    public void setCollections(CollectionRecord[] collections)
    {
        this.collections = collections;
    }
    
    public DataElementPathSet getUsedCollectionPaths()
    {
        return collections().filter( CollectionRecord::isUse ).map( CollectionRecord::getPath ).toCollection( DataElementPathSet::new );
    }

    public static class CollectionRecord extends Option implements SerializableAsText
    {
        protected DataElementPath path;
        protected boolean use;
        protected GenericMultiSelectItem queryEngineNames;
        protected String[] availableNames;

        public CollectionRecord(DataElementPath path, boolean use)
        {
            this(path, use, new String[]{});
        }
        
        public CollectionRecord(String path, boolean use)
        {
            this(path, use, new String[]{});
        }
        
        public CollectionRecord(DataElementPath path, boolean use, String[] queryEngineNames)
        {
            this.path = path;
            this.use = use;
            availableNames = queryEngineNames;
            GenericEditorData.registerValues("queryEngineNames" + path.getName(), queryEngineNames);
            setQueryEngineNamesStrings(new String[]{});
        }
        
        public CollectionRecord(String path, boolean use, String[] queryEngineNames)
        {
            this(DataElementPath.create(path), use, queryEngineNames);
        }
        
        public static CollectionRecord createInstance(String serialized)
        {
            try
            {
                JsonObject obj = JsonObject.readFrom( serialized );
                CollectionRecord result = new CollectionRecord( obj.get( "path" ).asString(), true );
                return result;
            }
            catch( ParseException | UnsupportedOperationException e )
            {
                throw new ParameterNotAcceptableException( e, "serialized", serialized );
            }
        }

        public DataElementPath getPath()
        {
            return path;
        }

        public String getName()
        {
            return path.toString();
        }
        
        public String getShortName()
        {
            return path.getName();
        }

        public boolean isUse()
        {
            return use || getQueryEngineNamesStrings().length > 0;
        }

        public void setUse(boolean use)
        {
            //TODO: refactor use
            boolean wasUsed = getQueryEngineNamesStrings().length > 0;
            if( use )
                setQueryEngineNamesStrings(availableNames);
            else
                setQueryEngineNamesStrings(new String[]{});
            this.use = use;
            firePropertyChange("isUse", wasUsed, use);
        }
        
        public GenericMultiSelectItem getQueryEngineNames()
        {
            return queryEngineNames;
        }
        public String[] getQueryEngineNamesStrings()
        {
            return queryEngineNames == null ? null : queryEngineNames.getStringValues();
        }
        public void setQueryEngineNames(GenericMultiSelectItem queryEngineNames)
        {
            this.queryEngineNames = queryEngineNames;
        }
        public void setQueryEngineNamesStrings(String[] queryEngineNames)
        {
            setQueryEngineNames(new GenericMultiSelectItem("queryEngineNames" + getShortName(), queryEngineNames));
        }

        @Override
        public String getAsText()
        {
            return new JsonObject().add("path", path.toString()).toString();
        }
    }

    public static class CollectionRecordBeanInfo extends BeanInfoEx
    {
        public CollectionRecordBeanInfo()
        {
            super(CollectionRecord.class, MessageBundle.class.getName());
        }

        @Override
        public void initProperties() throws Exception
        {
            initResources(MessageBundle.class.getName());

            PropertyDescriptor pde = new PropertyDescriptorEx("name", beanClass, "getShortName", null);
            add(pde, getResourceString("CN_COLLECTION_NAME"), getResourceString("CD_COLLECTION_NAME"));

            pde = new PropertyDescriptorEx("queryEngineNames", beanClass);
            add(pde, getResourceString("CN_SEARSH_ENGINE"), getResourceString("CD_SEARSH_ENGINE"));
        }
    }
}

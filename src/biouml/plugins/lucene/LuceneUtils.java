package biouml.plugins.lucene;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

import biouml.model.Module;
import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.exception.InternalException;
import ru.biosoft.jobcontrol.JobControl;

public class LuceneUtils
{
    private static final Logger log = Logger.getLogger(LuceneUtils.class.getName());

    /**
     * @param indexes non-parsed line
     * @return stream of indexes
     */
    public static StreamEx<String> indexedFields(String indexes)
    {
        if(indexes == null)
            return StreamEx.empty();
        return StreamEx.split( indexes, "[;,]" ).map( String::trim );
    }

    public static StreamEx<String> indexedFields(DataCollection<?> dc)
    {
        if(dc == null)
            return StreamEx.empty();
        return indexedFields(dc.getInfo().getProperty( LuceneQuerySystem.LUCENE_INDEXES ));
    }
    
    /**
     * Special additional function for searching all internal data collections
     * which contain in base data collection
     * 
     * @param dc
     * @param property - property name
     * @return array of strings
     * @throws Exception
     */
    public static List<String> getCollectionsNames(DataCollection<?> dc, String property) throws Exception
    {
        if( property != null )
            if( property.trim().isEmpty() )
                property = null;
        
        List<String> names = new ArrayList<>();
        addChildCollectionsNames(names, getLuceneParent(dc), dc, property);
        Collections.sort(names);

        return names;
    }
    
    public static DataCollection<?> getLuceneParent(DataCollection<?> dc)
    {
        return StreamEx.iterate( dc.getCompletePath(), DataElementPath::getParentPath ).takeWhile( path -> !path.isEmpty() )
                .map( DataElementPath::getDataCollection )
                .findFirst( parent -> getLuceneFacade( parent ) != null )
                .orElseThrow( () -> new InternalException( "No lucene collection found for " + dc.getCompletePath() ) );
    }
    
    public static DataCollection<?> optLuceneParent(DataCollection<?> dc)
    {
        return StreamEx.iterate( dc.getCompletePath(), DataElementPath::getParentPath ).takeWhile( path -> !path.isEmpty() )
            .map( DataElementPath::getDataCollection )
            .findFirst( parent -> getLuceneFacade( parent ) != null )
            .orElse( null );
    }

    protected static void addChildCollectionsNames(List<String> list, @Nonnull DataCollection<?> luceneFolder, DataCollection<?> parent, String property)
    {
        if( parent != null )
        {
            if(DataCollectionUtils.checkPrimaryElementType( parent, FolderCollection.class ))
            {
                list.add(CollectionFactory.getRelativeName(parent, luceneFolder));
                return;
            }
            else if( property != null )
            {
                String value = parent.getInfo().getProperty(property);
                if( value != null && value.trim().length() > 0 )
                {
                    list.add(CollectionFactory.getRelativeName(parent, luceneFolder));
                }
            }
            else
            {
                if( getPropertiesNames(parent).size() > 0 )
                {
                    list.add(CollectionFactory.getRelativeName(parent, luceneFolder));
                }
            }
            if( checkIfChildIndexPossible(parent) )
            {
                for( ru.biosoft.access.core.DataElement de : parent )
                {
                    try
                    {
                        if( de instanceof DataCollection )
                            addChildCollectionsNames(list, luceneFolder, (DataCollection<?>)de, property);
                    }
                    catch( Throwable t )
                    {
                        log.log(Level.SEVERE, "Lucene utils search names error: " + t);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Check if it is possible to create index for child collections
     * @param dc parent collection
     * @return
     */
    public static boolean checkIfChildIndexPossible(DataCollection<?> dc)
    {
        DataCollection<?> luceneParent = optLuceneParent( dc );
        if( luceneParent != null )
        {
            if( dc == luceneParent )
                return true;
            if( dc.getInfo().getProperty("lucene-children-indexed") != null ) return true;
            DataElementPath relativePath = ru.biosoft.access.core.DataElementPath
                    .create( dc.getCompletePath().getPathDifference( luceneParent.getCompletePath() ) );
            
            if(relativePath.toString().equals( "tmp" ) || relativePath.toString().equals( "Journal" ))
            {
                return false;
            }

            //TODO: use special property from DC info, current version should be by default
            if( relativePath.getDepth() < 2 && !relativePath.getName().equals(Module.DIAGRAM) )
            {
                return true;
            }
        }
        return false;
    }

    public static @Nonnull Vector<String> getPropertiesNames(DataCollection<?> dc)
    {
        Vector<String> v = new Vector<>();

        if( dc == null )
            return v;

        LuceneHelper helper = getLuceneHelper(dc);
        if( helper != null )
        {
            return helper.getPropertiesNames();
        }

        String c = dc.getInfo().getProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY);
        String pluginNames = dc.getInfo().getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);

        if( c == null )
            return v;

        try
        {
            Class<?> clazz = ClassLoading.loadClass( c, pluginNames );
            BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
            BeanDescriptor bd = beanInfo.getBeanDescriptor();
            PropertyDescriptor[] properties = (PropertyDescriptor[])bd.getValue(BeanInfoConstants.ORDER);
            if( properties == null )
                properties = beanInfo.getPropertyDescriptors();

            for( PropertyDescriptor property : properties )
            {
                String tag = (String)property.getValue(HtmlPropertyInspector.DISPLAY_NAME);
                if( tag != null )
                {
                    if( property.getName().equals("type") )
                        continue;
                    if( property.getPropertyType() == null )
                        continue;
                    if( property.getPropertyType().isArray() || property.getPropertyType() == String.class )
                        v.add(property.getName());
                }
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Undefined error: ", t);
            v = new Vector<>();
            try
            {
                Class<?> clazz = ClassLoading.loadClass( c, pluginNames );
                BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
                MethodDescriptor[] methods = beanInfo.getMethodDescriptors();
                for( MethodDescriptor method : methods )
                {
                    if( method.getMethod().getParameterTypes().length > 0 )
                        continue;
                    if( method.getMethod().getReturnType() == null )
                        continue;
                    if( method.isHidden() )
                        continue;
                    String methodName = method.getName();
                    if( methodName.length() > 4 )
                    {
                        if( methodName.startsWith("get") )
                        {
                            if( methodName.equals("getOrigin") || methodName.equals("getClass") )
                                continue;
                            String prop = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                            v.add(prop);
                        }
                    }
                }
            }
            catch( Throwable t2 )
            {
                log.log(Level.SEVERE, "Undefined error: ", t2);
            }
        }

        return v;
    }

    protected static LuceneHelper getLuceneHelper(DataCollection<?> dc)
    {
        String helperClassName = dc.getInfo().getProperty(LuceneQuerySystem.LUCENE_HELPER);
        if( helperClassName != null )
        {
            String pluginNames = dc.getInfo().getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);
            try
            {
                Class<? extends LuceneHelper> helperClass = ClassLoading.loadSubClass( helperClassName, pluginNames, LuceneHelper.class );
                return helperClass.getConstructor(DataCollection.class).newInstance(dc);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not load helper class: " + helperClassName, t);
            }
        }
        return null;
    }
    
    public static void buildIndexes(DataCollection<?> module, JobControl jobControl, Logger log) throws Exception
    {
        LuceneQuerySystem luceneFacade = getLuceneFacade(module);
        if( luceneFacade instanceof LuceneQuerySystemImpl )
        {
            ( (LuceneQuerySystemImpl)luceneFacade ).setLuceneDir(module.getInfo().getProperties().getProperty(
                    DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, ".")
                    + "/luceneIndex");
        }
        if( luceneFacade != null && !luceneFacade.getCollectionsNamesWithIndexes().isEmpty() )
        {
            luceneFacade.createIndex(log, jobControl);
        }
    }

    /**
     * Get lucene facade for module
     */
    public static LuceneQuerySystem getLuceneFacade(DataCollection<?> module)
    {
        QuerySystem querySystem = module.getInfo().getQuerySystem();
        if( querySystem instanceof LuceneQuerySystem )
            return (LuceneQuerySystem)querySystem;
        return null;
    }

    public static final String INDEX_FOLDER_NAME = "luceneIndex";
}

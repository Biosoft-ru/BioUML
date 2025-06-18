package ru.biosoft.server.servlets.webservices.providers;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.developmentontheedge.beans.model.ArrayProperty;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.CompositeProperty;
import com.developmentontheedge.beans.model.Property;

import biouml.model.Diagram;
import biouml.plugins.research.web.WebResearchProvider;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import biouml.plugins.server.access.AccessProtocol;
import ru.biosoft.access.BeanRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.NetworkDataCollection;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SessionCache;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.util.FieldMap;
import ru.biosoft.util.FileItem;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.workbench.editors.FileSelector;

/**
 * Provides beans functions
 */
public class WebBeanProvider extends WebJSONProviderSupport
{
    public static final String USE_CACHE = "useCache";
    private static final String USE_JSON_ORDER = "useJsonOrder";
    protected static final String FIELDS_ATTR = "fields";
    protected static final String JSON_ATTR = "json";
    
    public static final String BEANS_PREFIX = "beans/";

    protected static final Logger log = Logger.getLogger(WebBeanProvider.class.getName());
    
    public static void sendBeanStructure(String completeName, FieldMap fieldMap, JSONResponse response, boolean useCache, int showMode) throws Exception
    {
        Object bean = getBean(completeName, useCache);
        if(bean instanceof DataElement)
        {
            try
            {
                bean = DataCollectionUtils.fetchPrimaryElement((DataElement)bean, Permission.INFO);
            }
            catch(Exception e)
            {
                log.log( Level.SEVERE, "Error fetching bean: ", e );
            }
        }
        sendBeanStructure(completeName, bean, fieldMap, response, showMode);
    }
    
    public static void sendBeanStructure(String completeName, Object bean, JSONResponse response) throws WebException, IOException
    {
        sendBeanStructure(completeName, bean, response, Property.SHOW_USUAL);
    }

    public static void sendBeanStructure(String completeName, Object bean, JSONResponse response, int showMode) throws WebException, IOException
    {
        sendBeanStructure(completeName, bean, FieldMap.ALL, response, showMode);
    }

    public static void sendBeanStructure(String completeName, Object bean, FieldMap fieldMap, JSONResponse response, int showMode) throws WebException, IOException
    {
        if( bean == null )
        {
            throw new WebException(TextUtil2.isFullPath(completeName)?"EX_QUERY_NO_ELEMENT":"EX_INTERNAL_BEAN_NOT_FOUND", completeName);
        }
        JSONArray jsonProperties;
        JSONObject beanAttributes;
        try
        {
            ComponentModel model = ComponentFactory.getModel(bean, Policy.DEFAULT, true);
            jsonProperties = JSONUtils.getModelAsJSON(model, fieldMap, showMode);
            beanAttributes = JSONUtils.getBeanAttributes(model);
        }
        catch( Exception e )
        {
            throw new WebException(e, "EX_INTERNAL_READ_BEAN", completeName);
        }
        response.sendJSONBean(jsonProperties, beanAttributes);
    }

    public static void saveBeanChanges(final String completeName, final Object bean, final JSONArray jsonParams, boolean useJsonOrder) throws WebException
    {
        DataCollection<?> parent = bean instanceof DataElement
                ? ( (DataElement)bean ).getOrigin() != null ? ( (DataElement)bean ).getOrigin().getCompletePath().optDataCollection() : null
                : null;
        if( parent != null && !isClonedForEdit( parent ) && ( !parent.isMutable() || !SecurityManager.getPermissions(parent.getCompletePath()).isWriteAllowed() ) )
            throw new WebException("EX_ACCESS_READ_ONLY", completeName);
        preprocessJSON(bean, jsonParams);
        try
        {
            Object realBean = SecurityManager.runPrivileged(() -> bean instanceof DataElement ? DataCollectionUtils.fetchPrimaryElementPrivileged((DataElement)bean)
                    : bean);
            JSONUtils.correctBeanOptions(realBean, jsonParams, useJsonOrder);
            saveBean(completeName, realBean);
        }
        catch( Exception e )
        {
            throw new WebException(e, "EX_INTERNAL_UPDATE_BEAN", completeName);
        }
    }
    
    private static boolean isClonedForEdit(DataCollection<?> dc)
    {
        do {
            String strVal = dc.getInfo().getProperty( NetworkDataCollection.CLONE_FOR_EDIT_PROPERTY );
            boolean isCloned = Boolean.parseBoolean( strVal );
            if(isCloned)
                return true;
            dc = dc.getOrigin();
        } while(dc != null);
        return false;
    }

    public static void preprocessJSON(Object bean, JSONArray jsonParams)
    {
        CompositeProperty model = null;
        if(jsonParams == null) return;
        if( bean instanceof CompositeProperty )
        {
            model = (CompositeProperty)bean;
        }
        else
        {
            model = ComponentFactory.getModel(bean, Policy.DEFAULT, true);
        }
        /*
         * Order of cycles is important!
         */
        for( int j = 0; j < jsonParams.length(); j++ )
        {
            JSONObject jsonObject = jsonParams.optJSONObject(j);
            if(jsonObject == null) continue;
            String name = jsonObject.optString("name");
            if( name == null )
                continue;
            for( int i = 0; i < model.getPropertyCount(); i++ )
            {
                Property property = model.getPropertyAt(i);
                String propertyName = property.getName();
                if( property.isReadOnly() )
                    continue;
                if( name.equals(propertyName) )
                {
                    if( property instanceof CompositeProperty && !property.isHideChildren() )
                    {
                        preprocessJSON(property, jsonObject.optJSONArray("value"));
                    }
                    else if( property instanceof ArrayProperty )
                    {
                        Object oldValue = property.getValue();
                        if( oldValue != null && oldValue.getClass().isArray() )
                        {
                            Object[] oldArray = (Object[])oldValue;
                            JSONArray jsonArray = jsonObject.optJSONArray("value");
                            if(jsonArray == null) continue;
                            int index = 0;
                            for( Object oldObject : oldArray )
                            {
                                CompositeProperty elementModel = null;
                                if( oldObject instanceof CompositeProperty )
                                {
                                    elementModel = (CompositeProperty)oldObject;
                                }
                                else
                                {
                                    elementModel = ComponentFactory.getModel(oldObject, Policy.DEFAULT, true);
                                }
                                preprocessJSON(elementModel, jsonArray.optJSONArray(index));
                                index++;
                            }
                        }
                    }
                    else
                    {
                        Class<?> c = property.getPropertyEditorClass();
                        if( c != null && FileSelector.class.isAssignableFrom(c))
                        {
                            String fileID = jsonObject.optString("value");
                            if(fileID == null || fileID.equals("")) continue;
                            try
                            {
                                FileItem uploadedFile = WebServicesServlet.getUploadedFile(fileID);
                                jsonObject.put("filePath", uploadedFile.getAbsolutePath());
                                jsonObject.put("originalName", uploadedFile.getOriginalName());
                            }
                            catch( Exception e )
                            {
                                log.log(Level.SEVERE, "Error preprocessing upload parameters: ", e);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns bean by complete name
     * @throws Exception 
     */
    public static Object getBean(String completeName)
    {
        return getBean(completeName, true);
    }

    /**
     * Returns bean by complete name
     * @param useCache whether to use cache (if applicable)
     * @throws Exception 
     */
    public static Object getBean(String completeName, boolean useCache)
    {
        if(!useCache)
            WebServicesServlet.getSessionCache().removeObject(completeName);
        return BeanRegistry.getBean(completeName, WebServicesServlet.getSessionCache());
    }

    /**
     * Additional actions after bean changes
     * @throws WebException
     */
    public static void saveBean(String completeName, Object bean) throws WebException
    {
        try
        {
            SessionCache cache = WebServicesServlet.getSessionCache();
            BeanRegistry.saveBean( completeName, bean, cache );
        }
        catch( Exception e )
        {
            throw new WebException(e, "EX_INTERNAL_UPDATE_BEAN", completeName);
        }
    }
    
    public static boolean isUseJsonOrder(BiosoftWebRequest arguments) {
        boolean useJsonOrder = true;
        String useJsonOrderStr = arguments.get( USE_JSON_ORDER );
        if( "no".equals( useJsonOrderStr ) )
            useJsonOrder = false;
        return useJsonOrder;
    }

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        String path = arguments.getString(AccessProtocol.KEY_DE);
        String beanAction = arguments.getAction();
        String useCacheObj = arguments.get(USE_CACHE);
        int showMode = arguments.optInt(SHOW_MODE, Property.SHOW_USUAL);
        boolean useCache = useCacheObj == null || !useCacheObj.equals("no");
        if( beanAction.equals("get") )
        {
            sendBeanStructure(path, new FieldMap(arguments.get(FIELDS_ATTR)), response, useCache, showMode);
        }
        else if( beanAction.equals("set") )
        {
            Object bean = getBean(path);
            if( bean == null )
                throw new WebException("EX_INTERNAL_BEAN_NOT_FOUND", path);
            
            boolean useJsonOrder = WebBeanProvider.isUseJsonOrder( arguments );
            saveBeanChanges(path, bean, arguments.getJSONArray(JSON_ATTR), useJsonOrder);
            // Return changed bean back
            sendBeanStructure(path, bean, response, showMode);
        }
        else
            throw new WebException("EX_QUERY_PARAM_INVALID_VALUE", BiosoftWebRequest.ACTION);
    }
}

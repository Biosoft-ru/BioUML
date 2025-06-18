package biouml.plugins.server.access;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.developmentontheedge.application.ApplicationUtils;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import biouml.model.Module;
import biouml.model.util.ModulePackager;
import one.util.streamex.EntryStream;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.core.SymbolicLinkDataCollection;
import ru.biosoft.access.generic.DataElementFileTypeDriver;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.ProtectedDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.server.Connection;
import ru.biosoft.server.Response;
import ru.biosoft.server.Service;
import ru.biosoft.server.ServiceSupport;
import ru.biosoft.server.ServiceSupport.ServiceRequest;
import ru.biosoft.util.ClassExtensionRegistry;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.FileItem;
import ru.biosoft.util.JsonUtils;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.TextUtil2;

/**
 * Provides functionality to access to all data collections.
 */
public class AccessService extends AccessProtocol implements Service
{
    protected Logger log = Logger.getLogger(AccessService.class.getName());

    private ServiceSupport ss;

    public AccessService()
    {
        ss = new ServiceSupport()
        {
            @Override
            protected boolean processRequest(ServiceRequest request, int command) throws Exception
            {
                return AccessService.this.processRequest(request, command);
            }
        };
    }

    @Override
    public void processRequest(Integer command, Map data, Response out)
    {
        ss.processRequest(command, data, out);
    }

    ////////////////////////////////////////
    // Functions for data access
    //
    /**
     * Checks validity of current data collection.
     */
    protected boolean isCurrentDataCollectionValid(ServiceRequest request) throws IOException
    {
        if( request.getDataCollection() != null )
            return true;

        request.error("no data collection is loaded as current.");
        return false;
    }

    protected boolean processRequest(ServiceRequest request, int command) throws Exception
    {
        switch( command )
        {
            case AccessProtocol.DB_CHECK_MUTABLE:
                sendCheckMutable(request);
                break;
            case AccessProtocol.DB_DESCRIPTION:
                sendDescription(request);
                break;
            case AccessProtocol.DB_LIST_ID:
                sendNameList(request);
                break;
            case AccessProtocol.DB_SIZE:
                sendSize(request);
                break;
            case AccessProtocol.DB_ENTRY_BY_ID:
                sendEntry(request);
                break;
            case AccessProtocol.DB_CONTAINS_ENTRY:
                sendContainsEntry(request);
                break;
            case AccessProtocol.DB_ENTRIES_SET_BY_ID:
                sendEntriesSet(request);
                break;
            case AccessProtocol.DB_WRITE_ENTRY:
                writeEntry(request);
                break;
            case AccessProtocol.DB_WRITE_ENTRIES_SET:
                writeEntriesSet(request);
                break;
            case AccessProtocol.DB_REMOVE_ENTRY:
                removeEntry(request);
                break;
            case AccessProtocol.DB_GET_DE_SET:
                sendSet(request);
                break;
            case AccessProtocol.DB_GET_INFO:
                sendDCInfo(request);
                break;
            case AccessProtocol.DB_GET_DESCRIPTOR:
                sendDescriptor(request);
                break;
            case AccessProtocol.DB_LOGIN:
                login(request);
                break;
            case AccessProtocol.DB_CHECK_PROTECTED:
                sendCheckProtected(request);
                break;
            case AccessProtocol.DB_GET_PERMISSIONS:
                sendPermissions(request);
                break;
            case AccessProtocol.DB_GENERIC_NEW_FOLDER:
                sendGenericNewFolder(request);
                break;
            case AccessProtocol.DB_REMOVE_ELEMENT:
                sendRemoveElement(request);
                break;
            case AccessProtocol.DB_GENERIC_RENAME_FOLDER:
                sendGenericRenameFolder(request);
                break;
            case AccessProtocol.DB_FLAGGED_LIST:
                sendFlaggedList(request);
                break;
            case AccessProtocol.DB_CHECK_PERMISSION:
                sendCheckPermission(request);
                break;
            case AccessProtocol.DB_CHECK_ACCEESSIBLE:
                sendCheckAccessible(request);
                break;
            case AccessProtocol.DB_GET_CLASS_HIERARCHY:
                sendClassHierarchy(request);
                break;
            case AccessProtocol.DB_EXPORT_DC:
                sendExportDataCollection(request);
                break;
            case AccessProtocol.DB_GET_AS_FILE:
                sendGetAsFile(request);
                break;
            case AccessProtocol.DB_PUT_AS_FILE:
                sendPutAsFile(request);
                break;
            case AccessProtocol.DB_PUT_AS_PROPERTIES:
                sendPutAsProperties(request);
                break;
            case AccessProtocol.DB_UPDATE_PROPERTIES:
                sendUpdateProperties(request);
                break;
            case AccessProtocol.DB_GET_TARGET:
                sendDBTarget( request );
                break;
            default:
                return false;
        }
        return true;
    }

    private final ClassExtensionRegistry<Object> commonClasses = new ClassExtensionRegistry<>("ru.biosoft.access.commonClass", Object.class);

    /**
     * Sends tree of superclasses and superinterfaces for given class name
     * @param request
     * @throws IOException
     * @throws JSONException
     */
    protected void sendClassHierarchy(ServiceRequest request) throws IOException, JSONException
    {
        String classNames = request.get(AccessProtocol.CLASS_NAME);
        String addCommonClassesObj = request.get(AccessProtocol.ADD_COMMON_CLASSES);
        boolean addCommonClasses = addCommonClassesObj != null && addCommonClassesObj.equals("yes");
        Map<Class<?>, Set<Class<?>>> classMap = new HashMap<>();
        Set<Class<?>> toAdd = new HashSet<>();
        try
        {
            if( !classNames.isEmpty() )
            {
                for( String className : TextUtil2.split( classNames, ',' ) )
                {
                    Class<?> clazz = ClassLoading.loadClass( className );
                    classMap.put( clazz, new HashSet<Class<?>>() );
                    toAdd.add( clazz );
                }
            }
        }
        catch( LoggedClassNotFoundException e )
        {
            request.error("Class not found");
            return;
        }
        if(addCommonClasses)
        {
            for(Class<?> commonClass: commonClasses)
            {
                classMap.put(commonClass, new HashSet<Class<?>>());
                toAdd.add(commonClass);
            }
        }
        while( !toAdd.isEmpty() )
        {
            Set<Class<?>> newToAdd = new HashSet<>();
            for( Class<?> clazz : toAdd )
            {
                Set<Class<?>> parentSet = classMap.get(clazz);
                if( clazz.getSuperclass() != null )
                    parentSet.add(clazz.getSuperclass());
                for( Class<?> interfaceClass : clazz.getInterfaces() )
                    parentSet.add(interfaceClass);
                for( Class<?> parentClass : parentSet )
                {
                    if( !classMap.containsKey(parentClass)
                            && ( addCommonClasses || commonClasses.getExtension(parentClass.getName()) == null ) )
                    {
                        newToAdd.add(parentClass);
                        classMap.put(parentClass, new HashSet<Class<?>>());
                    }
                }
            }
            toAdd = newToAdd;
        }
        JsonObject classTree = EntryStream.of( classMap )
                .mapKeys(Class::toString)
                .mapValues(val -> val.stream().map( Class::toString ).map( JsonValue::valueOf ).collect( JsonUtils.toArray() ))
                .collect( JsonUtils.toObject() );
        request.send(classTree.toString());
    }

    /**
     * Send list of children with additional flags
     * @param request
     * @throws Exception
     */
    protected void sendFlaggedList(ServiceRequest request) throws Exception
    {
        DataCollection<?> dc = request.getDataCollection();
        if( dc != null )
        {
            DataElementPath targetPath = null;
            if(dc instanceof SymbolicLinkDataCollection)
            {
                dc = ((SymbolicLinkDataCollection)dc).getPrimaryCollection();
                targetPath = dc.getCompletePath();
            }
            String childClassName = request.get(AccessProtocol.CHILD_CLASS_NAME);
            String elementClassName = request.get(AccessProtocol.ELEMENT_CLASS_NAME);
            String referenceTypeName = request.get(AccessProtocol.REFERENCE_TYPE_NAME);
            boolean extended = Boolean.parseBoolean(request.get(AccessProtocol.KEY_EXTENDED));
            Class<? extends DataElement> childClass = TextUtil2.isEmpty(childClassName) ? null
                    : (Class<? extends DataElement>)ClassLoading.loadClass( childClassName );
            Class<? extends DataElement> elementClass = TextUtil2.isEmpty(elementClassName) ? null
                    : (Class<? extends DataElement>)ClassLoading.loadClass( elementClassName );
            Class<? extends ReferenceType> referenceType = TextUtil2.isEmpty(referenceTypeName) ? null
                    : ReferenceTypeRegistry.getReferenceType(referenceTypeName).getClass();
            int from = request.getInt(AccessProtocol.KEY_FROM, -1);
            int to = request.getInt(AccessProtocol.KEY_TO, -1);
            String wantedName = request.get(AccessProtocol.KEY_NAME);
            int size = dc.getSize();
            List<String> list = null;
            if(wantedName != null)
            {
                if(!dc.contains(wantedName))    // cut out the case when element is not found
                {
                    from = 0;
                    to = 0;
                } else
                {
                    int chunkSize = request.getInt(AccessProtocol.KEY_CHUNK_SIZE, 100);
                    int foundPos = -1;
                    try
                    {
                        foundPos = dc.getNameList().indexOf(wantedName);
                    }
                    catch( Exception e )
                    {
                    }
                    if(foundPos == -1)
                    {
                        from = 0;
                        to = 0;
                        list = Arrays.asList(wantedName);
                    } else
                    {
                        from = foundPos/chunkSize*chunkSize;
                        to = from+chunkSize;
                    }
                }
            }
            if(to > size) to = size;
            JSONArray names = new JSONArray();
            if(list == null)
            {
                try
                {
                    list = dc.getNameList();
                    if(from >= 0 && to >= 0) list = list.subList(Math.min(from, list.size()), Math.min(to, list.size()));
                }
                catch( Exception e )
                {
                    from = 0;
                    to = 0;
                }
            }
            DataElementPath path = DataElementPath.create(dc);
            Map<String, Integer> iconMap = new HashMap<>();
            JSONArray icons = new JSONArray();
            Map<String, Integer> classMap = new HashMap<>();
            JSONArray classes = new JSONArray();
            if( list != null )
            {
                Index titleIndex = null;
                QuerySystem qs = dc.getInfo().getQuerySystem();
                if(qs != null)
                {
                    titleIndex = qs.getIndex("title");
                }
                
                for(String name: list)
                {
                    JSONObject item = new JSONObject();
                    DataElementPath childPath = path.getChildPath(name);
                    DataElementDescriptor descriptor = dc.getDescriptor(name);
                    item.put(PROPERTY_NAME, name);
                    if(titleIndex != null)
                    {
                        Object title = titleIndex.get(name);
                        if(title != null && !title.toString().equals(name))
                        {
                            item.put(PROPERTY_TITLE, title.toString());
                        }
                    }
                    if(childClass != null || elementClass != null)
                    {
                        boolean enabled = DataCollectionUtils.isAcceptable(childPath, childClass, elementClass, referenceType);
                        item.put(PROPERTY_ENABLED, enabled);
                    }
                    item.put(PROPERTY_HAS_CHILDREN, descriptor == null || !descriptor.isLeaf());
                    Class<? extends DataElement> elementType = descriptor == null ? dc.getDataElementType() : descriptor.getType();
                    if(elementType == null) elementType = ru.biosoft.access.core.DataElement.class;
                    String className = elementType.getName();
                    if(!classMap.containsKey(className))
                    {
                        classes.put(className);
                        classMap.put(className, classes.length()-1);
                    }
                    item.put(PROPERTY_CLASS, classMap.get(className));
                    String iconId = IconFactory.getIconId(childPath);
                    if(iconId != null)
                    {
                        if(!iconMap.containsKey(iconId))
                        {
                            icons.put(iconId);
                            iconMap.put(iconId, icons.length()-1);
                        }
                        item.put(PROPERTY_ICON, iconMap.get(iconId));
                    }
                    int protection = DataCollectionUtils.getProtectionStatus(childPath);
                    if( protection != ProtectedDataCollection.PROTECTION_NOT_APPLICABLE )
                    {
                        item.put(PROPERTY_PROTECTION, protection);
                        item.put(PROPERTY_PERMISSION, SecurityManager.getPermissions(childPath).getPermissions());
                    }
                    if(extended)
                    {
                        String importType = getImportType(childPath);
                        if( importType != null )
                        {
                            item.put(PROPERTY_IMPORT_TYPE, importType);
                        }
                        if(canExportToFile(elementType))
                        {
                            item.put(PROPERTY_FILE, true);
                        }
                        if(descriptor != null)
                        {
                            ReferenceType refType = ReferenceTypeRegistry
                                    .optReferenceType( descriptor.getValue( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY ) );
                            if(refType != null)
                                item.put(PROPERTY_REFERENCE_TYPE, refType.toString());
                        }
                    }
                    names.put(item);
                }
            }
            JSONObject result = new JSONObject();
            result.put(PROPERTY_NAMES, names);
            result.put(PROPERTY_SIZE, size);
            if(from >= 0 && to >= 0)
            {
                result.put(PROPERTY_FROM, from);
                result.put(PROPERTY_TO, to);
            } else
            {
                result.put(PROPERTY_FROM, 0);
                result.put(PROPERTY_TO, size);
            }
            if(targetPath != null) result.put(PROPERTY_TARGET, targetPath.toString());
            if(icons.length()>0) result.put(PROPERTY_ICONS, icons);
            if(classes.length()>0) result.put(PROPERTY_CLASSES, classes);
            result.put(PROPERTY_ENABLED, DataCollectionUtils.isAcceptable(path, elementClass, null));
            request.send(result.toString());
        }
    }

    /**
     * Get import type for element
     * TODO: exclude not module collections and change body
     */
    protected static String getImportType(DataElementPath path)
    {
        if( path.isDescendantOf(DataElementPath.create("databases")) )
        {
            if(!path.getParentPath().getParentPath().isEmpty()) return null;
            DataCollection<? extends DataElement> dc = path.optDataCollection();
            if(dc != null && "disabled".equals(dc.getInfo().getProperty("remoteAccess")))
                return null;
            return IMPORT_TYPE_LINK;
        }
        else if( path.isDescendantOf(DataElementPath.create("data")) )
        {
            return IMPORT_TYPE_COPY;
        }
        return null;
    }

    //////////////////////////////////////////////
    // Protocol implementation functions
    //

    /**
     * Check if this data collection is mutable
     * @param request
     */
    protected void sendCheckMutable(ServiceRequest request) throws Exception
    {
        DataCollection<?> dc = request.getDataCollection();
        if( dc != null )
        {
            request.send(dc.isMutable()?"true":"false");
        }
    }

    /**
     * Sends the current data collection description.
     * @param request
     */
    protected void sendDescription(ServiceRequest request) throws IOException
    {
        DataCollection<?> dc = request.getDataCollection();
        if( dc != null )
        {
            StringBuffer description = new StringBuffer();

            description.append("Database: ");
            description.append(dc.getName());

            String version = dc.getInfo().getProperty(DB_VERSION);
            if( version != null )
            {
                description.append("\n");
                description.append("Version: ");
                description.append(version);
            }

            String update = dc.getInfo().getProperty(UPDATE);
            if( update != null )
            {
                description.append("\n");
                description.append("Upadate: ");
                description.append(update);
            }

            String protectionStatus = getProtectionStatus(dc.getInfo().getProperty(ProtectedDataCollection.PROTECTION_STATUS));
            description.append("\n");
            description.append("Availability: ");
            description.append(protectionStatus);

            String dependency = dc.getInfo().getProperty(DEPENDENCIES);
            if( dependency != null )
            {
                description.append("\n");
                description.append("Dependency: ");
                description.append(dependency);
            }

            String descr = dc.getInfo().getDescription();
            if( descr != null )
            {
                description.append("\n");
                description.append("Description: ");
                description.append(descr);
            }

            String showStatistics = dc.getInfo().getProperty(SHOW_STATISTICS);
            if( showStatistics != null && Boolean.parseBoolean(showStatistics) )
            {
                description.append("\n");
                description.append("Statistics:\n");
                printStatistics(description, dc, "\t");
            }

            request.send(description.toString());
        }
    }

    public static String getProtectionStatus(String statusNumber)
    {
        if( statusNumber == null )
        {
            return "public";
        }

        int sn = Integer.parseInt(statusNumber);
        if( sn == 1 )
        {
            return "public, read only";
        }
        else if( sn == 2 )
        {
            return "public read, protected write";
        }
        else if( sn == 3 )
        {
            return "protected, read only";
        }
        else if( sn == 4 )
        {
            return "protected, read/write";
        }
        else
        {
            return "unknown";
        }
    }

    public static void printStatistics(StringBuffer buffer, DataCollection<?> dc, String tab)
    {
        for(DataElement de: dc)
        {
            if( de instanceof DataCollection )
            {
                DataCollection<?> collection = (DataCollection<?>)de;
                if( collection.getName().equals("Data") || collection.getName().equals("Dictionaries") )
                {
                    buffer.append(tab);
                    buffer.append(collection.getName());
                    buffer.append(":\n");
                    printStatistics(buffer, collection, tab + "\t");
                }
                else
                {
                    buffer.append(tab);
                    buffer.append(collection.getName());
                    buffer.append(": ");
                    buffer.append(collection.getSize());
                    buffer.append("\n");
                }
            }
        }
    }

    /**
     * Sends the list of identificators or accession number for all entries
     * in current data collection.
     * @param request
     */
    protected void sendNameList(ServiceRequest request) throws IOException
    {
        DataCollection<?> dc = request.getDataCollection();
        if( dc != null )
        {
            if( dc.getSize() > 0 )
            {
                int from = request.getInt(AccessProtocol.KEY_FROM, -1);
                int to = request.getInt(AccessProtocol.KEY_TO, -1);
                List<String> list = dc.getNameList();
                if(from >= 0 && to >= 0) list = list.subList(Math.min(from, list.size()), Math.min(to, list.size()));
                StringBuffer s = new StringBuffer();
                for(String name: list)
                {
                    s.append(name).append('\n');
                }
                request.send(s.toString());
                return;
            }
            request.send("");
        }
    }
    /**
     * Sends number of elements in data collection.
     * @param request
     */
    protected void sendSize(ServiceRequest request) throws IOException
    {
        DataCollection<?> dc = request.getDataCollection();
        if( dc != null )
        {
            int size = dc.getSize();
            request.send(String.valueOf(size));
        }
    }

    /**
     * Checks whether database contains entry with the specified ID.
     * @param request
     */
    protected void sendContainsEntry(ServiceRequest request) throws Exception
    {
        DataElementPath element = request.getDataElementPath();
        if( element != null )
        {
            request.send(element.exists()?"true":"false");
        }
    }

    /**
     * Sends the required entry by its identificator or accession number.
     * @param request
     */
    protected void sendEntry(ServiceRequest request) throws Exception
    {
        DataElementPath path = request.getDataElementPath();
        if( path != null )
        {
            DataElement de = path.getDataElement();
            request.send("" + convertToString(de));
        }
    }

    private void sendPutAsProperties(ServiceRequest request) throws Exception
    {
        DataElementPath path = request.getDataElementPath();
        if(path != null)
        {
            String propertiesString = request.get(KEY_PROPERTIES);
            ExProperties properties = new ExProperties();
            properties.load(new StringReader(propertiesString));
            properties.remove(DataCollectionConfigConstants.CONFIG_FILE_PROPERTY);
            properties.remove(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY);
            properties.remove(DataCollectionConfigConstants.FILE_PATH_PROPERTY);
            properties.remove(DataCollectionConfigConstants.FILE_PROPERTY);
            properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, path.getName());
            DataCollection<?> dc = CollectionFactory.createCollection( path.getParentCollection(), properties );
            CollectionFactoryUtils.save(dc);
            request.send("");
        }
    }

    private void sendUpdateProperties(ServiceRequest request) throws Exception
    {
        DataElementPath path = request.getDataElementPath();
        if(path != null)
        {
            DataCollection<DataElement> dc = path.getDataCollection();
            String propertiesString = request.get(KEY_PROPERTIES);
            ExProperties properties = new ExProperties();
            properties.load(new StringReader(propertiesString));
            properties.remove(DataCollectionConfigConstants.CONFIG_FILE_PROPERTY);
            properties.remove(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY);
            properties.remove(DataCollectionConfigConstants.FILE_PATH_PROPERTY);
            properties.remove(DataCollectionConfigConstants.FILE_PROPERTY);
            properties.remove(DataCollectionConfigConstants.NAME_PROPERTY);
            properties.remove(DataCollectionConfigConstants.CLASS_PROPERTY);
            properties.remove(DataCollectionConfigConstants.PLUGINS_PROPERTY);
            boolean changed = false;
            for(Object key : properties.keySet())
            {
                String value = dc.getInfo().getProperty(key.toString());
                String newValue = properties.getProperty(key.toString());
                if(!Objects.equals( value, newValue ))
                {
                    dc.getInfo().getProperties().setProperty(key.toString(), newValue);
                    changed = true;
                }
            }
            if(changed)
                CollectionFactoryUtils.save(dc);
            request.send("");
        }
    }

    private void sendPutAsFile(ServiceRequest request) throws Exception
    {
        DataElementPath path = request.getDataElementPath();
        if( path != null )
        {
            DataCollection dc = path.getParentCollection();
            byte[] content = Base64.decodeBase64(request.get(KEY_BASE64_CONTENT));
            String transformerName = request.get(KEY_TRANSFORMER_NAME);
            DataElement de = loadFromStream(path, transformerName, new ByteArrayInputStream(content));
            dc.put(de);
            request.send("");
        }
    }
    
    /**
     * Sends ru.biosoft.access.core.DataElement as File.
     * The format is following:
     * - UTF string - transformer, can be empty if it is FileDataElement
     * - the file content
     * 
     * @param request
     * @throws Exception if file size is bigger 10 MB;
     */
    private void sendGetAsFile(ServiceRequest request) throws Exception
    {
        DataElementPath path = request.getDataElementPath();
        if( path != null )
        {
            DataElement de = path.getDataElement();
            FileItem fileItem = null;
            String transformerClass = null;
            boolean deleteFile = true;
            
            try
            {
            	// try to get original file and transformer for GenericDataCollection
            	DataElementDescriptor descriptor = de.getOrigin().getDescriptor(de.getName());
            	if( descriptor != null && 
            			descriptor.getValue(DataCollectionConfigConstants.FILE_PATH_PROPERTY) != null && 
            			descriptor.getValue(DataElementFileTypeDriver.TRANSFORMER_CLASS) != null )
            	{
            		File file = new File(descriptor.getValue(DataCollectionConfigConstants.FILE_PATH_PROPERTY));
            		transformerClass = descriptor.getValue(DataElementFileTypeDriver.TRANSFORMER_CLASS);
            		deleteFile = false;
            		fileItem = new FileItem(file);
            	}
            	
            	else
            	{
                    // see saveAsFile method, it is durty hack
            		fileItem = saveAsFile(de);

                    // here we make it more obvious
            		transformerClass = fileItem.getOriginalName();
            		deleteFile = !fileItem.getOriginalName().isEmpty(); 
            	}
            
                if(fileItem.length() > 10L*1024*1024*1024)
                    throw new Exception("Element is too big to transfer");

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);

                dos.writeUTF(transformerClass);
                dos.flush();
                ApplicationUtils.copyStream(baos, new FileInputStream(fileItem));
                
                request.sendBytes(baos.toByteArray());
            }
            finally
            {
                if(deleteFile && fileItem != null)
                    fileItem.delete();
            }
        }
    }
    
    /**
     * Sends the set required entry by their identificators.
     * @param request
     */
    protected void sendEntriesSet(ServiceRequest request) throws Exception
    {
        DataCollection<?> dc = request.getDataCollection();
        if( dc != null )
        {
            String ids = request.get(AccessProtocol.KEY_IDS);
            if( ids != null )
            {
                StringTokenizer tokens = new StringTokenizer(ids, "\r\n");
                StringBuffer answer = new StringBuffer();

                while( tokens.hasMoreTokens() )
                {
                    String id = tokens.nextToken();
                    String entry = convertToString(dc.get(id));
                    if( entry != null && entry.trim().length() > 0 )
                    {
                        answer.append(entry);
                    }
                }

                request.send( answer.toString() );
                return;
            }
            request.error("didn't send data element names");
            return;
        }
    }

    /**
     * Writes the entry to the data collection.
     * @param request
     */
    protected void writeEntry(ServiceRequest request) throws Exception
    {
        DataElementPath path = request.getDataElementPath();
        if( path != null )
        {
            String entry = request.get(AccessProtocol.KEY_ENTRY);
            if( entry != null )
            {
                DataElement de = convertToDataElement(path.optParentCollection(), path.getName(), entry);
                path.save(de);
                request.send(null);
                return;
            }
            request.error("didn't send data element entry");
            return;
        }
    }

    /**
     * Writes the entries set to the data collection.
     * @param request
     */
    protected void writeEntriesSet(ServiceRequest request) throws Exception
    {
        DataCollection<?> dc = request.getDataCollection();
        if( dc != null )
        {
            String entry = request.get(AccessProtocol.KEY_ENTRY);
            if( entry != null )
            {
                int start = 0;
                int end;
                while( ( end = entry.indexOf("\n//", start) ) != -1 )
                {
                    convertToDataElement(dc, "???", entry.substring(start, end + 3));
                    start = end + 4;
                }
                request.send(null);
                return;
            }
            request.error("didn't send data element entry set");
            return;
        }
    }

    /**
     * Removes the entry from the data collection.
     * @param request
     */
    protected void removeEntry(ServiceRequest request) throws Exception
    {
        DataElementPath path = request.getDataElementPath();
        if( path != null )
        {
            path.getParentCollection().release(path.getName());
            path.remove();
            request.send(null);
        }
    }

    /**
     * Special internal class for data exchange
     */
    public static class Entry implements Serializable
    {
        public Entry(String deName, String dcName, String entry)
        {
            this.deName = deName;
            this.dcName = dcName;
            this.entry = entry;
        }
        public String dcName;
        public String deName;
        public String entry;
    }

    protected void sendSet(ServiceRequest request) throws Exception
    {
        DataCollection<?> dc = request.getDataCollection();
        if( dc != null )
        {
            String kernelNames = request.get(AccessProtocol.KEY_DEs);
            if( kernelNames == null )
            {
                request.error("didn't send data element names");
                return;
            }

            ArrayList<Entry> answer = new ArrayList<>();
            StringTokenizer tokenizer = new StringTokenizer(kernelNames, "\n\r");
            while( tokenizer.hasMoreTokens() )
            {
                String name = tokenizer.nextToken();
                DataElement de = null;
                if( name.startsWith("databases/") )
                {
                    de = CollectionFactory.getDataElement(name);
                }
                else
                {
                    de = CollectionFactory.getDataElement(name, dc);
                }
                if( de != null )
                {
                    String dcName = "";
                    if( !de.getOrigin().getCompletePath().equals(dc.getCompletePath()) )
                        dcName = CollectionFactory.getRelativeName(de.getOrigin(), dc);
                    try
                    {
                        Entry entry = new Entry(de.getName(), dcName, convertToString(de));
                        answer.add(entry);
                    }
                    catch( Throwable t )
                    {
                        //preload has some bugs
                        log.log(Level.SEVERE, t.getMessage(), t);
                    }
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            answer.trimToSize();
            oos.writeObject(answer);
            oos.flush();
            request.getSessionConnection().send(baos.toByteArray(), Connection.FORMAT_SIMPLE);
        }
    }

    protected void sendDCInfo(ServiceRequest request) throws Exception
    {
        DataCollection<?> dc = request.getDataCollection();
        if( dc != null )
        {
            String deName = request.get(AccessProtocol.KEY_DE);
            DataElement targetDE = null;
            if( deName == null )
            {
                targetDE = dc;
            }
            else
            {
                DataElement de = dc.get(deName);
                if(de != null) targetDE = de;
            }

            if( targetDE == null )
            {
                request.send("null");
            }
            else
            {
                Properties properties;
                if(targetDE instanceof DataCollection)
                {
                    Properties serverProperties = null;
                    DataCollection<?> targetDC = (DataCollection<?>)targetDE;
                    if( targetDC.getInfo() != null )
                    {
                        serverProperties = targetDC.getInfo().getProperties();
                    }

                    properties = PropertiesResolver.getClientProperties(targetDC, serverProperties);
                } else
                {
                    properties = new Properties();
                    DataElementDescriptor descriptor = targetDE.getOrigin().getDescriptor(targetDE.getName());
                    String value = descriptor.getValue("SequencesCollection");
                    if(value != null) properties.setProperty("SequencesCollection", value);
                }

                StringWriter sw = new StringWriter();
                properties.store(sw, "");
                request.send( "" + sw.getBuffer().toString());
            }
        }
    }

    /**
     * Sends {@link DataElementDescriptor} for the specified data collection and data element name as serialized in text properties.
     */
    protected void sendDescriptor(ServiceRequest request) throws Exception
    {
        DataCollection<?> dc = request.getDataCollection();
        if( dc != null )
        {
            String deName = request.get(AccessProtocol.KEY_DE);
            if( deName != null  )
            {
            	DataElementDescriptor descriptor = dc.getDescriptor(deName);
            	if( descriptor != null )
            	{
                    Properties properties = new Properties();

                    Map<String, String> clonedProperties = descriptor.cloneProperties();
            		for(java.util.Iterator<String> it = clonedProperties.keySet().iterator(); it.hasNext();)
            		{
            			String key = it.next();
            			properties.put(key, clonedProperties.get(key)); 
            		}
            		
            		properties.put(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, descriptor.getType().getName());
            		properties.put(DataCollectionConfigConstants.IS_LEAF, 				    ""+descriptor.isLeaf());

                    StringWriter sw = new StringWriter();
                    properties.store(sw, "");
                    request.send( "" + sw.getBuffer().toString());
                    return;
            	}
            }
        }

        request.send("null");
    }
    
    /**
     * login to data collection
     * @param request
     */
    protected void login(ServiceRequest request) throws Exception
    {
        String username = request.get(SecurityManager.USERNAME);
        String password = request.get(SecurityManager.PASSWORD);
        if( username != null && password != null )
        {
            String dcName = request.get(Connection.KEY_DC);
            //common login
            Permission permission = SecurityManager.commonLogin( username, password, null, null );
            if( ( dcName != null ) && ( dcName.length() > 0 ) )
            {
                DataElementPath path = DataElementPath.create(dcName);
                if(path.getName().isEmpty())
                {
                    path = path.getParentPath();
                }
                permission = SecurityManager.getPermissions(path);
            }
            if( permission != null )
            {
                request.send(permission.toString());
            }
            else
            {
                request.send("null");
            }
            return;
        }
        request.error("can not login to server");
        return;
    }

    /**
     * Sends permissions for the specifies data collection.
     */
    protected void sendPermissions(ServiceRequest request) throws Exception
    {
   		String dcName = request.get(Connection.KEY_DC);
   		Permission permission = SecurityManager.getPermissions(dcName);

        if( permission != null )
            request.send(permission.toString());
        else
        	request.send("null");
    }
    
    /**
     * Send protection status for data collection
     * 
     *-1 - not applicable
     * 0 - not protected
     * 1 - public, read only
     * 2 - public read, protected write
     * 3 - protected, read only
     * 4 - protected, read/write
     * @param request
     * 
     */
    protected void sendCheckProtected(ServiceRequest request) throws Exception
    {
        DataCollection<?> dc = request.getDataCollection();
        byte status = dc == null ? 0 : (byte)DataCollectionUtils.getProtectionStatus(DataElementPath.create(dc));
        request.getSessionConnection().send(new byte[] {status}, Connection.FORMAT_SIMPLE);
    }

    protected void sendCheckAccessible(ServiceRequest request) throws Exception
    {
        String deName = request.get(AccessProtocol.KEY_DE);
        if( deName != null )
        {
            DataElementPath path = DataElementPath.create(deName);
            DataElement de = path.optDataElement();
            if( de != null )
            {
                Permission permission = SecurityManager.getPermissions(path);
                if( ( permission.getPermissions() & Permission.READ ) != 0 )
                {
                    request.getSessionConnection().send(new byte[] {1}, Connection.FORMAT_SIMPLE);
                    return;
                }
            }
        }
        request.getSessionConnection().send(new byte[] {0}, Connection.FORMAT_SIMPLE);
    }

    /**
     * Send permission info for data collection
     * 
     * 0 - available
     * 1 - not available
     * @param request
     * 
     */
    protected void sendCheckPermission(ServiceRequest request) throws Exception
    {
        DataCollection<?> dc = request.getDataCollection();
        if( dc != null )
        {
            if( dc.getClass().getName().equals("biouml.model.ProtectedModule") || dc.getClass().getName().equals("biouml.model.Module")
                    || dc.getClass().getName().equals("biouml.plugins.server.SqlModule") )
            {
                String protectionStatus = dc.getInfo().getProperty(ProtectedDataCollection.PROTECTION_STATUS);
                int permission = ( SecurityManager.getPermissions(dc.getCompletePath()) ).getPermissions();
                if( protectionStatus != null )
                {
                    String permStatus = "0";
                    if( protectionStatus.equals("2") )
                    {
                        if( ( permission & Permission.WRITE ) != 0 )
                        {
                            permStatus = "0";
                        }
                        else
                        {
                            permStatus = "1";
                        }
                    }
                    else if( protectionStatus.equals("3") )
                    {
                        if( ( permission & Permission.READ ) != 0 )
                        {
                            permStatus = "0";
                        }
                        else
                        {
                            permStatus = "1";
                        }
                    }
                    else if( protectionStatus.equals("4") )
                    {
                        if( ( permission & Permission.WRITE ) != 0 )
                        {
                            permStatus = "0";
                        }
                        else
                        {
                            permStatus = "1";
                        }
                    }
                    byte perm = Byte.parseByte(permStatus);
                    request.getSessionConnection().send(new byte[] {perm}, Connection.FORMAT_SIMPLE);
                }
            }
        }
    }

    /**
     * Create new folder in GenericDataCollection
     * @param request
     */
    protected void sendGenericNewFolder(ServiceRequest request) throws Exception
    {
        DataCollection<?> dc = request.getDataCollection();
        if( dc != null )
        {
            String de = request.get(AccessProtocol.KEY_DE);
            if( de != null ) de = de.trim();
            if( de != null && de.length() > 0 )
            {
                try
                {
                    String className = request.get(AccessProtocol.CLASS_NAME);
                    Class<? extends FolderCollection> clazz = className == null ? FolderCollection.class : ClassLoading.loadSubClass(
                            className, FolderCollection.class );
                    DataCollectionUtils.createSubCollection( DataElementPath.create( dc, de ),
                            DataCollectionUtils.CreateStrategy.REMOVE_WRONG_TYPE, clazz );
                    request.send("ok");
                }
                catch( Exception ex )
                {
                    if( !(ex instanceof IllegalArgumentException) )
                        log.log(Level.SEVERE, "While creating folder", ex);
                    request.error(ex.getMessage());
                }
            }
            else
            {
                request.error("Invalid input");
            }
        }
    }

    /**
     * Remove element
     * @param request
     */
    protected void sendRemoveElement(ServiceRequest request) throws Exception
    {
        DataCollection<?> dc = request.getDataCollection();
        String de = request.get(AccessProtocol.KEY_DE);
        if( dc == null || de == null || de.isEmpty() )
        {
            request.error("Invalid input");
            return;
        }
        try
        {
            dc.remove(de);
            request.send("ok");
        }
        catch( Exception ex )
        {
            request.error(ex.getMessage());
        }
    }

    /**
     * Rename folder in GenericDataCollection
     * @param request
     */
    protected void sendGenericRenameFolder(ServiceRequest request) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    protected void sendExportDataCollection(ServiceRequest request) throws Exception
    {
        DataCollection<?> dc = request.getDataCollection();
        if( dc != null && ( dc instanceof Module || dc instanceof LocalRepository ) )
        {
            try (TempFile exportFile = TempFiles.file( ModulePackager.BMD_FILE_EXTENTION );
                    DataOutputStream dos = new DataOutputStream( request.getSessionConnection().getOutputStream() );
                    BufferedInputStream bis = new BufferedInputStream( new FileInputStream( exportFile ) ))
            {
                String newName = request.get(AccessProtocol.KEY_DE);
                if( newName == null ) newName = dc.getName();
                ModulePackager.exportModule(dc, newName, null, null, exportFile.getAbsolutePath(), null, null);

                dos.writeInt(Connection.OK);
                dos.writeInt(Connection.FORMAT_SIMPLE);
                dos.writeInt(-1);//indicates extra length in LONG format
                dos.writeLong(exportFile.length());
                byte[] buffer = new byte[4096];
                while( true )
                {
                    int bytes = bis.read(buffer);
                    if( bytes <= 0 )
                        break;

                    dos.write(buffer, 0, bytes);
                }
            }
            catch( Exception e )
            {
                request.error(e.getMessage());
            }
        }
        else
        {
            request.error("No data collection specified");
        }
    }

    private void sendDBTarget(ServiceRequest request) throws IOException
    {
        DataCollection<?> dc = request.getDataCollection();
        if( dc != null )
        {
            DataElementPath targetPath = null;
            if( dc instanceof SymbolicLinkDataCollection )
            {
                dc = ( (SymbolicLinkDataCollection)dc ).getPrimaryCollection();
                targetPath = dc.getCompletePath();
                request.send( targetPath.toString() );
            }
            else
                request.send( "" );
        }
        else
            request.error( "No data collection specified" );
    }

}

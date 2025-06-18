package ru.biosoft.plugins.javascript.host;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.CompositeProperty;
import com.developmentontheedge.beans.model.Property;

import biouml.plugins.server.access.ClientDataCollection;
import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.DataElementExporterRegistry.ExporterInfo;
import ru.biosoft.access.DataElementImporterRegistry;
import ru.biosoft.access.DataElementImporterRegistry.ImporterInfo;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.Assert;
import ru.biosoft.access.file.FileBasedCollection;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;
import ru.biosoft.plugins.javascript.Global;
import ru.biosoft.plugins.javascript.JSObjectConverter;
import ru.biosoft.plugins.javascript.JScriptContext;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;
import ru.biosoft.server.ClientConnection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnGroup;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TextUtil2;

public class JavaScriptData extends JavaScriptHostObjectBase
{
    protected static final Logger log = Logger.getLogger(JavaScriptData.class.getName());

    public JavaScriptData()
    {
    }

    public boolean contains(String path)
    {
        beforeFunctionCall();

        return DataElementPath.create(path).exists();
    }

    public boolean contains2(DataCollection dc, String path)
    {
        beforeFunctionCall();

        if( dc == null )
        {
            return contains(path);
        }

        return null != CollectionFactory.getDataElement(path, dc);
    }

    public DataElement get(String path)
    {
        beforeFunctionCall();

        return CollectionFactory.getDataElement(path);
    }

    public DataElement get(String path, String className)
    {
        Assert.notNull( "path", path );
        Assert.notNull( "className", className );
        return DataElementPath.create( path ).getDataElement( ClassLoading.loadSubClass( className, DataElement.class ) );
    }

    public DataElement get2(DataCollection dc, String path)
    {
        beforeFunctionCall();

        return CollectionFactory.getDataElement(path, dc);
    }

    public FileDataElement createFile(FileBasedCollection dc, String fname)
    {
        FileDataElement de = new FileDataElement(fname, dc);
        try
        {
            dc.put(de);
        }
        catch( Exception e )
        {
            setLastError(e);
            return null;
        }
        return de;
    }

    /**
     * Debug method to read bean properties information for given bean
     * @param bean - bean to read
     * @return array of entries. Each entry correspond to single bean property and has 4 Strings: name (full path), class name, display name and description
     */
    public String[][] getObjectProperties(Object bean)
    {
        class PropertyInfo
        {
            Property property;
            String prefix;
            int pos = 0;

            public PropertyInfo(Property property, String prefix)
            {
                super();
                this.property = property;
                this.prefix = prefix;
            }
            
            public PropertyInfo next()
            {
                if(!(property instanceof CompositeProperty)) return null;
                Property subProperty = null;
                while(pos < property.getPropertyCount() && (subProperty == null || subProperty.getDescriptor().isHidden()))
                {
                    subProperty = property.getPropertyAt(pos++);
                }
                return subProperty == null || subProperty.getDescriptor().isHidden()?null:new PropertyInfo(subProperty, prefix+property.getName()+"/");
            }
        }
        class PropertyData
        {
            String name;
            String className;
            String displayName;
            String description;

            public PropertyData(String name, String className, String displayName, String description)
            {
                super();
                this.name = name;
                this.className = className;
                this.displayName = displayName;
                this.description = description;
            }
            
            public String[] toArray()
            {
                return new String[] {name, className, displayName, description};
            }
        }
        List<PropertyData> resultList = new ArrayList<>();
        Stack<PropertyInfo> properties = new Stack<>();
        properties.push(new PropertyInfo(ComponentFactory.getModel(bean), ""));
        while(!properties.empty())
        {
            PropertyInfo propertyInfo = properties.peek();
            PropertyInfo subPropertyInfo = propertyInfo.next();
            if(subPropertyInfo == null)
            {
                properties.pop();
                continue;
            }
            properties.push(subPropertyInfo);
            try
            {
                resultList.add(new PropertyData(subPropertyInfo.prefix+subPropertyInfo.property.getName(), subPropertyInfo.property
                        .getValueClass().getName(), subPropertyInfo.property.getDisplayName(), subPropertyInfo.property.getShortDescription()));
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "While getting property data for "+subPropertyInfo.prefix, e);
            }
        }
        return StreamEx.of(resultList).map( PropertyData::toArray ).toArray( String[][]::new );
    }

    public DataElement importFileDataElement(DataCollection dc, FileDataElement de, String name, String format) throws Exception
    {
        return this.importFileInternal(dc, de.getFile(), name, format);
    }

    public DataElement importFileDataElement(DataCollection dc, FileDataElement de, String name) throws Exception
    {
        return importFileInternal(dc, de.getFile(), name, null);
    }

    public DataElement importFile(String filePath, Object parent, String format) throws Exception
    {
        return importFile( filePath, parent, format, new NativeObject() );
    }
    
    public DataElement importFile(String filePath, Object parent, String format, ScriptableObject params) throws Exception
    {
        File file = new File(filePath);
        
        DataCollection<?> parentDC;
        if(parent == null)
            throw new NullPointerException("parent is null");
        if(parent instanceof DataCollection)
            parentDC = (DataCollection<?>)parent;
        else if (parent instanceof String)
            parentDC = DataElementPath.create( (String)parent ).getDataCollection();
        else if (parent instanceof DataElementPath)
            parentDC = ( (DataElementPath)parent ).getDataCollection();
        else
            throw new IllegalArgumentException("Unknown type of parent");
        
        Object nameObj = params.get( "name" );
        if(nameObj == null)
            nameObj = file.getName();
        String name = nameObj.toString();
        
        
        return importFileInternal( parentDC, file, name, format );
    }
    
    private DataElement importFileInternal(DataCollection<?> dc, File file, String name, String format) throws Exception
    {
        DataElementImporter importer = null;
        if( format == null ) // autodetect format
        {
            ImporterInfo[] info = DataElementImporterRegistry.getAutoDetectImporter(file, dc, false);
            if( info != null && info.length == 1 )
                importer = info[0].getImporter();
        }
        else
        {
            importer = DataElementImporterRegistry.getImporter(file, format, dc);
        }
        if( importer == null )
        {
            setLastError(new Exception("Cannot import"));
            return null;
        }
        return importer.doImport(dc, file, name, null, log);
    }
    
    public void export(Object dataElement, String filePath, String format) throws Exception
    {
        export( dataElement, filePath, format, new NativeObject() );
    }
    public void export(Object dataElement, String filePath, String format, ScriptableObject params) throws Exception
    {
        DataElement de;
        if(dataElement == null)
            throw new NullPointerException("dataElement is null");
        if(dataElement instanceof DataElement)
            de = (DataElement)dataElement;
        else if(dataElement instanceof DataElementPath)
            de = ( (DataElementPath)dataElement ).getDataElement();
        else if(dataElement instanceof String)
            de = DataElementPath.create( (String)dataElement ).getDataElement();
        else
            throw new IllegalArgumentException("Unknown type of dataElement");
        
        File file = new File(filePath);
        
        ExporterInfo exporterInfo = DataElementExporterRegistry.getExporterInfoByName( format );
        DataElementExporter exporter = exporterInfo.cloneExporter();
        if(exporter == null)
            throw new IllegalArgumentException("Unknown format: " + format);
        
        Object properties = exporter.getProperties( de, file );
        //TODO: change properties
        
        exporter.doExport( de, file );
    }

    public boolean put(DataCollection dc, String path, Object de4Put)
    {
        beforeFunctionCall();

        DataCollection dc4Modify;

        if( path.length() > 0 )
        {
            DataElement de = CollectionFactory.getDataElement(path, dc);
            if( ! ( de instanceof DataCollection ) )
                return false;

            dc4Modify = (DataCollection)de;
        }
        else
        {
            dc4Modify = dc;
        }

        try
        {
            if( de4Put instanceof DataElement )
            {
                dc4Modify.put((DataElement)de4Put);
            }
            else
            {
                for( JSObjectConverter converter : JScriptContext.getConverters() )
                {
                    if( converter.canConvert(de4Put) )
                    {
                        DataElement de = converter.convertToDataElement(dc4Modify, de4Put);
                        if( de != null )
                        {
                            dc4Modify.put(de);
                        }
                        break;
                    }
                }
            }
        }
        catch( Throwable e )
        {
            setLastError(e);
            return false;
        }

        return true;
    }

    public boolean remove(DataCollection dc, String pathStr)
    {
        beforeFunctionCall();

        DataElementPath path = DataElementPath.create(pathStr);
        if(!path.exists())
            return false;
        try
        {
            path.remove();
        }
        catch( Throwable e )
        {
            setLastError(e);
            return false;
        }

        return true;
    }

    public Object value(Object source, String element, String column)
    {
        beforeFunctionCall();

        DataCollection dc = resolveDataCollection(source);
        if( dc == null )
        {
            setLastError(new Throwable("Cannot resolve source to ru.biosoft.access.core.DataCollection"));
            return null;
        }
        try
        {
            DataElement rowDE = dc.get(element);
            return ComponentFactory.getModel(rowDE, Policy.DEFAULT, true).findProperty(column).getValue();
        }
        catch( Throwable e )
        {
            setLastError(e);
            return null;
        }
    }

    public Scriptable columnValues(Object source, String column)
    {
        DataCollection<?> dc = resolveDataCollection(source);
        if( dc == null )
        {
            setLastError(new Throwable("Cannot resolve source to ru.biosoft.access.core.DataCollection"));
            return null;
        }

        List<Object> objectsAL = new ArrayList<>();

        try
        {
            for( ru.biosoft.access.core.DataElement de : dc )
            {
                if( TextUtil2.isEmpty(column) )
                {
                    objectsAL.add(de.getName());
                }
                else
                {
                    Object val = ComponentFactory.getModel(de, Policy.DEFAULT, true).findProperty(column).getValue();
                    objectsAL.add(val);
                }
            }
        }
        catch( Throwable e )
        {
            setLastError(e);
            return null;
        }

        return getCurrentContext().newArray(scope, objectsAL.toArray());
    }

    public Scriptable columnNames(Object source)
    {
        DataCollection<?> dc = resolveDataCollection(source);
        if( dc == null )
        {
            setLastError(new Throwable("Cannot resolve source to ru.biosoft.access.core.DataCollection"));
            return null;
        }

        Object[] columnNameObjs = dc.stream().findAny()
                .map( de -> BeanUtil.properties( de ).map( Property::getName ).without( "name" ).toArray() ).orElse( new Object[0] );

        return getCurrentContext().newArray(scope, columnNameObjs);
    }

    public void recalc(DataElement rowDE, String column, String expression)
    {
        beforeFunctionCall();

        Script script;

        if( !scriptText2CompiledScript.containsKey(expression) )
        {
            try
            {
                script = getCurrentContext().compileString(expression, "", 1, null);
                scriptText2CompiledScript.put(expression, script);
            }
            catch( Throwable ex )
            {
                setLastError(ex);
                return;
            }
        }
        else
        {
            script = scriptText2CompiledScript.get(expression);
        }

        try
        {
            defineColumnVariables(rowDE);
            Object result = script.exec(getCurrentContext(), scope);
            ComponentFactory.getModel(rowDE, Policy.DEFAULT, true).findProperty(column).setValue(result);
        }
        catch( NoSuchMethodException e )
        {
            setLastError(new Throwable("No such column"));
            return;
        }
        catch( Throwable t )
        {
            setLastError(t);
            return;
        }
    }
    
    public void monitorJob(final JobControl job)
    {
        monitorJob( job, true );
    }
    
    public void monitorJob(final JobControl job, final boolean showProgress)
    {
        final ScriptEnvironment environment = Global.getEnvironment();
        if(environment == null) return;
        JobControlListener listener = new JobControlListenerAdapter()
        {
            int lastProgress = -1;
            long lastTime = System.currentTimeMillis();
            
            @Override
            public void valueChanged(JobControlEvent event)
            {
                if(environment.isStopped())
                    job.terminate();
                if(event.getPreparedness() == lastProgress) return;
                long timeDiff = System.currentTimeMillis()-lastTime;
                if(timeDiff < 2000) return;
                lastProgress = event.getPreparedness();
                lastTime = System.currentTimeMillis();
                if( showProgress )
                    environment.print(lastProgress+"%...");
            }

            @Override
            public void jobTerminated(JobControlEvent event)
            {
                if(event.getMessage() != null)
                    environment.print(event.getMessage());
                if(event.getException() != null)
                    environment.print(event.getException().getMessage());
                job.removeListener(this);
            }
        };
        job.addListener(listener);
    }

    public boolean save(DataCollection dc, String path)
    {
        beforeFunctionCall();

        DataCollection ancestorDC = CollectionFactory.getDataCollection(path);
        if( ancestorDC == null )
        {
            setLastError(new Throwable("Incorrect path"));
            return false;
        }

        try
        {
            ancestorDC.put(dc);
        }
        catch( Throwable e )
        {
            setLastError(e);
            return false;
        }

        return true;
    }

    protected TableDataCollection extractTableDataCollectionFrom(DataCollection dc)
    {
        TableDataCollection result = null;

        if( ! ( dc instanceof TableDataCollection ) )
        {
            try
            {
                Method getTableDataMethod = dc.getClass().getMethod("getTableData", new Class[0]);
                result = (TableDataCollection)getTableDataMethod.invoke(dc, new Object[0]);
            }
            catch( ClassCastException e )
            {
                setLastError(new Throwable("Incorrect type of leftSource"));
                return null;
            }
            catch( SecurityException e )
            {
                setLastError(new Throwable("Incorrect type of leftSource"));
                return null;
            }
            catch( NoSuchMethodException e )
            {
                setLastError(new Throwable("Incorrect type of leftSource"));
                return null;
            }
            catch( IllegalArgumentException e )
            {
            }
            catch( IllegalAccessException e )
            {
                setLastError(new Throwable("Incorrect type of leftSource"));
                return null;
            }
            catch( InvocationTargetException e )
            {
            }
        }
        else
        {
            result = (TableDataCollection)dc;
        }

        return result;
    }

    public TableDataCollection join(String type, TableDataCollection leftSource, TableDataCollection rightSource, String strLeftColumns,
            String strRightColumns, String newLeftColumnsStr, String newRightColumnsStr, String resultPath) throws Exception
    {
        beforeFunctionCall();

        //        DataCollection leftDc = resolveDataCollection(leftSource);
        //        DataCollection rightDc = resolveDataCollection(rightSource);
        //
        //        TableDataCollection leftTdc = extractTableDataCollectionFrom(leftDc);
        //        TableDataCollection rightTdc = extractTableDataCollectionFrom(rightDc);

        //        if( leftTdc == null || rightTdc == null )
        //        {
        //            return null;
        //        }

        int joinType;
        if( "inner".equalsIgnoreCase(type) )
        {
            joinType = TableDataCollectionUtils.INNER_JOIN;
        }
        else if( "left".equalsIgnoreCase(type) )
        {
            joinType = TableDataCollectionUtils.LEFT_JOIN;
        }
        else if( "right".equalsIgnoreCase(type) )
        {
            joinType = TableDataCollectionUtils.RIGHT_JOIN;
        }
        else if( "outer".equalsIgnoreCase(type) )
        {
            joinType = TableDataCollectionUtils.OUTER_JOIN;
        }
        else if( "left only".equalsIgnoreCase(type) )
        {
            joinType = TableDataCollectionUtils.LEFT_SUBSTRACTION;
        }
        else if( "right only".equalsIgnoreCase(type) )
        {
            joinType = TableDataCollectionUtils.RIGHT_SUBSTRACTION;
        }
        else if( "symmetric difference".equalsIgnoreCase(type) )
        {
            joinType = TableDataCollectionUtils.SYMMETRIC_DIFFERENCE;
        }
        else
        {
            setLastError(new Throwable("Unknown join type"));
            return null;
        }


        String[] leftColumns;
        String[] newLeftColumns;

        if(strLeftColumns.equals(ColumnGroup.ALL_COLUMNS_STR))
        {
            leftColumns = TableDataCollectionUtils.getColumnNames(leftSource);
        }
        else if( TextUtil2.isEmpty(strLeftColumns) )
        {
            leftColumns = new String[0];
        }
        else
        {
            leftColumns = TableDataCollectionUtils.parseStringToColumnNames(strLeftColumns, leftSource);
        }
        if( TextUtil2.isEmpty(newLeftColumnsStr) )
        {
            newLeftColumns = leftColumns;
        }
        else
        {
            newLeftColumns = TextUtil2.split( newLeftColumnsStr, ',' );
        }

        String unknownCols = StreamEx.of( leftColumns ).remove( col -> leftSource.getColumnModel().hasColumn( col ) ).joining( ", " );
        if( !unknownCols.isEmpty() )
        {
            setLastError(new Throwable("Unknown left columns:" + unknownCols));
            return null;
        }

        String[] rightColumns;
        String[] newRightColumns;
        if(strRightColumns.equals(ColumnGroup.ALL_COLUMNS_STR))
        {
            rightColumns = TableDataCollectionUtils.getColumnNames(rightSource);
        }
        else if( TextUtil2.isEmpty(strRightColumns) )
        {
            rightColumns = new String[0];
        }
        else
        {
            rightColumns = TableDataCollectionUtils.parseStringToColumnNames(strRightColumns, rightSource);
        }

        if( TextUtil2.isEmpty(newRightColumnsStr) )
        {
            newRightColumns = rightColumns;
        }
        else
        {
            newRightColumns = TextUtil2.split( newRightColumnsStr, ',' );
        }

        unknownCols = StreamEx.of( rightColumns ).remove( col -> rightSource.getColumnModel().hasColumn( col ) ).joining( ", " );
        if( !unknownCols.isEmpty() )
        {
            setLastError(new Throwable("Unknown right columns:" + unknownCols));
            return null;
        }

        // String resultName = CollectionFactory.getLastToken(resultPath);
        if( TextUtil2.isEmpty(resultPath) )
        {
            setLastError(new Throwable("Incorrect result path"));
            return null;
        }

        TableDataCollection result = TableDataCollectionUtils.join(joinType, leftSource, rightSource, DataElementPath.create(resultPath),
                leftColumns, rightColumns, newLeftColumns, newRightColumns);

        //        for( int i = 0; i < leftColumns.length; i++ )
        //            result.getColumnModel().renameColumn(i, newLeftColumns[i]);
        //
        //        for( int i = 0; i < rightColumns.length; i++ )
        //            result.getColumnModel().renameColumn(i + leftColumns.length, newRightColumns[i]);

        return result;
    }
    
    //TODO: maybe move this medthod to another java script host object (file?)
    /**
     * Method to get absolute paths for all files in folder given by folderPath
     * @param folderPath - absolute path to folder
     * @return array with absolute paths
     */
    public String[] getFiles(String folderPath )
    {
        File dir = new File(folderPath);
        if (dir.isDirectory())
        return StreamEx.of( new File(folderPath).listFiles() ).map( f->f.getAbsolutePath() ).toArray( String[]::new );
        else
            return new String[] {folderPath};
    }
    
    public DataCollection<?> attach(String path, String remoteURL, ScriptableObject params) throws Exception
    {
        Properties properties = new ExProperties();

        DataElementPath localDEPath = DataElementPath.create( path );
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, localDEPath.getName() );
        properties.setProperty( DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.server;ru.biosoft.server.tomcat" );
        properties.setProperty( DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, DataCollection.class.getName() );
        properties.setProperty( DataCollectionConfigConstants.IS_ROOT, String.valueOf( localDEPath.getDepth() == 1 ) );
        properties.setProperty( ClientDataCollection.SERVER_URL, remoteURL );
        properties.setProperty( ClientDataCollection.SERVER_DATA_COLLECTION_NAME, path );

        DataCollection parent = localDEPath.optParentCollection();

        ClientDataCollection<?> data = new ClientDataCollection<>( parent, properties );

        Object sessionObj = params.get( "sessionId" );
        Object user = params.get( "user" );
        Object password = params.get( "password" );
        if(sessionObj != null)
        {
            String sessionId = sessionObj.toString();
            ClientConnection con = data.getClientConnection();
            con.setSessionId( sessionId );            
        }else if(user != null)
        {
            data.login( user.toString(), password.toString() );
        }

        if( parent != null )
            parent.put( data );

        return data;

    }
}

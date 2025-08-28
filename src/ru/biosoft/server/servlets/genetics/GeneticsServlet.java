package ru.biosoft.server.servlets.genetics;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.DataElementExporterRegistry.ExporterInfo;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.server.AbstractJSONServlet;
import ru.biosoft.table.SqlTableDataCollection;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.ImageGenerator;
import ru.biosoft.util.TempFiles;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * Genetics servlet created for be_sites/genetics integration.
 * It provides access to data/Collaboration/Genetics project with some special manipulations.
 */
public class GeneticsServlet extends AbstractJSONServlet
{
    protected static final Logger log = Logger.getLogger(GeneticsServlet.class.getName());

    protected static final String GENETICS_PATH = "data/Collaboration/Genetics/Data";
    protected static final String GENETICS_SAMPLES = "samples";
    protected static final String GENETICS_RESULTS = "results";

    //
    // Servlet request keys
    //
    public static final String CREATE_SAMPLE = "createSample";
    public static final String UPDATE_SAMPLE = "updateSample";
    public static final String REMOVE = "remove";
    public static final String SAMPLE_LIST = "sampleList";
    public static final String RESULT_LIST = "resultList";
    public static final String EXPORT = "export";
    public static final String VIEW = "view";
    public static final String STAT_LIST = "numericStatistic";
    public static final String STAT_LIST_2 = "stringStatistic";

    //
    // Response constants
    //
    public static final String TYPE_OK = "ok";
    public static final String TYPE_ERROR = "error";

    public static final String ATTR_TYPE = "type";
    public static final String ATTR_MESSAGE = "message";
    public static final String ATTR_TABLE_ID = "tableId";
    public static final String ATTR_LIST = "list";

    @Override
    public String service(String localAddress, Object session, Map params, OutputStream out, Map<String, String> header)
    {
        try
        {
            String sessionId = getSessionId(session);
            SecurityManager.addThreadToSessionRecord(Thread.currentThread(), sessionId);

            if( localAddress.endsWith(EXPORT) )
            {
                return exportData(params, out, header);
            }
            else if( localAddress.endsWith(VIEW) )
            {
                return viewData(params, out, header);
            }

            JSONObject result = null;
            try
            {
                if( localAddress.endsWith(CREATE_SAMPLE) )
                {
                    result = createSample(params);
                }
                else if( localAddress.endsWith(UPDATE_SAMPLE) )
                {
                    result = updateSample(params);
                }
                else if( localAddress.endsWith(REMOVE) )
                {
                    result = removeElement(params);
                }
                else if( localAddress.endsWith(SAMPLE_LIST) )
                {
                    result = getSampleList(params);
                }
                else if( localAddress.endsWith(RESULT_LIST) )
                {
                    result = getResultList(params, out);
                }
                else if( localAddress.endsWith(STAT_LIST) )
                {
                    result = getStatisticsList(params);
                }
                else if( localAddress.endsWith(STAT_LIST_2) )
                {
                    result = getStringStatisticsList(params);
                }
                else
                {
                    result = errorResponse("unknown request command");
                }
            }
            catch( Exception e )
            {
                result = errorResponse(e.getMessage());
            }

            OutputStreamWriter ow = new OutputStreamWriter(out, "UTF8");
            ow.write(result.toString());
            ow.flush();
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Support servlet exception", e);
        }
        finally
        {
            SecurityManager.commonLogout();
        }
        return "text/html";
    }

    /**
     * Create new table for sample and return SQL table name
     */
    protected JSONObject createSample(Map params) throws Exception
    {
        login(params);
        String jsonParams = getStrictParameter(params, "params");

        JSONObject properties = new JSONObject(jsonParams);
        String sampleName = properties.getString("sample");
        JSONArray columns = properties.getJSONArray("columns");
        if( ( sampleName == null ) || ( columns == null ) )
        {
            return errorResponse("Incorrect request parameters");
        }
        DataElementPath parentCompleteName = DataElementPath.create(GENETICS_PATH, GENETICS_SAMPLES);

        Permission permission = SecurityManager.getPermissions(parentCompleteName);
        checkPermission(permission, "put");
        DataCollection<DataElement> geneticsSamples = parentCompleteName.getDataCollection();
        if( !geneticsSamples.contains(sampleName) )
        {

            TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(geneticsSamples, sampleName);
            if( table instanceof SqlTableDataCollection )
            {
                //add description
                String description = properties.getString("description");
                if( description != null )
                {
                    table.getInfo().setDescription(description);
                }

                Properties props = table.getInfo().getProperties();

                //add comments
                String comments = properties.getString("comment");
                if( comments != null )
                {
                    props.put("comments", comments);
                }

                //add condition
                String condition = properties.getString("condition");
                if( condition != null )
                {
                    props.put("condition", condition);
                }

                //add author
                String author = properties.getString("author");
                if( author != null )
                {
                    props.put("author", author);
                }

                //add date
                String date = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
                props.put("date", date);

                //add columns
                for( int i = 0; i < columns.length(); i++ )
                {
                    JSONObject cInfo = columns.getJSONObject(i);
                    String cName = cInfo.getString("name");
                    if( cInfo.has("type") )
                    {
                        String type = cInfo.getString("type");
                        if( type != null && type.equals("integer") )
                        {
                            table.getColumnModel().addColumn(cName, Integer.class);
                        }
                    }
                    else
                    {
                        table.getColumnModel().addColumn(cName, String.class);
                    }
                }

                //get SQL table name
                String tableId = ( (SqlTableDataCollection)table ).getTableId();
                JSONObject root = new JSONObject();
                root.put(ATTR_TYPE, TYPE_OK);
                root.put(ATTR_TABLE_ID, tableId);
                return root;
            }
            return errorResponse("table is not SQL-based");
        }
        return errorResponse("sample with the same name already exists, try another name");
    }

    /**
     * Update table data from SQL table.
     */
    protected JSONObject updateSample(Map params) throws Exception
    {
        login(params);
        String jsonParams = getStrictParameter(params, "params");

        JSONObject properties = new JSONObject(jsonParams);
        String sampleName = properties.getString("sample");
        DataElementPath parentPath = DataElementPath.create(GENETICS_PATH, GENETICS_SAMPLES);

        Permission permission = SecurityManager.getPermissions(parentPath);
        checkPermission(permission, "get");
        AbstractDataCollection<?> geneticsSamples = parentPath.optDataElement(AbstractDataCollection.class);
        if( geneticsSamples != null )
        {
            geneticsSamples.removeFromCache(sampleName);
        }
        JSONObject root = new JSONObject();
        root.put(ATTR_TYPE, TYPE_OK);
        return root;
    }

    /**
     * Remove element
     */
    protected JSONObject removeElement(Map params) throws Exception
    {
        String jsonParams = getStrictParameter(params, "params");
        String user = getStrictParameter(params, "user");
        String pass = getStrictParameter(params, "pass");

        JSONObject properties = new JSONObject(jsonParams);
        String collectionName = properties.getString("collection");
        String elementName = properties.getString("name");
        DataElementPath parentCompleteName = DataElementPath.create(GENETICS_PATH, collectionName);

        Permission permission = SecurityManager.login(parentCompleteName.toString(), user, pass);
        checkPermission(permission, "remove");
        DataCollection geneticsResults = parentCompleteName.getDataCollection();
        if( geneticsResults.contains(elementName) )
        {
            try
            {
                geneticsResults.remove(elementName);
                JSONObject root = new JSONObject();
                root.put(ATTR_TYPE, TYPE_OK);
                return root;
            }
            catch( Exception e )
            {
                return errorResponse("can't remove element: " + e.getMessage());
            }
        }
        return errorResponse("can't find element with the name '" + elementName + "'");
    }

    /**
     * Get list of sample info objects
     */
    protected JSONObject getSampleList(Map params) throws Exception
    {
        DataElementPath parentCompleteName = DataElementPath.create(GENETICS_PATH, GENETICS_SAMPLES);
        login(params);
        Permission permission = SecurityManager.getPermissions(parentCompleteName);
        checkPermission(permission, "put");
        DataCollection<DataElement> geneticsSamples = parentCompleteName.getDataCollection();

        JSONArray infoArray = new JSONArray();
        for( DataElement de : geneticsSamples )
        {
            if( ! ( de instanceof TableDataCollection ) )
                continue;
            TableDataCollection tde = (TableDataCollection)de;
            JSONObject info = new JSONObject();
            info.put("title", de.getName());
            info.put( "completeName", tde.getCompletePath().toString() );
            info.put( "description", tde.getDescription() );
            JSONArray columns = new JSONArray();
            for( TableColumn column : tde.getColumnModel() )
            {
                columns.put(column.getName());
            }
            info.put("attributes", columns);
            info.put( "size", tde.getSize() );

            Properties props = tde.getInfo().getProperties();
            info.put("condition", props.getProperty("condition"));
            info.put("date", props.getProperty("date"));
            info.put("author", props.getProperty("author"));
            info.put("comments", props.getProperty("comments"));

            infoArray.put(info);
        }

        JSONObject root = new JSONObject();
        root.put(ATTR_TYPE, TYPE_OK);
        root.put(ATTR_LIST, infoArray);
        return root;
    }

    /**
     * Get list of result info objects
     */
    protected JSONObject getResultList(Map params, OutputStream out) throws Exception
    {
        login(params);
        DataElementPath parentPath = DataElementPath.create(GENETICS_PATH, GENETICS_RESULTS);
        Permission permission = SecurityManager.getPermissions(parentPath);
        checkPermission(permission, "put");
        DataCollection<DataElement> geneticsResults = parentPath.getDataCollection();

        JSONArray infoArray = new JSONArray();
        List<String> nameList = geneticsResults.getNameList();
        for( String name : nameList )
        {
            DataElement de = geneticsResults.get(name);
            JSONObject info = new JSONObject();
            info.put("title", de.getName());
            info.put("completeName", DataElementPath.create(de).toString());

            if( de instanceof DataCollection )
            {
                Properties props = ( (DataCollection<?>)de ).getInfo().getProperties();
                info.put("description", props.getProperty("description"));
                info.put("date", props.getProperty("date"));
                info.put("author", props.getProperty("author"));
                info.put("comments", props.getProperty("comments"));
            }
            infoArray.put(info);
        }

        JSONObject root = new JSONObject();
        root.put(ATTR_TYPE, TYPE_OK);
        root.put(ATTR_LIST, infoArray);
        return root;
    }

    /**
     * Get list of numeric statistics info objects
     */
    protected JSONObject getStatisticsList(Map params) throws Exception
    {
        String jsonParams = getStrictParameter(params, "params");
        String user = getStrictParameter(params, "user");
        String pass = getStrictParameter(params, "pass");

        JSONObject properties = new JSONObject(jsonParams);
        String collectionName = properties.getString("collection");
        if( collectionName == null )
        {
            return errorResponse("Missing parameter: 'collection'");
        }
        String elementName = properties.getString("name");
        if( elementName == null )
        {
            return errorResponse("Missing parameter: 'name'");
        }
        DataElementPath elementPath = DataElementPath.create(GENETICS_PATH, collectionName, elementName);
        Permission permission = SecurityManager.login(elementPath.toString(), user, pass);
        checkPermission(permission, "get");
        DataElement de = elementPath.optDataElement();
        if( de instanceof TableDataCollection )
        {
            TableDataCollection tdc = (TableDataCollection)de;
            JSONArray infoArray = new JSONArray();

            for( TableColumn column : tdc.getColumnModel() )
            {
                StatInfo stat = Statistics.getNumericColumnStatistics(tdc, column);
                if( stat != null )
                {
                    JSONObject info = new JSONObject();
                    info.put("name", column.getName());
                    info.put("number", stat.getNumber());
                    info.put("average", stat.getAverage());
                    info.put("dispersion", stat.getDispersion());
                    info.put("deviation", stat.getDeviation());
                    info.put("median", stat.getMedian());
                    info.put("min", stat.getMin());
                    info.put("max", stat.getMax());

                    infoArray.put(info);
                }
            }

            JSONObject root = new JSONObject();
            root.put(ATTR_TYPE, TYPE_OK);
            root.put(ATTR_LIST, infoArray);
            return root;
        }
        return errorResponse("Data element '" + elementPath.toString() + "' is not a table");
    }

    /**
     * Get list of non-numeric statistics info objects
     */
    protected JSONObject getStringStatisticsList(Map params) throws Exception
    {
        String jsonParams = getStrictParameter(params, "params");
        String user = getStrictParameter(params, "user");
        String pass = getStrictParameter(params, "pass");

        JSONObject properties = new JSONObject(jsonParams);
        String collectionName = properties.getString("collection");
        if( collectionName == null )
        {
            return errorResponse("Missing parameter: 'collection'");
        }
        String elementName = properties.getString("name");
        if( elementName == null )
        {
            return errorResponse("Missing parameter: 'name'");
        }
        DataElementPath elementPath = DataElementPath.create(GENETICS_PATH, collectionName, elementName);
        Permission permission = SecurityManager.login(elementPath.toString(), user, pass);
        checkPermission(permission, "get");
        DataElement de = elementPath.optDataElement();
        if( de instanceof TableDataCollection )
        {
            TableDataCollection tdc = (TableDataCollection)de;
            JSONArray infoArray = new JSONArray();

            for( TableColumn column : tdc.getColumnModel() )
            {
                StatInfo2 stat = Statistics.getStringColumnStatistics(tdc, column);
                if( stat != null )
                {
                    JSONObject info = new JSONObject();
                    info.put("name", column.getName());
                    info.put("number", stat.getNumber());

                    JSONArray values = new JSONArray();
                    for( String value : stat.getValues().keySet() )
                    {
                        JSONObject valStat = new JSONObject();
                        valStat.put("value", value);
                        valStat.put("number", stat.getValues().get(value));
                        values.put(valStat);
                    }
                    info.put("values", values);

                    infoArray.put(info);
                }
            }

            JSONObject root = new JSONObject();
            root.put(ATTR_TYPE, TYPE_OK);
            root.put(ATTR_LIST, infoArray);
            return root;
        }
        return errorResponse("Data element '" + elementPath.toString() + "' is not a table");
    }

    /**
     * Export data element
     */
    protected String exportData(Map params, OutputStream out, Map<String, String> header) throws Exception
    {
        try
        {
            login(params);
            String jsonParams = getStrictParameter(params, "params");

            JSONObject properties = new JSONObject(jsonParams);
            String sampleName = properties.getString("sampleName");
            if( sampleName == null )
            {
                return htmlErrorResponse(out, "Missing parameter: 'sampleName'");
            }
            DataElementPath elementPath = DataElementPath.create(GENETICS_PATH, GENETICS_SAMPLES, sampleName);
            Permission permission = SecurityManager.getPermissions(elementPath);
            checkPermission(permission, "get");
            String exporterName = "Tab-separated text (*.txt)";
            ExporterInfo[] exporterInfo = DataElementExporterRegistry.getExporterInfo(exporterName, elementPath.optDataElement());

            if( exporterInfo != null )
            {
                String suffix = exporterInfo[0].getSuffix();
                if( suffix.indexOf('.') == -1 )
                    suffix = "." + suffix;

                header.put("Content-Disposition", "attachment;filename=\"" + sampleName + suffix + "\"");
                doExport(elementPath.getDataElement(), exporterInfo[0].cloneExporter(), suffix, out);

                return "text/plain";
            }
            return htmlErrorResponse(out, "Unknown exporter: " + exporterName);
        }
        catch( Exception e )
        {
            return htmlErrorResponse(out, e.getMessage());
        }
    }

    /**
     * View data element
     */
    protected String viewData(Map params, OutputStream out, Map<String, String> header) throws Exception
    {
        try
        {
            String jsonParams = getStrictParameter(params, "params");
            String user = getStrictParameter(params, "user");
            String pass = getStrictParameter(params, "pass");

            JSONObject properties = new JSONObject(jsonParams);
            String collectionName = properties.getString("collection");
            if( collectionName == null )
            {
                return htmlErrorResponse(out, "Missing parameter: 'collection'");
            }
            String elementName = properties.getString("name");
            if( elementName == null )
            {
                return htmlErrorResponse(out, "Missing parameter: 'name'");
            }
            DataElementPath elementPath = DataElementPath.create(GENETICS_PATH, collectionName, elementName);
            Permission permission = SecurityManager.login(elementPath.toString(), user, pass);
            checkPermission(permission, "get");
            DataElement de = elementPath.getDataElement();
            if( de instanceof TableDataCollection )
            {
                String exporterName = "HTML document (*.html)";
                ExporterInfo[] exporterInfo = DataElementExporterRegistry.getExporterInfo(exporterName, elementPath.optDataElement());

                if( exporterInfo != null )
                {
                    String suffix = exporterInfo[0].getSuffix();
                    if( suffix.indexOf('.') == -1 )
                        suffix = "." + suffix;

                    doExport(de, exporterInfo[0].cloneExporter(), suffix, out);
                    return "text/html";
                }
                return htmlErrorResponse(out, "Unknown exporter: " + exporterName);
            }
            else if( de instanceof ImageDataElement )
            {
                BufferedImage image = ( (ImageDataElement)de ).getImage(null);
                ImageGenerator.encodeImage(image, "PNG", out);
                out.close();
                return "image/png";
            }
            else
            {
                return htmlErrorResponse(out, "Unsupported element type: " + de.getClass().getName());
            }
        }
        catch( Exception e )
        {
            return htmlErrorResponse(out, e.getMessage());
        }
    }

    protected void doExport(@Nonnull ru.biosoft.access.core.DataElement dataElement, DataElementExporter exporter, String suffix, OutputStream out) throws Exception
    {
        File file = TempFiles.file(suffix);
        exporter.doExport(dataElement, file);

        ApplicationUtils.copyStream(out, new FileInputStream(file));
    }

    /**
     * Get session ID from session object
     */
    protected String getSessionId(Object session)
    {
        try
        {
            Method getIdMethod = session.getClass().getMethod("getId", new Class[] {});
            return (String)getIdMethod.invoke(session, new Object[] {});
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not get ID of session");
            return null;
        }
    }

    /**
     * Print error to output stream
     */
    protected String htmlErrorResponse(OutputStream out, String error) throws Exception
    {
        OutputStreamWriter ow = new OutputStreamWriter(out, "UTF8");
        ow.write(error);
        ow.flush();
        return "text/html";
    }
}

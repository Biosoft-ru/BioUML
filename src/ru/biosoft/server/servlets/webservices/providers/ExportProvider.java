package ru.biosoft.server.servlets.webservices.providers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.json.JSONArray;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.DataElementExporterRegistry.ExporterInfo;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebJob;
import ru.biosoft.util.ObjectExtensionRegistry;
import ru.biosoft.util.TempFiles;


/**
 * @author lan
 *
 */
public class ExportProvider extends WebProviderSupport
{
    @Override
    public void process(BiosoftWebRequest arguments, BiosoftWebResponse resp) throws Exception
    {
        if("list".equals(arguments.optAction()))
        {
            new JSONResponse(resp).sendJSON(new JSONArray(DataElementExporterRegistry.formats().toList()));
            return;
        }
        DataElementPath path = arguments.getDataElementPath();
        if(!isExportAllowed(path))
            throw new SecurityException("Export of " + path + " is not available for current user");
        String type = arguments.getString("type");
        DataElement de = getExportedDataElement(path, arguments.getString("detype"), arguments);
        if( type.equals("de") )
        {
            JSONArray jsonParameters = arguments.optJSONArray("parameters");
            exportDataElement(de, arguments.getString("exporter"), jsonParameters, resp,
                    WebJob.getWebJob(arguments.get("jobID")).createJobControl());
        }
        else if( type.equals("deInfo") )
        {
            exportDataElementInfo(de, new JSONResponse(resp));
            return;
        }
        else if( type.equals("deParams") )
        {
            String exporter = arguments.getString("exporter");
            JSONArray jsonArray = arguments.optJSONArray("json");
            ExporterInfo[] exporterInfo = DataElementExporterRegistry.getExporterInfo(exporter, de);
            Object parameters = null;
            if( exporterInfo.length > 0  && exporterInfo[0].getExporter() != null )
            {
                String suffix = exporterInfo[0].getSuffix();
                if( suffix.indexOf('.') == -1 )
                    suffix = "." + suffix;

                try
                {
                    File file = TempFiles.file("export"+suffix);
                    parameters = exporterInfo[0].cloneExporter().getProperties(de, file);
                    file.delete();
                    if(jsonArray != null)
                        JSONUtils.correctBeanOptions( parameters, jsonArray );
                }
                catch( IOException e )
                {
                    log.log( Level.SEVERE, e.getMessage(), e );
                }
            }
            WebBeanProvider.sendBeanStructure("Exporter " + exporter, parameters, new JSONResponse(resp));
        }
        else throw new WebException("EX_QUERY_PARAM_INVALID_VALUE", "type");
    }

    /** DataCollection property, either true of false.
     *  If export=false then export will not be available for this DataCollection and its descendants.
     *  If not specified then export property of nearest parent will take action.
     */
    private static final String EXPORT_PROPERTY = "export";

    private boolean isExportAllowed(DataElementPath path)
    {
        if(SecurityManager.isAdmin())
            return true;
        path = path.getTargetPath();
        Permission perm = SecurityManager.getPermissions( path );
        if(!perm.isReadAllowed())
            return false;
        if(perm.isAdminAllowed())
            return true;

        DataCollection<?> dc = path.optDataCollection();
        if(dc == null)
            dc = path.optParentCollection();
        while(dc != null) {
            String exportProperty = dc.getInfo().getProperty( EXPORT_PROPERTY );
            if(exportProperty != null)
                return "true".equals( exportProperty );
            dc = dc.getOrigin();
        }
        return true;
    }

    /**
     * Retrieve or construct ru.biosoft.access.core.DataElement which will be consequently exported
     * This method uses extension point "ru.biosoft.server.servlets.exportedDe" to get ru.biosoft.access.core.DataElement provider,
     * which depends on particular de type.
     * @param path - element name (URL de parameter)
     * @param type - element type passed by client (URL detype parameter)
     * @param arguments - Map containing all URL parameters
     * @return created ru.biosoft.access.core.DataElement (always not null)
     * @throws WebException
     */
    public static @Nonnull ru.biosoft.access.core.DataElement getExportedDataElement(DataElementPath path, String type, BiosoftWebRequest arguments) throws WebException
    {
        DataElement de;
        ExportedDeProvider provider = getExportedElementProvider( type );
        if( provider != null )
            de = provider.getExportedDataElement( type, arguments );
        else
            throw new WebException( "EX_QUERY_PARAM_INVALID_VALUE", "detype" );
        return de;
    }

    /**
     * Generates info block describing exporters available for given ru.biosoft.access.core.DataElement
     * @param de - ru.biosoft.access.core.DataElement (presumably created by getExportedDataElement)
     * @param response - JSONResponse to output info
     * @throws IOException
     */
    public static void exportDataElementInfo(DataElement de, JSONResponse response) throws IOException
    {
        List<String> formats = DataElementExporterRegistry.getExporterFormats(de);
        if( formats.size() == 0 )
        {
            response.error("There is no suitable formats to export this element");
            return;
        }
        JSONArray jsonFormats = new JSONArray();
        for( String format : formats )
        {
            jsonFormats.put(format);
        }
        response.sendJSON(jsonFormats);
    }

    /**
     * Exports given ru.biosoft.access.core.DataElement using given exporter
     * @param de - ru.biosoft.access.core.DataElement to export
     * @param exporter - exporter format to use
     * @param jsonParameters - optional parameters for exporter (can be null)
     * @param resp - HTTP response object
     */
    protected void exportDataElement(@Nonnull ru.biosoft.access.core.DataElement de, String exporter, JSONArray jsonParameters, BiosoftWebResponse resp,
            FunctionJobControl job)
    {
        try
        {
            ExporterInfo[] exporterInfo = DataElementExporterRegistry.getExporterInfo(exporter, de);

            // info should be not empty because previously we have selected only suitable formats
            if( exporterInfo != null )
            {
                String suffix = exporterInfo[0].getSuffix();
                if( suffix.indexOf('.') == -1 )
                    suffix = "." + suffix;

                File file = TempFiles.file("export_"+suffix);

                DataElementExporter exporterInstance = exporterInfo[0].cloneExporter();
                if( jsonParameters != null )
                {
                    Object parameters = exporterInstance.getProperties(de, file);
                    if( parameters != null )
                    {
                        JSONUtils.correctBeanOptions(parameters, jsonParameters);
                    }
                }

                exporterInstance.doExport(de, file, job);

                if( job != null && job.getStatus() != JobControl.COMPLETED )
                {
                    file.delete();
                    return;
                }

                resp.setHeader("Content-Disposition", "attachment;filename=\"" + de.getName() + suffix + "\"");
                //resp.setHeader("Content-Length", String.valueOf(file.length()));
                resp.setContentType(exporterInfo[0].getContentType());

                ApplicationUtils.copyStream(resp.getOutputStream(), new FileInputStream(file));
                file.delete();
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not export data element", e);
        }
    }


    private static final ObjectExtensionRegistry<ExportedDeProvider> exportedDeProviders = new ObjectExtensionRegistry<>(
            "ru.biosoft.server.servlets.exportedDe", "prefix", ExportedDeProvider.class );

    public static ExportedDeProvider getExportedElementProvider(String type)
    {
        return exportedDeProviders.getExtension( type );
    }
}

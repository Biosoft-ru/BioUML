package ru.biosoft.server.servlets.webservices.providers;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.WriterHandler;
import com.eclipsesource.json.JsonValue;

import biouml.model.Diagram;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.analysiscore.AnalysisParametersBeanProvider;
import ru.biosoft.jobcontrol.ClassJobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebJob;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.server.servlets.webservices.WebSession;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;
import ru.biosoft.util.JsonUtils;

/**
 * @author lan
 *
 */
public class AnalysisProvider extends WebJSONProviderSupport
{

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws WebException, IOException
    {
        String action = arguments.optAction();
        if(action != null && action.equals("list"))
        {
            response.sendJSON( AnalysisMethodRegistry.getAnalysisNamesWithGroup().map( JsonValue::valueOf ).collect( JsonUtils.toArray() ) );
            return;
        }
        else if( action != null && action.equals( "completeName" ) )
        {
            String name = arguments.getString( "name" );
            AnalysisMethodInfo info = AnalysisMethodRegistry.getMethodInfo( name );
            if( info != null )
            {
                response.sendString( info.getCompletePath().toString() );
            }
            else
            {
                response.error( "Method '" + name + "' not found" );
            }
            return;

        }
        else if( action != null && action.equals( "search" ) )
        {
            //methods
            String searchStr = arguments.getString( "str" ).trim().toLowerCase();
            VectorDataCollection<MethodInfoWrapper> resultByName = new VectorDataCollection<>( "analysis search", MethodInfoWrapper.class,
                    null );
            VectorDataCollection<MethodInfoWrapper> resultByDescr = new VectorDataCollection<>( "analysis search", MethodInfoWrapper.class,
                    null );
            AnalysisMethodRegistry.getAnalysisNamesWithGroup().map( name -> {
                try
                {
                    AnalysisMethodInfo info = AnalysisMethodRegistry.getMethodInfo( name );
                    //TODO: remove getDescription, now it's workaround to eliminate analyses that can not init parameters
                    info.getDescriptionHTML();
                    return info;
                }
                catch( Exception e )
                {
                    return null;
                }

            } ).nonNull().forEach( mi -> {
                if( mi.getName().toLowerCase().contains( searchStr ) )
                    resultByName.put( new MethodInfoWrapper( mi ) );
                else if( mi.getDescriptionHTML() != null && mi.getDescriptionHTML().toLowerCase().contains( searchStr ) )
                {
                    resultByDescr.put( new MethodInfoWrapper( mi ) );
                }
            } );
            //galaxy
            DataElementPathSet galaxyPaths = new DataElementPathSet( DataElementPath.create( "analyses" ), "Galaxy", "Galaxy2" );
            for( DataElementPath galaxyPath : galaxyPaths )
            {
                if( galaxyPath.exists() )
                {
                    DataCollection gdc = galaxyPath.getDataElement( ru.biosoft.access.core.DataCollection.class );
                    StreamEx<Object> str = StreamEx.of( gdc.stream( DataCollection.class ) )
                            .flatMap( dc -> ( (DataCollection)dc ).getNameList().stream() );
                    str.map( name -> {
                        try
                        {
                            AnalysisMethodInfo info = AnalysisMethodRegistry.getMethodInfo( (String)name );
                            //TODO: remove getDescription, now it's workaround to eliminate analyses that can not init parameters
                            info.getDescriptionHTML();
                            return info;
                        }
                        catch( Exception e )
                        {
                            return null;
                        }

                    } ).nonNull().forEach( mi -> {
                        if( mi.getName().toLowerCase().contains( searchStr ) )
                            resultByName.put( new MethodInfoWrapper( mi ) );
                        else if( mi.getDescriptionHTML() != null && mi.getDescriptionHTML().toLowerCase().contains( searchStr ) )
                        {
                            resultByDescr.put( new MethodInfoWrapper( mi ) );
                        }
                    } );
                }
            }
            //workflows
            DataElementPath workflowsPath = DataElementPath.create( "analyses/Workflows" );
            if(workflowsPath.exists())
            {
                DataCollection<?> wfc = workflowsPath.getDataElement( ru.biosoft.access.core.DataCollection.class );
                StreamEx<Diagram> str2 = StreamEx.ofTree( wfc, DataCollection.class, dc -> {
                    if( dc instanceof Diagram )
                        return StreamEx.empty();
                    else
                        return dc.stream();
                } ).select( Diagram.class );
                str2.filter( d -> d.getName().toLowerCase().contains( searchStr ) || d.getTitle().toLowerCase().contains( searchStr ) ).forEach( d -> {
                    resultByName.put( new MethodInfoWrapper( d ) );
                } );
            }

            //Sort column, names have higher priority than descriptions
            int index = 1;
            for( MethodInfoWrapper miw : resultByName )
                miw.setIndex( index++ );
            for( MethodInfoWrapper miw : resultByDescr )
            {
                miw.setIndex( index++ );
                resultByName.put( miw );
            }
            if( !resultByName.isEmpty() )
            {
                WebServicesServlet.getSessionCache().addObject( "beans/searchResult", resultByName, true );
                response.sendString( "ok" );
            }
            else
            {
                WebServicesServlet.getSessionCache().removeObject( "beans/searchResult" );
                response.sendString( "" );
            }
            return;
        }
        DataElementPath de = arguments.getDataElementPath();
        final AnalysisMethod method = AnalysisMethodRegistry.getAnalysisMethod(de.getName());
        JSONArray jsonParams = arguments.optJSONArray(JSON_ATTR);
        if( jsonParams != null )
        {
            AnalysisParameters parameters = method.getParameters();
            int showMode = arguments.optInt(SHOW_MODE, Property.SHOW_USUAL);
            if( showMode == Property.SHOW_EXPERT )
                parameters.setExpertMode(true);
            WebBeanProvider.preprocessJSON(parameters, jsonParams);
            boolean useJsonOrder = WebBeanProvider.isUseJsonOrder( arguments );
            try
            {
                JSONUtils.correctBeanOptions(parameters, jsonParams, useJsonOrder);
            }
            catch( Exception e )
            {
                throw new WebException(e, "EX_INTERNAL_UPDATE_BEAN", JSON_ATTR);
            }
        }

        final String jobID = arguments.get("jobID");
        if( jobID != null )
        {
            final WebJob webJob = WebJob.getWebJob(jobID);
            final Logger log = webJob.getJobLogger();
            method.setLogger(log);
            ClassJobControl job = method.getJobControl();
            Writer writer = new Writer()
            {
                StringBuffer buffer = new StringBuffer();

                @Override
                public void close() throws IOException
                {
                }

                @Override
                public void flush() throws IOException
                {
                    webJob.addJobMessage(buffer.toString());
                    buffer = new StringBuffer();
                }

                @Override
                public void write(char[] bytes, int offset, int len) throws IOException
                {
                    buffer.append(bytes, offset, len);
                }
            };

            final Handler webLogHandler = new WriterHandler( writer, new PatternFormatter( "%4$s - %5$s%n" ) );
            webLogHandler.setLevel( Level.INFO );
            log.setLevel(Level.ALL);
            log.addHandler( webLogHandler );

            TaskManager taskManager = TaskManager.getInstance();
            TaskInfo taskInfo = taskManager.addAnalysisTask( method, false );
            webJob.setTask(taskInfo);
            taskInfo.setTransient("parameters", method.getParameters());

            WebSession session = WebSession.getCurrentSession();
            job.addListener(new JobControlListenerAdapter()
            {
                @Override
                public void jobTerminated(JobControlEvent event)
                {
                    log.removeHandler( webLogHandler );
                    for(DataElementPath path : webJob.getJobResults())
                        session.pushRefreshPath( path );
                }
            });
            taskManager.runTask(taskInfo);
        }

        if( action != null )
        {
            if( action.equals("script") )
            {
                Map<String, String> scripts = method.generateScripts((AnalysisParameters)WebBeanProvider.getBean(AnalysisParametersBeanProvider.PREFIX
                        + de.getName()));
                response.sendJSON(new JSONObject(scripts));
            }
            else if( action.equals("overwritePrompt") )
            {
                try
                {
                    JSONArray array = JSONUtils.toSimpleJSONArray(
                            ( (AnalysisParameters)WebBeanProvider.getBean(
                            AnalysisParametersBeanProvider.PREFIX + de.getName()) ).getExistingOutputNames());
                    JSONObject object = new JSONObject();
                    object.put("paths", array);
                    response.sendJSON(object);
                }
                catch( JSONException e )
                {
                }
            }
            else if( action.equals("changeMode") )
            {
                int showMode = arguments.optInt(SHOW_MODE, Property.SHOW_USUAL);
                ( (AnalysisParameters)WebBeanProvider
                        .getBean(AnalysisParametersBeanProvider.PREFIX + de.getName()) )
                        .setExpertMode(showMode == Property.SHOW_EXPERT);
                method.getParameters().setExpertMode( showMode == Property.SHOW_EXPERT );
                WebBeanProvider.saveBean(AnalysisParametersBeanProvider.PREFIX + de.getName(), method.getParameters());
                WebBeanProvider.sendBeanStructure(AnalysisParametersBeanProvider.PREFIX + de.getName(),
                        method.getParameters(), response, showMode);
            }
            else
                throw new WebException("EX_QUERY_PARAM_INVALID_VALUE", BiosoftWebRequest.ACTION);
        }
        else if( jsonParams != null )
        {
            int showMode = arguments.optInt(SHOW_MODE, Property.SHOW_USUAL);
            WebBeanProvider.saveBean(AnalysisParametersBeanProvider.PREFIX + de.getName(), method.getParameters());
            WebBeanProvider.sendBeanStructure(AnalysisParametersBeanProvider.PREFIX + de.getName(), method.getParameters(),
                    response, showMode);
        }
    }

    public static class MethodInfoWrapper implements DataElement
    {
        DataElement info;
        int index;
        public MethodInfoWrapper(DataElement info)
        {
            this.info = info;
        }
        public String name;
        public String description;
        public DataElementPath path;

        @Override
        public String getName()
        {
            return info.getName();
        }

        public String getDescription()
        {
            if( info instanceof AnalysisMethodInfo )
            {
                String description = ( (AnalysisMethodInfo)info ).getShortDescription();
                if( description.isEmpty() )
                    return ( (AnalysisMethodInfo)info ).getDescriptionHTML();
                else
                    return description;
            }
            else
                return "";
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            return info.getOrigin();
        }

        public DataElementPath getPath()
        {
            return info.getCompletePath();
        }

        public int getIndex()
        {
            return index;
        }

        public void setIndex(int index)
        {
            this.index = index;
        }
    }

    public static class MethodInfoWrapperBeanInfo extends BeanInfoEx
    {
        public MethodInfoWrapperBeanInfo()
        {
            super( MethodInfoWrapper.class, ru.biosoft.analysis.gui.MessageBundle.class.getName() );
        }

        @Override
        public void initProperties() throws Exception
        {
            add( new PropertyDescriptorEx( "index", beanClass, "getIndex", null ), "ID", "ID" );
            add( new PropertyDescriptorEx( "path", beanClass, "getPath", null ), getResourceString( "PN_METHOD_NAME" ),
                    getResourceString( "PD_METHOD_NAME" ) );
            add( new PropertyDescriptorEx( "description", beanClass, "getDescription", null ), getResourceString( "PN_METHOD_DESCRIPTION" ),
                    getResourceString( "PD_METHOD_DESCRIPTION" ) );
        }
    }
}

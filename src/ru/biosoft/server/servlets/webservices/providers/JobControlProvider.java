package ru.biosoft.server.servlets.webservices.providers;

import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebJob;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;
import ru.biosoft.util.BeanUtil;
import biouml.model.Diagram;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.model.Property;
import ru.biosoft.jobcontrol.JobControl;

/**
 * @author lan
 *
 */
public class JobControlProvider extends WebJSONProviderSupport
{
    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        String action = arguments.optAction();
        String jobID = arguments.getString("jobID");
        if( "attach".equals(action) )
        {
            String taskID = arguments.getString("taskID");
            TaskInfo task = TaskManager.getInstance().getTask(taskID);
            if(task == null || (!task.getUser().equals( "*" ) && !task.getUser().equals(SecurityManager.getSessionUser())))
                throw new WebException("EX_QUERY_NO_ELEMENT", taskID);
            WebJob.attach(jobID, task);
            int displayHidden = arguments.optInt( "displayHidden", 1 );
            if( displayHidden == 0 && task.getAttributes().hasProperty( "isTaskHidden" ) )
            {
                response.sendString( "hidden" );
                return;
            }
            Object bean = task.getTransient("parameters");
            if(bean == null) {
                bean = task.getAttributes();
                if(task.getType().equals( TaskInfo.ANALYSIS ))
                    bean = AnalysisDPSUtils.readParametersFromAttributes( task.getAttributes() );
                else if(task.getType().equals( TaskInfo.WORKFLOW ))
                {
                    Diagram workflow = task.getSource().getDataElement(Diagram.class);
                    DynamicPropertySet parameters = WorkflowItemFactory.getWorkflowParameters( workflow );
                    BeanUtil.copyBean( bean, parameters );
                    bean = parameters;
                }
            }
            
            if(bean == null)
            {
                response.sendString("ok");
            } else
            {
                WebBeanProvider.sendBeanStructure(task.getSource().toString(), bean, response, Property.SHOW_EXPERT);
            }
            return;
        }
        WebJob webJob = WebJob.getWebJob(jobID);
        JobControl job = webJob.getJobControl();
        if( job == null )
            throw new WebException("EX_QUERY_PARAM_NO_JOB", jobID);
        if( "cancel".equals(action) )
        {
            job.terminate();
        }
        response.sendStatus(job.getStatus(), job.getPreparedness(), webJob.getJobResults(), webJob.getJobMessage());
    }
}

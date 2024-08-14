package biouml.plugins.research.workflow;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.plugins.research.workflow.engine.WorkflowEngine;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.BeanInfoEx2;

public class RunWorkflowAnalysis extends AnalysisMethodSupport<RunWorkflowAnalysis.RunWorkflowParameters> implements AnalysisMethod
{

    public RunWorkflowAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new RunWorkflowParameters() );
        this.jobControl = new SubWorkflowJobControl();
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        FunctionJobControl adapter = new WorkflowEngineAdapter( jobControl );
        WorkflowEngine engine = createWorkflowEngine( parameters.getWorkflow(), parameters.getWorkflowProperties(),
                parameters.isIgnoreFail(), parameters.isSkipCompleted(), log, adapter );
        engine.start();
        return new Object[] {};
    }

    public static WorkflowEngine createWorkflowEngine(Diagram workflow, DynamicPropertySet parameters, boolean ignoreFail, boolean skipCompleted, Logger log,
            FunctionJobControl jobControl) throws Exception
    {
        WorkflowEngine workflowEngine = new WorkflowEngine();
        workflowEngine.setLogger( log );
        workflowEngine.setWorkflow( workflow );
        workflowEngine.setSkipCompleted( skipCompleted );
        workflowEngine.setIgnoreFail( ignoreFail );
        workflowEngine.setParameters( parameters );
        workflowEngine.setJobControl( jobControl );
        workflowEngine.initWorkflow();
        return workflowEngine;
    }

    public static class WorkflowEngineAdapter extends FunctionJobControl
    {
        private AnalysisJobControl primary;
        public WorkflowEngineAdapter(AnalysisJobControl primaryJobControl)
        {
            super( null );
            this.primary = primaryJobControl;
        }

        @Override
        public int getStatus()
        {
            return primary.getStatus();
        }

        @Override
        public int getPreparedness()
        {
            return primary.getPreparedness();
        }
        @Override
        public void setPreparedness(int percent)
        {
            primary.setPreparedness( percent );
        }

        @Override
        public void resultsAreReady(Object[] results)
        {
            primary.resultsAreReady( results );
        }

        @Override
        public void functionStarted(String msg)
        {
            primary.begin();
        }

        @Override
        public void functionFinished(String msg)
        {
            primary.end();
        }

        @Override
        public void functionTerminatedByError(Throwable t)
        {
            JobControlException jce = new JobControlException( t );
            primary.exceptionOccured(jce);
            primary.end(jce);
        }
    }

    public class SubWorkflowJobControl extends AnalysisJobControl
    {
        public SubWorkflowJobControl()
        {
            super( RunWorkflowAnalysis.this );
        }

        @Override
        public void run()
        {
            begin();
            try
            {
                doRun();
            }
            catch( JobControlException ex )
            {
                exceptionOccured(ex);
                end(ex);
            }
        }

        @Override
        protected void doRun() throws JobControlException
        {
            try
            {
               method.justAnalyzeAndPut();
            }
            catch( Throwable e )
            {
                LoggedException buex = ExceptionRegistry.translateException( e );
                method.getLogger().log(Level.SEVERE, buex.log());
                throw new JobControlException(buex);
            }
        }
    }


    @SuppressWarnings ( "serial" )
    public static class RunWorkflowParameters extends AbstractAnalysisParameters
    {
        private DataElementPath workflowPath;
        private Diagram workflow;
        private DynamicPropertySet workflowProperties;
        private boolean ignoreFail;
        private boolean skipCompleted;

        @PropertyName ( "Workflow Path" )
        @PropertyDescription ( "Path to nested workflow" )
        public DataElementPath getWorkflowPath()
        {
            return workflowPath;
        }
        public void setWorkflowPath(DataElementPath workflowPath)
        {
            DataElementPath oldPath = this.workflowPath;
            this.workflowPath = workflowPath;
            workflow = workflowPath.optDataElement( Diagram.class );
            if( workflow != null )
            {
                try
                {
                    workflow = workflow.clone( workflow.getOrigin(), workflow.getName() );
                    DynamicPropertySet workflowParameters = WorkflowItemFactory.getWorkflowParameters( workflow );
                    setWorkflowProperties( workflowParameters );
                }
                catch( Exception e )
                {
                    throw new RuntimeException( e );
                }
            }
            firePropertyChange( "workflowPath", oldPath, workflowPath );
        }

        Diagram getWorkflow()
        {
            return workflow;
        }

        @PropertyName ( "Workflow Parameters" )
        @PropertyDescription ( "Parameters of the nested workflow" )
        public DynamicPropertySet getWorkflowProperties()
        {
            return workflowProperties;
        }
        public void setWorkflowProperties(DynamicPropertySet workflowProperties)
        {
            DynamicPropertySet oldProperties = this.workflowProperties;
            this.workflowProperties = workflowProperties;
            firePropertyChange( "workflowProperties", oldProperties, workflowProperties );
        }

        @Override
        public void write(Properties properties, String prefix)
        {
            properties.put( prefix + "workflowPath", TextUtil.toString( workflowPath ) );
            properties.put( prefix + "ignoreFail", TextUtil.toString( ignoreFail ) );
            if(workflowProperties != null)
                BeanUtil.writeBeanToProperties( workflowProperties, properties, prefix + "workflow." );
        }

        @Override
        public void read(Properties properties, String prefix)
        {
            if(!properties.containsKey( prefix + "workflowPath" ))
                return;
            DataElementPath path = (DataElementPath)TextUtil.fromString( DataElementPath.class, properties.getProperty( prefix + "workflowPath" ) );
            if( properties.containsKey( prefix + "ignoreFail" ) )
                setIgnoreFail( (Boolean)TextUtil.fromString( Boolean.class, properties.getProperty( prefix + "ignoreFail" ) ) );
            setWorkflowPath( path );
            if(workflowProperties != null)
                BeanUtil.readBeanFromProperties( workflowProperties, properties, prefix + "workflow." );
        }

        @Override
        public String[] getInputNames()
        {
            return StreamEx.of( super.getInputNames() ).remove( name->name.equals( "workflowPath" ) ).toArray( String[]::new );
        }

        @PropertyName ( "Ignore failed steps" )
        @PropertyDescription ( "Ignore failed analysis and execute available ones" )
        public boolean isIgnoreFail()
        {
            return ignoreFail;
        }
        public void setIgnoreFail(boolean ignoreFail)
        {
            this.ignoreFail = ignoreFail;
        }

        @PropertyName ( "Skip completed steps" )
        public boolean isSkipCompleted()
        {
            return skipCompleted;
        }
        public void setSkipCompleted(boolean skipCompleted)
        {
            Object oldValue = this.skipCompleted;
            this.skipCompleted = skipCompleted;
            firePropertyChange( "skipCompleted", oldValue, skipCompleted );
        }
    }

    public static class RunWorkflowParametersBeanInfo extends BeanInfoEx2<RunWorkflowParameters>
    {
        public RunWorkflowParametersBeanInfo()
        {
            super( RunWorkflowParameters.class );
        }
        @Override
        protected void initProperties() throws Exception
        {
            property( "workflowPath" ).inputElement( Diagram.class ).add();
            add( "workflowProperties" );
            addExpert( "ignoreFail" );
            addExpert( "skipCompleted" );
        }
    }

}

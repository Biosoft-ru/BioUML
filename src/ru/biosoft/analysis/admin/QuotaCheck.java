package ru.biosoft.analysis.admin;

import java.util.Arrays;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.TextUtil;

@ClassIcon ( "resources/check-quotas.png" )
public class QuotaCheck extends AnalysisMethodSupport<QuotaCheck.QuotaCheckParameters>
{
    public QuotaCheck(DataCollection<?> origin, String name)
    {
        super( origin, name, new QuotaCheckParameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        String[] projects = parameters.getProjects();
        if(projects == null || projects.length == 0)
        {
            log.warning("No projects were selected, exiting");
            return null;
        }
        jobControl.forCollection(Arrays.asList(projects), projectName -> {
            try
            {
                DataCollection<? extends DataElement> project = CollectionFactoryUtils.getUserProjectsPath().getChildPath(projectName)
                        .getChildPath("Data").optDataCollection();
                if(project == null)
                {
                    log.warning( "Project " + projectName + ": not found; skipping" );
                    return true;
                }
                log.info( "Project " + projectName + ": checking quota..." );

                DataCollection<?> primaryCollection = (DataCollection<?>)SecurityManager
                        .runPrivileged( () -> DataCollectionUtils.fetchPrimaryCollectionPrivileged( project ) );

                GenericDataCollection gdc = primaryCollection.cast( GenericDataCollection.class );
                long currentSize = gdc.getDiskSize();
                long newSize = gdc.recalculateSize();
                if( currentSize == newSize )
                    log.info( "Size correct: " + TextUtil.formatSize( newSize ) );
                else
                    log.warning( "Size corrected: " + TextUtil.formatSize( currentSize ) + " -> " + TextUtil.formatSize( newSize ) );
            }
            catch( Throwable t )
            {
                log.warning("Error: "+ExceptionRegistry.log(t));
            }
            return true;
        });
        return null;
    }

    public static class QuotaCheckParameters extends AbstractAnalysisParameters
    {
        String[] projects = new String[0];

        @PropertyName("Projects to check")
        @PropertyDescription("List of projects for which quota will be checked")
        public String[] getProjects()
        {
            return projects;
        }
        public void setProjects(String[] projects)
        {
            Object oldValue = this.projects;
            this.projects = projects;
            firePropertyChange("projects", oldValue, projects);
        }
    }

    public static class QuotaCheckParametersBeanInfo extends BeanInfoEx
    {
        public QuotaCheckParametersBeanInfo()
        {
            super( QuotaCheckParameters.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("projects", ProjectsSelector.class);
        }
    }
}

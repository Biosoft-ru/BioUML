package ru.biosoft.analysis.admin;

import java.sql.Connection;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlDataElement;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.TextUtil;

/**
 * @author lan
 *
 */
public class CleanUpSqlDatabase extends AnalysisMethodSupport<CleanUpSqlDatabase.CleanUpSqlDatabaseParameters>
{
    public CleanUpSqlDatabase(DataCollection<?> origin, String name)
    {
        super(origin, name, new CleanUpSqlDatabaseParameters());
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
                    log.warning("Project "+projectName+": not found; skipping");
                    return true;
                }
                Connection connection = SqlConnectionPool.getConnection(project);
                if(connection == null)
                {
                    log.warning("Project "+projectName+": unable to obtain project SQL connection; skipping");
                    return true;
                }
                log.info("Project "+projectName+" ("+connection+"): gathering statistics...");
                Set<String> usedTables = new HashSet<>();
                int steps = 0;
                Deque<ru.biosoft.access.core.DataElementPath> paths = new ArrayDeque<>();
                paths.add(project.getCompletePath());
                while(!paths.isEmpty())
                {
                    DataElementPath collection = paths.pop();
                    for(DataElementPath child: collection.getChildren())
                    {
                        Class<? extends DataElement> type = DataCollectionUtils.getElementType(child);
                        if(SqlDataElement.class.isAssignableFrom(type))
                        {
                            SqlDataElement element = child.optDataElement(SqlDataElement.class);
                            if(element == null)
                            {
                                log.warning("Cannot get "+child+"; skipping...");
                            } else
                            {
                                usedTables.addAll(Arrays.asList(element.getUsedTables()));
                                steps++;
                                if((steps % 100) == 0)
                                    log.info(usedTables.size()+"/"+paths.size()+"...");
                            }
                        } else if(FolderCollection.class.isAssignableFrom(type))
                        {
                            paths.add(child);
                        }
                        if(jobControl.isStopped()) return false;
                    }
                    System.gc();
                }
                List<String> orphanedTables = SqlUtil.stringStream( connection, "SHOW TABLES" ).remove( usedTables::contains ).sorted()
                        .toList();
                long usedSpace = 0;
                long wasteSpace = 0;
                log.info("Getting table sizes...");
                for(String table1: usedTables)
                    usedSpace+=SqlUtil.getTableSize(connection, table1);
                for(String table2: orphanedTables)
                {
                    long tableSize = SqlUtil.getTableSize(connection, table2);
                    wasteSpace+=tableSize;
                    if(parameters.isVerbose())
                        log.info( "Orphaned: " + table2 + "; size: " + TextUtil.formatSize( tableSize ) );
                    if(jobControl.isStopped()) return false;
                }
                log.info( "Used tables: " + usedTables.size() + "; size: " + TextUtil.formatSize( usedSpace ) );
                log.info( "Orphaned tables: " + orphanedTables.size() + "; size: " + TextUtil.formatSize( wasteSpace ) );
                if(jobControl.isStopped()) return false;
                if(parameters.isActualCleanUp())
                {
                    for(String table3: orphanedTables)
                    {
                        SqlUtil.dropTable(connection, table3);
                        if(jobControl.isStopped()) return false;
                    }
                }
            }
            catch( Throwable t )
            {
                log.warning("Error: "+ExceptionRegistry.log(t));
            }
            return true;
        });
        return null;
    }

    public static class CleanUpSqlDatabaseParameters extends AbstractAnalysisParameters
    {
        String[] projects = new String[0];
        boolean actualCleanUp = true;
        boolean verbose = true;

        @PropertyName("Projects to clean up")
        @PropertyDescription("List of projects which will SQL databases will be checked for orphaned tables")
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

        @PropertyName("Perform clean up")
        @PropertyDescription("Whether to actually drop orphaned tables or just to gather statistics")
        public boolean isActualCleanUp()
        {
            return actualCleanUp;
        }
        public void setActualCleanUp(boolean actualCleanUp)
        {
            Object oldValue = this.actualCleanUp;
            this.actualCleanUp = actualCleanUp;
            firePropertyChange("actualCleanUp", oldValue, actualCleanUp);
        }

        @PropertyName("Print table names")
        @PropertyDescription("Whether to print orphaned table names")
        public boolean isVerbose()
        {
            return verbose;
        }
        public void setVerbose(boolean verbose)
        {
            Object oldValue = this.verbose;
            this.verbose = verbose;
            firePropertyChange("verbose", oldValue, verbose);
        }
    }

    public static class CleanUpSqlDatabaseParametersBeanInfo extends BeanInfoEx
    {
        public CleanUpSqlDatabaseParametersBeanInfo()
        {
            super( CleanUpSqlDatabaseParameters.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("projects", ProjectsSelector.class);
            add("verbose");
            add("actualCleanUp");
        }
    }
}

package biouml.plugins.research.web;

import java.sql.Connection;
import java.util.Locale;
import java.util.Properties;

import biouml.plugins.research.ResearchBuilder;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.Repository;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.history.HistoryFacade;
import ru.biosoft.access.security.GlobalDatabaseManager;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.bean.BeanInfoEx2;

@SuppressWarnings ( "serial" )
public class CustomProjectAnalysis extends AnalysisMethodSupport<CustomProjectAnalysis.CustomProjectAnalysisParameters>
{
    public CustomProjectAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new CustomProjectAnalysisParameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        String dbProjectName = parameters.getDatabaseName();
        String dbUser = parameters.getDatabaseUser();
        String dbPassword = parameters.getDatabasePassword();
        
        Connection rootConnection = GlobalDatabaseManager.getDatabaseConnection();

        String jdbcUrl = GlobalDatabaseManager.getCurrentDBUrl() + "/" + dbProjectName + "?allowLoadLocalInfile=true";
        Properties props = new Properties();
        props.setProperty(SqlDataCollection.JDBC_DRIVER_PROPERTY, SqlDataCollection.JDBC_DEFAULT_DRIVER);
        props.setProperty(SqlDataCollection.JDBC_URL_PROPERTY, jdbcUrl);
        props.setProperty(SqlDataCollection.JDBC_USER_PROPERTY, dbUser);
        props.setProperty(SqlDataCollection.JDBC_PASSWORD_PROPERTY, dbPassword);
        props.setProperty(GenericDataCollection.PREFERED_TABLE_IMPLEMENTATION_PROPERTY, "SQL");
        
        String historyCollection = Application.getGlobalValue("DefaultHistoryCollection", null);
        if(historyCollection != null)
            props.setProperty(HistoryFacade.HISTORY_COLLECTION, historyCollection);

        SqlUtil.createDatabase(rootConnection, dbProjectName, dbUser, dbPassword);

        ResearchBuilder researchBuilder = new ResearchBuilder(props);
        return researchBuilder.createResearch( parameters.getRepository().getDataElement( Repository.class ), parameters.getName(), true );
    }

    @Override
    protected void writeProperties(DataElement de) throws Exception
    {
        // do not write properties in this analysis
    }

    public static class CustomProjectAnalysisParameters extends AbstractAnalysisParameters
    {
        DataElementPath repository;
        String name;
        String databaseName;
        String databaseUser = "example";
        String databasePassword = "example";

        @PropertyName ( "Repository" )
        public DataElementPath getRepository()
        {
            return repository;
        }
        public void setRepository(DataElementPath repository)
        {
            Object oldValue = this.repository;
            this.repository = repository;
            firePropertyChange( "repository", oldValue, this.repository );
        }

        @PropertyName ( "New project name" )
        public String getName()
        {
            return name;
        }
        public void setName(String name)
        {
            Object oldValue = this.name;
            this.name = name.trim();
            firePropertyChange( "name", oldValue, this.name );
        }

        @PropertyName ( "Database name" )
        public String getDatabaseName()
        {
            return databaseName;
        }
        public void setDatabaseName(String databaseName)
        {
            Object oldValue = this.databaseName;
            this.databaseName = databaseName;
            firePropertyChange( "databaseName", oldValue, this.databaseName );
        }

        @PropertyName ( "Database user" )
        public String getDatabaseUser()
        {
            return databaseUser;
        }
        public void setDatabaseUser(String databaseUser)
        {
            Object oldValue = this.databaseUser;
            this.databaseUser = databaseUser;
            firePropertyChange( "databaseUser", oldValue, this.databaseUser );
        }

        @PropertyName ( "Database password" )
        public String getDatabasePassword()
        {
            return databasePassword;
        }
        public void setDatabasePassword(String databasePassword)
        {
            Object oldValue = this.databasePassword;
            this.databasePassword = databasePassword;
            firePropertyChange( "databasePassword", oldValue, this.databasePassword );
        }
        
        public String getDatabaseNameTemplate()
        {
            String res = "example_" + getName().toLowerCase( Locale.ENGLISH ).replaceAll( "[@\\.\\-\\ ]+", "_" ).replaceAll( "\\W", "" );
            return res.length() > 56 ? res.substring( 0, 56 ) : res;
        }
    }

    public static class CustomProjectAnalysisParametersBeanInfo extends BeanInfoEx2<CustomProjectAnalysisParameters>
    {
        public CustomProjectAnalysisParametersBeanInfo()
        {
            super( CustomProjectAnalysisParameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "repository" ).inputElement( Repository.class ).add();
            property( "name" ).add();
            property( new PropertyDescriptorEx( "databaseNameTemplate", beanClass, "getDatabaseNameTemplate", null ) ).hidden().add();
            property( "databaseName" ).auto( "$databaseNameTemplate$" ).add();
            property( "databaseUser" ).add();
            property( "databasePassword" ).add();
        }
    }
}

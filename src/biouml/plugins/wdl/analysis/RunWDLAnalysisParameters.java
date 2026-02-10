package biouml.plugins.wdl.analysis;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.StringReader;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.plugins.wdl.WorkflowSettings;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.parser.AstStart;
import biouml.plugins.wdl.parser.WDLParser;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

public class RunWDLAnalysisParameters extends AbstractAnalysisParameters implements PropertyChangeListener
{
    private DataElementPath wdlPath;
    private WorkflowSettings settings = new WorkflowSettings();

    public WorkflowSettings getSettings()
    {
        return settings;
    }
    
    @PropertyName ( "Parameters" )
    public DynamicPropertySet getParameters()
    {
        return settings.getParameters();
    }

    public void setParameters(DynamicPropertySet parameters)
    {
        Object oldValue = settings.getParameters();
        settings.setParameters( parameters );
        firePropertyChange( "parameters", oldValue, parameters );
    }

    @PropertyName ( "Output" )
    public DataElementPath getOutputPath()
    {
        return settings.getOutputPath();
    }

    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = settings.getOutputPath();
        settings.setOutputPath( outputPath );
        firePropertyChange( "outputPath", oldValue, outputPath );
    }

    @PropertyName ( "WDL" )
    public DataElementPath getWdlPath()
    {
        return wdlPath;
    }

    public void setWdlPath(DataElementPath wdlPath)
    {
        DataElementPath oldValue = this.wdlPath;
        this.wdlPath = wdlPath;
        firePropertyChange( "wdlPath", oldValue, wdlPath );
    }

    @PropertyName ( "Parameters Json" )
    public DataElementPath getJsonPath()
    {
        return settings.getJson();
    }

    public void setJsonPath(DataElementPath jsonPath)
    {
        DataElementPath oldValue = settings.getJson();
        settings.setJson( jsonPath );
        firePropertyChange( "jsonPath", oldValue, jsonPath );
    }

    @PropertyName ( "Use Json" )
    public boolean isUseJson()
    {
        return settings.isUseJson();
    }

    public boolean isNotUseJson()
    {
        return !isUseJson();
    }
    public void setUseJson(boolean useJson)
    {
        boolean oldValue = settings.isUseJson();
        settings.setUseJson( useJson );
        firePropertyChange( "useJson", oldValue, useJson );
    }

    public void reloadParameters(String wdl) throws Exception
    {
        AstStart start = new WDLParser().parse( new StringReader( wdl ) );
        Diagram diagram = new WDLImporter().generateDiagram( start, null, "analysisDiagram" );
        settings.initParameters( diagram );
        firePropertyChange( "*", null, null );
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // TODO Auto-generated method stub
    }
}
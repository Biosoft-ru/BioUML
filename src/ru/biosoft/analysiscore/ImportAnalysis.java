package ru.biosoft.analysiscore;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.DataElementImporterRegistry;
import ru.biosoft.access.DataElementImporterRegistry.ImporterInfo;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.application.Application;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;

@ClassIcon("resources/import-analysis.gif")
public class ImportAnalysis extends AnalysisMethodSupport<ImportAnalysisParameters>
{
    private final ImporterInfo importerInfo;
    private final DataElementImporter importer;

    public ImportAnalysis(DataCollection<?> parent, String name)
    {
        super(parent, name, new ImportAnalysisParameters(DataElementImporterRegistry.getImporterInfo(name)));
        this.importerInfo = DataElementImporterRegistry.getImporterInfo(name);
        this.importer = importerInfo.cloneImporter();
    }

    @Override
    public String getDescription()
    {
        return importerInfo.getDescription();
    }

    @Override
    public void setDescription(String description)
    {
        // Ignore: description is read from importer
    }

    @Override
    public void setParameters(AnalysisParameters parameters)
    {
        if(parameters instanceof ImportAnalysisParameters && ((ImportAnalysisParameters)parameters).getImporterInfo() == importerInfo)
        {
            this.parameters = (ImportAnalysisParameters)parameters;
        }
    }

    @Override
    public String getName()
    {
        return importerInfo.getFormat();
    }

    @Override
    public DataElement justAnalyzeAndPut() throws Exception
    {
        validateParameters();
        FunctionJobControl fjc = new FunctionJobControl( log, new JobControlListenerAdapter()
        {
            @Override
            public void valueChanged(JobControlEvent event)
            {
                jobControl.setPreparedness(event.getPreparedness());
            }
        });
        Application.getPreferences().addValue(DataElementImporter.PREFERENCES_IMPORT_DIRECTORY, parameters.getFile().getParent(),
                DataElementImporter.PN_PREFERENCES_IMPORT_DIRECTORY, DataElementImporter.PD_PREFERENCES_IMPORT_DIRECTORY);
        copyProperties();
        DataElement de = importer.doImport(parameters.getResultPath().getParentCollection(), parameters.getFile(), parameters.getResultPath().getName(), fjc, log);
        return de;
    }

    private void copyProperties()
    {
        Object newProperties = parameters.getProperties();
        Object properties = importer.getProperties(parameters.getResultPath().getParentCollection(), parameters.getFile(), parameters.getResultPath().getName());
        if(properties != null && newProperties != null)
        {
            ComponentModel newModel = ComponentFactory.getModel(newProperties);
            ComponentModel model = ComponentFactory.getModel(properties);
            for(int i=0; i<newModel.getPropertyCount(); i++)
            {
                Property newProperty = newModel.getPropertyAt(i);
                if(newProperty == null) continue;
                Property property = model.findProperty(newProperty.getCompleteName());
                if(property == null || !property.getValueClass().equals(newProperty.getValueClass())) continue;
                try
                {
                    property.setValue(newProperty.getValue());
                }
                catch(Exception e)
                {
                    log.log(Level.WARNING, "Setting importer property failed: "+newProperty.getCompleteName()+":", e);
                }
            }
        }
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths(new String[0], new String[] {"resultPath"});
        checkNotEmpty("file");
        DataElementPath path = parameters.getResultPath();
        DataCollection<?> parent = path.optParentCollection();
        if(importer.accept(parent, null) <= DataElementImporter.ACCEPT_UNSUPPORTED)
            throw new IllegalArgumentException("Specified output collection is unsupported by importer");
        if(importer.accept(parent, parameters.getFile()) <= DataElementImporter.ACCEPT_UNSUPPORTED)
            throw new IllegalArgumentException("Specified file is unsupported by importer");
    }

    @Override
    public Map<String, String> generateScripts(AnalysisParameters parameters)
    {
        return Collections.emptyMap();
    }
}

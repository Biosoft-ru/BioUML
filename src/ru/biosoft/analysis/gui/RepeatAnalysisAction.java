package ru.biosoft.analysis.gui;

import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.AbstractElementAction;
import ru.biosoft.analysis.document.AnalysisDocument;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.GUI;

@SuppressWarnings ( "serial" )
public class RepeatAnalysisAction extends AbstractElementAction
{
    @Override
    protected void performAction(DataElement de) throws Exception
    {
        DataCollection<?> dc = (DataCollection<?>)de;
        Properties prop = dc.getInfo().getProperties();
        AnalysisMethodInfo methodInfo = AnalysisMethodRegistry.getMethodInfo( prop.getProperty("analysisName") );
        AnalysisParameters anPar = AnalysisParametersFactory.read(dc);
        AnalysisDocument analysisDocument = new AnalysisDocument(methodInfo, anPar);
        for (Document document : GUI.getManager().getDocuments())
        {
            if(!(document instanceof AnalysisDocument))
                continue;
            AnalysisDocument doc = (AnalysisDocument)document;
            if (doc.getDisplayName().equals(methodInfo.getName()))
            {
                GUI.getManager().replaceDocument( doc, analysisDocument );
                return;
            }
        }
        GUI.getManager().addDocument(analysisDocument);
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        if(!(de instanceof DataCollection)) return false;
        Properties properties = ( (DataCollection<?>)de ).getInfo().getProperties();
        return properties.containsKey("analysisName");
    }
}

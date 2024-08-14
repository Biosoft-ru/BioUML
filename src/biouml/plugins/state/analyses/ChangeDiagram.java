package biouml.plugins.state.analyses;

import javax.swing.undo.UndoableEdit;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import biouml.model.Diagram;

public class ChangeDiagram extends AnalysisMethodSupport<ChangeDiagramParameters>
{
    public ChangeDiagram(DataCollection<?> origin, String name)
    {
        super( origin, name, new ChangeDiagramParameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        Diagram inputDiagram = parameters.getDiagramPath().getDataElement( Diagram.class );
        Diagram outputDiagram = inputDiagram.clone( parameters.getOutputDiagram().getParentCollection(), parameters.getOutputDiagram().getName() );
        for( StateChange change : parameters.getChanges() )
        {
            UndoableEdit edit = CreateStateAnalysis.createEdit( outputDiagram, change );
            edit.undo();
            edit.redo();
        }
        parameters.getOutputDiagram().save( outputDiagram );
        return outputDiagram;
    }
}

package biouml.plugins.fbc.analysis;

import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.Diagram;

public class FbcAnalysisParametersBeanInfo extends BeanInfoEx2<FbcAnalysisParameters>
{
    public FbcAnalysisParametersBeanInfo()
    {
        super(FbcAnalysisParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "diagramPath" ).inputElement( Diagram.class ).add();
        property( "fbcDataTablePath" ).inputElement( TableDataCollection.class ).add();
        property( "fbcResultPath" ).outputElement( TableDataCollection.class ).add();

        property( "typeObjectiveFunction" ).tags( FbcAnalysisParameters.OBJECTIVE_TYPES ).expert().add();
        property( "solverType" ).tags( FbcAnalysisParameters.AVAILABLE_SOLVER_TYPES ).expert().add();
        property( "maxIter" ).expert().hidden( "isGurobiSolver" ).add();
    }
}

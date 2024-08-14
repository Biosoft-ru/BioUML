package biouml.plugins.fbc.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.DPSUtils;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.plugins.fbc.FbcConstant;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxBounds;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxObjFunc;
import biouml.plugins.sbml.SbmlDiagramType_L3v1;
import biouml.standard.diagram.DiagramUtility;

public class ReconTransformerAnalysis extends AnalysisMethodSupport<ReconTransformerParameters> implements FbcConstant
{
    public ReconTransformerAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new ReconTransformerParameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        Diagram diagram = parameters.getDiagramPath().getDataElement( Diagram.class );
        DataElementPath resultPath = parameters.getResultPath();

        DataCollection<DataElement> origin = resultPath.getParentCollection();
        String name = resultPath.getName();

        diagram = diagram.clone( origin, name );
        diagram.setType( new SbmlDiagramType_L3v1() );
        EModel emodel = diagram.getRole( EModel.class );
        List<Node> reactionList = DiagramUtility.getReactionNodes( diagram );

        DynamicProperty dp = new DynamicProperty( FBC_ACTIVE_OBJECTIVE, String.class, "OBJF" );
        DPSUtils.makeTransient( dp );
        diagram.getAttributes().add( dp );

        Map<String, String> listObj = new HashMap<>();
        listObj.put( "OBJF", "maximize" );
        dp = new DynamicProperty( FBC_LIST_OBJECTIVES, Map.class, listObj );
        DPSUtils.makeTransient( dp );
        diagram.getAttributes().add( dp );

        dp = new DynamicProperty( "Packages", String[].class, new String[] {"fbc"} );
        DPSUtils.makeTransient( dp );
        diagram.getAttributes().add( dp );

        for( Node reactionNode : reactionList )
        {
            FluxBounds bounds = new FluxBounds();
            String reactionName = reactionNode.getName();
            double lb = emodel.getVariable( reactionName + "_LOWER_BOUND" ).getInitialValue();
            double ub = emodel.getVariable( reactionName + "_UPPER_BOUND" ).getInitialValue();
            double objCoef = emodel.getVariable( reactionName + "_OBJECTIVE_COEFFICIENT" ).getInitialValue();
            bounds.addBound( /*reactionName + "_U", "",*/ FBC_LESS_EQUAL, String.valueOf(ub) );
            bounds.addBound( /*reactionName + "_L", "", */FBC_GREATER_EQUAL, String.valueOf(lb) );

            dp = new DynamicProperty( FBC_BOUNDS, FluxBounds.class, bounds );
            DPSUtils.makeTransient( dp );
            reactionNode.getAttributes().add( dp );

            if( objCoef == 0 )
                continue;
            FluxObjFunc objFunc = new FluxObjFunc();
            objFunc.addObjectiveCoefficient( "", "", "OBJF", "OBJF", objCoef );

            dp = new DynamicProperty( FBC_OBJECTIVES, FluxObjFunc.class, objFunc );
            DPSUtils.makeTransient( dp );
            reactionNode.getAttributes().add( dp );
        }

        origin.remove( name );
        diagram.save();

        return diagram;
    }

}

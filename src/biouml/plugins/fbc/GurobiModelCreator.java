package biouml.plugins.fbc;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.exception.BiosoftCustomException;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxBounds;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxObjFunc;
import biouml.standard.diagram.DiagramUtility;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

public class GurobiModelCreator extends FbcModelCreator
{
    private Diagram diagram;
    private List<Node> reactionsNode;

    private Map<String, Double> parameterValues;
    private List<FluxInfo> fluxes;
    private int goalType;

    @Override
    public FbcModel createModel(Diagram diagram, TableDataCollection fbcData, String typeObjectiveFunction)
    {
        diagram = prepareDiagram( diagram );
        this.diagram = diagram;
        reactionsNode = DiagramUtility.getReactionNodes( diagram );
        goalType = getObjectiveFunction( typeObjectiveFunction );

        parameterValues = new HashMap<>();
        if( fbcData != null )
            fluxes = getFluxes( fbcData );
        else
            fluxes = getFluxes();

        if( diagram.getRole() instanceof EModel )
        {
            EModel emodel = diagram.getRole( EModel.class );
            for( FluxInfo info : fluxes )
            {
                Variable var = info.lowerBound.isEmpty() ? null : emodel.getVariable( info.lowerBound );
                if( var != null )
                    parameterValues.put( var.getName(), var.getInitialValue() );
                var = info.upperBound.isEmpty() ? null : emodel.getVariable( info.upperBound );
                if( var != null )
                    parameterValues.put( var.getName(), var.getInitialValue() );
            }
        }

        return initModel();
    }

    @Override
    public FbcModel createModel(Diagram diagram)
    {
        DynamicPropertySet dps = diagram.getAttributes();
        String activObj = dps.getValueAsString( FBC_ACTIVE_OBJECTIVE );
        String objType = ( (HashMap<String, String>)dps.getValue( FBC_LIST_OBJECTIVES ) ).get( activObj );
        return createModel( diagram, null, objType );
    }

    private FbcModel initModel()
    {
        try
        {
            GRBEnv env = new GRBEnv();
            GRBModel model = new GRBModel( env );

            ArrayList<GRBVar> vars = initVariables( model );
            initConstraints( vars, model );

            return new GurobiModel( model, StreamEx.of( reactionsNode ).map( Node::getName ).toArray( String[]::new ) );
        }
        catch( GRBException e )
        {
            throw new BiosoftCustomException( e, "Internal error occurred in Gurobi solver." );
        }
    }

    private int getObjectiveFunction(String objType)
    {
        if( MAX.equals( objType ) )
            return GRB.MAXIMIZE;
        else if( MIN.equals( objType ) )
            return GRB.MINIMIZE;
        else
            throw new IllegalArgumentException( "Type objective function is incorrect" );
    }

    private ArrayList<GRBVar> initVariables(GRBModel model) throws GRBException
    {
        ArrayList<GRBVar> vars = new ArrayList<>();
        ArrayList<Double> coefficients = new ArrayList<>();

        for( FluxInfo info : fluxes )
        {
            coefficients.add( info.coef );
            vars.add( info.createVar( model ) );
        }

        model.update();
        GRBLinExpr expr = new GRBLinExpr();
        EntryStream.zip( coefficients, vars ).removeKeys( coef -> coef == 0 ).forKeyValue( expr::addTerm );
        model.setObjective( expr, goalType );
        return vars;
    }

    private void initConstraints(ArrayList<GRBVar> vars, GRBModel model) throws GRBException
    {
        if( ! ( diagram.getRole() instanceof EModel ) )
            return;

        EModel emodel = diagram.getRole( EModel.class );
        DataCollection<VariableRole> varCollection = emodel.getVariableRoles();
        int num = 0;
        for( Node de : StreamEx.of( varCollection.stream() ).remove( varRole -> varRole.isBoundaryCondition() || varRole.isConstant() )
                .map( VariableRole::getDiagramElement ).select( Node.class ) )
        {
            GRBLinExpr expr = new GRBLinExpr();
            EntryStream.zip( reactionsNode, vars ).mapKeys( node -> ApacheModelCreator.getStochiometry( de, node ) )
                    .removeKeys( stoichiometry -> stoichiometry == 0.0 ).forKeyValue( expr::addTerm );
            try
            {
                model.addConstr( expr, GRB.EQUAL, 0.0, "c" + ( num++ ) );
            }
            catch( GRBException e )
            {
                throw new GRBException( "Can't add constraint to model" );
            }
        }
    }

    @Override
    public String toString()
    {
        return "Gurobi solver";
    }

    @Override
    public FbcModel getUpdatedModel(Map<String, Double> values)
    {
        for( String param : parameterValues.keySet() )
        {
            Double newValue = values.get( param );
            if( newValue != null )
                parameterValues.put( param, newValue );
        }
        return initModel();
    }

    private List<FluxInfo> getFluxes()
    {
        List<FluxInfo> fluxes = new ArrayList<>();

        for( Node node : reactionsNode )
        {
            DynamicPropertySet dps = node.getAttributes();
            DynamicProperty dp = dps.getProperty( FBC_BOUNDS );
            if( dp == null )
                continue;
            FluxBounds fluxBounds = (FluxBounds)dps.getValue( FBC_BOUNDS );
            String upperBound = "", equal = "", lowerBound = "";
            double coefficient = 0;
            for( int i = 0; i < fluxBounds.sign.size(); i++ )
            {
                if( fluxBounds.sign.get( i ).equals( FBC_LESS_EQUAL ) )
                    upperBound = fluxBounds.value.get( i );
                else if( fluxBounds.sign.get( i ).equals( FBC_GREATER_EQUAL ) )
                    lowerBound = fluxBounds.value.get( i );
                else if( fluxBounds.sign.get( i ).equals( FBC_EQUAL ) )
                    equal = fluxBounds.value.get( i );
            }
            dp = dps.getProperty( FBC_OBJECTIVES );
            String activObj = diagram.getAttributes().getValueAsString( FBC_ACTIVE_OBJECTIVE );
            if( dp != null )
            {
                FluxObjFunc fluxObj = (FluxObjFunc)dp.getValue();
                int index = fluxObj.idObj.indexOf( activObj );
                if( index != -1 )
                    coefficient = fluxObj.coefficient.get( index );
            }
            fluxes.add( new FluxInfo( node.getName(), lowerBound, upperBound, equal, coefficient ) );
        }

        return fluxes;
    }
    private List<FluxInfo> getFluxes(TableDataCollection fbcData)
    {
        List<FluxInfo> fluxes = new ArrayList<>();

        for( Node node : reactionsNode )
        {
            String name = node.getName();
            try
            {
                RowDataElement rowDataElement = fbcData.get( name );
                String upperObj = rowDataElement.getValueAsString( "Less" );
                String equalObj = rowDataElement.getValueAsString( "Equal" );
                String lowerObj = rowDataElement.getValueAsString( "Greater" );
                fluxes.add( new FluxInfo( name, lowerObj, upperObj, equalObj, Double.parseDouble( rowDataElement
                        .getValueAsString( "Coefficient Objective Function" ) ) ) );
            }
            catch( Exception e )
            {
                throw new IllegalArgumentException( "Data table is incorrect, can't add element " + name );
            }
        }
        return fluxes;
    }

    public class FluxInfo
    {
        final String upperBound;
        final String lowerBound;
        final String reactionName;
        final double coef;

        public FluxInfo(String reactionName, String lowerBound, String upperBound, String eqObj, double coef)
        {
            this.reactionName = reactionName;
            if( eqObj.isEmpty() )
            {
                this.lowerBound = lowerBound;
                this.upperBound = upperBound;
            }
            else
            {
                this.lowerBound = eqObj;
                this.upperBound = eqObj;
            }
            this.coef = coef;
        }

        public GRBVar createVar(GRBModel model) throws GRBException
        {
            Double lowerBoundValue = getValue( lowerBound );
            Double upperBoundValue = getValue( upperBound );
            return model.addVar( lowerBoundValue == null ? -GRB.INFINITY : lowerBoundValue, upperBoundValue == null ? GRB.INFINITY
                    : upperBoundValue, 0.0, GRB.CONTINUOUS, reactionName );
        }
    }

    protected Double getValue(String str)
    {
        try
        {
            return Double.valueOf( str );
        }
        catch( NumberFormatException e )
        {
            Double value = parameterValues.get( str );
            return value != null ? value : null;
        }
    }

}

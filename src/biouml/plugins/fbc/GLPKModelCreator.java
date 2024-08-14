package biouml.plugins.fbc;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.glp_prob;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.Preferences;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxObjFunc;
import biouml.plugins.fbc.table.FbcBuilderDataTableAnalysis;
import biouml.plugins.sbml.SbmlModelReader;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.SpecieReference;
import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;

public class GLPKModelCreator extends FbcModelCreator
{
    protected Logger log = Logger.getLogger( GLPKModelCreator.class.getName() );
    private glp_prob problem;
    private String[] reactionNames;
    private Map<String, Double> constraintValues;
    private TableDataCollection fbcData;

    public GLPKModelCreator() throws IOException
    {
        //library path should be loaded in launcher
        //In case property was changed, try to load it again here
        Preferences preferences = Application.getPreferences();
        String path = preferences.getStringValue( "JAVA_LIBRARY_PATH", "" );
        ClassLoading.addJavaLibraryPath( path );
        //test whether glpk library was successfully loaded 
        GLPK.glp_version();
    }

    public GLPKModelCreator(String javaPath) throws IOException
    {
        ClassLoading.addJavaLibraryPath( javaPath );
    }

    @Override
    public FbcModel createModel(Diagram diagram, TableDataCollection fbcData, String typeObjectiveFunction)
    {
        log.info( "GLPK version: " + GLPK.glp_version() );
        diagram = prepareDiagram( diagram );
        List<Node> reactionNodes = DiagramUtility.getReactionNodes( diagram );
        reactionNames = reactionNodes.stream().map( n -> n.getName() ).toArray( String[]::new );

        glp_prob linearProblem;
        boolean useFBC = true;
        if( fbcData == null )
        {
            FbcBuilderDataTableAnalysis analysis = new FbcBuilderDataTableAnalysis( null, null );
            fbcData = analysis.getFbcData( diagram );
            useFBC = false;
        }
        this.fbcData = fbcData;

        try
        {
            linearProblem = GLPK.glp_create_prob();
            GLPK.glp_set_prob_name( linearProblem, diagram.getName() );

            //Define columns from reactions
            initConstraintValues( diagram );
            defineReactionConstraints( fbcData, reactionNames, linearProblem );

            //Create stochiometry constraints
            defineStochiometryConstraints( diagram, reactionNodes, linearProblem );

            // Define objective
            GLPK.glp_set_obj_name( linearProblem, "Objective function " + diagram.getName() );
            GLPK.glp_set_obj_dir( linearProblem, getObjectiveFunctionType( typeObjectiveFunction ) );
            if( useFBC )
                setLinearObjectiveFunction( fbcData, reactionNames, linearProblem );
            else
                setLinearObjectiveFunction( diagram, reactionNodes, linearProblem );

            //TODO: Free memory!
            //GLPK.glp_delete_prob(lp);

            problem = linearProblem;
            return new GLPKModel( linearProblem, reactionNames );
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "GLPK model creation error", e );
        }
        return null;
    }



    private void initConstraintValues(Diagram diagram)
    {
        constraintValues = new HashMap<>();
        if( diagram.getRole() instanceof EModel )
        {
            EModel emodel = diagram.getRole( EModel.class );
            for( Variable var : emodel.getVariables() )
            {
                constraintValues.put( var.getName(), var.getInitialValue() );
            }
        }
    }

    protected static double getStochiometry(Node varNode, Node reactionNode)
    {
        EModel emodel = Diagram.getDiagram( varNode ).getRole( EModel.class );
        for( Edge e : reactionNode.getEdges() )
        {
            if( e.getKernel() instanceof SpecieReference )
            {
                SpecieReference specieReference = (SpecieReference)e.getKernel();
                String stoichiometry = specieReference.getStoichiometry();
                double val = 0;
                try
                {
                    val = Double.parseDouble( stoichiometry );
                }
                catch( Exception ex )
                {
                    Variable var = emodel.getVariable( stoichiometry );
                    if( var != null )
                        val = var.getInitialValue();
                }
                if( e.nodes().has( varNode ) )
                    return specieReference.isProduct() ? val : specieReference.isReactant() ? -val : 0;
            }
        }
        return 0;
    }

    private static boolean hasOnlyZero(double[] vector)
    {
        return Arrays.stream( vector ).allMatch( e -> e == 0 );
    }


    private void defineStochiometryConstraints(Diagram diagram, List<Node> reactionsNode, glp_prob problem)
    {

        if( ! ( diagram.getRole() instanceof EModel ) )
            return;

        EModel emodel = diagram.getRole( EModel.class );
        List<double[]> constraints = StreamEx.of( emodel.getVariableRoles().stream() )
                .filter( v -> !v.isBoundaryCondition() && !v.isConstant() ).map( v -> v.getDiagramElement() ).select( Node.class )
                .map( node -> reactionsNode.stream().mapToDouble( rNode -> getStochiometry( node, rNode ) ).toArray() )
                .remove( GLPKModelCreator::hasOnlyZero ).toList();

        GLPK.glp_add_rows( problem, constraints.size() );
        for( int i = 0; i < constraints.size(); i++ )
        {
            int index = i + 1;
            processConstraint( problem, constraints.get( i ), index );
        }

    }

    private void processConstraint(glp_prob problem, double[] constraint, int index)
    {
        SWIGTYPE_p_int ind;
        SWIGTYPE_p_double val;
        GLPK.glp_set_row_name( problem, index, "c" + index );
        GLPK.glp_set_row_bnds( problem, index, GLPKConstants.GLP_FX, 0., 0. );
        ind = GLPK.new_intArray( constraint.length + 1 );
        val = GLPK.new_doubleArray( constraint.length + 1 );
        for( int j = 0; j < constraint.length; j++ )
        {
            GLPK.intArray_setitem( ind, j + 1, j + 1 );
            double value = constraint[j];
            GLPK.doubleArray_setitem( val, j + 1, value );
        }

        //set (replace) row of the constraint matrix
        int len = constraint.length;
        GLPK.glp_set_mat_row( problem, index, len, ind, val );
        GLPK.delete_intArray( ind );
        GLPK.delete_doubleArray( val );
    }

    private void defineReactionConstraints(TableDataCollection fbcData, String[] reactionNames, glp_prob problem)
    {
        // Define columns for reaction nodes
        GLPK.glp_add_cols( problem, reactionNames.length );
        //        GLPK.glp_set_col_name( lp, 1, "x1" );
        //        GLPK.glp_set_col_kind( lp, 1, GLPKConstants.GLP_CV );
        //        GLPK.glp_set_col_bnds( lp, 1, GLPKConstants.GLP_DB, 0, 100. );
        try
        {
            for( int i = 0; i < reactionNames.length; i++ )
            {
                int index = i + 1;
                GLPK.glp_set_col_name( problem, index, reactionNames[i] );
                //set (change) column kind .
                //GLP_CV - continuous variable; GLP_IV - integer variable; GLP_BV - binary variable
                GLPK.glp_set_col_kind( problem, index, GLPKConstants.GLP_CV );
                RowDataElement rowDataElement = fbcData.get( reactionNames[i] );
                addReactionBounds( rowDataElement, index, problem );
            }
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException( "Data table is incorrect" );
        }
    }

    private void addReactionBounds(RowDataElement rowDataElement, int index, glp_prob problem)
    {
        String lessObj = rowDataElement.getValueAsString( "Less" );
        String equalObj = rowDataElement.getValueAsString( "Equal" );
        String greatObj = rowDataElement.getValueAsString( "Greater" );

        //GLPK Bounds types:
        //GLP_FR -inf < x < +inf Free variable 
        //GLP_LO lb <= x < +inf Variable with lower bound 
        //GLP_UP -inf < x <= ub Variable with upper bound 
        //GLP_DB lb <= x <= ub Double-bounded variable 
        //GLP_FX x = lb Fixed variable

        double lessVal = Double.MIN_VALUE, greaterVal = Double.MAX_VALUE;
        //TODO: should we treat constant first?
        if( !equalObj.isEmpty() )
        {
            GLPK.glp_set_col_bnds( problem, index, GLPKConstants.GLP_FX, getBoundValue( equalObj ), greaterVal );
        }
        else
        {
            if( !lessObj.isEmpty() )
                greaterVal = getBoundValue( lessObj );
            if( !greatObj.isEmpty() )
                lessVal = getBoundValue( greatObj );
            boolean upInf = lessObj.isEmpty() || greaterVal == Double.POSITIVE_INFINITY;
            boolean downInf = greatObj.isEmpty() || lessVal == Double.NEGATIVE_INFINITY;
            int type = upInf ? ( downInf ? GLPKConstants.GLP_FR : GLPKConstants.GLP_LO )
                    : ( downInf ? GLPKConstants.GLP_UP : GLPKConstants.GLP_DB );
            if( Double.compare( lessVal, greaterVal ) == 0 )
            {
                type = GLPKConstants.GLP_FX;
            }
            GLPK.glp_set_col_bnds( problem, index, type, lessVal, greaterVal );
        }
    }

    private double getBoundValue(String obj)
    {
        if( constraintValues.containsKey( obj ) )
            return constraintValues.get( obj );
        else
        {
            double value = 0.0;
            try
            {
                value = SbmlModelReader.parseSBMLDoubleValue( obj );
            }
            catch( NumberFormatException e )
            {
                log.warning( "Can not parse value " + obj );
            }
            return value;
        }
    }

    private static void setLinearObjectiveFunction(Diagram diagram, List<Node> reactionsNode, glp_prob problem)
    {
        try
        {
            String activObj = diagram.getAttributes().getValueAsString( FBC_ACTIVE_OBJECTIVE );
            for( int i = 0; i < reactionsNode.size(); i++ )
            {
                int ind = i + 1;
                Node reaction = reactionsNode.get( i );
                DynamicPropertySet dps = reaction.getAttributes();
                DynamicProperty dp = dps.getProperty( FBC_OBJECTIVES );
                if( dp != null )
                {
                    FluxObjFunc fluxObj = (FluxObjFunc)dp.getValue();
                    int index = fluxObj.idObj.indexOf( activObj );
                    double coef = index >= 0 ? fluxObj.coefficient.get( index ) : 0;
                    GLPK.glp_set_obj_coef( problem, ind, coef );
                }
                else
                    GLPK.glp_set_obj_coef( problem, ind, 0.0 );
            }
        }
        catch( Exception ex )
        {

        }
    }

    private void setLinearObjectiveFunction(TableDataCollection fbcData, String[] reactionNames, glp_prob problem)
    {
        try
        {
            for( int i = 0; i < reactionNames.length; i++ )
            {
                int index = i + 1;
                double coef = (Double)fbcData.get( reactionNames[i] ).getValue( "Coefficient Objective Function" );
                GLPK.glp_set_obj_coef( problem, index, coef );
            }
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException( "Data table is incorrect" );
        }
    }

    private static int getObjectiveFunctionType(String objType)
    {
        if( objType.equals( FbcConstant.MAX ) )
            return GLPKConstants.GLP_MAX;
        else if( objType.equals( FbcConstant.MIN ) )
            return GLPKConstants.GLP_MIN;
        else
            throw new IllegalArgumentException( "Objective function type '" + objType + "' is incorrect" );
    }

    @Override
    public FbcModel createModel(Diagram diagram)
    {
        DynamicPropertySet dps = diagram.getAttributes();
        String activObj = dps.getValueAsString( FBC_ACTIVE_OBJECTIVE );
        if( activObj == null )
            throw new IllegalArgumentException( "Diagram '" + diagram.getName() + "' is not sbml:fbc or was corrupted." );
        Object objectives = dps.getValue( FBC_LIST_OBJECTIVES );
        if( ! ( objectives instanceof Map ) )
            throw new IllegalArgumentException( "Diagram '" + diagram.getName() + "' is not sbml:fbc or was corrupted." );
        String objType = ( (Map<String, String>)objectives ).get( activObj );
        if( objType == null )
            throw new IllegalArgumentException( "Diagram '" + diagram.getName() + "' is not sbml:fbc or was corrupted." );
        return createModel( diagram, null, objType );
    }

    @Override
    public FbcModel getUpdatedModel(Map<String, Double> values)
    {
        for( String key : constraintValues.keySet() )
        {
            if( values.containsKey( key ) )
                constraintValues.put( key, values.get( key ) );

            for( int i = 0; i < reactionNames.length; i++ )
            {
                int index = i + 1;
                try
                {
                    RowDataElement rowDataElement = fbcData.get( reactionNames[i] );
                    addReactionBounds( rowDataElement, index, problem );
                }
                catch( Exception e )
                {
                }
            }
        }
        return new GLPKModel( problem, reactionNames );
    }

}

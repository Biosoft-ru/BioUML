package biouml.plugins.fbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxBounds;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxObjFunc;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.SpecieReference;
import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

public class ApacheModelCreator extends FbcModelCreator
{
    private static final String COEFFICIENT_OBJECTIVE_FUNCTION = "Coefficient Objective Function";
    private Map<String, Double> contstraintValues;
    private String[] reactionNames;
    private int maxIter = 10000;
    private GoalType goalType = GoalType.MAXIMIZE;
    private Collection<LinearConstraint> stochiometryConstraints;
    private List<LinearConstraintInfo> constraints;
    private LinearObjectiveFunction f;

    public ApacheModelCreator()
    {
    }
    public ApacheModelCreator(int maxIter)
    {
        this.maxIter = maxIter;
    }

    /**
     * returns model for flux balance calculate
     */
    @Override
    public FbcModel createModel(Diagram diagram, TableDataCollection fbcData, String typeObjectiveFunction)
    {
        diagram = prepareDiagram(diagram);
        List<Node> reactionsNode = DiagramUtility.getReactionNodes(diagram);
        reactionNames = reactionsNode.stream().map(n -> n.getName()).toArray(String[]::new);
        goalType = getTypeFBA(typeObjectiveFunction);
        stochiometryConstraints = getStochiometryConstraints(diagram, reactionsNode);

        if( fbcData != null )
        {
            f = getLinearObjectiveFunction(fbcData);
            constraints = getConstraints(fbcData);
        }
        else
        {          
            f = getLinearObjectiveFunction(diagram, reactionsNode);
            constraints = getConstraints(reactionsNode);
        }
        
        initConstraintValues(diagram);
        return new ApacheModel(f, getCurrentConstraints(), reactionNames, goalType, maxIter, false);
    }
    /**
     * returns model for flux balance calculate.
     * Here can be used only diagram which was got from "SBML:fbc" package
     * @param diagram
     * @return
     */
    @Override
    public FbcModel createModel(Diagram diagram)
    {
        DynamicPropertySet dps = diagram.getAttributes();
        String activObj = dps.getValueAsString(FBC_ACTIVE_OBJECTIVE);
        if( activObj == null )
            throw new IllegalArgumentException("Diagram '" + diagram.getName() + "' is not sbml:fbc or was corrupted.");
        Object objectives = dps.getValue(FBC_LIST_OBJECTIVES);
        if( ! ( objectives instanceof Map ) )
            throw new IllegalArgumentException("Diagram '" + diagram.getName() + "' is not sbml:fbc or was corrupted.");
        String objType = ( (Map<String, String>)objectives ).get(activObj);
        if( objType == null )
            throw new IllegalArgumentException("Diagram '" + diagram.getName() + "' is not sbml:fbc or was corrupted.");
        return createModel(diagram, null, objType);
    }

    
    private void initConstraintValues(Diagram diagram)
    {
        contstraintValues = new HashMap<>();
        if( diagram.getRole() instanceof EModel )
        {
            EModel emodel = diagram.getRole(EModel.class);
            for( LinearConstraintInfo info : constraints )
            {
                Variable var = emodel.getVariable(info.rightSide);
                if( var != null )
                    contstraintValues.put(var.getName(), var.getInitialValue());
            }
        }
    }
    
    @Override
    public FbcModel getUpdatedModel(Map<String, Double> values)
    {
        for( String key : contstraintValues.keySet() )
        {
            if( values.containsKey(key) )
                contstraintValues.put(key, values.get(key));
        }
        return new ApacheModel(f, getCurrentConstraints(), reactionNames, goalType, maxIter, false);
    }

    private static GoalType getTypeFBA(String objType)
    {
        if( objType.equals(FbcConstant.MAX) )
            return GoalType.MAXIMIZE;
        else if( objType.equals(FbcConstant.MIN) )
            return GoalType.MINIMIZE;
        else
            throw new IllegalArgumentException("Type objective function is incorrect");
    }

    private static boolean hasOnlyZero(double[] vector)
    {
        return Arrays.stream(vector).allMatch(e -> e == 0);
    }

    protected static double getStochiometry(Node varNode, Node reactionNode)
    {
        EModel emodel = Diagram.getDiagram(varNode).getRole(EModel.class);
        for( Edge e : reactionNode.getEdges() )
        {
            if( e.getKernel() instanceof SpecieReference )
            {
                SpecieReference specieReference = (SpecieReference)e.getKernel();
                String stoichiometry = specieReference.getStoichiometry();
                double val = 0;
                try
                {
                    val = Double.parseDouble(stoichiometry);
                }
                catch( Exception ex )
                {
                    Variable var = emodel.getVariable(stoichiometry);
                    if( var != null )
                        val = var.getInitialValue();
                }
                if( e.nodes().has(varNode) )
                    return specieReference.isProduct() ? val : specieReference.isReactant() ? -val : 0;
            }
        }
        return 0;
    }

    private List<LinearConstraintInfo> getConstraints(List<Node> reactionNodes)
    {
        List<LinearConstraintInfo> result = new ArrayList<>();

        for( int i = 0; i < reactionNodes.size(); i++ )
        {
            Node node = reactionNodes.get(i);
            DynamicPropertySet dps = node.getAttributes();
            DynamicProperty dp = dps.getProperty(FBC_BOUNDS);
            if( dp == null )
                continue;
            FluxBounds fluxBounds = (FluxBounds)dps.getValue(FBC_BOUNDS);
            double[] vectorBound = new double[reactionNames.length];
            vectorBound[i] = 1;
            for( int j = 0; j < fluxBounds.sign.size(); j++ )
            {
                Relationship sign = null;
                if( fluxBounds.sign.get(j).equals(FBC_LESS_EQUAL) )
                    sign = Relationship.LEQ;
                else if( fluxBounds.sign.get(j).equals(FBC_GREATER_EQUAL) )
                    sign = Relationship.GEQ;
                else if( fluxBounds.sign.get(j).equals(FBC_EQUAL) )
                    sign = Relationship.EQ;

                result.add(new LinearConstraintInfo(vectorBound, sign, fluxBounds.value.get(j)));
            }
        }
        return result;
    }

    private List<LinearConstraint> getCurrentConstraints()
    {
        List<LinearConstraint> result = new ArrayList<>();
        result.addAll(stochiometryConstraints);
        result.addAll(StreamEx.of(constraints).map(c -> c.getConstraint()).nonNull().toList());
        return result;
    }

    private List<LinearConstraintInfo> getConstraints(TableDataCollection fbcData)
    {
        List<LinearConstraintInfo> result = new ArrayList<>();
        try
        {
            for( int i = 0; i < reactionNames.length; i++ )
            {
                double[] vectorConstraint = new double[reactionNames.length];
                vectorConstraint[i] = 1;
                RowDataElement rowDataElement = fbcData.get(reactionNames[i]);
                String lessObj = rowDataElement.getValueAsString("Less");
                String equalObj = rowDataElement.getValueAsString("Equal");
                String greatObj = rowDataElement.getValueAsString("Greater");

                if( !lessObj.isEmpty() )
                    result.add(new LinearConstraintInfo(vectorConstraint, Relationship.LEQ, lessObj));
                if( !equalObj.isEmpty() )
                    result.add(new LinearConstraintInfo(vectorConstraint, Relationship.EQ, equalObj));
                if( !greatObj.isEmpty() )
                    result.add(new LinearConstraintInfo(vectorConstraint, Relationship.GEQ, greatObj));
            }
            return result;
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException("Data table is incorrect");
        }
    }

    private Collection<LinearConstraint> getStochiometryConstraints(Diagram diagram, List<Node> reactionsNode)
    {
        if( ! ( diagram.getRole() instanceof EModel ) )
            return new ArrayList<>();

        EModel emodel = diagram.getRole(EModel.class);
        return emodel.getVariableRoles().stream().filter(v -> !v.isBoundaryCondition() && !v.isConstant()).map(v -> v.getDiagramElement())
                .filter( Node.class::isInstance )
                .map( node -> reactionsNode.stream().mapToDouble( rNode -> getStochiometry( (Node)node, rNode ) ).toArray() )
                .filter( d -> ! ( ApacheModelCreator.hasOnlyZero( d ) ) )
                .map( stoichiometry -> new LinearConstraint( stoichiometry, Relationship.EQ, 0 ) ).collect( Collectors.toList() );
    }

    private LinearObjectiveFunction getLinearObjectiveFunction(Diagram diagram, List<Node> reactionsNode)
    {
        try
        {
            String activObj = diagram.getAttributes().getValueAsString(FBC_ACTIVE_OBJECTIVE);
            double[] vectorWeight = reactionsNode.stream().mapToDouble(reaction -> {
                DynamicPropertySet dps = reaction.getAttributes();
                DynamicProperty dp = dps.getProperty(FBC_OBJECTIVES);
                if( dp == null )
                    return 0;
                FluxObjFunc fluxObj = (FluxObjFunc)dp.getValue();
                int index = fluxObj.idObj.indexOf(activObj);
                return index >= 0 ? fluxObj.coefficient.get(index) : 0;
            }).toArray();
            return new LinearObjectiveFunction(vectorWeight, 0);
        }
        catch( Exception ex )
        {

        }
        return null;
    }
    private LinearObjectiveFunction getLinearObjectiveFunction(TableDataCollection fbcData)
    {
        try
        {
            double[] vectorWeight = new double[reactionNames.length];
            for( int i = 0; i < vectorWeight.length; i++ )
                vectorWeight[i] = (Double)fbcData.get(reactionNames[i]).getValue(COEFFICIENT_OBJECTIVE_FUNCTION);
            return new LinearObjectiveFunction(vectorWeight, 0);
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException("Data table is incorrect");
        }
    }

    @Override
    public String toString()
    {
        return "Apache solver";
    }

    private class LinearConstraintInfo
    {
        private Relationship relation;
        private double[] vector;
        private String rightSide;

        public LinearConstraintInfo(double[] vector, Relationship relation, String rightSide)
        {
            this.relation = relation;
            this.vector = vector;
            this.rightSide = rightSide;
        }

        public LinearConstraint getConstraint()
        {
            return createConstraint(vector, relation, rightSide);
        }
    }

    private LinearConstraint createConstraint(double[] vector, Relationship rel, String str)
    {
        try
        {
            if( str.isEmpty() )
                return null;

            double val = contstraintValues.containsKey(str) ? contstraintValues.get(str) : Double.parseDouble(str);
            return new LinearConstraint(vector, rel, val);
        }
        catch( Exception ex )
        {
            return null;
        }   
    }
}

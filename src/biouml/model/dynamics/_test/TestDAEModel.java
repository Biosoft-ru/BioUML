package biouml.model.dynamics._test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import biouml.model.dynamics.DAEModelUtilities;
import biouml.model.dynamics.Equation;
import biouml.standard.diagram.DiagramGenerator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;

public class TestDAEModel extends TestCase
{
    public TestDAEModel(String name)
    {
        super(name);
    }
    
    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestDAEModel.class.getName());
        suite.addTest(new TestDAEModel("testSimpleOrder"));
        suite.addTest(new TestDAEModel("testFullAlgebraic"));
        suite.addTest(new TestDAEModel("testSelfAlgebraic"));
        suite.addTest(new TestDAEModel("testComplex"));
        return suite;
    }


    public void testSimpleOrder() throws Exception
    {
        List<Equation> orderedEquations = new ArrayList<>();
        List<Equation> cycledEquations = new ArrayList<>();
        
        DiagramGenerator generator = new DiagramGenerator("");
        
        Equation scalar2 = generator.createEquation("y", "2*x", Equation.TYPE_SCALAR).getRole(Equation.class);
        Equation scalar3 = generator.createEquation("z", "exp(y)-x", Equation.TYPE_SCALAR).getRole(Equation.class);
        Equation scalar1 = generator.createEquation("x", "5+1", Equation.TYPE_SCALAR).getRole(Equation.class);
      
        Set<Equation> equations = generator.getEModel().getEquations().toSet();
        DAEModelUtilities.reorderAssignmentRules(equations, orderedEquations, cycledEquations);
        
        assertTrue(orderedEquations.equals(StreamEx.of(scalar1, scalar2, scalar3).toList()));
        assertTrue(cycledEquations.isEmpty());
    }
    
    public void testFullAlgebraic() throws Exception
    {
        List<Equation> orderedEquations = new ArrayList<>();
        List<Equation> cycledEquations = new ArrayList<>();
        
        DiagramGenerator generator = new DiagramGenerator("");
        Equation scalar1 = generator.createEquation("x", "5+y", Equation.TYPE_SCALAR).getRole(Equation.class);
        Equation scalar2 = generator.createEquation("y", "2*x - z", Equation.TYPE_SCALAR).getRole(Equation.class);
        Equation scalar3 = generator.createEquation("z", "exp(y)-x", Equation.TYPE_SCALAR).getRole(Equation.class);
      
        Set<Equation> equations = generator.getEModel().getEquations().toSet();
        DAEModelUtilities.reorderAssignmentRules(equations, orderedEquations, cycledEquations);
        
        assertTrue(orderedEquations.isEmpty());
        assertEquals(StreamEx.of(scalar1, scalar2, scalar3).toList(), cycledEquations );
    }
    
    public void testSelfAlgebraic() throws Exception
    {
        List<Equation> orderedEquations = new ArrayList<>();
        List<Equation> cycledEquations = new ArrayList<>();
        
        DiagramGenerator generator = new DiagramGenerator("");
        
        Equation algebraic = generator.createEquation("y", "2 - y", Equation.TYPE_SCALAR).getRole(Equation.class);
        Equation scalar2 = generator.createEquation("x", "cos(cos(z))", Equation.TYPE_SCALAR).getRole(Equation.class);
        Equation scalar1 = generator.createEquation("z", "exponentiale", Equation.TYPE_SCALAR).getRole(Equation.class);
        
        Set<Equation> equations = generator.getEModel().getEquations().toSet();
        DAEModelUtilities.reorderAssignmentRules(equations, orderedEquations, cycledEquations);
        
        assertEquals(StreamEx.of(algebraic).toList(), cycledEquations);
        assertEquals(StreamEx.of(scalar1, scalar2).toList(), orderedEquations);
    }
    
    public void testComplex() throws Exception
    {
        List<Equation> orderedEquations = new ArrayList<>();
        List<Equation> cycledEquations = new ArrayList<>();

        DiagramGenerator generator = new DiagramGenerator("");

        Equation scalarEnd3 = generator.createEquation("z3", "2*z2", Equation.TYPE_SCALAR).getRole(Equation.class);
        Equation algebraic1 = generator.createEquation("y", "2 - p*x", Equation.TYPE_SCALAR).getRole(Equation.class);
        Equation scalarEnd2 = generator.createEquation("z2", "2*z/p", Equation.TYPE_SCALAR).getRole(Equation.class);
        Equation algebraic2 = generator.createEquation("x", "5 + y", Equation.TYPE_SCALAR).getRole(Equation.class);
        Equation scalarEnd = generator.createEquation("z", "exp(y)/tanh(x)", Equation.TYPE_SCALAR).getRole(Equation.class);
        Equation scalarStart = generator.createEquation("p", "cos(pi)", Equation.TYPE_SCALAR).getRole(Equation.class);

        Set<Equation> equations = generator.getEModel().getEquations().toSet();
        DAEModelUtilities.reorderAssignmentRules(equations, orderedEquations, cycledEquations);

        assertEquals(StreamEx.of(algebraic1, algebraic2).toSet(), StreamEx.of(cycledEquations).toSet());
        assertEquals(StreamEx.of(scalarStart, scalarEnd, scalarEnd2, scalarEnd3).toList(), orderedEquations);
    }
}

package ru.biosoft.templates._test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.VariableRole;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.HTMLLinearFormatter;
import ru.biosoft.math.model.VariableResolver;
import ru.biosoft.workbench.Framework;

public class FormatterTest extends TestCase
{
    public static final String repositoryPath = "../data";
    public static final String resourcesPath = "../data_resources";

    /** Standart JUnit constructor */
    public FormatterTest(String name)
    {
        super(name);
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(FormatterTest.class.getName());

        suite.addTest(new FormatterTest("testFormatterTest"));
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //


    public void testFormatterTest() throws Exception
    {
        Framework.initRepository(repositoryPath);
        Framework.initRepository(resourcesPath);
        //Diagram de = (Diagram)CollectionFactory.getDataElement("databases/Biopath/Diagrams/DGR0400L_test");

        DataElement targetDiagramObj = CollectionFactory.getDataElement("databases/Biomodels/Diagrams/BIOMD0000000035.xml");
        assertNotNull("Can not load diagram", targetDiagramObj);
        Diagram de = (Diagram)targetDiagramObj;

        HTMLLinearFormatter formatter = new HTMLLinearFormatter();

        processODEEquations(de, formatter);
        //processEvents(de, formatter);

    }

    private void processEvents(Diagram de, HTMLLinearFormatter formatter)
    {
        int orderNumber = 0;
        EModel model = de.getRole( EModel.class );

        Event[] events = model.getEvents();
        for( Event event : events )
        {
            orderNumber = orderNumber + 1;
            String title = ( (Node)event.getParent() ).getTitle();
            String[] expressions = event.getExpressions();
            String trigger = event.getTrigger();
            String delay = event.getDelay();
            ArrayList<String> formulas = new ArrayList<>();
            for( String formula : expressions )
            {
                if( formula != null )
                {
                    getFormula(de, formatter, formula, event);
                    formulas.add(formula);
                }
            }
            String comment = "";
            if( ( (Node)event.getParent() ).getComment() != null )
                comment = ( (Node)event.getParent() ).getComment();
        }
    }
    private void processODEEquations(Diagram diagram, HTMLLinearFormatter formatter)
    {
        int orderNumber = 0;
        EModel model = diagram.getRole(EModel.class);
        List<Equation> equations = model.getEquations().toList();
        int lastInd = equations.size() - 1;

        if( model.getVariableRoles() != null && model.getVariableRoles().getSize() > 0 )
        {
            Iterator<VariableRole> iterator = model.getVariableRoles().iterator();
            for( int boo = 0; boo < model.getVariableRoles().getSize(); ++boo )
            {
                if( iterator.hasNext() )
                {
                    VariableRole variable = iterator.next();

                    DiagramElement de = variable.getDiagramElement();

                    if( !de.getKernel().getType().equals( "compartment" ) )
                    {
                        String ID = variable.getName();
                        String name = de.getTitle();
                        String formula = "d" + name + "/dt";
                        int termsNumber = 0;

                        for( int foo = 0; foo < lastInd + 1; ++foo )
                        {
                            Equation equation = equations.get( foo );
                            String eFormula = equation.getFormula();
                            String eVariable = equation.getVariable();
                            if( eVariable.equals( ID ) && ( eFormula.startsWith( "$$" ) || eFormula.startsWith( "-$$" ) ) )
                            {
                                String formulaID;

                                if( eFormula.startsWith( "$$" ) )
                                    formulaID = eFormula;
                                else
                                    formulaID = eFormula.substring( 1, eFormula.length() );

                                for( int goo = 0; goo < lastInd + 1; ++goo )
                                {
                                    Equation equation2 = equations.get( goo );
                                    String eFormula2 = equation2.getFormula();
                                    String eVariable2 = equation2.getVariable();
                                    if( eVariable2.equals( formulaID ) )
                                    {
                                        if( eFormula.startsWith( "-$$" ) )
                                            eFormula2 = getFormulaWithMinus( diagram, formatter, eFormula2, equation2 );
                                        else
                                            eFormula2 = getFormula( diagram, formatter, eFormula2, equation2 );

                                        if( termsNumber == 0 )
                                            formula = eFormula2;
                                        else
                                        {
                                            if( !eFormula2.startsWith( "&minus;" ) )
                                                formula = formula + " + " + eFormula2;
                                            else
                                                formula = formula + " " + eFormula2;
                                        }

                                        termsNumber = termsNumber + 1;
                                    }
                                }
                            }
                        }
                        if( termsNumber > 0 )
                        {
                            orderNumber = orderNumber + 1;
                            System.out.println( orderNumber + ": " + formula );
                        }
                    }
                }
            }
        }
    }


    private String getFormula(Diagram diagram, HTMLLinearFormatter formatter, String formula, Role role)
    {
        String result = formula.replace(" ", "");

        EModel model = diagram.getRole(EModel.class);
        AstStart start = model.readMath(result, role);
        VariableResolver resolver = model.getVariableResolver(EModel.VARIABLE_NAME_BY_TITLE_BRIEF);
        boolean isMinus = false;
        String[] formattedTree = formatter.format(start, resolver, isMinus);

        result = formattedTree[1];

        return result;
    }

    private String getFormulaWithMinus(Diagram diagram, HTMLLinearFormatter formatter, String formula, Role role)
    {
        EModel model = diagram.getRole(EModel.class);
        AstStart start = model.readMath(formula, role);
        VariableResolver resolver = model.getVariableResolver(EModel.VARIABLE_NAME_BY_TITLE_BRIEF);
        boolean isMinus = true;
        String[] formattedTree = formatter.format(start, resolver, isMinus);

        return formattedTree[1];
    }
}

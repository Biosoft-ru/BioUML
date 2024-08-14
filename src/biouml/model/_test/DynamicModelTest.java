package biouml.model._test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.util.Properties;

import javax.swing.JFrame;

import biouml.model.Compartment;
import biouml.model.DefaultDiagramViewBuilder;
import biouml.model.Diagram;
import biouml.model.DiagramElementStyle;
import biouml.model.DiagramFilter;
import biouml.model.DiagramType;
import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.ModuleType;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.Cell;
import biouml.standard.type.Gene;
import biouml.standard.type.Protein;
import biouml.standard.type.Reaction;
import biouml.standard.type.Relation;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;

/**
 * Test {@link biouml.model.dynamics}package.
 * 
 * A simple hypothetic pharmocinetic model is used as an example.
 */
public class DynamicModelTest extends TestCase
{
    /** Standart JUnit constructor */
    public DynamicModelTest ( String name )
    {
        super ( name );
    }

    /** Make suite if tests. */
    public static Test suite ( )
    {
        TestSuite suite = new TestSuite ( DynamicModelTest.class.getName ( ) );
        
        suite.addTest ( new DynamicModelTest ( "testCreateModule" ) );
        suite.addTest ( new DynamicModelTest ( "testInitData" ) );
        suite.addTest ( new DynamicModelTest ( "testCreateDiagram" ) );
        //suite.addTest ( new DynamicModelTest ( "testCreateDiagramView" ) );
        
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    public void testCreateModule ( ) throws Exception
    {
        TestDynamicModuleType type = new TestDynamicModuleType ( );
        module = type.createModule ( null, "Hypothetic pharmokinetic example" );
        assertEquals ( "number of types in the module", 3, module.getSize ( ) );
        CollectionFactory.registerRoot ( module );
    }

    public void testInitData ( ) throws Exception
    {
        DataCollection<?> data = ( DataCollection<?> ) module.get ( Module.DATA );

        // create compartments
        DataCollection<?> compartments = ( DataCollection<?> ) data.get ( "compartment" );

        assertNotNull ( "compartment", compartments );

        module.putKernel ( new biouml.standard.type.Compartment ( compartments, "blood" ) );
        module.putKernel ( new biouml.standard.type.Compartment ( compartments, "liver" ) );

        assertEquals ( "number of compartments", 2, compartments.getSize ( ) );

        // create proteins
        DataCollection<?> proteins = ( DataCollection<?> ) data.get ( "protein" );
        assertNotNull ( "protein", proteins );

        module.putKernel ( new Protein ( proteins, "E" ) );

        assertEquals ( "number of proteins", 1, proteins.getSize ( ) );

        // create substance
        DataCollection<?> substances = ( DataCollection<?> ) data.get ( "substance" );
        assertNotNull ( "substance", substances );

        module.putKernel ( new Substance ( substances, "A" ) );
        module.putKernel ( new Substance ( substances, "B" ) );
        assertEquals ( "number of substances", 2, substances.getSize ( ) );

        // create reactions
        DataCollection<?> reactions = ( DataCollection<?> ) data.get ( "reaction" );
        assertNotNull ( "reaction", reactions );

        module.putKernel( new Reaction( reactions, "blood_A_to_liver_A" ) );
        module.putKernel( new Reaction( reactions, "liver_A_to_blood_A" ) );
        module.putKernel( new Reaction( reactions, "A_to_B_by_E" ) );
        assertEquals ( "number of reactions", 3, reactions.getSize ( ) );

        // create relations
        DataCollection<?> relations = ( DataCollection<?> ) data.get ( "SpecieReference" );
        assertNotNull ( "SpecieReference", relations );

        module.putKernel( new SpecieReference( relations, "blood_A_as_reactant" ) );
        module.putKernel( new SpecieReference( relations, "liver_A_as_product" ) );
        module.putKernel( new SpecieReference( relations, "liver_A_as_reactant" ) );
        module.putKernel( new SpecieReference( relations, "blood_A_as_product" ) );
        
        module.putKernel( new SpecieReference( relations, "A_blood_as_reactant" ) );
        module.putKernel( new SpecieReference( relations, "A_liv_as_reactant" ) );
        module.putKernel( new SpecieReference( relations, "A_blood_as_product" ) );
        module.putKernel( new SpecieReference( relations, "A_liv_as_product" ) );

        module.putKernel( new SpecieReference( relations, "A_as_reactant" ) );
        module.putKernel( new SpecieReference( relations, "A_as_product" ) );
        module.putKernel( new SpecieReference( relations, "B_as_product" ) );
        module.putKernel( new SpecieReference( relations, "E_as_reactant" ) );
        assertEquals ( "number of relations", 12, relations.getSize ( ) );
    }

    public void testCreateDiagram ( ) throws Exception
    {
        DiagramType diagramType = new PathwaySimulationDiagramType ( );
        diagram = new Diagram ( module, new Stub ( null, "pharmo_simple" ),
                diagramType );
        diagram.setRole ( new EModel ( diagram ) );
        module.putDiagram ( diagram );

        int lev_1 = 30;
        int lev_2 = 100;
        int h = 250;

        biouml.model.Compartment blood = new biouml.model.Compartment (
                diagram, module.getKernel (
                        biouml.standard.type.Compartment.class, "blood" ) );
        diagram.put ( blood );
        blood.setLocation ( new Point ( 10, 30 ) );
        blood.setShapeType ( biouml.model.Compartment.SHAPE_RECTANGLE );
        blood.setPredefinedStyle(DiagramElementStyle.STYLE_NOT_SELECTED);
        blood.getCustomStyle().setBrush(new Brush(Color.pink) );
        blood.setShapeSize ( new Dimension ( 120, h ) );

        Node A_blood = new Node ( blood, module.getKernel (
                Substance.class, "A" ) );
        A_blood.setRole ( new VariableRole ( A_blood, 100 ) );
        A_blood.setLocation ( new Point ( 10, lev_2 ) );
        blood.put ( A_blood );

        biouml.model.Compartment liver = new biouml.model.Compartment (
                diagram, module.getKernel (
                        biouml.standard.type.Compartment.class, "liver" ) );
        liver.setShapeType ( biouml.model.Compartment.SHAPE_RECTANGLE );
        liver.setPredefinedStyle(DiagramElementStyle.STYLE_NOT_SELECTED);
        liver.getCustomStyle().setBrush(new Brush(new Color ( 240, 240, 200 )));
        liver.setShapeSize ( new Dimension ( 540, h ) );
        diagram.put ( liver );
        liver.setLocation ( new Point ( 300, 30 ) );

        Node A_liv = new Node ( liver, module.getKernel (
                Substance.class, "A" ) );
        A_liv.setRole ( new VariableRole ( A_liv, 0 ) );
        A_liv.setLocation ( new Point ( 15, lev_2 ) );
        liver.put ( A_liv );

        Node B = new Node ( liver, module.getKernel ( Substance.class,
                "B" ) );
        B.setRole ( new VariableRole ( B, 0 ) );
        B.setLocation ( new Point ( 450, lev_2 ) );
        liver.put ( B );

        Node E = new Node ( liver, module.getKernel ( Protein.class,
                "E" ) );
        E.setRole ( new VariableRole ( E, 1 ) );
        E.setLocation ( new Point ( 270, lev_1 ) );
        liver.put ( E );

        Node R_E = new Node ( liver, module.getKernel (
                Reaction.class, "A_to_B_by_E" ) );
        R_E.setLocation ( new Point ( 260, lev_2 + 50 ) );
        liver.put ( R_E );

        Edge e1 = new Edge ( liver, module.getKernel (
                SpecieReference.class, "A_as_reactant" ), A_liv, R_E );
        e1.setRole ( new Equation ( e1, A_liv.getRole ( VariableRole.class )
                .getName ( ), "-k_3*k_E0*$A/(k_Km+$A/v_liver)" ) );
        liver.put ( e1 );

        Edge e2 = new Edge ( liver, module.getKernel (
                SpecieReference.class, "B_as_product" ), R_E, B );
        e2.setRole ( new Equation ( e2, B.getRole ( VariableRole.class )
                .getName ( ), "k_3*k_E0*$liver.A/(k_Km+$liver.A/v_liver)" ) );
        liver.put ( e2 );

        Edge e3 = new Edge ( liver, module.getKernel (
                SpecieReference.class, "E_as_reactant" ), E, R_E );
        e3.setRole ( new Equation ( e3, E.getRole ( VariableRole.class )
                .getName ( ), "0" ) );
        liver.put ( e3 );

        Node T_1 = new Node ( diagram, module.getKernel (
                Reaction.class, "blood_A_to_liver_A" ) );
        T_1.setLocation ( new Point ( 160, lev_2 ) );
        diagram.put ( T_1 );

        Node T_2 = new Node ( diagram, module.getKernel (
                Reaction.class, "liver_A_to_blood_A" ) );
        T_2.setLocation ( new Point ( 160, lev_2 + 60 ) );
        diagram.put ( T_2 );

        Edge t1 = new Edge ( diagram, module.getKernel (
                SpecieReference.class, "A_blood_as_reactant" ), A_blood, T_1 );
        t1.setRole ( new Equation ( t1, A_blood.getRole ( VariableRole.class )
                .getName ( ), "-k_1*$A" ) );
        diagram.put ( t1 );

        Edge t2 = new Edge ( diagram, module.getKernel (
                SpecieReference.class, "A_blood_as_product" ), T_2, A_blood );
        t2.setRole ( new Equation ( t2, A_blood.getRole ( VariableRole.class )
                .getName ( ), "k_2*$liver.A" ) );
        diagram.put ( t2 );

        Edge t3 = new Edge ( diagram, module.getKernel (
                SpecieReference.class, "A_liv_as_reactant" ), T_1, A_liv );
        t3.setRole ( new Equation ( t3, A_liv.getRole ( VariableRole.class )
                .getName ( ), "-k_2*$A" ) );
        diagram.put ( t3 );

        Edge t4 = new Edge ( diagram, module.getKernel (
                SpecieReference.class, "A_liv_as_product" ), A_liv, T_2 );
        t4.setRole ( new Equation ( t4, A_liv.getRole ( VariableRole.class )
                .getName ( ), "k_1*$blood.A" ) );
        diagram.put ( t4 );
        
        assertEquals ( "Invalid diagram elements count", 8, diagram.getSize ( ) );
    }

    //////////////////////////////////////////////////////////////////
    //

    public static class TestDynamicModuleType extends DataElementSupport
            implements ModuleType
    {
        public TestDynamicModuleType ( )
        {
            super ( "Test dynamic diagram type", null );
        }

        @Override
        @SuppressWarnings ( "unchecked" )
        public Class<? extends DiagramType>[] getDiagramTypes ( )
        {
            return new Class[] { TestDynamicDiagramType.class };
        }
        
        @Override
        public String[] getXmlDiagramTypes()
        {
            return null;
        }

        @Override
        public boolean isCategorySupported ( )
        {
            return true;
        }

        @Override
        public String getCategory ( Class<? extends DataElement> c )
        {
            if ( biouml.standard.type.Compartment.class.isAssignableFrom ( c ) )
                return Module.DATA + "/compartment";

            if ( Protein.class.isAssignableFrom ( c ) )
                return Module.DATA + "/protein";

            if ( Substance.class.isAssignableFrom ( c ) )
                return Module.DATA + "/substance";

            if ( Reaction.class.isAssignableFrom ( c ) )
                return Module.DATA + "/reaction";

            if ( SpecieReference.class.isAssignableFrom ( c ) )
                return Module.DATA + "/SpecieReference";

            return "other";
        }

        @Override
        public Module createModule ( Repository parent, String name )
                throws Exception
        {
            // Create primary data collection (root)
            DataCollection<?> primaryDC = new VectorDataCollection<> ( "DynamicModuleExample" );

            Properties props = new Properties ( );
            props.setProperty ( DataCollectionConfigConstants.NAME_PROPERTY, primaryDC.getName() );
            props.put( DataCollectionConfigConstants.PRIMARY_COLLECTION, primaryDC );
            props.setProperty ( Module.TYPE_PROPERTY, TestDynamicModuleType.class.getName ( ) );
            Module module = new Module ( null, props );

            //init data collections for types
            module.put ( new VectorDataCollection<Diagram> ( Module.DIAGRAM, module, null ) );

            DataCollection<DataCollection<?>> data = new VectorDataCollection<> ( Module.DATA, module, null );
            module.put ( data );
            
            data.put ( new VectorDataCollection<Compartment> ( "compartment", data, null ) );
            data.put ( new VectorDataCollection<Protein> ( "protein", data, null ) );
            data.put ( new VectorDataCollection<Substance> ( "substance", data, null ) );
            data.put ( new VectorDataCollection<Reaction> ( "reaction", data, null ) );
            data.put ( new VectorDataCollection<SpecieReference> ( "SpecieReference", data, null ) );

            module.put ( new VectorDataCollection<> ( Module.METADATA, module, null ) );
            return module;
        }

        @Override
        public String getVersion ( )
        {
            return "1.0";
        }

        @Override
        public boolean canCreateEmptyModule ( )
        {
            return false;
        }
    }

    public static class TestDynamicDiagramType extends DiagramTypeSupport
    {
        @Override
        public Class[] getNodeTypes ( )
        {
            return new Class[] { biouml.standard.type.Compartment.class,
                    Protein.class, Substance.class, Reaction.class };
        }

        @Override
        public Class[] getEdgeTypes ( )
        {
            return null;
        }

        DiagramViewBuilder builder;

        @Override
        public DiagramViewBuilder getDiagramViewBuilder ( )
        {
            if ( builder == null )
                builder = new biouml.standard.diagram.PathwayDiagramViewBuilder();

            return builder;
        }

        @Override
        public DiagramFilter getDiagramFilter ( Diagram diagram )
        {
            return null;
        }

        @Override
        public SemanticController getSemanticController ( )
        {
            return null;
        }
    }

    static Module module;
    static Diagram diagram;

    public void testCreateDiagramView() throws Exception
    {
        DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();

        JFrame frame = new JFrame();
        frame.setVisible(true);
        CompositeView diagramView = builder.createDiagramView(diagram, frame.getGraphics());
        assertNotNull( diagramView );
    }

    public static class TestModuleType extends DataElementSupport implements ModuleType
    {
        public TestModuleType()
        {
            super("Test diagram type", null);
        }

        @Override
        @SuppressWarnings ( "unchecked" )
        public Class<? extends DiagramType>[] getDiagramTypes()
        {
            return new Class[] {TestDiagramType.class};
        }
        
        @Override
        public String[] getXmlDiagramTypes()
        {
            return null;
        }

        @Override
        public boolean isCategorySupported()
        {
            return true;
        }

        @Override
        public String getCategory(Class<? extends DataElement> c)
        {
            if(biouml.standard.type.Compartment.class.isAssignableFrom(c))
                return "compartment";

            if(Gene.class.isAssignableFrom(c))
                return "gene";

            if(Protein.class.isAssignableFrom(c))
                return "protein";

            if(Relation.class.isAssignableFrom(c))
                return "relation";

            return "other";
        }

        @Override
        public Module createModule(Repository parent, String name) throws Exception
        {
            // Create primary data collection (root)
            DataCollection<?> primaryDC = new VectorDataCollection<>("testModule");

            Properties props = new Properties();
            props.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, primaryDC.getName());
            props.setProperty(Module.TYPE_PROPERTY, TestModuleType.class.getName());
            Module module = new Module(primaryDC, props);

            //init data collections for types
            module.put(new VectorDataCollection<Diagram>(Module.DIAGRAM, module, null));

            DataCollection<DataCollection<?>> data = new VectorDataCollection<>(Module.DATA, module, null);
            module.put(data);
            data.put(new VectorDataCollection<Compartment>("compartment", data, null));
            data.put(new VectorDataCollection<Gene>("gene", data, null));
            data.put(new VectorDataCollection<Protein>("protein", data, null));
            data.put(new VectorDataCollection<Relation>("relation", data, null));

            module.put(new VectorDataCollection<>(Module.METADATA, module, null));

            return module;
        }

        @Override
        public String getVersion()
        {
            return "1.0";
        }

        @Override
        public boolean canCreateEmptyModule()
        {
            return false;
        }
    }

    //////////////////////////////////////////////////////////////////

    public static class TestDiagramType extends DiagramTypeSupport
    {
        @Override
        public Class[] getNodeTypes()
        {
            return new Class[]{Cell.class, Protein.class, Gene.class};
        }

        @Override
        public Class[] getEdgeTypes()
        {
            return null;
        }

        DiagramViewBuilder builder;
        @Override
        public DiagramViewBuilder getDiagramViewBuilder()
        {
            if(builder == null)
                builder = new DefaultDiagramViewBuilder();

            return builder;
        }

        @Override
        public DiagramFilter getDiagramFilter(Diagram diagram)
        {
            return null;
        }

        @Override
        public SemanticController getSemanticController()
        {
            return null;
        }
    }

}
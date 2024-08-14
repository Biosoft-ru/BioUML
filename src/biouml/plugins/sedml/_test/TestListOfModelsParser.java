package biouml.plugins.sedml._test;

import java.io.InputStream;

import org.jlibsedml.Change;
import org.jlibsedml.ChangeAttribute;
import org.jlibsedml.Model;
import org.jlibsedml.SEDMLDocument;
import org.jlibsedml.SedML;

import ru.biosoft.access._test.AbstractBioUMLTest;
import biouml.model.Diagram;
import biouml.plugins.research.workflow.WorkflowDiagramType;
import biouml.plugins.research.workflow.yaml.WorkflowUpdater;
import biouml.plugins.sedml.ListOfModelsParser;

import com.developmentontheedge.application.ApplicationUtils;

public class TestListOfModelsParser extends AbstractBioUMLTest
{
    public void testWF1() throws Exception
    {
        Diagram wf = createWorkflow( "wf1.yaml" );
        SEDMLDocument document = new SEDMLDocument();
        SedML sedml = document.getSedMLModel();
        new ListOfModelsParser( wf, sedml ).parse();
        assertEquals( 1, sedml.getModels().size() );
        Model model = sedml.getModelWithId( "model" );
        assertNotNull( model );
        assertFalse( model.hasChanges() );
        assertEquals( "urn:miriam:biomodels.db:BIOMD0000000140", model.getSource() );
    }

    public void testWF2() throws Exception
    {
        Diagram wf = createWorkflow( "wf2.yaml" );
        SEDMLDocument document = new SEDMLDocument();
        SedML sedml = document.getSedMLModel();
        new ListOfModelsParser( wf, sedml ).parse();
        assertEquals( 1, sedml.getModels().size() );
        Model model = sedml.getModelWithId( "model" );
        assertNotNull( model );
        assertTrue( model.hasChanges() );
        assertEquals( 1, model.getListOfChanges().size() );
        Change change = model.getListOfChanges().get( 0 );
        assertTrue( change instanceof ChangeAttribute );
        ChangeAttribute changeAttr = (ChangeAttribute)change;
        assertEquals( "/sbml:sbml/sbml:model/sbml:listOfParameters/sbml:parameter[@id=\"J0_v0\"]/@value", changeAttr.getTargetXPath()
                .toString() );
        assertEquals( changeAttr.getNewValue(), "8" );
        assertEquals( "urn:miriam:biomodels.db:BIOMD0000000140", model.getSource() );
    }

    public void testWF3() throws Exception
    {
        Diagram wf = createWorkflow( "wf3.yaml" );
        SEDMLDocument document = new SEDMLDocument();
        SedML sedml = document.getSedMLModel();
        new ListOfModelsParser( wf, sedml ).parse();
        assertEquals( 2, sedml.getModels().size() );
        Model model1 = sedml.getModelWithId( "model1" );
        assertNotNull( model1 );
        assertFalse( model1.hasChanges() );
        assertEquals( "model.xml", model1.getSource() );
        Model model2 = sedml.getModelWithId( "model2" );
        assertNotNull( model2 );
        assertTrue( model2.hasChanges() );
        assertEquals( "model1", model2.getSource() );
        assertEquals( 1, model2.getListOfChanges().size() );
        Change change = model2.getListOfChanges().get( 0 );
        assertTrue( change instanceof ChangeAttribute );
        ChangeAttribute changeAttr = (ChangeAttribute)change;
        assertEquals( "/sbml:sbml/sbml:model/sbml:listOfParameters/sbml:parameter[@id=\"J0_v0\"]/@value", changeAttr.getTargetXPath()
                .toString() );
        assertEquals( changeAttr.getNewValue(), "8" );
    }
    
    public void testWF4() throws Exception
    {
        Diagram wf = createWorkflow( "wf4.yaml" );
        SEDMLDocument document = new SEDMLDocument();
        SedML sedml = document.getSedMLModel();
        new ListOfModelsParser( wf, sedml ).parse();
        assertEquals( 3, sedml.getModels().size() );
        Model model1 = sedml.getModelWithId( "model1" );
        assertNotNull( model1 );
        assertFalse( model1.hasChanges() );
        assertEquals( "model.xml", model1.getSource() );
        
        Model model2 = sedml.getModelWithId( "model2" );
        assertNotNull( model2 );
        assertTrue( model2.hasChanges() );
        assertEquals( "model1", model2.getSource() );
        assertEquals( 1, model2.getListOfChanges().size() );
        Change change = model2.getListOfChanges().get( 0 );
        assertTrue( change instanceof ChangeAttribute );
        ChangeAttribute changeAttr = (ChangeAttribute)change;
        assertEquals( "/sbml:sbml/sbml:model/sbml:listOfParameters/sbml:parameter[@id=\"J0_v0\"]/@value", changeAttr.getTargetXPath()
                .toString() );
        assertEquals( changeAttr.getNewValue(), "8" );
        
        Model model3 = sedml.getModelWithId( "model3" );
        assertNotNull( model3 );
        assertTrue( model2.hasChanges() );
        assertEquals( "model2", model3.getSource() );
        assertEquals( 1, model3.getListOfChanges().size() );
        change = model3.getListOfChanges().get( 0 );
        assertTrue( change instanceof ChangeAttribute );
        changeAttr = (ChangeAttribute)change;
        assertEquals( "/sbml:sbml/sbml:model/sbml:listOfParameters/sbml:parameter[@id=\"J0_v1\"]/@value", changeAttr.getTargetXPath()
                .toString() );
        assertEquals( changeAttr.getNewValue(), "9" );

    }
    
    public void testWF5() throws Exception
    {
        Diagram wf = createWorkflow( "wf5.yaml" );
        SEDMLDocument document = new SEDMLDocument();
        SedML sedml = document.getSedMLModel();
        new ListOfModelsParser( wf, sedml ).parse();
        assertEquals( 3, sedml.getModels().size() );
        Model model1 = sedml.getModelWithId( "model1" );
        assertNotNull( model1 );
        assertFalse( model1.hasChanges() );
        assertEquals( "model.xml", model1.getSource() );
        
        Model model2 = sedml.getModelWithId( "model_1" );
        assertNotNull( model2 );
        assertTrue( model2.hasChanges() );
        assertEquals( "model1", model2.getSource() );
        assertEquals( 1, model2.getListOfChanges().size() );
        Change change = model2.getListOfChanges().get( 0 );
        assertTrue( change instanceof ChangeAttribute );
        ChangeAttribute changeAttr = (ChangeAttribute)change;
        assertEquals( "/sbml:sbml/sbml:model/sbml:listOfParameters/sbml:parameter[@id=\"J0_v0\"]/@value", changeAttr.getTargetXPath()
                .toString() );
        assertEquals( changeAttr.getNewValue(), "8" );
        
        Model model3 = sedml.getModelWithId( "model3" );
        assertNotNull( model3 );
        assertTrue( model2.hasChanges() );
        assertEquals( "model_1", model3.getSource() );
        assertEquals( 1, model3.getListOfChanges().size() );
        change = model3.getListOfChanges().get( 0 );
        assertTrue( change instanceof ChangeAttribute );
        changeAttr = (ChangeAttribute)change;
        assertEquals( "/sbml:sbml/sbml:model/sbml:listOfParameters/sbml:parameter[@id=\"J0_v1\"]/@value", changeAttr.getTargetXPath()
                .toString() );
        assertEquals( changeAttr.getNewValue(), "9" );
    }
    
    public void testWF6() throws Exception
    {
        Diagram wf = createWorkflow( "wf6.yaml" );
        SEDMLDocument document = new SEDMLDocument();
        SedML sedml = document.getSedMLModel();
        new ListOfModelsParser( wf, sedml ).parse();
        assertEquals( 3, sedml.getModels().size() );
        Model model1 = sedml.getModelWithId( "model1" );
        assertNotNull( model1 );
        assertFalse( model1.hasChanges() );
        assertEquals( "urn:miriam:biomodels.db:BIOMD0000000140", model1.getSource() );
        
        Model model2 = sedml.getModelWithId( "model2" );
        assertNotNull( model2 );
        assertTrue( model2.hasChanges() );
        assertEquals( "model1", model2.getSource() );
        assertEquals( 1, model2.getListOfChanges().size() );
        Change change = model2.getListOfChanges().get( 0 );
        assertTrue( change instanceof ChangeAttribute );
        ChangeAttribute changeAttr = (ChangeAttribute)change;
        assertEquals( "/sbml:sbml/sbml:model/sbml:listOfParameters/sbml:parameter[@id=\"J0_v0\"]/@value", changeAttr.getTargetXPath()
                .toString() );
        assertEquals( changeAttr.getNewValue(), "8" );
        
        Model model3 = sedml.getModelWithId( "model3" );
        assertNotNull( model3 );
        assertTrue( model2.hasChanges() );
        assertEquals( "model1", model3.getSource() );
        assertEquals( 1, model3.getListOfChanges().size() );
        change = model3.getListOfChanges().get( 0 );
        assertTrue( change instanceof ChangeAttribute );
        changeAttr = (ChangeAttribute)change;
        assertEquals( "/sbml:sbml/sbml:model/sbml:listOfParameters/sbml:parameter[@id=\"J0_v1\"]/@value", changeAttr.getTargetXPath()
                .toString() );
        assertEquals( changeAttr.getNewValue(), "9" );
    }

    private Diagram createWorkflow(String name) throws Exception
    {
        InputStream is = getClass().getResourceAsStream( "resources/" + name );
        String yaml = ApplicationUtils.readAsString( is );
        WorkflowDiagramType diagramType = new WorkflowDiagramType();
        Diagram workflow = diagramType.createDiagram( null, name, null );
        diagramType.getDiagramViewBuilder().createDiagramView( workflow, ApplicationUtils.getGraphics() );
        WorkflowUpdater updater = new WorkflowUpdater( workflow );
        updater.updateWorkflow( yaml );
        return workflow;
    }
}

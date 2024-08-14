package biouml.standard.state._test;

import java.util.List;

import javax.swing.undo.UndoableEdit;

import biouml.model.Diagram;
import biouml.standard.state.DiagramStateUtility;
import biouml.standard.state.State;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.core.Transformer;

/**
 * @author anna
 *
 */
public class StateTest extends AbstractBioUMLTest
{
    private DataCollection<Diagram> diagramsDC = null;
    private DataCollection<State> statesDC = null;
    public StateTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(StateTest.class);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(StateTest.class.getName());
        suite.addTest(new StateTest("testCreateState"));
        suite.addTest(new StateTest("testReadState"));
        suite.addTest(new StateTest("testApplyState"));
        suite.addTest(new StateTest("testDMLState"));
        suite.addTest(new StateTest("testPropertiesState"));
        suite.addTest(new StateTest("testWorkflowState"));
        return suite;
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        CollectionFactory.createRepository("../data");
        diagramsDC = DataElementPath.create("databases/test/state/Diagrams").getDataCollection(Diagram.class);
        assertNotNull("Diagrams collection is null", diagramsDC);
        statesDC = DataElementPath.create("databases/test/state/States").getDataCollection(State.class);
        assertNotNull("Diagrams collection is null", statesDC);
    }
    
    public void testCreateState() throws Exception
    {
        Diagram originalDiagram = diagramsDC.get("diagram_1");
        assertNotNull("Diagram BIOMD0000000012 not found in test collection", originalDiagram);
        Diagram changedDiagram = diagramsDC.get("diagram_2");
        assertNotNull("Diagram BIOMD0000000012_changed not found in test collection", changedDiagram);
        statesDC.remove("test_state_create");
        State state = DiagramStateUtility.createState(originalDiagram, changedDiagram, "test_state_create");
        statesDC.put(state);
    }
    
    public void testReadState() throws Exception
    {
        Transformer transformer = ( (TransformedDataCollection)statesDC ).getTransformer();
        DataElement input = ((TransformedDataCollection)statesDC ).getPrimaryCollection().get("test_state_read");
        CollectionFactory.createRepository("../data_resources");
        DataElement state = transformer.transformInput(input);
        DataCollection testDC = CollectionFactory.getDataCollection("data/Collaboration/MyProject/Data/all");
        if(testDC != null)
            testDC.put(state);
        assertNotNull("State read from collection is null", state);
        assertEquals("Names are different", "test_state_read", state.getName());
        assertTrue("Element is not a state", state instanceof State);
    }
    
    public void testApplyState() throws Exception
    {
        diagramsDC.remove("cloned_diagram_1");
        Diagram diagram = diagramsDC.get("diagram_1");
        Diagram dClone = diagram.clone(diagramsDC, "cloned_diagram_1");
        State state = statesDC.get("test_state");
        DiagramStateUtility.applyState(dClone, state);
        diagramsDC.put(dClone);
    }

    public void testDMLState() throws Exception
    {
        DataCollection<Diagram> diagramDC = DataElementPath.create("databases/test/state/DML").getDataCollection(Diagram.class);
        Diagram d1 = diagramDC.get("DGR0004");
        assertNotNull("Diagram DGR0004", d1);
        Diagram d2 = diagramDC.get("DGR0004_changed");
        assertNotNull("Diagram DGR0004_changed", d2);
        State state = DiagramStateUtility.createState(d1, d2, "changes_DGR0004");
        List<UndoableEdit> edits = state.getStateUndoManager().getEdits();
        String[] editNames = new String[] {"Remove RLT001602: SBS000018 to PRT000030 from DGR0004", "Remove SBS000018 from DGR0004",
                "Add SBS000018 to CMP0002", "Add RLT001602: SBS000018 to PRT000030 to CMP0002", "Add GEN000226 to DGR0004"};
        assertEquals("Number of edits", editNames.length, edits.size());
        for(int i=0; i<editNames.length; i++)
        {
            assertEquals("Edit#"+(i+1), editNames[i], edits.get(i).toString());
        }
        statesDC.put(state);
        State state2 = DiagramStateUtility.createState(d2, d1, "changes_DGR0004_back");
        edits = state2.getStateUndoManager().getEdits();
        String[] editNames2 = new String[] {"Remove GEN000226 from DGR0004_changed",
                "Remove RLT001602: SBS000018 to PRT000030 from CMP0002", "Remove SBS000018 from CMP0002",
                "Add SBS000018 to DGR0004_changed", "Add RLT001602: SBS000018 to PRT000030 to DGR0004_changed"};
        assertEquals("Number of edits", editNames2.length, edits.size());
        for(int i=0; i<editNames2.length; i++)
        {
            assertEquals("Edit#"+(i+1), editNames2[i], edits.get(i).toString());
        }
        statesDC.put(state2);

        Diagram d1Clone = d1.clone(diagramsDC, "DGR0004_changed");
        DiagramStateUtility.applyState(d1Clone, state);
        assertEquals("Diagram size", d2.getSize(), d1Clone.getSize());
        State cloneState = DiagramStateUtility.createState(d1Clone, d2, "ditto");
        assertEquals("Number of edits", 0, cloneState.getStateUndoManager().getEdits().size());
    }

    public void testPropertiesState() throws Exception
    {
        DataCollection<Diagram> diagramDC = DataElementPath.create("databases/test/state/DML").getDataCollection(Diagram.class);
        Diagram d1 = diagramDC.get("DGR0004");
        assertNotNull("Diagram DGR0004", d1);
        Diagram d2 = diagramDC.get("DGR0004_prop");
        assertNotNull("Diagram DGR0004_prop", d2);
        State state = DiagramStateUtility.createState(d1, d2, "prop_DGR0004");
        List<UndoableEdit> edits = state.getStateUndoManager().getEdits();
        String[] editNames = new String[] {"Change property 'location' of 'Node:HO-1'",
                "Change property 'path' of 'Edge:RCT000245: PRT000030 as modifier'", "Change property 'path' of 'Edge:increase'",
                "Change property 'title' of 'Node:LPS!!!'", "Change property 'comment' of 'Node:LPS!!!'",
                "Change property 'path' of 'Edge:induce'", "Change property 'path' of 'Edge:induce'",
                "Change property 'path' of 'Edge:induce'", "Change property 'path' of 'Edge:induce'",
                "Change property 'path' of 'Edge:induce'"};
        assertEquals("Number of edits", editNames.length, edits.size());
        for(int i=0; i<editNames.length; i++)
        {
            assertEquals("Edit#"+(i+1), editNames[i], edits.get(i).toString());
        }
        statesDC.put(state);
        State state2 = DiagramStateUtility.createState(d2, d1, "prop_DGR0004_back");
        edits = state2.getStateUndoManager().getEdits();
        String[] editNames2 = new String[] {"Change property 'location' of 'Node:HO-1'",
                "Change property 'path' of 'Edge:RCT000245: PRT000030 as modifier'", "Change property 'path' of 'Edge:increase'",
                "Change property 'title' of 'Node:LPS'", "Change property 'comment' of 'Node:LPS'",
                "Change property 'path' of 'Edge:induce'", "Change property 'path' of 'Edge:induce'",
                "Change property 'path' of 'Edge:induce'", "Change property 'path' of 'Edge:induce'",
                "Change property 'path' of 'Edge:induce'"};
        assertEquals("Number of edits", editNames2.length, edits.size());
        for(int i=0; i<editNames2.length; i++)
        {
            assertEquals("Edit#"+(i+1), editNames2[i], edits.get(i).toString());
        }
        statesDC.put(state2);

        Diagram d1Clone = d1.clone(diagramsDC, "DGR0004_prop");
        DiagramStateUtility.applyState(d1Clone, state);
        assertEquals("Diagram size", d2.getSize(), d1Clone.getSize());
        State cloneState = DiagramStateUtility.createState(d1Clone, d2, "ditto");
        assertEquals("Number of edits", 0, cloneState.getStateUndoManager().getEdits().size());
    }

    public void testWorkflowState() throws Exception
    {
        DataCollection<Diagram> diagramDC = DataElementPath.create("databases/test/state/DML").getDataCollection(Diagram.class);
        Diagram d1 = diagramDC.get("Workflow");
        assertNotNull("Workflow", d1);
        Diagram d2 = diagramDC.get("Workflow_1");
        assertNotNull("Workflow_1", d2);
        State state = DiagramStateUtility.createState(d1, d2, "changes_Workflow");
        List<UndoableEdit> edits = state.getStateUndoManager().getEdits();
        String[] editNames = new String[] {"Add Annotate table to Workflow", "Change property 'attributes/parameter-rank' of 'Node:Output'", "Change property 'attributes/parameter-reference-type' of 'Node:Output'", "Add Result to Workflow", "Add Output->inputTablePath to Workflow", "Add outputTablePath->Result to Workflow"};
        assertEquals("Number of edits", editNames.length, edits.size());
        for(int i=0; i<editNames.length; i++)
        {
            assertEquals("Edit#"+(i+1), editNames[i], edits.get(i).toString());
        }
        statesDC.put(state);
        State state2 = DiagramStateUtility.createState(d2, d1, "changes_Workflow_back");
        edits = state2.getStateUndoManager().getEdits();
        String[] editNames2 = new String[] {"Remove Output->inputTablePath from Workflow_1", "Remove outputTablePath->Result from Workflow_1", "Remove Annotate table from Workflow_1", "Remove Result from Workflow_1", "Change property 'attributes/parameter-rank' of 'Node:Output'", "Change property 'attributes/parameter-reference-type' of 'Node:Output'"};
        assertEquals("Number of edits", editNames2.length, edits.size());
        for(int i=0; i<editNames2.length; i++)
        {
            assertEquals("Edit#"+(i+1), editNames2[i], edits.get(i).toString());
        }
        statesDC.put(state2);
        
        statesDC.release(state.getName());
        state = statesDC.get(state.getName());
        edits = state.getStateUndoManager().getEdits();
        assertEquals("Number of edits", editNames.length, edits.size());
        for(int i=0; i<editNames.length; i++)
        {
            assertEquals("Edit#"+(i+1), editNames[i], edits.get(i).toString());
        }

        Diagram d1Clone = d1.clone(diagramsDC, "Workflow_1");
        DiagramStateUtility.applyState(d1Clone, state);
        assertEquals("Diagram size", d2.getSize(), d1Clone.getSize());
        State cloneState = DiagramStateUtility.createState(d1Clone, d2, "ditto");
        assertEquals("Number of edits", 0, cloneState.getStateUndoManager().getEdits().size());
    }
}

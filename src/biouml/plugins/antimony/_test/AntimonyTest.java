package biouml.plugins.antimony._test;

import java.io.File;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.Preferences;

import biouml.model.Diagram;
import biouml.plugins.antimony.AntimonyEditor;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.standard.type.DiagramInfo;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;

public class AntimonyTest extends AbstractBioUMLTest
{
    public AntimonyTest(String name)
    {
        super(name);
    }

    Diagram antimonyDiagram;
    AntimonyEditor editor;

    protected void preprocess(String antimonyText) throws Exception
    {
        setPreferences();
        //create diagram
        antimonyDiagram = new SbgnDiagramType().createDiagram(null, "diagramTest", new DiagramInfo("diagramTest"));

        //create editor
        editor = new AntimonyEditor();
        editor.explore(antimonyDiagram, null);

        //TODO: remove this hack (rework to emulate real work of AntimonyEditor)
        //editor.getAntimonyOptions().setAdditionalProperties(""); // HACK to get empty antimony options

        //add text and change diagram
        editor.setText(antimonyText);
        antimonyDiagram = editor.getAntimony().generateDiagram(editor.getText(), false);
        AntimonyEditor.addListeners(antimonyDiagram, null, editor);
        editor.setDiagram(antimonyDiagram);
    }

    protected void compareResult(String result) throws Exception
    {
        //compare results
        String antimonyText = editor.getText();
        String resultText = ApplicationUtils.readAsString(new File(result));
        antimonyText = AntimonyTestUtil.clean(antimonyText);
        resultText = AntimonyTestUtil.clean(resultText);
        assertEquals(resultText, antimonyText);
    }

    protected void setPreferences() throws Exception
    {
        CollectionFactory.createRepository("../data");
        Application.setPreferences(new Preferences());
    }
}

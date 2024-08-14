

package biouml.plugins.test.editors;

import java.util.List;

import one.util.streamex.StreamEx;

import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.plugins.test.tests.TestVariable;
import biouml.standard.diagram.Util;

import com.developmentontheedge.beans.editors.StringTagEditor;

public class TestVariableDiagramEditor extends StringTagEditor
{
    @Override
    public String[] getTags()
    {
        TestVariable var = (TestVariable)getBean();
        Diagram d = var.getCurrentDiagram();
        List<SubDiagram> subDiagrams = Util.getSubDiagrams(d);
        return StreamEx.of( subDiagrams ).map( SubDiagram::getName ).prepend( d.getName() ).sorted().toArray( String[]::new );
    }
}

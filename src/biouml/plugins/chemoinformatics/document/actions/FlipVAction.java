package biouml.plugins.chemoinformatics.document.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.gui.Document;
import biouml.plugins.chemoinformatics.document.StructureDocument;
import biouml.plugins.chemoinformatics.document.StructurePanel;

public class FlipVAction extends AbstractAction
{
    public static final String KEY = "Flip structure vertical";

    public FlipVAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Document activeDocument = Document.getActiveDocument();
        if( activeDocument instanceof StructureDocument )
        {
            StructurePanel structurePanel = ( (StructureDocument)activeDocument ).getStructurePanel();
            structurePanel.get2DHub().flip(false);
            structurePanel.get2DHub().updateView();
        }
    }
}

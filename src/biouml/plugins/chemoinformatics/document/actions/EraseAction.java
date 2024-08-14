package biouml.plugins.chemoinformatics.document.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openscience.jchempaint.controller.RemoveModule;

import ru.biosoft.gui.Document;
import biouml.plugins.chemoinformatics.document.StructureDocument;
import biouml.plugins.chemoinformatics.document.StructurePanel;

public class EraseAction extends AbstractAction
{
    public static final String KEY = "Erase structure";

    public EraseAction()
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
            RemoveModule newActiveModule = new RemoveModule(structurePanel.get2DHub());
            newActiveModule.setID("eraser");
            structurePanel.get2DHub().setActiveDrawModule(newActiveModule);
            structurePanel.get2DHub().updateView();
        }
    }
}

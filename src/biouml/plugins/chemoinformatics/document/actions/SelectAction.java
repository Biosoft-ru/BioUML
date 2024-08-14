package biouml.plugins.chemoinformatics.document.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openscience.jchempaint.controller.IControllerModule;
import org.openscience.jchempaint.controller.SelectSquareModule;

import ru.biosoft.gui.Document;
import biouml.plugins.chemoinformatics.document.StructureDocument;
import biouml.plugins.chemoinformatics.document.StructurePanel;

public class SelectAction extends AbstractAction
{
    public static final String KEY = "Select structure";

    public SelectAction()
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
            IControllerModule newActiveModule = new SelectSquareModule(structurePanel.get2DHub());

            newActiveModule.setID("select");
            structurePanel.get2DHub().setActiveDrawModule(newActiveModule);
        }
    }
}

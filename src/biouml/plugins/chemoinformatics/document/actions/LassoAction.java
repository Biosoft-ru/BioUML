package biouml.plugins.chemoinformatics.document.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openscience.jchempaint.controller.IControllerModule;
import org.openscience.jchempaint.controller.SelectLassoModule;

import ru.biosoft.gui.Document;
import biouml.plugins.chemoinformatics.document.StructureDocument;
import biouml.plugins.chemoinformatics.document.StructurePanel;

public class LassoAction extends AbstractAction
{
    public static final String KEY = "Lasso structure";

    public LassoAction()
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
            IControllerModule newActiveModule = new SelectLassoModule(structurePanel.get2DHub());

            newActiveModule.setID("lasso");
            structurePanel.get2DHub().setActiveDrawModule(newActiveModule);
        }
    }
}

package biouml.plugins.chemoinformatics.document.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openscience.cdk.renderer.RendererModel;
import org.openscience.jchempaint.renderer.JChemPaintRendererModel;

import ru.biosoft.gui.Document;
import biouml.plugins.chemoinformatics.document.StructureDocument;
import biouml.plugins.chemoinformatics.document.StructurePanel;

public class ZoomOutAction extends AbstractAction
{
    public static final String KEY = "Zoom out structure";

    public ZoomOutAction()
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
            RendererModel rendererModel = structurePanel.getRenderPanel().getRenderer().getRenderer2DModel();
            if( rendererModel instanceof JChemPaintRendererModel )
            {
                JChemPaintRendererModel jcpModel = (JChemPaintRendererModel)rendererModel;
                jcpModel.setZoomFactor(jcpModel.getZoomFactor() * 0.8);
            }

            structurePanel.get2DHub().updateView();
            structurePanel.updateStatusBar();
            structurePanel.getRenderPanel().update(structurePanel.getRenderPanel().getGraphics());
        }
    }
}

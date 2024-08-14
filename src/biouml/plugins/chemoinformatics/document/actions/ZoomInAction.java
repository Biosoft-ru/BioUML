package biouml.plugins.chemoinformatics.document.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openscience.jchempaint.renderer.RendererModel;

import ru.biosoft.gui.Document;
import biouml.plugins.chemoinformatics.document.StructureDocument;
import biouml.plugins.chemoinformatics.document.StructurePanel;

public class ZoomInAction extends AbstractAction
{
    public static final String KEY = "Zoom in structure";

    public ZoomInAction()
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
            double zoom = rendererModel.getZoomFactor();

            rendererModel.setZoomFactor(zoom * 1.2);

            structurePanel.get2DHub().updateView();
            structurePanel.updateStatusBar();
            structurePanel.getRenderPanel().update(structurePanel.getRenderPanel().getGraphics());
        }
    }
}

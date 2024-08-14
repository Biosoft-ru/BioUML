package biouml.plugins.chemoinformatics.document;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Action;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.MoleculeSet;
import org.openscience.cdk.interfaces.IMolecule;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;

import biouml.plugins.chemoinformatics.JavaScriptCDK;
import biouml.plugins.chemoinformatics.MessageBundle;
import biouml.plugins.chemoinformatics.document.actions.EraseAction;
import biouml.plugins.chemoinformatics.document.actions.FlipHAction;
import biouml.plugins.chemoinformatics.document.actions.FlipVAction;
import biouml.plugins.chemoinformatics.document.actions.LassoAction;
import biouml.plugins.chemoinformatics.document.actions.SelectAction;
import biouml.plugins.chemoinformatics.document.actions.ZoomInAction;
import biouml.plugins.chemoinformatics.document.actions.ZoomOutAction;
import biouml.standard.type.CDKRenderer;
import biouml.standard.type.Structure;
import ru.biosoft.access.exception.BiosoftParseException;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.SaveDocumentAction;

/**
 * Structure document
 * Open structure in JChemPaint-based editor
 */
public class StructureDocument extends Document implements PropertyChangeListener
{
    protected static final Logger log = Logger.getLogger(StructureDocument.class.getName());

    protected Structure structure;
    protected StructurePanel structurePanel;

    public StructureDocument(Structure structure)
    {
        super(structure);
        viewPane = new ViewPane();

        this.structure = structure;

        if( structure != null )
        {
            try
            {
                IMolecule molecule = CDKRenderer.loadMolecule( structure );
                MoleculeSet moleculeSet = new MoleculeSet();
                moleculeSet.addMolecule(molecule);
                ChemModel chemModel = new ChemModel();
                chemModel.setMoleculeSet(moleculeSet);

                structurePanel = new StructurePanel(chemModel);
                structurePanel.addPropertyChangeListener(this);
                viewPane.add(structurePanel);
            }
            catch( BiosoftParseException e )
            {
                log.log(Level.SEVERE, "Can not parse structure", e);
            }
        }
    }

    /**
     * Get structure panel for current document
     */
    public StructurePanel getStructurePanel()
    {
        return structurePanel;
    }

    @Override
    public String getDisplayName()
    {
        return ( (Structure)getModel() ).getName();
    }

    @Override
    public boolean isChanged()
    {
        return structurePanel.isModified();
    }

    @Override
    public boolean isMutable()
    {
        if( ( structure.getOrigin() != null ) && structure.getOrigin().isMutable() )
        {
            return true;
        }
        return false;
    }

    @Override
    public void save()
    {
        try
        {
            String data = JavaScriptCDK.getStructureData(structurePanel.getChemModel());
            structure.setData(data);
            if( structure.getOrigin() != null )
            {
                structure.getOrigin().put(structure);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not save structure", e);
        }
        super.save();
    }

    private static boolean actionInitialized = false;

    @Override
    public Action[] getActions(ActionType actionType)
    {
        ActionManager actionManager = Application.getActionManager();
        if( !actionInitialized )
        {
            actionInitialized = true;
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);

            Action action = new ZoomInAction();
            actionManager.addAction(ZoomInAction.KEY, action);
            initializer.initAction(action, ZoomInAction.KEY);

            action = new ZoomOutAction();
            actionManager.addAction(ZoomOutAction.KEY, action);
            initializer.initAction(action, ZoomOutAction.KEY);

            action = new FlipHAction();
            actionManager.addAction(FlipHAction.KEY, action);
            initializer.initAction(action, FlipHAction.KEY);

            action = new FlipVAction();
            actionManager.addAction(FlipVAction.KEY, action);
            initializer.initAction(action, FlipVAction.KEY);

            action = new SelectAction();
            actionManager.addAction(SelectAction.KEY, action);
            initializer.initAction(action, SelectAction.KEY);

            action = new LassoAction();
            actionManager.addAction(LassoAction.KEY, action);
            initializer.initAction(action, LassoAction.KEY);

            action = new EraseAction();
            actionManager.addAction(EraseAction.KEY, action);
            initializer.initAction(action, EraseAction.KEY);
        }
        if( actionType == ActionType.TOOLBAR_ACTION )
        {
            Action zoomInAction = actionManager.getAction(ZoomInAction.KEY);
            Action zoomOutAction = actionManager.getAction(ZoomOutAction.KEY);
            Action flipHAction = actionManager.getAction(FlipHAction.KEY);
            Action flipVAction = actionManager.getAction(FlipVAction.KEY);
            Action selectAction = actionManager.getAction(SelectAction.KEY);
            Action lassoAction = actionManager.getAction(LassoAction.KEY);
            Action eraseAction = actionManager.getAction(EraseAction.KEY);

            return new Action[] {flipVAction, flipHAction, zoomInAction, zoomOutAction, eraseAction, lassoAction, selectAction};
        }
        return null;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        Application.getActionManager().enableActions( true, SaveDocumentAction.KEY );
    }
}

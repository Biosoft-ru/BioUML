package biouml.plugins.chemoinformatics.document;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.EventObject;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.event.ICDKChangeListener;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.jchempaint.AbstractJChemPaintPanel;
import org.openscience.jchempaint.RenderPanel;
import org.openscience.jchempaint.SwingPopupModule;
import org.openscience.jchempaint.action.SaveAction;
import org.openscience.jchempaint.applet.JChemPaintEditorApplet;
import org.openscience.jchempaint.controller.ControllerHub;
import org.openscience.jchempaint.controller.IChangeModeListener;
import org.openscience.jchempaint.controller.IChemModelEventRelayHandler;
import org.openscience.jchempaint.controller.IControllerModule;
import org.openscience.jchempaint.controller.MoveModule;
import org.openscience.jchempaint.renderer.RendererModel;
import org.openscience.jchempaint.renderer.selection.AbstractSelection;

public class StructurePanel extends AbstractJChemPaintPanel implements IChemModelEventRelayHandler, ICDKChangeListener, KeyListener,
        IChangeModeListener
{
    private static final String[] LEFT_ACTIONS = new String[] {"bondTool", "double_bondTool", "triple_bondTool", "up_bond", "down_bond",
            "undefined_bond", "undefined_stereo_bond", "reactionArrow"};
    private static final String[] RIGHT_ACTIONS = new String[] {"triangle", "square", "pentagon", "hexagon", "benzene", "octagon",
            "pasteTemplate"};
    private static final String[] BOTTOM_ACTIONS = new String[] {"C", "H", "O", "N", "P", "S", "F", "Cl", "Br", "I", "enterR", "plus",
            "minus", "enterelement", "periodictable"};
    private JComponent lastActionButton;
    private JComponent lastSecondaryButton;
    private boolean modified = false;
    private JPanel centerContainer = null;
    private JToolBar lefttoolbar;
    private JToolBar lowertoolbar;
    private JToolBar righttoolbar;
    private String lastSelectId;

    public StructurePanel(IChemModel chemModel)
    {
        this.setLayout(new BorderLayout());
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setLayout(new BorderLayout());
        this.add(topContainer, BorderLayout.NORTH);
        try
        {
            renderPanel = new RenderPanel(chemModel, getWidth(), getHeight(), false, debug, false, null);
        }
        catch( IOException e )
        {
            announceError(e);
        }
        renderPanel.getHub().addChangeModeListener(this);
        renderPanel.setName("renderpanel");
        centerContainer = new JPanel();
        centerContainer.setLayout(new BorderLayout());
        centerContainer.add(new JScrollPane(renderPanel), BorderLayout.CENTER);
        this.add(centerContainer);

        customizeView();
        SwingPopupModule inputAdapter = new SwingPopupModule(renderPanel, renderPanel.getHub());
        //setupPopupMenus(inputAdapter);
        renderPanel.getHub().registerGeneralControllerModule(inputAdapter);
        renderPanel.getHub().setEventHandler(this);
        renderPanel.getRenderer().getRenderer2DModel().addCDKChangeListener(this);
        //we set this to true always, the user should have no option to switch it off
        renderPanel.getHub().getController2DModel().setAutoUpdateImplicitHydrogens(true);
        this.addKeyListener(this);
        renderPanel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseExited(MouseEvent e)
            {
                //this avoids ghost phantom rings if the user leaves the panel
                StructurePanel.this.get2DHub().clearPhantoms();
                StructurePanel.this.get2DHub().updateView();
            }
        });
    }

    /**
     * Helps in keeping the current action button highlighted
     *
     * @return The last action button used
     */
    @Override
    public JComponent getLastActionButton()
    {
        return lastActionButton;
    }

    @Override
    public boolean isModified()
    {
        return this.modified;
    }

    @Override
    public void setModified(boolean modified)
    {
        this.modified = modified;
    }

    /**
     * Helps in keeping the current action button highlighted - needs to be set
     * if a new action button is choosen
     *
     * @param actionButton
     *            The new action button
     */
    @Override
    public void setLastActionButton(JComponent actionButton)
    {
        lastActionButton = actionButton;
    }

    /**
     * Shows and hides menus, statusbar, toolbars according to settings.
     */
    @Override
    public void customizeView()
    {
        if( lefttoolbar == null )
        {
            lefttoolbar = StructureToolbar.createToolbar(this, SwingConstants.VERTICAL, LEFT_ACTIONS);
        }
        centerContainer.add(lefttoolbar, BorderLayout.WEST);
        if( righttoolbar == null )
        {
            righttoolbar = StructureToolbar.createToolbar(this, SwingConstants.VERTICAL, RIGHT_ACTIONS);
        }
        centerContainer.add(righttoolbar, BorderLayout.EAST);
        if( lowertoolbar == null )
        {
            lowertoolbar = StructureToolbar.createToolbar(this, SwingConstants.HORIZONTAL, BOTTOM_ACTIONS);
        }
        centerContainer.add(lowertoolbar, BorderLayout.SOUTH);
        revalidate();
    }

    /**
     * Gets the current gui configuration string of this panel.
     * 
     * @return The current gui configuration string of this panel.
     */
    @Override
    public String getGuistring()
    {
        return guistring;
    }

    /**
     * Gets the SVG of the chemical entities in this panel.
     * 
     * @return The SVG of the chemical entities in this panel.
     */
    @Override
    public String getSVGString()
    {
        return this.renderPanel.toSVG();
    }

    /**
     * Takes an image snapshot of this panel.
     * 
     * @return The snapshot.
     */
    @Override
    public Image takeSnapshot()
    {
        return this.renderPanel.takeSnapshot();
    }

    /**
     * Shows a warning if the JCPPanel has unsaved content and does save, if the
     * user wants to do it.
     *
     * @return
     *         OptionPane.YES_OPTION/OptionPane.NO_OPTION/OptionPane.CANCEL_OPTION
     */
    @Override
    public int showWarning()
    {
        if( modified && !guistring.equals(JChemPaintEditorApplet.GUI_APPLET) )
        {
            int answer = JOptionPane.showConfirmDialog(this, renderPanel.getChemModel().getID() + " "
                    + "has unsaved data. Do you want to save it?", "Unsaved data", JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if( answer == JOptionPane.YES_OPTION )
            {
                SaveAction saveaction = new SaveAction(this, false);
                saveaction.actionPerformed(new ActionEvent(this, 12, ""));
                if( saveaction.getWasCancelled() )
                    answer = JOptionPane.CANCEL_OPTION;
            }
            return answer;
        }
        else if( guistring.equals(JChemPaintEditorApplet.GUI_APPLET) )
        {
            return JOptionPane.YES_OPTION;
        }
        else
        {
            return JOptionPane.YES_OPTION;
        }
    }

    @Override
    public void coordinatesChanged()
    {
        setModified(true);
        updateStatusBar();
    }

    @Override
    public void selectionChanged()
    {
        updateStatusBar();
    }

    @Override
    public void structureChanged()
    {
        setModified(true);
        firePropertyChange("data", null, null);
        updateStatusBar();
        //if something changed in the structure, selection should be cleared
        //this is behaviour like eg in word processors, if you type, selection goes away
        this.getRenderPanel().getRenderer().getRenderer2DModel().setSelection(AbstractSelection.EMPTY_SELECTION);
        updateUndoRedoControls();
        this.get2DHub().updateView();
    }

    @Override
    public void structurePropertiesChanged()
    {
        setModified(true);
        updateStatusBar();
        //if something changed in the structure, selection should be cleared
        //this is behaviour like eg in word processors, if you type, selection goes away
        this.getRenderPanel().getRenderer().getRenderer2DModel().setSelection(AbstractSelection.EMPTY_SELECTION);
    }

    @Override
    public void stateChanged(EventObject event)
    {
        // update undo/redo controls
    }

    @Override
    public void keyPressed(KeyEvent arg0)
    {
    }

    @Override
    public void keyReleased(KeyEvent arg0)
    {
        RendererModel model = renderPanel.getRenderer().getRenderer2DModel();
        ControllerHub relay = renderPanel.getHub();
        if( model.getHighlightedAtom() != null )
        {
            try
            {
                IAtom closestAtom = model.getHighlightedAtom();
                char x = arg0.getKeyChar();
                if( Character.isLowerCase(x) )
                    x = Character.toUpperCase(x);
                IsotopeFactory ifa;
                ifa = IsotopeFactory.getInstance(closestAtom.getBuilder());
                IIsotope iso = ifa.getMajorIsotope(Character.toString(x));
                if( iso != null )
                {
                    relay.setSymbol(closestAtom, Character.toString(x));
                }
                this.get2DHub().updateView();
            }
            catch( IOException e )
            {
                announceError(e);
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent arg0)
    {
    }

    @Override
    public void zoomChanged()
    {
        this.updateStatusBar();
    }

    @Override
    public void modeChanged(IControllerModule newActiveModule)
    {
        //we set the old button to inactive colour
        if( this.getLastActionButton() != null )
            this.getLastActionButton().setBackground(Color.WHITE);
        if( this.lastSecondaryButton != null )
            this.lastSecondaryButton.setBackground(Color.WHITE);
        String actionid = newActiveModule.getID();
        //this is because move mode does not have a button
        if( actionid.equals("move") )
            actionid = lastSelectId;
        //we remember the last activated move mode so that we can switch back to it after move
        if( newActiveModule.getID().equals("select") || newActiveModule.getID().equals("lasso") )
            lastSelectId = newActiveModule.getID();
        if( ! ( newActiveModule instanceof MoveModule ) )
        {
            this.renderPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            this.get2DHub().updateView();
        }
        this.updateStatusBar();
    }

    /**
     * Sets the lastSecondaryButton attribute. Only to be used once from JCPToolBar.
     * 
     * @param lastSecondaryButton The lastSecondaryButton.
     */
    @Override
    public void setLastSecondaryButton(JComponent lastSecondaryButton)
    {
        this.lastSecondaryButton = lastSecondaryButton;
    }

    @Override
    public void updateStatusBar()
    {
        //TODO:
    }

    @Override
    public void updateUndoRedoControls()
    {
        //TODO:
    }
}

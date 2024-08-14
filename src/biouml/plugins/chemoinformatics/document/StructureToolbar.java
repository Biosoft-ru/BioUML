package biouml.plugins.chemoinformatics.document;

import java.awt.Color;
import java.awt.Insets;
import java.net.URL;
import java.util.logging.Level;
import java.util.MissingResourceException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicBorders;

import java.util.logging.Logger;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.jchempaint.JCPMenuTextMaker;
import org.openscience.jchempaint.JCPPropertyHandler;
import org.openscience.jchempaint.action.JCPAction;
import org.openscience.jchempaint.controller.AddBondDragModule;

/**
 * Toolbar for structure editor
 */
public class StructureToolbar
{
    protected static final Logger log = Logger.getLogger(StructureToolbar.class.getName());

    public static JToolBar createToolbar(StructurePanel parent, int orientation, String[] actions)
    {
        JToolBar toolbar2 = new JToolBar(orientation);
        JButton button = null;
        Box box = null;
        int counter = 0;
        for( String action : actions )
        {
            if( action.equals("-") )
            {
                toolbar2.add(box);
                if( orientation == SwingConstants.HORIZONTAL )
                {
                    toolbar2.add(Box.createHorizontalStrut(5));
                }
                else if( orientation == SwingConstants.VERTICAL )
                {
                    toolbar2.add(Box.createVerticalStrut(5));
                }
                counter = 0;
            }
            else
            {
                if( box != null )
                    toolbar2.add(box);
                box = new Box(BoxLayout.Y_AXIS);
                button = createToolbarButton(action, parent, action.length() < 3);
                /*if (toolKeys[i].equals("lasso"))
                {
                    selectButton = button;
                }*/
                if( button != null )
                {
                    box.add(button);
                    if( action.equals("bond") )
                    {
                        //button.setBackground(Color.GRAY);
                        button.setBackground(new Color(238, 238, 238));
                        parent.setLastActionButton(button);
                        AddBondDragModule activeModule = new AddBondDragModule(parent.get2DHub(), IBond.Stereo.NONE, true);
                        activeModule.setID(action);
                        parent.get2DHub().setActiveDrawModule(activeModule);
                        parent.updateStatusBar();
                    }
                    else if( action.equals("C") )
                    {
                        button.setBackground(Color.GRAY);
                        parent.setLastSecondaryButton(button);
                    }
                    else
                    {
                        button.setBackground(Color.white);
                    }
                }
                else
                {
                    log.log(Level.SEVERE, "Could not create button" + action);
                }
                counter++;
            }
        }
        if( box != null )
            toolbar2.add(box);
        if( orientation == SwingConstants.HORIZONTAL )
        {
            toolbar2.add(Box.createHorizontalGlue());
        }
        return toolbar2;
    }

    static JButton createToolbarButton(String key, StructurePanel parent, boolean elementtype)
    {
        JCPPropertyHandler jcpph = JCPPropertyHandler.getInstance(false);
        JButton b = null;


        URL url = jcpph.getResource(key + JCPAction.imageSuffix);
        if( url == null )
        {
            log.log(Level.SEVERE, "Cannot find resource: " + key + JCPAction.imageSuffix);
            return null;
        }
        ImageIcon image = new ImageIcon(url);
        b = new JButton(image)
        {
            @Override
            public float getAlignmentY()
            {
                return 0.5f;
            }
        };
        String astr = null;
        if( elementtype )
            astr = jcpph.getResourceString("symbol" + key + JCPAction.actionSuffix);
        else
            astr = jcpph.getResourceString(key + JCPAction.actionSuffix);

        if( astr == null )
        {
            astr = key;
        }

        JCPAction a = new JCPAction().getAction(parent, astr);
        if( a != null )
        {
            b.setActionCommand(astr);
            log.log(Level.FINE, "Coupling action to button...");
            b.addActionListener(a);
            b.setEnabled(a.isEnabled());
        }
        else
        {
            log.log(Level.SEVERE, "Could not find JCPAction class for:" + astr);
            b.setEnabled(false);
        }
        try
        {
            String tip = JCPMenuTextMaker.getInstance("applet").getText(key + JCPAction.TIPSUFFIX);
            if( tip != null )
            {
                b.setToolTipText(tip);
            }
        }
        catch( MissingResourceException e )
        {
            log.warning("Could not find Tooltip resource for: " + key);
        }
        URL disabledurl = jcpph.getResource(key + JCPAction.disabled_imageSuffix);
        if( disabledurl != null )
        {
            ImageIcon disabledimage = new ImageIcon(disabledurl);
            b.setDisabledIcon( disabledimage );
        }
        b.setRequestFocusEnabled(false);
        b.setMargin(new Insets(1, 1, 1, 1));
        b.setName(key);
        //parent.buttons.put(key, b);

        CompoundBorder compBorder1 = new CompoundBorder(new EmptyBorder(0, 0, 0, 0), new LineBorder(new Color(164, 164, 164), 1, true));
        Color highlighter = new Color(80, 144, 166);
        CompoundBorder compBorder2 = new CompoundBorder(new BasicBorders.RolloverButtonBorder(highlighter, highlighter, highlighter,
                highlighter), compBorder1);
        b.setBorder(compBorder2);
        return b;
    }
}

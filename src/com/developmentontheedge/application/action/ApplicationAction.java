
package com.developmentontheedge.application.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

/**
 * The <code>ApplicationAction</code> class extends functionality of AbstractAction
 */
@SuppressWarnings ( "serial" )
public class ApplicationAction extends AbstractAction
{
    /** Action key for sending of any parameters to action*/
    public static final String PARAMETER = "Parameter";

    public ApplicationAction(Class<? extends ResourceBundle> resourceClass, String key)
    {
        new ActionInitializer( resourceClass ).initAction( this, key );
    }

    public ApplicationAction(String name, String cmd)
    {
        putValue(NAME, name);
        putValue(ACTION_COMMAND_KEY,  cmd );
    }

    /**
     *
     * Defines an <code>Action</code> object with the specified
     * name,long/short description string,mnemonic key,icon, command string and the listener.
     *
     * @param name of action
     * @param shortDesc short description of action
     * @param longDesc long description of action
     * @param mnemonic hot key
     * @param imageFile image for icon on button or menu items
     * @param cmd corresponding string command
     * @param listener listener of action
     */
    public ApplicationAction(String name, String shortDesc, String longDesc, int mnemonic, String imageFile,
                             String cmd, ActionListener listener)
    {
        this(name,shortDesc,longDesc,mnemonic,imageFile,listener);
        putValue(ACTION_COMMAND_KEY,  cmd );
    }

    /**
     * Defines an <code>Action</code> object with the specified
     * name,long/short description string,mnemonic key,icon and action listener.
     * @param name       name of action
     * @param shortDesc  shortDesc short description of action
     * @param longDesc   longDesc long description of action
     * @param mnemonic   mnemonic hot key
     * @param imageFile  image for icon  on button or menu items
     * @param listener   listener listener of action
     */
    public ApplicationAction(String name, String shortDesc,String longDesc,int mnemonic,String imageFile,ActionListener listener)
    {
        putValue(NAME, name);
        putValue(SHORT_DESCRIPTION,  shortDesc );
        putValue(LONG_DESCRIPTION,   longDesc);
        putValue(MNEMONIC_KEY,       mnemonic);

        if (imageFile!=null)
        {
            URL url =  getClass().getResource("resources/"+imageFile);
            if( url != null )
                putValue( SMALL_ICON, new ImageIcon( url ) );
        }

        if (listener != null)
            addActionListener(listener);
    }

    /**
     * Defines an <code>Action</code> object with the specified
     * name,long/short description string,mnemonic key,icon .
     * @param name              name of  action
     * @param shortDesc         shortDesc short description of  action
     * @param longDesc          longDesc long description of  action
     * @param mnemonic          mnemonic hot key
     * @param imageFile         image for icon on button or menu items
     */
    public ApplicationAction(String name, String shortDesc,String longDesc,int mnemonic,String imageFile)
    {
        this(name,shortDesc,longDesc,mnemonic,imageFile,null);
    }

    /**
     * Overridden method for translation action events to the all listeners
     * @param evt ActionEvent
     */
    @Override
    public void actionPerformed(ActionEvent evt)
    {
        fireActionPerformed(evt) ;
    }

    public void removeActionListeners(Class<? extends ActionListener> clazz)
    {
        for(int i=actionListeners.size()-1; i>=0; i--)
        {
            ActionListener l = actionListeners.get(i);
            if ( clazz == l.getClass() )
                actionListeners.remove(l);
        }
    }

    /**
     * Adds the specified action listener to receive action event .
     */
    public void  addActionListener(ActionListener l)
    {

        if (actionListeners == null)
            actionListeners = new Vector<>();

        actionListeners.add(l);
    }

    protected void  fireActionPerformed(ActionEvent evt)
    {
        ActionEvent event = new ActionEvent(this, evt.getID(), evt.getActionCommand());
        if (actionListeners != null)
        {
            for (int i=actionListeners.size()-1; i>=0; i--)
            {
                ActionListener l = actionListeners.get(i);
                if (l!=null)
                    l.actionPerformed(event) ;
            }
        }
    }

    /** Action listener*/
    private Vector<ActionListener> actionListeners = null;

    ////////////////////////////////////////////////////////////////////////////
    // Actions
    //
/*
    public class ActionNew extends ApplicationAction
    {
        public ActionNew()
        {
            super("New", "New project","Create new project",'N',"new.gif",null);
        }

        /**
         * Overridden method for translation action events to the all listeners
         * @param evt ActionEvent
         *
        public void actionPerformed(ActionEvent evt)
        {
            boolean openEnabled = true;
            Action openAction = Application.getActionManager().getAction( ApplicationActionManager.ACTION_OPEN );
            if( openAction!=null )
            {
                openEnabled = openAction.isEnabled();
                openAction.setEnabled( false );
            }
            setEnabled( false );
            new NewThread( openEnabled ).start();
        }

        private class NewThread extends Thread
        {
            private boolean openEnabled = true;

            public NewThread( boolean openEnabled )
            {
                this.openEnabled = openEnabled;
            }
            public void run()
            {
                Object doc = aFrame.getDocumentFactory().createDocument();
                Action openAction = aFrame.getActionManager().getAction( ApplicationActionManager.ACTION_OPEN );
                if( openAction!=null )
                {
                    if( doc!=null )
                        openAction.setEnabled( true );
                    else
                        openAction.setEnabled( openEnabled );
                }
                setEnabled( true );
            }
        }
    }

    public class ActionOpen extends ApplicationAction
    {
        public ActionOpen()
        {
            super("Open", "Open project", "Open project", 'O', "open.gif", null);
        }

        /**
         * Overridden method for translation action events to the all listeners
         * @param evt ActionEvent
         *
        public void actionPerformed(final ActionEvent e)
        {
            final String name = (String)getValue( ApplicationAction.PARAMETER );
            putValue(ApplicationAction.PARAMETER,null);

            Action newAction = aFrame.getActionManager().getAction( ApplicationActionManager.ACTION_NEW );
            if( newAction!=null )
                newAction.setEnabled( false );
            setEnabled( false );
            (new Thread()
             {
                 public void run()
                 {
                    if ( name  != null ) aFrame.getDocumentFactory().openDocument( name );
                    else                 aFrame.getDocumentFactory().openDocumentDialog();

                    setEnabled( true );
                    Action newAction = aFrame.getActionManager().getAction( ApplicationActionManager.ACTION_NEW );
                    if( newAction!=null )
                        newAction.setEnabled( true );
                 }
             }).start();
        }
    }

    public static class ActionSave extends ApplicationAction
    {
        public ActionSave(ActionListener l)
        {
            super("Save","Save current object","Save",'S',"save.gif",l);
        }
    }

    public static class ActionSaveAs extends ApplicationAction
    {
        public ActionSaveAs(ActionListener l)
        {
            super("Save as...", "Save current object As ...", "Save as", 'A', "saveas.gif", l);
        }
    }

    public class ActionClose extends ApplicationAction
    {
        ApplicationFrame frame;
        public ActionClose(ApplicationFrame frame)
        {
            super("Close","Close project","Close project",'C',"close.gif",null);
            this.frame = frame;
        }

        /**
         * Overridden method for translation action events to the all listeners
         * @param evt ActionEvent
         *
        public void actionPerformed(ActionEvent evt)
        {
            ApplicationDocument doc = frame.getCurrentDocument();

            if (doc == null)
                return;

            frame.getDocumentFactory().closeDocument( doc );
        }
    }

    public class ActionDelete extends ApplicationAction
    {
        public ActionDelete( ActionListener l )
        {
            super("Delete", "Delete resource", "Delete...", '+', "delete.gif", ApplicationActionManager.ACTION_DELETE, l);
        }
    }
*/
    public static class ActionExit extends ApplicationAction
    {
        public ActionExit(ActionListener l)
        {
            super("Exit", "Exit from application", "exit", '\33', "exit.gif", l);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
/*
    public class ActionZoomIn extends ApplicationAction
    {
        public ActionZoomIn(ActionListener l)
        {
            super("Zoom in","scale image","scale image",'+',"zoomin.gif", ApplicationActionManager.ACTION_ZOOM_IN ,l);
        }
    }

    public class ActionZoomOut extends ApplicationAction
    {
        public ActionZoomOut( ActionListener l )
        {
            super("Zoom out","scale image","scale image",'+',"zoomout.gif",ApplicationActionManager.ACTION_ZOOM_OUT,l);
        }
    }

    public static class ActionEditPreferences extends ApplicationAction
    {
        public ActionEditPreferences(ActionListener l)
        {
            super("Preferences...","Edit application preferences","Preferences",'P',"preferrences.gif", l);
        }
    }
*/
    public static class ActionHelp extends ApplicationAction
    {
        public ActionHelp(ActionListener l)
        {
            super("Help","Help","Help",'F',"help.gif",l);
        }
    }

    public static class ActionAboutDialog extends ApplicationAction
    {
        public ActionAboutDialog(ActionListener l)
        {
           super("About...", "About", "About application", 'A', /*"about.gif" - repaint */ null, l);
        }
    }


    ////////////////////////////////////////////////////////////////////////////

     public static class ActionPrintSetup extends ApplicationAction
     {
        public ActionPrintSetup(ActionListener l)
        {
            super("Print Setup...", "Change the printer and printing options", "Print Setup", 'T', "printSetup.gif", l);
        }
    }

    public static class ActionPrintPreview extends ApplicationAction
    {
        public ActionPrintPreview(ActionListener l)
        {
            super("Print Preview...", "Previews how this document will print", "Print Preview", 'V', "printPreview.gif", l);
        }
    }

    public static class ActionPrint extends ApplicationAction
    {
        public ActionPrint(ActionListener l)
        {
           super("Print...", "Print current document or selection", "print", 'P', "print.gif", l);
        }
    }
}


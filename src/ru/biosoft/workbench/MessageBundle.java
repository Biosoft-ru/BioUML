package ru.biosoft.workbench;

import java.awt.Event;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.ListResourceBundle;

import javax.swing.Action;
import javax.swing.KeyStroke;

import java.util.logging.Logger;

import ru.biosoft.workbench.documents.RedoAction;
import ru.biosoft.workbench.documents.UndoAction;
import ru.biosoft.workbench.script.ExecuteAction;

public class MessageBundle extends ListResourceBundle
{
    private final Logger log = Logger.getLogger(MessageBundle.class.getName());

    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            //----- Look & Feel issues-------------------------------------/
            {"PREFERENCES_LOOK_AND_FEEL_PN",    "Look and feel"},
            {"PREFERENCES_LOOK_AND_FEEL_PD",    "Preferred look and feel."+
                                                "<br><font color=red>Warning:</font> sometimes you need to restart the application " +
                                                "so look and feel was updated properly.</i>"    },

            {"PREFERENCES_LOOK_AND_FEEL_THEME_PN",    "Look and feel, theme"},
            {"PREFERENCES_LOOK_AND_FEEL_THEME_PD",    "Preferred theme for selected look and feel. " +
                                                      "Some look and feels have not themes." +
                                                      "<br><font color=\"red\">Warning:</font> sometimes you need to restart the application " +
                                                      "so look and feel theme was updated properly."    },

            //----- Workbench error messages --------------------------------------/
            {"ERROR_WORKBENCH_START",           "Can not start workbench, error: {0}"},
            {"ERROR_WORKBENCH_SPLASH",          "Can not load splash screen, error: {0}."},

            {"ERROR_WORKBENCH_REGISTRY",        "Workbench registry error: plugin={0}, extension={1}, \n  error: {2}"},

            {"ERROR_PERSPECTIVE_FACTORY",       "Perpective layout error, perpective={0}, factory={1}, error: {2}."},
            {"ERROR_PERSPETIVE_UNKNOWN_VIEW",   "Perpective loading error, perspective={0}, unknown view={1}."},
            {"ERROR_PERSPETIVE_CREATE_VIEW",    "Can not create view: perspective={0}, view={1}, error: {2}."},

            {"WARN_PERSPETIVE_VIEW_RELATIONSHIP", "Unknown view relationship, perspective={0}, view={1}, relationship={2}."},
            {"WARN_PERSPETIVE_VIEW_RELATIVE_NONE","Perspective already contains other views and 'none' constant is not suitable here.\n"+
                                                  "  perspective={0}, view={1}."},
            {"WARN_PERSPETIVE_VIEW_RELATIVE_ABSENTS", "Can not find the reference relative which new view should be placed.\n"+
                                                  "  perspective={0}, view={1}, relative={3}."},

            {"ERROR_ACTION_PERFORMED",          "Action performed error, acyion id={0}, error: {1}"},


            {"Information", "Information"},
            {"PluginAction.operationNotAvailableMessage", "The chosen operation is not currently available."},

            // Undo action
            {UndoAction.KEY + Action.SMALL_ICON, "undo.gif"},
            {UndoAction.KEY + Action.NAME, "Undo"},
            {UndoAction.KEY + Action.SHORT_DESCRIPTION, "Undo"},
            {UndoAction.KEY + Action.LONG_DESCRIPTION, "Undo"},
            {UndoAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_Z},
            {UndoAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK)},
            {UndoAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-undo"},

            // Redo action
            {RedoAction.KEY + Action.SMALL_ICON, "redo.gif"},
            {RedoAction.KEY + Action.NAME, "Redo"},
            {RedoAction.KEY + Action.SHORT_DESCRIPTION, "Redo"},
            {RedoAction.KEY + Action.LONG_DESCRIPTION, "Redo"},
            {RedoAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_Y},
            {RedoAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK)},
            {RedoAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-redo"},

            // Execute action
            {ExecuteAction.KEY + Action.SMALL_ICON, "run.gif"},
            {ExecuteAction.KEY + Action.NAME, "Run"},
            {ExecuteAction.KEY + Action.SHORT_DESCRIPTION, "Execute JavaScript"},
            {ExecuteAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_F8},
            {ExecuteAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)},
            {ExecuteAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-run"},
        };
    }

    /**
     * Returns string from the resource bundle for the specified key.
     * If the sting is absent the key string is returned instead and
     * the message is printed in <code>java.util.logging.Logger</code> for the component.
     */
    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch (Throwable t)
        {
            log.log(Level.SEVERE, "Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
}


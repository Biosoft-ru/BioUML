package ru.biosoft.gui.resources;

import java.util.ListResourceBundle;

import javax.swing.Action;
import javax.swing.text.DefaultEditorKit;

import ru.biosoft.gui.ClearLogAction;
import ru.biosoft.gui.HtmlView;
import ru.biosoft.gui.PluggedEditorsTabbedPane;
import ru.biosoft.gui.PropertiesEditor;
import ru.biosoft.gui.PropertiesView;
import ru.biosoft.gui.TabularPropertiesEditor;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private static Object[][] contents =
    {
        { "CN_EDITORS_MANAGER"          ,"Diagram editors control"},
        { "CD_EDITORS_MANAGER"          ,"Control to enable/disable tabs for different parts of diagram editor."},

        { "PN_EDITORS_MANAGER"          ,"Diagram editors"},
        { "PD_EDITORS_MANAGER"          ,"Control to enable/disable tabs for different parts of diagram editor."},

        // Action for properties view part
        { ClearLogAction.KEY      + Action.SMALL_ICON           , "clear.gif"},
        { ClearLogAction.KEY      + Action.NAME                 , "Clear log"},
        { ClearLogAction.KEY      + Action.SHORT_DESCRIPTION    , "Clear application log"},
//        { ClearLogAction.KEY      + Action.MNEMONIC_KEY         , "Ctrl L"},
//        { ClearLogAction.KEY      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK) },
        { ClearLogAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-properties-clear-log"},

        // Action for properties view part
        { PropertiesView.ACTION_NAME      + Action.SMALL_ICON           , "htmlTab.gif"},
        { PropertiesView.ACTION_NAME      + Action.NAME                 , "View"},
        { PropertiesView.ACTION_NAME      + Action.SHORT_DESCRIPTION    , "Properties view as HTML text"},
//        { PropertiesView.ACTION_NAME      + Action.MNEMONIC_KEY         , "Ctrl V"},
//        { PropertiesView.ACTION_NAME      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_V, Event.CTRL_MASK) },
        { PropertiesView.ACTION_NAME      + Action.ACTION_COMMAND_KEY   , "cmd-properties-view"},

        // Action for classic Property Inspector
        { PropertiesEditor.ACTION_NAME      + Action.SMALL_ICON           , "propertyInspectorTab.gif"},
        { PropertiesEditor.ACTION_NAME      + Action.NAME                 , "Edit"},
        { PropertiesEditor.ACTION_NAME      + Action.SHORT_DESCRIPTION    , "Properties editor"},
//        { PropertiesEditor.ACTION_NAME      + Action.MNEMONIC_KEY         , "Ctrl E"},
//        { PropertiesEditor.ACTION_NAME      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.CTRL_MASK) },
        { PropertiesEditor.ACTION_NAME      + Action.ACTION_COMMAND_KEY   , "cmd-properties-edit"},

        // Action for tabular Property Inspector
        { TabularPropertiesEditor.ACTION_NAME      + Action.SMALL_ICON           , "propertyInspectorTab.gif"},
        { TabularPropertiesEditor.ACTION_NAME      + Action.NAME                 , "Edit"},
        { TabularPropertiesEditor.ACTION_NAME      + Action.SHORT_DESCRIPTION    , "Tabular properties editor"},
        { TabularPropertiesEditor.ACTION_NAME      + Action.ACTION_COMMAND_KEY   , "cmd-tabular-properties-edit"},

        // Action for classic Property Inspector
        { PluggedEditorsTabbedPane.ACTION_NAME      + Action.SMALL_ICON           , "propertyInspectorTab.gif"},
        { PluggedEditorsTabbedPane.ACTION_NAME      + Action.NAME                 , "Editors"},
        { PluggedEditorsTabbedPane.ACTION_NAME      + Action.SHORT_DESCRIPTION    , "Plugged editors"},
        { PluggedEditorsTabbedPane.ACTION_NAME      + Action.ACTION_COMMAND_KEY   , "cmd-plugged-editors"},

        // Action for properties view part
        { HtmlView.ACTION_NAME      + Action.SMALL_ICON           , "htmlTab.gif"},
        { HtmlView.ACTION_NAME      + Action.NAME                 , "View"},
        { HtmlView.ACTION_NAME      + Action.SHORT_DESCRIPTION    , "View"},

        // HTML Editor actions
        { DefaultEditorKit.copyAction           + Action.SMALL_ICON           , "copy.gif"},
        { DefaultEditorKit.copyAction           + Action.SHORT_DESCRIPTION    , "Copy"},
        { DefaultEditorKit.copyAction           + Action.ACTION_COMMAND_KEY   , "cmd-copy"},

        { DefaultEditorKit.cutAction            + Action.SMALL_ICON           , "cut.gif"},
        { DefaultEditorKit.cutAction            + Action.SHORT_DESCRIPTION    , "Cut"},
        { DefaultEditorKit.cutAction            + Action.ACTION_COMMAND_KEY   , "cmd-cut"},

        { DefaultEditorKit.pasteAction          + Action.SMALL_ICON           , "paste.gif"},
        { DefaultEditorKit.pasteAction          + Action.SHORT_DESCRIPTION    , "Paste"},
        { DefaultEditorKit.pasteAction          + Action.ACTION_COMMAND_KEY   , "cmd-paste"},

        { "font-bold"                           + Action.SMALL_ICON           , "bold.gif"},
        { "font-bold"                           + Action.SHORT_DESCRIPTION    , "Bold font style"},
        { "font-bold"                           + Action.ACTION_COMMAND_KEY   , "cmd-bold"},

        { "font-italic"                         + Action.SMALL_ICON           , "italic.gif"},
        { "font-italic"                         + Action.SHORT_DESCRIPTION    , "Italic font style"},
        { "font-italic"                         + Action.ACTION_COMMAND_KEY   , "cmd-italic"},

        { "font-underline"                      + Action.SMALL_ICON           , "underline.gif"},
        { "font-underline"                      + Action.SHORT_DESCRIPTION    , "Underline font style"},
        { "font-underline"                      + Action.ACTION_COMMAND_KEY   , "cmd-underline"},

        { "left"                                + Action.SMALL_ICON           , "left.gif"},
        { "left"                                + Action.SHORT_DESCRIPTION    , "Left alignment"},
        { "left"                                + Action.ACTION_COMMAND_KEY   , "cmd-left"},

        { "center"                              + Action.SMALL_ICON           , "center.gif"},
        { "center"                              + Action.SHORT_DESCRIPTION    , "Center alignment"},
        { "center"                              + Action.ACTION_COMMAND_KEY   , "cmd-center"},

        { "right"                               + Action.SMALL_ICON           , "right.gif"},
        { "right"                               + Action.SHORT_DESCRIPTION    , "Right alignment"},
        { "right"                               + Action.ACTION_COMMAND_KEY   , "cmd-right"},
    };

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
            System.out.println("Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
}

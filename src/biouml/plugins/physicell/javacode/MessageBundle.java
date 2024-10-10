package biouml.plugins.physicell.javacode;

import java.awt.event.KeyEvent;
import java.util.ListResourceBundle;

import javax.swing.Action;
import javax.swing.KeyStroke;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            // Format code action
            {FormatCodeAction.KEY + Action.SMALL_ICON, "format.gif"},
            {FormatCodeAction.KEY + Action.NAME, "Format"},
            {FormatCodeAction.KEY + Action.SHORT_DESCRIPTION, "Format java code"},
            {FormatCodeAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_F8},
            {FormatCodeAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)},
            {FormatCodeAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-format"},
        };
    };

}
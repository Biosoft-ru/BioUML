package biouml.workbench.htmlgen;

import java.util.ListResourceBundle;

import javax.swing.Action;

/**
 * 
 * @pending description for RE
 */
public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            // Generate Html for diagram action
            {GenerateHTMLAction.KEY + Action.SMALL_ICON, "generateHTML.gif"},
            {GenerateHTMLAction.KEY + Action.NAME, "Generate HTML..."},
            {GenerateHTMLAction.KEY + Action.SHORT_DESCRIPTION, "Generate HTML"},
            {GenerateHTMLAction.KEY + Action.LONG_DESCRIPTION, "Generate HTML"},
            {GenerateHTMLAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-generate-html"},
        };
    }

    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch (Throwable th)
        {

        }
        return key;
    }
}
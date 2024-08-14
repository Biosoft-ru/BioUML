package biouml.plugins.bionetgen.diagram;

import java.util.ListResourceBundle;

import javax.swing.Action;

import biouml.plugins.bionetgen.diagram.BionetgenEditor.ApplyBionetgen;
import biouml.plugins.bionetgen.diagram.BionetgenEditor.DeployBionetgen;
import biouml.plugins.bionetgen.diagram.BionetgenEditor.RecreateText;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
                //--- Actions ---------------------------------------------------/
                {ApplyBionetgen.KEY + Action.SMALL_ICON, "apply.gif"},
                {ApplyBionetgen.KEY + Action.NAME, "Apply"},
                {ApplyBionetgen.KEY + Action.SHORT_DESCRIPTION, "Apply BioNetGen"},
                {ApplyBionetgen.KEY + Action.ACTION_COMMAND_KEY, "Apply"},
        
                {DeployBionetgen.KEY + Action.SMALL_ICON, "process.gif"},
                {DeployBionetgen.KEY + Action.NAME, "Deploy"},
                {DeployBionetgen.KEY + Action.SHORT_DESCRIPTION, "Deploy diagram"},
                {DeployBionetgen.KEY + Action.ACTION_COMMAND_KEY, "Deploy"},
        
                {RecreateText.KEY + Action.SMALL_ICON, "recreate.gif"},
                {RecreateText.KEY + Action.NAME, "Recreate text"},
                {RecreateText.KEY + Action.SHORT_DESCRIPTION,
                        "Recreate BNG-text (create new text using diagram, all user changes will be removed)"},
                {RecreateText.KEY + Action.ACTION_COMMAND_KEY, "Recreate text"}};
    }

    /**
     * Returns string from the resource bundle for the specified key.
     * If the string is absent the key string is returned instead and
     * the message is printed in <code>java.util.logging.Logger</code> for the component.
     */
    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch( Throwable t )
        {
            System.out.println("Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
}
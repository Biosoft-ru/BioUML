package biouml.workbench;

import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JFrame;

import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.dialog.OkCancelDialog;

public class PreferencesDialog extends OkCancelDialog
{
    protected Map oldValues;

    public PreferencesDialog(JFrame frame)
    {
        super(frame,
              BioUMLApplication.getMessageBundle().getResourceString("PREFERENCES_DIALOG_TITLE"));

        // save original preferences so we can restore them
        // if cancel button will be pressed
        Preferences preferences = Application.getPreferences();
        oldValues = preferences.valuesMap();

        PropertyInspector inspectorPane = new PropertyInspector();
        inspectorPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        inspectorPane.setShowToolTip(false);
        inspectorPane.setShowToolTipPane(true);
        inspectorPane.setToolTipPanePreferredHeight(120);

        setContent(inspectorPane);
        inspectorPane.explore(preferences);
        setSize(500, 420);
        ApplicationUtils.moveToCenter(this);
    }

    @Override
    protected void cancelPressed()
    {
        // restore old preferences
        Application.getPreferences().updateValues(oldValues);

        super.cancelPressed();
    }

}
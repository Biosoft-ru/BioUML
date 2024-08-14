package ru.biosoft.gui.setupwizard;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.application.Application;

public class SetupWizardSupport
{
    public static Preferences getSetupPreferences() throws Exception
    {
        Preferences preferences = Application.getPreferences();
        Preferences setupPreferences = (Preferences)preferences.getValue(SetupWizardDialog.SETUP_WIZARD_PREFERENCES);
        if( setupPreferences == null )
        {
            setupPreferences = new Preferences();
            preferences.add(new DynamicProperty(SetupWizardDialog.SETUP_WIZARD_PREFERENCES, SetupWizardDialog.SETUP_WIZARD_PREFERENCES,
                    "Setup wizard preferences", Preferences.class, setupPreferences));
        }

        return setupPreferences;
    }

    public static boolean isActive() throws Exception
    {
        Preferences setupPreferences = getSetupPreferences();
        Boolean value = (Boolean)setupPreferences.getValue(SetupWizardDialog.SETUP_WIZARD_USAGE);
        if( value == null )
        {
            value = true;
            setupPreferences.add(new DynamicProperty(SetupWizardDialog.SETUP_WIZARD_USAGE, SetupWizardDialog.SETUP_WIZARD_USAGE,
                    "Show on startup", Boolean.class, value));
        }

        return value;
    }
}

package ru.biosoft.gui.setupwizard;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Icon;

import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.application.Application;

public class OpenSetupWizardAction extends AbstractAction
{
    public static final String KEY = "Setup wizard";

    protected WizardPageInfo[] pageInfos;
    protected String title;
    protected Preferences setupPreferences;
    protected Icon icon;

    public OpenSetupWizardAction()
    {
        super(KEY);
    }
    
    public void init(WizardPageInfo[] pageInfos, String title, Preferences setupPreferences, Icon icon)
    {
        this.pageInfos = pageInfos;
        this.title = title;
        this.setupPreferences = setupPreferences;
        this.icon = icon;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        SetupWizardDialog setupWizard = new SetupWizardDialog(Application.getApplicationFrame(), pageInfos, title, setupPreferences, icon);
        setupWizard.showWizard();
    }
}

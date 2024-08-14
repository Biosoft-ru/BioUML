package ru.biosoft.workbench;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.util.TextUtil;

import com.developmentontheedge.application.Application;

public class AboutAction extends AbstractAction
{
    public static final String KEY = "About";
    private static String startupPluginName;

    public static void setStartupPluginName(String name)
    {
        startupPluginName = TextUtil.nullToEmpty( name );
    }

    public static String getStartupPluginName()
    {
        return startupPluginName;
    }

    public AboutAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        AboutDialog aboutDialog = new AboutDialog(Application.getApplicationFrame(), Application.getApplicationFrame().getTitle());
        aboutDialog.doModal();
    }
}

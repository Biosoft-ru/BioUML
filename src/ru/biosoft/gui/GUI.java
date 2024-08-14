package ru.biosoft.gui;

public class GUI
{
    private static UiManager uiManager = new DefaultUiManager();

    public static void setManager(UiManager uiManager)
    {
        SecurityManager sm = System.getSecurityManager();
        if( sm != null )
        {
            sm.checkPermission(new RuntimePermission("setupApplication"));
        }
        GUI.uiManager = uiManager;
    }
    
    public static UiManager getManager()
    {
        return uiManager;
    }
}

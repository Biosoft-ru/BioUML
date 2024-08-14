package com.developmentontheedge.application;

import javax.swing.JOptionPane;

public class SwingUIStrategy implements UIStrategy
{

    @Override
    public void showErrorBox(String msg, String title)
    {
        JOptionPane.showMessageDialog(Application.getActiveApplicationFrame(), msg, title, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void showInfoBox(String msg, String title)
    {
        JOptionPane.showMessageDialog(Application.getActiveApplicationFrame(), msg, title, JOptionPane.INFORMATION_MESSAGE);
    }
}

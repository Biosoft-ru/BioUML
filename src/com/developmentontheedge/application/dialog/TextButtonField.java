package com.developmentontheedge.application.dialog;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class TextButtonField extends JPanel
{
    protected final JTextField textField  = new JTextField();
    protected final JButton button;
    
    public TextButtonField(String text)
    {
        super(new BorderLayout());
        button = new JButton(text);
        add(textField, BorderLayout.CENTER);
        add(button, BorderLayout.EAST);
    }

    @Override
    public void setEnabled( boolean enabled )
    {
        textField.setEnabled( enabled );
        button.setEnabled( enabled );
    }

    public JTextField getTextField()
    {
        return textField;
    }


}

package ru.biosoft.graphics.chart;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import com.developmentontheedge.beans.editors.CustomEditorSupport;

public class ChartViewer extends CustomEditorSupport
{

    @Override
    public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
    {
        if( ! ( getValue() instanceof Chart ) || ( (Chart)getValue() ).isEmpty() )
        {
            return new JLabel();
        }
        JButton button = new JButton("View");
        button.addActionListener(evt -> {
            JFrame frame = new JFrame("Plot");
            final JComponent component;
            final JFreeChart chart = getValue() instanceof Chart ? ( (Chart)getValue() ).getChart() : null;
            if(chart != null)
            {
                component = new ChartPanel(chart);
            } else
            {
                component = new JLabel("Chart cannot be loaded");
            }
            frame.setSize(500, 500);
            frame.add(component, BorderLayout.CENTER);
            frame.doLayout();
            frame.setAlwaysOnTop(true);
            frame.setResizable(true);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.setEnabled(true);
            frame.setFocusable(true);
        });
        return button;
    }

    @Override
    public Component getCustomEditor(Component parent, boolean isSelected)
    {
        return getCustomRenderer(parent, isSelected, false);
    }
}

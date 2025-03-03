package biouml.plugins.physicell.document;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.FeatureDescriptor;
import java.lang.reflect.Method;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import com.developmentontheedge.beans.editors.CustomEditorSupport;

public class SliderEditor extends CustomEditorSupport
{
    private JSlider slider = new JSlider();
    private JPanel view = null;
    private JTextField field = new JTextField();

    @Override
    public void setDescriptor(FeatureDescriptor descriptor)
    {
        super.setDescriptor( descriptor );
        initBounds();
    }

    private void initBounds()
    {
        int max = 100;
        int min = 0;
        try
        {
            Object maxObject = this.getDescriptor().getValue( "max" );
            if( maxObject instanceof Integer )
                max = (Integer)maxObject;
            else if( maxObject instanceof Method )
                max = (Integer) ( (Method)maxObject ).invoke( this.getBean(), (Object[])null );

            Object minObject = this.getDescriptor().getValue( "min" );
            if( minObject instanceof Integer )
                min = (Integer)minObject;
            else if( minObject instanceof Method )
                min = (Integer) ( (Method)minObject ).invoke( this.getBean(), (Object[])null );
        }
        catch( Exception ex )
        {

        }
        slider.setMaximum( max );
        slider.setMinimum( min );
    }

    private Component createComponent(Component parent)
    {
        view = new JPanel();
        field.setSize( 40, 18 );
        field.setPreferredSize( new Dimension( 40, 18 ) );
        slider.setSize( new Dimension( 1000, 18 ) );
        slider.setPreferredSize( new Dimension( 1000, 18 ) );
        FlowLayout layout = new FlowLayout( FlowLayout.LEFT );
        layout.setVgap( 0 );
        view.setLayout( layout );
        view.add( field );
        view.add( slider );

        field.setInputVerifier( new InputVerifier()
        {
            @Override
            public boolean verify(JComponent input)
            {
                String text = ( (JTextField)input ).getText();
                try
                {
                    Integer.parseInt( text );
                    return true;
                }
                catch( NumberFormatException nfe )
                {
                    return false;
                }
            }
        } );

        field.addFocusListener( new FocusListener()
        {

            @Override
            public void focusGained(FocusEvent e)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void focusLost(FocusEvent e)
            {
                slider.setValue( Integer.parseInt( field.getText() ) );
                firePropertyChange();
            }

        } );

        slider.addMouseListener( new MouseListener()
        {

            @Override
            public void mouseReleased(MouseEvent e)
            {
                firePropertyChange();
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseClicked(MouseEvent e)
            {
                // TODO Auto-generated method stub

            }
        } );
        return view;
    }

    @Override
    public void setValue(Object value)
    {
        int val = Integer.parseInt( value.toString() );
        slider.setValue( val );
        field.setText( String.valueOf( value ) );
    }

    public Object getValue()
    {
        return slider.getValue();
    }

    @Override
    public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
    {
        return getCustomEditor( parent, isSelected );
    }

    @Override
    public Component getCustomEditor(Component parent, boolean isSelected)
    {
        if( view == null )
            createComponent( parent );
        return view;
    }

    @Override
    public Component getCustomEditor()
    {
        if( view == null )
            createComponent( null );
        return view;
    }

    @Override
    protected Object processValue()
    {
        return slider.getValue();
    }
}
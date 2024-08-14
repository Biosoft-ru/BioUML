package ru.biosoft.math.xml;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.ComponentView;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Attribute;

import com.developmentontheedge.application.ApplicationUtils;

import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.InlineView;
import javax.swing.text.html.StyleSheet;

import ru.biosoft.math.SimpleImageView;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.view.FormulaViewBuilder;

public class MathMLEditorKit extends HTMLEditorKit
{
    private static HTMLFactory factory = null;
    private FormulaViewBuilder viewBuilder = new FormulaViewBuilder();

    @Override
    public Document createDefaultDocument()
    {
        StyleSheet styles = getStyleSheet();
        StyleSheet ss = new StyleSheet();
        ss.addStyleSheet( styles );
        HTMLDocument doc = new HTMLDocument( ss );
        doc.setParser( getParser() );
        doc.setAsynchronousLoadPriority( 4 );
        return doc;
    }

    public void setMultiplySign(String symbol)
    {
        viewBuilder.setMultiplySign( symbol );
    }

    @Override
    public ViewFactory getViewFactory()
    {
        if( factory == null )
        {
            factory = new HTMLFactory()
            {
                private boolean mathOpened = false;
                private int pos = 0;

                @Override
                public View create(Element elem)
                {
                    AttributeSet attrs = elem.getAttributes();
                    Object elementName = attrs.getAttribute( AbstractDocument.ElementNameAttribute );
                    Object o = ( elementName != null ) ? null : attrs.getAttribute( StyleConstants.NameAttribute );

                    boolean endtag = "true".equals( attrs.getAttribute( Attribute.ENDTAG ) );

                    if( o instanceof HTML.Tag )
                    {
                        HTML.Tag kind = (HTML.Tag)o;
                        if( kind.toString().equals( "math" ) )
                        {
                            if( !endtag )
                            {
                                mathOpened = true;
                                pos = elem.getStartOffset();
                                return new HiddenView( elem );
                            }
                            else
                            {
                                mathOpened = false;
                                int length = elem.getEndOffset() - pos;
                                HTMLDocument doc = (HTMLDocument)elem.getDocument();
                                try
                                {
                                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                    MathMLEditorKit.this.write( stream, doc, pos, length );
                                    String text = stream.toString();
                                    int start = text.indexOf( "<math" );
                                    int end = text.indexOf( "</math>" ) + 7;
                                    text = text.substring( start, end );
                                    MathMLParser parser = new MathMLParser();
                                    parser.parse( text );
                                    AstStart node = parser.getStartNode();
                                    ru.biosoft.graphics.View view = viewBuilder.createView( node, ApplicationUtils.getGraphics() );
                                    view.setLocation( 0, 0 );
                                    BufferedImage img = MathMLEditorKit.generateImage( view, 1 );
                                    return new SimpleImageView( elem, img );
                                }
                                catch( Exception ex )
                                {
                                    ex.printStackTrace();
                                }
                            }
                        }
                        View v = super.create( elem );
                        if( mathOpened && v instanceof InlineView )
                            return new HiddenView( elem );
                        return v;
                    }
                    return super.create( elem );
                }
            };
        }
        return factory;
    }

    public static BufferedImage generateImage(ru.biosoft.graphics.View view, double scale)
    {
        Rectangle r = view.getBounds();
        int width = (int)Math.ceil( ( r.width + 2 * r.x ) * scale );
        int height = (int)Math.ceil( ( r.height + 2 * r.y ) * scale );
        BufferedImage image = new BufferedImage( width, height, BufferedImage.TYPE_INT_RGB );
        Graphics2D graphics = image.createGraphics();
        AffineTransform at = new AffineTransform();
        at.scale( scale, scale );
        graphics.setColor( Color.white );
        graphics.fill( new Rectangle( 0, 0, width, height ) );
        graphics.setTransform( at );
        view.paint( graphics );
        return image;
    }

    static class HiddenView extends ComponentView implements DocumentListener
    {
        HiddenView(Element e)
        {
            super( e );
        }

        @Override
        public void insertUpdate(DocumentEvent e)
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void removeUpdate(DocumentEvent e)
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void changedUpdate(DocumentEvent e)
        {
            // TODO Auto-generated method stub
        }
    }
}
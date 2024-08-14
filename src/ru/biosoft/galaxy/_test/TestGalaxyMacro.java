package ru.biosoft.galaxy._test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ru.biosoft.galaxy.preprocess.GalaxyMacro;

public class TestGalaxyMacro extends TestCase
{

    public void testApplyMacro() throws Exception
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.newDocument();

        Element macroElem = doc.createElement( "macro" );
        macroElem.setAttribute( "name", "simple" );
        Element tag1 = doc.createElement( "tag1" );
        tag1.setAttribute( "name", "value" );
        tag1.appendChild( doc.createElement( "innertag" ) );
        tag1.appendChild( doc.createTextNode( "some text" ) );
        macroElem.appendChild( tag1 );
        macroElem.appendChild( doc.createElement( "tag2" ) );


        GalaxyMacro macro = new GalaxyMacro( macroElem );
        assertEquals( "simple", macro.getName() );

        Element sourceElem = doc.createElement( "source" );
        Element expandElem = doc.createElement( "expand" );
        expandElem.setAttribute( "macro", "simple" );
        sourceElem.appendChild( expandElem );

        GalaxyMacro.applyMacros( Collections.singletonMap( macro.getName(), macro ), sourceElem );

        assertEquals( 2, sourceElem.getChildNodes().getLength() );

        Element tag1Clone = (Element)sourceElem.getFirstChild();
        assertEquals( "tag1", tag1Clone.getTagName() );
        assertEquals( "value", tag1Clone.getAttribute( "name" ) );

        assertEquals( 2, tag1Clone.getChildNodes().getLength() );
        Element innerTag = (Element)tag1Clone.getFirstChild();
        assertEquals( "innertag", innerTag.getTagName() );
        assertEquals( "some text", tag1Clone.getTextContent() );

        Element tag2Clone = (Element)sourceElem.getLastChild();
        assertEquals( "tag2", tag2Clone.getTagName() );
    }

    public void testYield() throws Exception
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.newDocument();

        Element macroElem = doc.createElement( "macro" );
        macroElem.setAttribute( "name", "simple" );
        macroElem.appendChild( doc.createElement( "yield" ) );

        GalaxyMacro macro = new GalaxyMacro( macroElem );

        Element sourceElem = doc.createElement( "source" );
        Element expandElem = doc.createElement( "expand" );
        expandElem.setAttribute( "macro", "simple" );
        expandElem.appendChild( doc.createElement( "param" ) );
        sourceElem.appendChild( expandElem );

        GalaxyMacro.applyMacros( Collections.singletonMap( macro.getName(), macro ), sourceElem );

        assertEquals( 1, sourceElem.getChildNodes().getLength() );
        Element param = (Element)sourceElem.getFirstChild();
        assertEquals( "param", param.getTagName() );
    }

    public void testExpandInMacro() throws Exception
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.newDocument();

        Element simple1MacroElem = doc.createElement( "macro" );
        simple1MacroElem.setAttribute( "name", "simple1" );
        simple1MacroElem.appendChild( doc.createElement( "tag" ) );
        GalaxyMacro simple1Macro = new GalaxyMacro( simple1MacroElem );

        Element simple2MacroElem = doc.createElement( "macro" );
        simple2MacroElem.setAttribute( "name", "simple2" );
        Element expandElem = doc.createElement( "expand" );
        expandElem.setAttribute( "macro", "simple1" );
        simple2MacroElem.appendChild( expandElem );
        GalaxyMacro simple2Macro = new GalaxyMacro( simple2MacroElem );

        Map<String, GalaxyMacro> scope = new HashMap<>();
        scope.put( simple1Macro.getName(), simple1Macro );
        scope.put( simple2Macro.getName(), simple2Macro );

        for( GalaxyMacro macro : scope.values() )
            macro.init( scope );

        Element source = simple2Macro.getSource();
        assertEquals( 1, source.getChildNodes().getLength() );
        assertEquals( "tag", ( (Element)source.getFirstChild() ).getTagName() );

    }
}

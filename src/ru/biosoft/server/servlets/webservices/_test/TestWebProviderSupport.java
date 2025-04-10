package ru.biosoft.server.servlets.webservices._test;

import java.awt.Point;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;

import junit.framework.TestCase;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.providers.WebProviderSupport;

/**
 * @author lan
 *
 */
public class TestWebProviderSupport extends TestCase
{
    public void testArguments() throws Exception
    {
        final VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        final TextDataElement text = new TextDataElement("text", vdc);
        vdc.put(text);
        
        Map<String, String> arguments = new HashMap<>();
        arguments.put("testString", "test");
        arguments.put("testEmptyString", "");
        arguments.put("testInt", "123");
        arguments.put("testIntWrong", "wrong");
        arguments.put("x", "324.5");
        arguments.put("y", "-12.99");
        final JSONArray jsonArray = new JSONArray(Arrays.asList("a","b","c"));
        arguments.put("json", jsonArray.toString());
        arguments.put("testBadName", "a/b");
        arguments.put("de", "test/text");
        arguments.put("dc", "test");
        arguments.put("wrongDE", "test/wrong");
        (new WebProviderSupport()
        {
            @Override
            public void process(BiosoftWebRequest arguments, BiosoftWebResponse resp) throws Exception
            {
                assertEquals("test", arguments.getString("testString"));
                assertEquals("", arguments.getString("testEmptyString"));
                WebException ex = null;
                try
                {
                    arguments.getString("testNull");
                }
                catch( WebException e )
                {
                    ex = e;
                }
                assertNotNull(ex);
                assertEquals("EX_QUERY_PARAM_MISSING", ex.getId());
                
                assertEquals(123, arguments.optInt("testInt"));
                assertEquals(0, arguments.optInt("testInt2"));
                assertEquals(5, arguments.optInt("testIntWrong", 5));
                assertEquals(new Point(324,-12), arguments.getPoint());
                
                assertEquals(jsonArray.toString(), arguments.getJSONArray("json").toString());
                try
                {
                    ex = null;
                    arguments.getJSONArray("testString");
                }
                catch( WebException e )
                {
                    ex = e;
                }
                assertNotNull(ex);
                assertEquals("EX_QUERY_PARAM_NO_JSON", ex.getId());
                
                assertEquals("test", arguments.getElementName("testString"));
                try
                {
                    ex = null;
                    arguments.getElementName("testEmptyString");
                }
                catch( WebException e )
                {
                    ex = e;
                }
                assertNotNull(ex);
                assertEquals("EX_INPUT_NAME_EMPTY", ex.getId());
                try
                {
                    ex = null;
                    arguments.getElementName("testBadName");
                }
                catch( WebException e )
                {
                    ex = e;
                }
                assertNotNull(ex);
                assertEquals("EX_INPUT_NAME_INVALID", ex.getId());
                
                assertEquals(DataElementPath.create("test/text"), arguments.getDataElementPath());
                TextDataElement tde = arguments.getDataElement(TextDataElement.class);
                assertEquals(text, tde);
                DataCollection<?> dc = arguments.getDataCollection( "dc" );
                assertEquals(vdc, dc);
                DataElement de = arguments.getDataElement();
                tde = castDataElement(de, TextDataElement.class);
                assertNotNull(tde);
                
                try
                {
                    ex = null;
                    castDataElement(null, TextDataElement.class);
                }
                catch(WebException e)
                {
                    ex = e;
                }
                assertNotNull(ex);
                assertEquals("EX_QUERY_NO_ELEMENT_TYPE", ex.getId());
                
                try
                {
                    ex = null;
                    arguments.getDataCollection();
                }
                catch(WebException e)
                {
                    ex = e;
                }
                assertNotNull(ex);
                assertEquals("EX_QUERY_INVALID_ELEMENT_TYPE", ex.getId());

                try
                {
                    ex = null;
                    getDataElement(DataElementPath.create("qwerty"), ImageDataElement.class);
                }
                catch(WebException e)
                {
                    ex = e;
                }
                assertNotNull(ex);
                assertEquals("EX_QUERY_NO_ELEMENT_TYPE", ex.getId());
                assertEquals("qwerty", ex.getParameters()[0].toString());
                assertEquals("image", ex.getParameters()[1].toString());
            }
        }).process(new BiosoftWebRequest(arguments), null);
    }
}

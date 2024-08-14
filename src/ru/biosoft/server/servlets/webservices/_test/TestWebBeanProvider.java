package ru.biosoft.server.servlets.webservices._test;

import java.util.HashMap;
import java.util.Map;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.annot.PropertyName;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;

/**
 * @author lan
 *
 */
public class TestWebBeanProvider extends AbstractProviderTest
{
    public static class Bean
    {
        int a = 5;
        String b = "test string";
        
        @PropertyName("A property")
        public int getA()
        {
            return a;
        }
        public void setA(int a)
        {
            this.a = a;
            setB(a+"-value");
        }
        
        @PropertyName("B property")
        public String getB()
        {
            return b;
        }
        public void setB(String b)
        {
            this.b = b;
        }
    }
    
    public static class BeanBeanInfo extends BeanInfoEx
    {
        public BeanBeanInfo()
        {
            super( Bean.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("a");
            add("b");
        }
    }
    
    public void testWebBeanProvider() throws Exception
    {
        Bean bean = new Bean();
        WebServicesServlet.getSessionCache().addObject("beans/test", bean, false);
        
        Map<String, String> arguments = new HashMap<>();
        arguments.put("de", "beans/unknown");
        arguments.put(BiosoftWebRequest.ACTION, "get");
        
        JsonObject response = getResponseJSON("bean", arguments);
        assertErrorCodeEquals("EX_INTERNAL_BEAN_NOT_FOUND", response);
        
        arguments.put("de", "beans/test");
        response = getResponseJSON("bean", arguments);
        assertOk(response);
        assertFalse(response.get("attributes").asObject().get("expertOptions").asBoolean());
        JsonArray values = response.get("values").asArray();
        assertEquals(2, values.size());
        JsonObject aObject = values.get(0).asObject();
        assertEquals("A property", aObject.get("displayName").asString());
        assertEquals("5", aObject.get("value").asString());
        JsonObject bObject = values.get(1).asObject();
        assertEquals("B property", bObject.get("description").asString());
        assertEquals("test string", bObject.get("value").asString());
        
        arguments.put("fields", "a");
        response = getResponseJSON("bean", arguments);
        assertOk(response);
        assertFalse(response.get("attributes").asObject().get("expertOptions").asBoolean());
        values = response.get("values").asArray();
        assertEquals(1, values.size());
        aObject = values.get(0).asObject();
        assertEquals("5", aObject.get("value").asString());
        
        arguments.put(BiosoftWebRequest.ACTION, "set");
        values = new JsonArray();
        values.add(new JsonObject().add("name", "b").add("value", "blahblah"))
              .add(new JsonObject().add("name", "a").add("value", "10"));
        arguments.put("json", values.toString());
        arguments.put( "useJsonOrder", "no" );
        response = getResponseJSON("bean", arguments);
        assertOk(response);
        assertEquals(10, bean.getA());
        assertEquals("blahblah", bean.getB());
        
        values = new JsonArray();
        values.add(new JsonObject().add("name", "b").add("value", "blahblah"))
              .add(new JsonObject().add("name", "a").add("value", "10"));
        arguments.put("json", values.toString());
        arguments.put( "useJsonOrder", "yes" );
        response = getResponseJSON("bean", arguments);
        assertOk(response);
        assertEquals(10, bean.getA());
        assertEquals("10-value", bean.getB());
        
        values = new JsonArray();
        values.add(new JsonObject().add("name", "b").add("value", "blahblah"))
              .add(new JsonObject().add("name", "a").add("value", "10"));
        arguments.put("json", values.toString());
        arguments.remove( "useJsonOrder" );//default useJsonOrder=yes
        response = getResponseJSON("bean", arguments);
        assertOk(response);
        assertEquals(10, bean.getA());
        assertEquals("10-value", bean.getB());

    }
}

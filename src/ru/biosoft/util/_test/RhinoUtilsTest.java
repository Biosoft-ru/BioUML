package ru.biosoft.util._test;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;

import ru.biosoft.plugins.javascript.JScriptContext;
import ru.biosoft.util.RhinoUtils;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author lan
 *
 */
public class RhinoUtilsTest extends TestCase
{
    public RhinoUtilsTest(String name)
    {
        super(name);
    }
    
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(RhinoUtilsTest.class.getName());
        suite.addTest(new RhinoUtilsTest("testToJSON"));
        return suite;
    }

    public void testToJSON() throws JSONException
    {
        Context context = JScriptContext.getContext();
        String arrayStr = "[1,2,3,[4,5],\"test\",true,{a:1,b:true,c:\"qqq\"}]";
        Object nativeArray = context.evaluateString(JScriptContext.getScope(), arrayStr, "testToJSON", 0, null);
        assertTrue(nativeArray instanceof NativeArray);
        JSONArray jsonArray = RhinoUtils.toJSONArray((NativeArray)nativeArray);
        JSONArray jsonArray2 = new JSONArray(arrayStr);
        assertEquals(jsonArray2.toString(), jsonArray.toString());
        
        String objectStr = "{a:1,b:2,c:['qqq','www','eee',{'r':'w'}],d:false}";
        Object nativeObject = context.evaluateString(JScriptContext.getScope(), "a="+objectStr+";a", "testToJSON", 0, null);
        assertTrue(nativeObject instanceof NativeObject);
        JSONObject jsonObject = RhinoUtils.toJSONObject((NativeObject)nativeObject);
        JSONObject jsonObject2 = new JSONObject(objectStr);
        assertEquals(jsonObject.toString(), jsonObject2.toString());
    }
}

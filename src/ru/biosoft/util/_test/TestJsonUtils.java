package ru.biosoft.util._test;

import com.eclipsesource.json.JsonObject;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.JoinTableParameters;
import ru.biosoft.util.JsonUtils;
import junit.framework.TestCase;

public class TestJsonUtils extends TestCase
{
    public void testFromBean()
    {
        JoinTableParameters bean = new JoinTableParameters();
        bean.getLeftGroup().setTablePath( DataElementPath.create("test/left") );
        bean.getLeftGroup().setNumerical( false );
        bean.getRightGroup().setTablePath( DataElementPath.create("test/right") );
        bean.setJoinType( 2 );
        JsonObject object = JsonUtils.fromBean( bean );
        assertEquals("{\"leftGroup\":"
                //                + "{\"tablePath\":\"test/left\",\"columns\":[{\"name\":\"(all columns)\",\"newName\":\"(all columns)\",\"timePoint\":0}],\"namesDescription\":\"(all columns)\"},"
                + "{\"tablePath\":\"test/left\",\"columns\":\"[\\\"(all columns)\\\"]\",\"namesDescription\":\"(all columns)\"},"
                + "\"rightGroup\":"
                //                + "{\"tablePath\":\"test/right\",\"columns\":[{\"name\":\"(all columns)\",\"newName\":\"(all columns)\",\"timePoint\":0}],\"namesDescription\":\"(all columns)\"},"
                + "{\"tablePath\":\"test/right\",\"columns\":\"[\\\"(all columns)\\\"]\",\"namesDescription\":\"(all columns)\"},"
                + "\"joinType\":2,"
                + "\"mergeColumns\":true,"
                + "\"output\":\"test/Joined\"}", object.toString());
    }
}

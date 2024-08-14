package ru.biosoft.access._test;

import ru.biosoft.access.sql.Query;
import junit.framework.TestCase;

/**
 * @author lan
 *
 */
public class TestQuery extends TestCase
{
    public void testQuery()
    {
        Query query = new Query("SELECT * FROM $table$ WHERE name=$id$ AND count>$min$");
        assertEquals("(template) SELECT * FROM $table$ WHERE name=$id$ AND count>$min$", query.toString());
        query = query.name("unknown", "val");
        assertEquals("(template) SELECT * FROM $table$ WHERE name=$id$ AND count>$min$", query.toString());
        query = query.name("table", "myTable");
        assertEquals("(template) SELECT * FROM `myTable` WHERE name=$id$ AND count>$min$", query.toString());
        query = query.str("id", "I'm");
        assertEquals("(template) SELECT * FROM `myTable` WHERE name='I''m' AND count>$min$", query.toString());
        query = query.num("min", 5);
        assertEquals("SELECT * FROM `myTable` WHERE name='I''m' AND count>5", query.toString());
        
        assertEquals("SELECT * FROM `myTable` WHERE name='I''m' AND count>5", new Query("SELECT * FROM $table$ WHERE name=$id$ AND count>$min$").name("myTable").str("I'm").num(5).get());
        
        assertEquals("blahblahblah", new Query("blahblahblah").str("1", "2").str("2").get());
        
        assertEquals("SELECT `id` FROM table WHERE `id`>10.0", new Query("SELECT $col$ FROM table WHERE $col$>$cutoff$").name("col", "id").num("cutoff", 10.0).get());
        
        assertEquals("DESCRIBE `me`", Query.describe("me").get());
        assertEquals("SELECT * FROM `me`", Query.all("me").get());
        assertEquals("SELECT COUNT(*) FROM `me`", Query.count("me").get());
        assertEquals("SELECT * FROM `me` WHERE `field` = 'value'", Query.byCondition("me", "field", "value").get());
        assertEquals("SELECT `field` FROM `me`", Query.field("me", "field").get());
        assertEquals("SELECT `field` FROM `me` ORDER BY 1", Query.sortedField("me", "field").get());
    }
}

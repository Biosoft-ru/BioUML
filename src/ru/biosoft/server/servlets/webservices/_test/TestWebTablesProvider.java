package ru.biosoft.server.servlets.webservices._test;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import biouml.plugins.ensembl.tabletype.AffymetrixProbeTableType;
import one.util.streamex.IntStreamEx;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.MapAsVector;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.WebJob;
import ru.biosoft.server.servlets.webservices.providers.WebTablesProvider;
import ru.biosoft.server.servlets.webservices.providers.WebTablesProvider.TableQueryResponse;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.JsonUtils;
import ru.biosoft.util.j2html.TagCreator;

/**
 * @author lan
 *
 */
public class TestWebTablesProvider extends AbstractProviderTest
{
    private VectorDataCollection<DataElement> vdc;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
    }

    public void testTableSceleton() throws Exception
    {
        TableDataCollection tdc = new StandardTableDataCollection(vdc, "table");
        vdc.put(tdc);
        tdc.getColumnModel().addColumn("text", String.class);

        Map<String, String> arguments = new HashMap<>();
        arguments.put("de", "test/table");
        JsonObject result = getResponseJSON("table/sceleton", arguments);
        assertValueEquals("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" " +
                "class=\"display editable_table sortable_table\"><thead>" +
                "<tr><th>ID</th><th>text</th></tr></thead><tbody>" +
                "<tr><td colspan=\"2\" class=\"dataTables_empty\">" +
                "Loading data from server</td></tr></tbody></table>", result);
    }

    public void testTableData() throws Exception
    {
        TableDataCollection tdc = new StandardTableDataCollection(vdc, "test");
        tdc.getInfo().getProperties().setProperty(DataCollectionConfigConstants.URL_TEMPLATE, "http://example.com/$id$");
        tdc.getColumnModel().addColumn("text", String.class);
        tdc.getColumnModel().addColumn("integer", Integer.class);
        tdc.getColumnModel().addColumn("float", Double.class);
        tdc.getColumnModel().addColumn("view", CompositeView.class);
        tdc.getColumnModel().addColumn("set", StringSet.class);
        tdc.getColumnModel().addColumn("set2", StringSet.class);
        tdc.getColumnModel().addColumn("boolean", DataType.BooleanType.class);
        tdc.getColumnModel().addColumn("chart", Chart.class);
        tdc.getColumnModel().addColumn("path", DataElementPath.class);
        tdc.getColumnModel().addColumn("textEmpty", String.class);
        tdc.getColumnModel().addColumn( "textLong", String.class );
        tdc.getColumnModel().addColumn( "textLong2", String.class );
        vdc.put(tdc);

        Object[] rowValues = new Object[tdc.getColumnModel().getColumnCount()];
        int i=0;
        rowValues[i++] = "text content";
        rowValues[i++] = 1;
        rowValues[i++] = 2.0;
        CompositeView view = new CompositeView();
        view.add(new BoxView(new Pen(1, Color.BLACK), null, 0, 0, 30, 10));
        rowValues[i++] = view;
        StringSet shortSet = new StringSet(Arrays.asList("set", "of", "strings"));
        rowValues[i++] = shortSet;
        StringSet longSet = IntStreamEx.range( 30 ).mapToObj( String::valueOf ).toCollection( StringSet::new );
        rowValues[i++] = longSet;
        rowValues[i++] = true;
        Chart chart = new Chart();
        chart.addSeries(new double[][] {{1,1},{2,2}});
        rowValues[i++] = chart;
        rowValues[i++] = DataElementPath.create("test/table");
        rowValues[i++] = null;
        rowValues[i++] = "V$AFP1_Q6,V$ALX3_01,V$ALX3_03,V$ALX3_04,V$ALX4_02,V$ALX4_05,V$AMEF2_Q6,V$APA1_02,V$ARNTL_01,V$ARX_01,"
                + "V$ARX_03,V$ATF3_05,V$ATF4_02,V$ATF4_Q5,V$ATF7_01,V$BARHL2_04,V$BARHL2_05,V$BARX1_03,V$BARX1_04,V$BARX2_01,"
                + "V$BATF3_01,V$BATF3_02,V$BATF_02,V$BATF_03,V$BCL6B_05,V$BCL6B_06,V$BCL6_01,V$BCL6_02,V$BCL6_Q3_01,V$BHLHE23_01,"
                + "V$BHLHE41_02,V$BLIMP1_02,V$BRN2_01,V$BRN3B_01,V$BRN3B_Q2,V$BRN3C_01,V$BRN4_01,V$BSX_03,V$CART1_01,V$CART1_02,"
                + "V$CART1_03,V$CDC5_01,V$CDP_02,V$CDX1_01,V$CDX1_03,V$CDX1_Q5,V$CDX2_01,V$CDX2_03,V$CDX2_04,V$CDX2_Q5,V$CDX2_Q5_01,"
                + "V$CDX2_Q5_02,V$CDX_Q5,V$CEBPA_01,V$CEBPA_03,V$CEBPA_Q4,V$CEBPA_Q6,V$CEBPB_01";
        rowValues[i++] = "ENSG00000004848,ENSG00000005073,ENSG00000005102,ENSG00000005513,ENSG00000006377,ENSG00000007372,ENSG00000007866,"
                + "ENSG00000009709,ENSG00000009950,ENSG00000012504,ENSG00000016082,ENSG00000028277,ENSG00000037965,ENSG00000039600,"
                + "ENSG00000043039,ENSG00000049768,ENSG00000052850,ENSG00000054598,ENSG00000057657,ENSG00000063515,ENSG00000064195,"
                + "ENSG00000064218,ENSG00000064835,ENSG00000065970,ENSG00000066336,ENSG00000068305,ENSG00000072310,ENSG00000078399,"
                + "ENSG00000081386,ENSG00000089116,ENSG00000091010,ENSG00000092067,ENSG00000096401,ENSG00000100146,ENSG00000100987,"
                + "ENSG00000101412,ENSG00000101665,ENSG00000103241,ENSG00000103343";
        TableDataCollectionUtils.addRow(tdc, "test", rowValues);

        Map<String, String> arguments = new HashMap<>();
        arguments.put("de", "test/test");
        arguments.put("read", "true");
        JsonObject result = getResponseJSON("table/datatables", arguments);

        assertNotNull(result);
        assertEquals(1, result.get("iTotalDisplayRecords").asInt());
        assertEquals(1, result.get("iTotalRecords").asInt());
        JsonArray data = result.get("aaData").asArray().get(0).asArray();
        assertNotNull(data);
        // ID
        assertEquals("<a target=\"_blank\" style=\"white-space: nowrap\" href=\"" + WebTablesProvider.MAP_PATH + "?de=test%2Ftest%2Ftest\">test</a>",
                unwrap(data.get(0).asString(), 0));
        // Text
        assertEquals("text content", unwrap(data.get(1).asString(), 1));
        // Integer
        assertEquals("1", unwrap(data.get(2).asString(), 2));
        // Double
        assertEquals("<span title=\"2.0\">2</span>", unwrap(data.get(3).asString(), 3));
        // View
        assertEquals( "<div width=\"200px\" id=\"viewer\"><span class=\"table_script_node\">" + "showViewPane('viewer', '"
                + StringEscapeUtils.escapeHtml( view.toJSON().toString() ) + "')</span></div>",
                data.get( 4 ).asString().replaceAll( "viewer_\\d+", "viewer" ) );
        // Set
        assertEquals( "<span>set, of, strings</span>", data.get( 5 ).asString() );
        // Set
        assertEquals( "<span>"
                + "<span>0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,... "
                + "<span class=\"clickable\" onclick=\"BioUMLTable.toggleCellContent(this)\">(more)</span></span>"
                + "<span style=\"display:none\">0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, "
                + "21, 22, 23, 24, 25, 26, 27, 28, 29 <span class=\"clickable\" onclick=\"BioUMLTable.toggleCellContent(this)\">(less)</span></span></span>", data
                .get( 6 ).asString() );
        // Boolean
        assertEquals("true", unwrap(data.get(7).asString(), 7));
        // Chart
        assertEquals("<input type=\"button\" class=\"ui-state-default\" value=\"View\" " +
                "onclick=\"showImage('properties\\/tableCell\\/test\\/test\\/test\\/7')\">", data.get(8).asString());
        // Path
        assertEquals("<div id=\"viewer\"><span class=\"table_script_node\">" +
                "showDataElementLink('viewer', 'test\\/table','','table', false);</span></div>", data.get(9).asString().replaceAll("viewer_\\d+", "viewer"));
        // Empty
        assertEquals("<p class=\"cellControl\">&nbsp;</p>", data.get(10).asString());

        // Long text
        assertEquals( "<p class=\"cellControl\" id=\"0:11\">"
                + "V$AFP1_Q6,V$ALX3_01,V$ALX3_03,V$ALX3_04,V$ALX4_02,V$ALX4_05, "
                + "V$AMEF2_Q6,V$APA1_02,V$ARNTL_01,V$ARX_01,V$ARX_03,V$ATF3_05, "
                + "V$ATF4_02,V$ATF4_Q5,V$ATF7_01,V$BARHL2_04,V$BARHL2_05, "
                + "V$BARX1_03,V$BARX1_04,V$BARX2_01,V$BATF3_01,V$BATF3_02, "
                + "V$BATF_02,V$BATF_03,V$BCL6B_05,V$BCL6B_06,V$BCL6_01,V$BCL6_ "
                + "02,V$BCL6_Q3_01,V$BHLHE23_01,V$BHLHE41_02,V$BLIMP1_02, "
                + "V$BRN2_01,V$BRN3B_01,V$BRN3B_Q2,V$BRN3C_01,V$BRN4_01,V$BSX_ "
                + "03,V$CART1_01,V$CART1_02,V$CART1_03,V$CDC5_01,V$CDP_02, "
                + "V$CDX1_01,V$CDX1_03,V$CDX1_Q5,V$CDX2_01,V$CDX2_03,V$CDX2_04, "
                + "V$CDX2_Q5,V$CDX2_Q5_01,V$CDX2_Q5_02,V$CDX_Q5,V$CEBPA_01, V$CEBPA_03,V$CEBPA_Q4,V$CEBP "
                + "<span class='clickable' onclick='displayTableCell(this)'>(more)</span></p>",
                data.get( 11 ).asString() );
        // Long text
        assertEquals(
                "<p class=\"cellControl\" id=\"0:12\">"
                        + "ENSG00000004848,ENSG00000005073,ENSG00000005102, ENSG00000005513,ENSG00000006377,ENSG00000007372, "
                        + "ENSG00000007866,ENSG00000009709,ENSG00000009950, ENSG00000012504,ENSG00000016082,ENSG00000028277, "
                        + "ENSG00000037965,ENSG00000039600,ENSG00000043039, ENSG00000049768,ENSG00000052850,ENSG00000054598, "
                        + "ENSG00000057657,ENSG00000063515,ENSG00000064195, ENSG00000064218,ENSG00000064835,ENSG00000065970, "
                        + "ENSG00000066336,ENSG00000068305,ENSG00000072310, ENSG00000078399,ENSG00000081386,ENSG00000089116, "
                        + "ENSG00000091010,ENSG00000092067,ENSG00000096401, ENSG00000100146,ENSG00000100987,ENSG00000101412, "
                        + "ENSG00000101665,ENSG0000 "
                        + "<span class='clickable' onclick='displayTableCell(this)'>(more)</span></p>",
                data.get( 12 ).asString() );

        arguments.put("cellId", "0:6");
        result = getResponseJSON("table/cell", arguments);
        assertValueEquals( TagCreator.p().withText( longSet.toString() ).toString(), result );

        result = getResponseJSON("table/rawdata", arguments);
        assertOk(result);
        assertEquals(tdc.getColumnModel().getColumnCount()+1, result.get("values").asArray().size());
        data = result.get("values").asArray();
        assertNotNull(data);
        assertEquals("test", data.get(0).asArray().get(0).asString());
        assertEquals("text content", data.get(1).asArray().get(0).asString());
        assertEquals(1, data.get(2).asArray().get(0).asInt());
        assertEquals(2.0, data.get(3).asArray().get(0).asDouble());
        assertEquals( view, new CompositeView( JsonUtils.toOrgJson( data.get( 4 ).asArray().get( 0 ).asObject() ) ) );
        assertEquals( "[\"set\",\"of\",\"strings\"]", data.get( 5 ).asArray().get( 0 ).asArray().toString() );
        assertEquals( "[\"0\",\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"7\",\"8\",\"9\",\"10\",\"11\",\"12\",\"13\",\"14\","
                + "\"15\",\"16\",\"17\",\"18\",\"19\",\"20\",\"21\",\"22\",\"23\",\"24\",\"25\",\"26\",\"27\",\"28\",\"29\"]", data.get( 6 ).asArray().get( 0 ).asArray()
                .toString() );
        assertEquals("true", data.get(7).asArray().get(0).asString());
        assertEquals("[{\"data\":[[1,1],[2,2]],\"color\":\"rgb(255,0,0)\"}]", data.get(8).asArray().get(0).asString());
        assertEquals("test/table", data.get(9).asArray().get(0).asString());

        assertEquals(
                "V$AFP1_Q6,V$ALX3_01,V$ALX3_03,V$ALX3_04,V$ALX4_02,V$ALX4_05,V$AMEF2_Q6,V$APA1_02,V$ARNTL_01,V$ARX_01,"
                        + "V$ARX_03,V$ATF3_05,V$ATF4_02,V$ATF4_Q5,V$ATF7_01,V$BARHL2_04,V$BARHL2_05,V$BARX1_03,V$BARX1_04,V$BARX2_01,"
                        + "V$BATF3_01,V$BATF3_02,V$BATF_02,V$BATF_03,V$BCL6B_05,V$BCL6B_06,V$BCL6_01,V$BCL6_02,V$BCL6_Q3_01,V$BHLHE23_01,"
                        + "V$BHLHE41_02,V$BLIMP1_02,V$BRN2_01,V$BRN3B_01,V$BRN3B_Q2,V$BRN3C_01,V$BRN4_01,V$BSX_03,V$CART1_01,V$CART1_02,"
                        + "V$CART1_03,V$CDC5_01,V$CDP_02,V$CDX1_01,V$CDX1_03,V$CDX1_Q5,V$CDX2_01,V$CDX2_03,V$CDX2_04,V$CDX2_Q5,V$CDX2_Q5_01,"
                        + "V$CDX2_Q5_02,V$CDX_Q5,V$CEBPA_01,V$CEBPA_03,V$CEBPA_Q4,V$CEBPA_Q6,V$CEBPB_01",
                data.get( 11 ).asArray().get( 0 ).asString() );
        assertEquals( "ENSG00000004848,ENSG00000005073,ENSG00000005102,ENSG00000005513,ENSG00000006377,ENSG00000007372,ENSG00000007866,"
                + "ENSG00000009709,ENSG00000009950,ENSG00000012504,ENSG00000016082,ENSG00000028277,ENSG00000037965,ENSG00000039600,"
                + "ENSG00000043039,ENSG00000049768,ENSG00000052850,ENSG00000054598,ENSG00000057657,ENSG00000063515,ENSG00000064195,"
                + "ENSG00000064218,ENSG00000064835,ENSG00000065970,ENSG00000066336,ENSG00000068305,ENSG00000072310,ENSG00000078399,"
                + "ENSG00000081386,ENSG00000089116,ENSG00000091010,ENSG00000092067,ENSG00000096401,ENSG00000100146,ENSG00000100987,"
                + "ENSG00000101412,ENSG00000101665,ENSG00000103241,ENSG00000103343", data.get( 12 ).asArray().get( 0 ).asString() );
    }

    public void testChangeData() throws Exception
    {
        TableDataCollection tdc = new StandardTableDataCollection(null, "test");
        tdc.getColumnModel().addColumn("text", String.class);
        TableDataCollectionUtils.addRow(tdc, "test", new Object[] {"yes"});

        assertEquals("yes", tdc.get("test").getValue("text"));

        Map<String, String> arguments = new HashMap<>();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TableQueryResponse response = new TableQueryResponse(tdc, null, new BiosoftWebRequest(arguments), out);
        JSONArray data = new JSONArray();
        JSONObject change = new JSONObject();
        change.put("name", "0:1");
        change.put("value", "no");
        data.put(change);
        response.sendChangeData( "test", data, null );
        JsonObject result = JsonObject.readFrom( new String(out.toByteArray(), StandardCharsets.UTF_8));
        assertOk(result);

        assertEquals("no", tdc.get("test").getValue("text"));
    }

    public void testFilteredTable() throws Exception
    {
        TableDataCollection tdc = createTestTable();

        Map<String, String> arguments = new HashMap<>();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new TableQueryResponse(tdc, null, new BiosoftWebRequest(arguments), out).sendTableData("test", true);
        JsonObject result = JsonObject.readFrom( new String(out.toByteArray(), StandardCharsets.UTF_8));
        assertNotNull(result);
        assertEquals(5, result.get("iTotalRecords").asInt());

        arguments.put("filter", "ID.indexOf('a')>=0");
        arguments.put("action", "checkFilter");
        arguments.put("de", "test/test");

        assertOk(getResponseJSON("table", arguments));

        out = new ByteArrayOutputStream();
        new TableQueryResponse(tdc, null, new BiosoftWebRequest(arguments), out).sendTableData("test", true);
        result = JsonObject.readFrom( new String(out.toByteArray(), StandardCharsets.UTF_8));
        assertNotNull(result);
        assertEquals(2, result.get("iTotalRecords").asInt());
    }

    public void testConvertExpressions() throws Exception
    {
        TableDataCollection tdc = new StandardTableDataCollection(vdc, "test");
        TableColumn col = tdc.getColumnModel().addColumn("Expr", String.class);
        String exprIndex = String.valueOf( tdc.getColumnModel().optColumnIndex( "Expr" ) + 1 );
        TableDataCollectionUtils.addRow(tdc, "AB", new Object[] {""});
        TableDataCollectionUtils.addRow(tdc, "CD", new Object[] {""});
        col.setExpression("ID+ID");
        vdc.put(tdc);

        assertEquals("", tdc.get("AB").getValues(false)[0]);

        Map<String, String> arguments = new HashMap<>();
        arguments.put("action", "columns");
        arguments.put("colaction", "toValues");
        arguments.put("de", "test/test");
        arguments.put( "jsoncols", new JSONArray( Arrays.asList( exprIndex ) ).toString() );
        assertOk(getResponseJSON("table", arguments));
        assertEquals("ABAB", tdc.get("AB").getValues(false)[0]);
    }

    public void testColumnsResolver() throws Exception
    {
        TableDataCollection tdc = new StandardTableDataCollection(vdc, "test");
        tdc.getColumnModel().addColumn("myCol", Double.class);
        vdc.put(tdc);
        Map<String, String> arguments = new HashMap<>();
        arguments.put("action", "datatables");
        arguments.put("de", "test/test");
        arguments.put("type2", "columns");
        arguments.put("read", "true");

        JsonObject json = getResponseJSON("table", arguments);
        assertNotNull(json);
        assertEquals(2, json.get("iTotalRecords").asInt());
        JsonArray rowData = json.get("aaData").asArray().get(0).asArray();
        assertEquals("<p class=\"cellControl\" id=\"0:0\">0</p>", rowData.get(0).asString());
        assertEquals("<p class=\"cellControl\" id=\"0:1\">id</p>", rowData.get(1).asString());
        assertEquals("<p class=\"cellControl\" id=\"0:2\">Text</p>", rowData.get(2).asString());
        rowData = json.get("aaData").asArray().get(1).asArray();
        assertEquals("<p class=\"cellControl\" id=\"1:1\">myCol</p>", rowData.get(1).asString());
        assertEquals("<p class=\"cellControl\" id=\"1:2\">Float</p>", rowData.get(2).asString());

        arguments.put("action", "columns");
        json = getResponseJSON("table", arguments);
        assertOk(json);
        JsonArray columnsJSON = json.get("values").asArray();
        assertEquals("_", columnsJSON.get(0).asObject().get("jsName").asString());
        assertEquals("Column_name", columnsJSON.get(1).asObject().get("jsName").asString());
        assertEquals("Type", columnsJSON.get(2).asObject().get("jsName").asString());
        assertEquals("Description", columnsJSON.get(3).asObject().get("jsName").asString());
        assertEquals("Expression", columnsJSON.get(4).asObject().get("jsName").asString());

        arguments.remove("type2");
        json = getResponseJSON("table", arguments);
        assertOk(json);
        columnsJSON = json.get("values").asArray();
        assertEquals("ID", columnsJSON.get(0).asObject().get("name").asString());
        assertEquals("Text", columnsJSON.get(0).asObject().get("type").asString());
        assertEquals("myCol", columnsJSON.get(1).asObject().get("name").asString());
        assertEquals("Float", columnsJSON.get(1).asObject().get("type").asString());
    }

    public void testExportTable() throws Exception
    {
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);

        TableDataCollection tdc = createTestTable();

        Map<String, String> arguments = new HashMap<>();
        arguments.put("filter", "ID.indexOf('a')>=0");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataElementPath path = DataElementPath.create("test/filtered");
        WebJob wj = WebJob.getWebJob( "exportJob1" );
        new TableQueryResponse(tdc, null, new BiosoftWebRequest(arguments), out).exportFilteredTable(tdc, path, wj);
        JsonObject result = JsonObject.readFrom( new String(out.toByteArray(), StandardCharsets.UTF_8));
        assertOk(result);
        waitForJob( "exportJob1" );

        TableDataCollection exported = path.getDataElement(TableDataCollection.class);
        assertEquals(new HashSet<>(Arrays.asList("a","ab")), new HashSet<>(exported.getNameList()));
    }

    public void testColumnActions() throws Exception
    {
        TableDataCollection tdc = new StandardTableDataCollection(vdc, "table");
        vdc.put(tdc);
        tdc.getColumnModel().addColumn("text", String.class);

        Map<String, String> arguments = new HashMap<>();
        arguments.put("de", "test/table");
        arguments.put("colaction", "add");
        assertOk(getResponseJSON("table/columns", arguments));
        assertEquals(2, tdc.getColumnModel().getColumnCount());
        assertEquals(ColumnModel.NEW_COLUMN_NAME, tdc.getColumnModel().getColumn(1).getName());

        String textInd = String.valueOf( tdc.getColumnModel().optColumnIndex( "text" ) + 1 );
        arguments.put("colaction", "remove");
        arguments.put( "jsoncols", new JSONArray( Arrays.asList( textInd ) ).toString() );
        assertOk(getResponseJSON("table/columns", arguments));
        assertEquals(1, tdc.getColumnModel().getColumnCount());
        assertEquals(ColumnModel.NEW_COLUMN_NAME, tdc.getColumnModel().getColumn(0).getName());
    }

    public void testCreateTable() throws Exception
    {
        JSONArray columns = new JSONArray();
        JSONObject column = new JSONObject();
        column.put("name", "ID");
        column.put("type", "Integer");
        columns.put(column);
        column = new JSONObject();
        column.put("name", "AffyID");
        column.put("type", "Text");
        column.put("referenceType", ReferenceTypeRegistry.getReferenceType(AffymetrixProbeTableType.class).toString());
        columns.put(column);
        JSONArray data = new JSONArray();
        JSONArray columnData = new JSONArray();
        columnData.put("1");
        columnData.put("2");
        columnData.put("3");
        data.put(columnData);
        columnData = new JSONArray();
        columnData.put("1_at");
        columnData.put("2_at");
        columnData.put("3_at");
        data.put(columnData);

        Map<String, String> arguments = new HashMap<>();
        arguments.put("de", "test/newTable");
        arguments.put("data", data.toString());
        arguments.put("columns", columns.toString());
        JsonObject json = getResponseJSON("table/createTable", arguments);
        assertOk(json);

        TableDataCollection tdc = (TableDataCollection)vdc.get("newTable");
        assertNotNull(tdc);
        assertEquals(3, tdc.getSize());
        assertEquals(1, tdc.getColumnModel().getColumnCount());
        assertTrue(Boolean.valueOf(tdc.getInfo().getProperty(TableDataCollection.INTEGER_IDS)));
        TableColumn columnInfo = tdc.getColumnModel().getColumn(0);
        assertEquals("AffyID", columnInfo.getName());
        assertEquals(String.class, columnInfo.getValueClass());
        assertEquals(ReferenceTypeRegistry.getReferenceType(AffymetrixProbeTableType.class).toString(), columnInfo.getValue(ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY));
        RowDataElement row = tdc.get("2");
        assertNotNull(row);
        assertEquals("2_at", row.getValue("AffyID"));
    }

    public void testSiteTables() throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, "track");
        WritableTrack track = TrackUtils.createTrack(vdc, properties);
        vdc.put(track);
        Sequence sequence = new LinearSequence("seq", "ACGTACGTACGTACGTACAGCATCGACGATCGATCGTAC", Nucleotide15LetterAlphabet.getInstance());
        vdc.put(new MapAsVector("seq", vdc, sequence, null));
        track.addSite(new SiteImpl(null, "1", 2, 5, StrandType.STRAND_PLUS, sequence));
        track.addSite(new SiteImpl(null, "2", 4, 6, StrandType.STRAND_PLUS, sequence));
        track.addSite(new SiteImpl(null, "3", 12, 5, StrandType.STRAND_PLUS, sequence));
        track.addSite(new SiteImpl(null, "4", 17, 5, StrandType.STRAND_PLUS, sequence));

        Map<String, String> arguments = new HashMap<>();
        arguments.put("de", "test/track");
        arguments.put("read", "true");
        arguments.put( "type", "track" );
        JsonObject result = getResponseJSON("table/datatables", arguments);
        assertEquals(4, result.get("iTotalDisplayRecords").asInt());
        assertEquals(4, result.get("iTotalRecords").asInt());
        JsonArray data = result.get("aaData").asArray().get(0).asArray();
        assertNotNull(data);
        assertEquals("1", unwrap(data.get(0).asString(), 0));
        assertEquals("seq", unwrap(data.get(1).asString(), 1));
        assertEquals("2", unwrap(data.get(2).asString(), 2));
        assertEquals("6", unwrap(data.get(3).asString(), 3));
        assertEquals("5", unwrap(data.get(4).asString(), 4));
        assertEquals("+", unwrap(data.get(5).asString(), 5));
        assertEquals("misc_feature", unwrap(data.get(6).asString(), 6));

        arguments = new HashMap<>();
        arguments.put("de", "test/seq");
        arguments.put("read", "true");
        arguments.put("track", "test/track");
        arguments.put("from", "10");
        arguments.put("to", "15");
        arguments.put( "type", "sites" );
        result = getResponseJSON("table/datatables", arguments);
        assertEquals(1, result.get("iTotalDisplayRecords").asInt());
        assertEquals(1, result.get("iTotalRecords").asInt());
        data = result.get("aaData").asArray().get(0).asArray();
        assertNotNull(data);
        assertEquals("3", unwrap(data.get(0).asString(), 0));
        assertEquals("seq", unwrap(data.get(1).asString(), 1));
        assertEquals("12", unwrap(data.get(2).asString(), 2));
        assertEquals("16", unwrap(data.get(3).asString(), 3));
        assertEquals("5", unwrap(data.get(4).asString(), 4));
        assertEquals("+", unwrap(data.get(5).asString(), 5));
        assertEquals("misc_feature", unwrap(data.get(6).asString(), 6));
    }

    private String unwrap(String string, int i)
    {
        int pos1 = string.indexOf('>');
        assertTrue(string, pos1 > 0);
        String prefix = string.substring(0, pos1+1);
        int pos2 = string.lastIndexOf('<');
        assertTrue(string, pos2 > 0);
        String suffix = string.substring(pos2);
        assertEquals(string, "</p>", suffix);
        Matcher matcher = Pattern.compile("<p class=\"cellControl\" id=\"0\\:(\\d+)\">").matcher(prefix);
        assertTrue(string, matcher.matches());
        assertEquals(string, i, Integer.parseInt(matcher.group(1)));
        return string.substring(pos1+1, pos2);
    }

    protected TableDataCollection createTestTable() throws Exception
    {
        TableDataCollection tdc = new StandardTableDataCollection(vdc, "test");
        TableDataCollectionUtils.addRow(tdc, "a", new Object[0]);
        TableDataCollectionUtils.addRow(tdc, "ab", new Object[0]);
        TableDataCollectionUtils.addRow(tdc, "bc", new Object[0]);
        TableDataCollectionUtils.addRow(tdc, "cd", new Object[0]);
        TableDataCollectionUtils.addRow(tdc, "de", new Object[0]);
        vdc.put(tdc);
        return tdc;
    }
}

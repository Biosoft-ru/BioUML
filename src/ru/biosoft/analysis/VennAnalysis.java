package ru.biosoft.analysis;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.analysis.javascript.JavaScriptAnalysis;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.EllipseView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.access.ViewDataElement;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

@ClassIcon("resources/venn.gif")
public class VennAnalysis extends AnalysisMethodSupport<VennAnalysisParameters>
{
    private static final int maxRadius = 300;
    private static final int minRadius = 100;
    private static final int standardRadius = 150;
    private static final int labelSpace = 15;
    
    private static final String LEFT_TABLE_ALIAS = "T1";
    private static final String RIGHT_TABLE_ALIAS = "T2";
    private static final String CENTER_TABLE_ALIAS = "T3";
    
    private static class TableInfo
    {
        TableDataCollection table;
        String name;
        String alias;
        Brush brush;

        public TableInfo(TableDataCollection table, String name, String alias, Brush brush)
        {
            this.table = table;
            this.name = shorten((table != null && (name == null || name.trim().isEmpty())) ? table.getName() : name);
            this.brush = brush;
            this.alias = alias;
        }

        private static String shorten(String original)
        {
            if(original == null) return null;
            if(original.length() > 20) return original.substring(0, 18)+"...";
            return original;
        }
    }
    
    private final List<TableInfo> tablesInfo = new ArrayList<>();
    
    public VennAnalysis(DataCollection<?> origin, String name) throws Exception
    {
        super(origin, name, JavaScriptAnalysis.class, new VennAnalysisParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        boolean leftIsEmpty = parameters.getTable1() == null;
        boolean rightIsEmpty = parameters.getTable2() == null;
        boolean centerIsEmpty = parameters.getTable3() == null;
        if( leftIsEmpty && rightIsEmpty && centerIsEmpty )
            throw new IllegalArgumentException("At least one table must be selected");
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        log.info("Creating sub-tables...");
        DataCollection outputFolder = DataCollectionUtils.createSubCollection(parameters.getOutput());
        jobControl.pushProgress(0, 90);
        initTablesInfo();
        createSubTables(tablesInfo.get(0), tablesInfo.get(1), tablesInfo.get(2));
        jobControl.popProgress();
        if(jobControl.isStopped())
            return outputFolder;
        jobControl.pushProgress(90, 100);
        log.info("Creating diagram...");
        CompositeView view = createDiagramView(tablesInfo.get(0), tablesInfo.get(1), tablesInfo.get(2));
        ViewDataElement result = new ViewDataElement("Diagram", parameters.getOutput().getDataCollection(), view);
        CollectionFactoryUtils.save(result);
        jobControl.popProgress();
        return new ru.biosoft.access.core.DataElement[] {outputFolder, result};
    }
    
    private void initTablesInfo()
    {
        tablesInfo.clear();
        tablesInfo.add(new TableInfo(parameters.getTable1(), parameters.getTable1Name(), LEFT_TABLE_ALIAS,
                       new Brush(parameters.getCircle1Color())));
        tablesInfo.add(new TableInfo(parameters.getTable2(), parameters.getTable2Name(), RIGHT_TABLE_ALIAS,
                       new Brush(parameters.getCircle2Color())));
        tablesInfo.add(new TableInfo(parameters.getTable3(), parameters.getTable3Name(), CENTER_TABLE_ALIAS,
                       new Brush(parameters.getCircle3Color())));

    }

    private void createSubTables(TableInfo tableInfo1, TableInfo tableInfo2, TableInfo tableInfo3) throws Exception
    {
        if( tableInfo3.table == null )
        {
            createSubTables(tableInfo1, tableInfo2);
            return;
        }
        else if( tableInfo2.table == null )
        {
            createSubTables(tableInfo1, tableInfo3);
            return;
        }
        else if( tableInfo1.table == null )
        {
            createSubTables(tableInfo2, tableInfo3);
            return;
        }
        copyTable(tableInfo1);
        jobControl.setPreparedness(10);
        if(jobControl.isStopped())
            return;
        copyTable(tableInfo2);
        jobControl.setPreparedness(20);
        if(jobControl.isStopped())
            return;
        copyTable(tableInfo3);
        jobControl.setPreparedness(30);
        if(jobControl.isStopped())
            return;
        String formatIntersectionDescription = "Rows present in %s, but not in %s and %s";
        filterTable(String.format(formatIntersectionDescription, tableInfo1.alias, tableInfo2.alias, tableInfo3.alias),
                    tableInfo1.table, tableInfo2.table, tableInfo3.table);
        jobControl.setPreparedness(40);
        if(jobControl.isStopped())
            return;
        filterTable(String.format(formatIntersectionDescription, tableInfo2.alias, tableInfo1.alias, tableInfo3.alias),
                    tableInfo2.table, tableInfo1.table, tableInfo3.table);
        jobControl.setPreparedness(50);
        if(jobControl.isStopped())
            return;
        filterTable(String.format(formatIntersectionDescription, tableInfo3.alias, tableInfo1.alias, tableInfo2.alias),
                    tableInfo3.table, tableInfo1.table, tableInfo2.table);
        jobControl.setPreparedness(60);
        if(jobControl.isStopped())
            return;
        try
        {
            formatIntersectionDescription = "Rows present in %s and %s, but not in %s";
            TableDataCollection t1t2 = TableDataCollectionUtils.join(TableDataCollectionUtils.INNER_JOIN, tableInfo1.table, tableInfo2.table, parameters.getOutput().getChildPath("T1+T2"));
            filterTable(String.format(formatIntersectionDescription, tableInfo1.alias, tableInfo2.alias, tableInfo3.alias),
                        t1t2, tableInfo3.table);
            jobControl.setPreparedness(70);
            if(jobControl.isStopped())
                return;
            TableDataCollection t2t3 = TableDataCollectionUtils.join(TableDataCollectionUtils.INNER_JOIN, tableInfo2.table, tableInfo3.table, parameters.getOutput().getChildPath("T2+T3"));
            filterTable(String.format(formatIntersectionDescription, tableInfo2.alias, tableInfo3.alias, tableInfo1.alias),
                        t2t3, tableInfo1.table);
            jobControl.setPreparedness(80);
            if(jobControl.isStopped())
                return;
            TableDataCollection t1t3 = TableDataCollectionUtils.join(TableDataCollectionUtils.INNER_JOIN, tableInfo1.table, tableInfo3.table, parameters.getOutput().getChildPath("T1+T3"));
            filterTable(String.format(formatIntersectionDescription, tableInfo1.alias, tableInfo3.alias, tableInfo2.alias),
                        t1t3, tableInfo2.table);
            jobControl.setPreparedness(90);
            if(jobControl.isStopped())
                return;
            String name = "Rows present in all three tables";
            log.info("Creating "+name);
            TableDataCollectionUtils.join(TableDataCollectionUtils.INNER_JOIN, t1t2, tableInfo3.table, parameters.getOutput().getChildPath(name));
        }
        finally
        {
            DataCollection<DataElement> result = parameters.getOutput().getDataCollection();
            result.remove("T1+T2");
            result.remove("T1+T3");
            result.remove("T2+T3");
        }
    }

    private void createSubTables(TableInfo tableInfo1, TableInfo tableInfo2) throws Exception
    {
        if( tableInfo1.table != null )
            copyTable(tableInfo1);
        jobControl.setPreparedness(20);
        if(jobControl.isStopped())
            return;
        if( tableInfo2.table != null )
            copyTable(tableInfo2);
        if( tableInfo1.table == null || tableInfo2.table == null )
            return;
        jobControl.setPreparedness(40);
        if(jobControl.isStopped())
            return;
        String name = "Rows present in both tables";
        log.info("Creating "+name);
        TableDataCollectionUtils.join(TableDataCollectionUtils.INNER_JOIN, tableInfo1.table, tableInfo2.table, parameters.getOutput().getChildPath(name));
        jobControl.setPreparedness(60);
        if(jobControl.isStopped())
            return;
        String formatIntersectionDescription = "Rows present in %s, but not in %s";
        filterTable(String.format(formatIntersectionDescription, tableInfo1.alias, tableInfo2.alias), tableInfo1.table, tableInfo2.table);
        jobControl.setPreparedness(80);
        if(jobControl.isStopped())
            return;
        filterTable(String.format(formatIntersectionDescription, tableInfo2.alias, tableInfo1.alias), tableInfo2.table, tableInfo1.table);
    }

    private void filterTable(String name, TableDataCollection table, TableDataCollection... tables) throws Exception
    {
        log.info("Creating "+name);
        TableDataCollection result = table.clone(parameters.getOutput().getDataCollection(), name);
        for(TableDataCollection filterTable: tables)
            for(String rowName: filterTable.getNameList())
                result.remove(rowName);
        CollectionFactoryUtils.save(result);
    }

    private void copyTable(TableInfo tableInfo)
    {
        String name = tableInfo.alias + " " + tableInfo.table.getName();
        log.info("Creating "+name);
        TableDataCollection result = tableInfo.table.clone(parameters.getOutput().getDataCollection(), name);
        CollectionFactoryUtils.save(result);
    }

    public CompositeView createDiagramView(TableInfo table1, TableInfo table2, TableInfo table3)
    {
        if( table3.table == null )
        {
            return createDiagramView(table1, table2);
        }
        else if( table2.table == null )
        {
            return createDiagramView(table1, table3);
        }
        else if( table1.table == null )
        {
            return createDiagramView(table2, table3);
        }

        List<String> names1 = table1.table.getNameList();
        List<String> names2 = table2.table.getNameList();
        List<String> names3 = table3.table.getNameList();
        Set<String> names12 = new HashSet<>(names1);
        names12.retainAll(names2);
        Set<String> names23 = new HashSet<>(names2);
        names23.retainAll(names3);
        Set<String> names13 = new HashSet<>(names1);
        names13.retainAll(names3);
        Set<String> names123 = new HashSet<>(names12);
        names123.retainAll(names3);

        int count1 = table1.table.getSize();
        int count2 = table2.table.getSize();
        int count3 = table3.table.getSize();
        int count123 = names123.size();
        int count12 = names12.size();
        int count13 = names13.size();
        int count23 = names23.size();
        int sum = count1 + count2 + count3;

        int r1 = standardRadius;
        int r2 = standardRadius;
        int r3 = standardRadius;

        if( !parameters.isSimple() )
        {
            r1 = Math.max(count1 * maxRadius / sum, minRadius);
            r2 = Math.max(count2 * maxRadius / sum, minRadius);
            r3 = Math.max(count3 * maxRadius / sum, minRadius);
        }

        count1 -= count12 + count13 - count123;
        count2 -= count12 + count23 - count123;
        count3 -= count13 + count23 - count123;
        count12 -= count123;
        count13 -= count123;
        count23 -= count123;

        Point c1 = new Point();
        Point c2 = new Point();
        Point c3 = new Point();
        Point c12;
        Point c21;
        Point c13;
        Point c31;
        Point c23;
        Point c32;

        Pen pen = new Pen(1, Color.black);

        int maxR = Math.max(r1, r2);

        //Center of first circle
        c1.x = r1;
        c1.y = maxR + labelSpace;

        //Center of second sircle
        c2.x = c1.x + maxR;
        c2.y = c1.y;

        //Points of circle1 and circle2 intersections
        //cos12 is angle between (C1,C12) and (C1,C2) lines where
        //C1 is the center of first circle,
        //C2 is the center of second circle,
        //C12 is the point of circles intersection
        double val = (double)Math.min(r1, r2) / maxR;
        double cos12 = 1 - ( val * val ) / 2;
        double sin12 = Math.sqrt(1 - cos12 * cos12);
        if( r1 > r2 )
        {
            c12 = rotate(c1, c2, sin12);
            c21 = rotate(c1, c2, -sin12);
        }
        else
        {
            c21 = rotate(c2, c1, sin12);
            c12 = rotate(c2, c1, -sin12);
        }

        //Center of third circle
        if( r3 < r1 && r3 < r2 )
        {
            c3 = c12;
        }
        else if( r3 > r1 && r3 > r2 )
        {
            int maxR2 = maxR / 2;
            c3.x = c1.x + maxR2;//maxR / 2;
            c3.y = (int) ( c1.y + Math.sqrt(r3 * r3 - maxR2 * maxR2) );
        }
        else
        {
            double cosPsi = 1 - ( Math.pow((double)r3 / (double)maxR, 2) / 2 );
            double sinPsi = Math.sqrt(1 - cosPsi * cosPsi);

            if( r1 > r2 ) //r1 > r3 > r2
            {
                c3.x = (int) ( c1.x * ( 1 + cosPsi ) );
            }
            else
            //r2 > r3 > r1
            {
                c3.x = (int) ( c1.x + r2 * ( 1 - cosPsi ) );
            }
            c3.y = (int) ( c1.y * ( 1 + sinPsi ) );
        }

        //Points of circle1 and circle3 intersection
        //Angle between (C1,C13) and (C1,C3) lines where
        //C1 is the center of first circle,
        //C3 is the center of third circle,
        //C13 is the point of circles intersection
        double cos13 = 1 - ( Math.pow((double)Math.min(r1, r3) / Math.max(r1, r3), 2) ) / 2;
        double sin13 = Math.sqrt(1 - cos13 * cos13);
        if( r1 > r3 )
        {
            c13 = rotate(c1, c3, sin13);
            c31 = rotate(c1, c3, -sin13);
        }
        else
        {
            c31 = rotate(c3, c1, sin13);
            c13 = rotate(c3, c1, -sin13);
        }

        //Points of circle2 and sircle3 intersection
        //Angle between (C2,C23) and (C2,C3) lines where
        //C2 is the center of second circle,
        //C3 is the center of third circle,
        //C23 is the point of circles intersection
        double cos23 = 1 - ( Math.pow((double)Math.min(r2, r3) / Math.max(r2, r3), 2) ) / 2;
        double sin23 = Math.sqrt(1 - cos23 * cos23);
        if( r2 > r3 )
        {
            c23 = rotate(c2, c3, sin23);
            c32 = rotate(c2, c3, -sin23);
        }
        else
        {
            c32 = rotate(c3, c2, sin23);
            c23 = rotate(c3, c2, -sin23);
        }

        Point inCenter = getInCenter(c1, c2, c3);

        Point t1 = shift(inCenter, c1, r1 / 3.0, false);
        Point t2 = shift(inCenter, c2, r2 / 3.0, false);
        Point t3 = shift(inCenter, c3, r3 / 3.0, false);
        Point t123 = getInCenter(c12, c23, c31);
        Point t12 = getInCenter(c23, c21, c31);
        Point t23 = getInCenter(c31, c32, c12);
        Point t31 = getInCenter(c12, c13, c23);

        ColorFont font = new ColorFont("Tahoma", Font.BOLD, 12, Color.BLACK);
        Graphics g = ApplicationUtils.getGraphics();

        TextView text1 = new TextView(String.valueOf(count1), t1, TextView.CENTER, font, g);
        TextView text2 = new TextView(String.valueOf(count2), t2, TextView.CENTER, font, g);
        TextView text3 = new TextView(String.valueOf(count3), t3, TextView.CENTER, font, g);
        TextView text123 = new TextView(String.valueOf(count123), t123, TextView.CENTER, font, g);
        TextView text12 = new TextView(String.valueOf(count12), t12, TextView.CENTER, font, g);
        TextView text13 = new TextView(String.valueOf(count13), t31, TextView.RIGHT, font, g);
        TextView text23 = new TextView(String.valueOf(count23), t23, TextView.LEFT, font, g);

        TextView label1 = new TextView(table1.name + " (" + table1.table.getSize() + ")", new Point(0, c1.y - r1 - 5), TextView.LEFT, font,
                g);
        TextView label2 = new TextView(table2.name + " (" + table2.table.getSize() + ")", new Point(r1 + maxR + r2, c2.y - r2 - 5),
                TextView.RIGHT, font, g);
        TextView label3 = new TextView(table3.name + " (" + table3.table.getSize() + ")", new Point(c3.x, c3.y + r3 + labelSpace),
                TextView.CENTER, font, g);

        CompositeView result = new CompositeView();

        result.add(new EllipseView(null, table1.brush, c1.x - r1, c1.y - r1, 2 * r1, 2 * r1), CompositeView.REL, new Point(0, 0));
        result.add(new EllipseView(null, table2.brush, c2.x - r2, c2.y - r2, 2 * r2, 2 * r2), CompositeView.REL, new Point(0, 0));
        result.add(new EllipseView(null, table3.brush, c3.x - r3, c3.y - r3, 2 * r3, 2 * r3), CompositeView.REL, new Point(0, 0));
        result.add(new EllipseView(pen, null, c1.x - r1, c1.y - r1, 2 * r1, 2 * r1), CompositeView.REL, new Point(0, 0));
        result.add(new EllipseView(pen, null, c2.x - r2, c2.y - r2, 2 * r2, 2 * r2), CompositeView.REL, new Point(0, 0));
        result.add(new EllipseView(pen, null, c3.x - r3, c3.y - r3, 2 * r3, 2 * r3), CompositeView.REL, new Point(0, 0));

        result.add(text1, CompositeView.REL, new Point(0, 0));
        result.add(text2, CompositeView.REL, new Point(0, 0));
        result.add(text3, CompositeView.REL, new Point(0, 0));
        result.add(text12, CompositeView.REL, new Point(0, 0));
        result.add(text13, CompositeView.REL, new Point(0, 0));
        result.add(text23, CompositeView.REL, new Point(0, 0));
        result.add(text123, CompositeView.REL, new Point(0, 0));

        result.add(label1, CompositeView.REL, new Point(0, 0));
        result.add(label2, CompositeView.REL, new Point(0, 0));
        result.add(label3, CompositeView.REL, new Point(0, 0));

        return result;
    }

    public CompositeView createDiagramView(TableInfo table)
    {
        CompositeView result = new CompositeView();
        if( table == null )
            return result;

        Graphics g = ApplicationUtils.getGraphics();
        Pen pen = new Pen(1, Color.black);
        ColorFont font = new ColorFont("Tahoma", Font.BOLD, 12, Color.BLACK);
        TextView label = new TextView(table.name + " (" + table.table.getSize() + ")", new Point(150, 5), TextView.CENTER, font, g);
        TextView text = new TextView(String.valueOf(table.table.getSize()), new Point(150, 150 + labelSpace), TextView.CENTER, font, g);
        result.add(new EllipseView(pen, table.brush, 0, labelSpace, 300, 300), CompositeView.REL, new Point(0, 0));
        result.add(text, CompositeView.REL, new Point(0, 0));
        result.add(label, CompositeView.REL, new Point(0, 0));

        return result;
    }

    public CompositeView createDiagramView(TableInfo table1, TableInfo table2)
    {
        if( table1.table == null )
        {
            return createDiagramView(table2);
        }
        else if( table2.table == null )
        {
            return createDiagramView(table1);
        }

        int count1 = table1.table.getSize();
        int count2 = table2.table.getSize();
        Set<String> names12 = new HashSet<>(table1.table.getNameList());
        names12.retainAll(table2.table.getNameList());
        int count12 = names12.size();

        int r1 = standardRadius;
        int r2 = standardRadius;

        if( !parameters.isSimple() )
        {
            r1 = (int)Math.max( ( (double)count1 / ( count1 + count2 ) * maxRadius ), minRadius);
            r2 = (int)Math.max( ( (double)count2 / ( count1 + count2 ) * maxRadius ), minRadius);
        }

        int r = Math.max(r1, r2);

        Point t12 = new Point();
        t12.y = r + labelSpace;
        if( r1 >= r2 )
        {
            t12.x = 2 * r1 - r2 / 2;
        }
        else
        {
            t12.x = 3 * r1 / 2;
        }

        count1 -= count12;
        count2 -= count12;

        Graphics g = ApplicationUtils.getGraphics();
        Pen pen = new Pen(1, Color.black);
        ColorFont font = new ColorFont("Tahoma", Font.BOLD, 12, Color.BLACK);

        TextView text1 = new TextView(String.valueOf(count1), new Point(2 * r1 / 3, r + labelSpace), TextView.CENTER, font, g);
        TextView text2 = new TextView(String.valueOf(count2), new Point(r1 + r + r2 / 3, r + labelSpace), TextView.CENTER, font, g);
        TextView text12 = new TextView(String.valueOf(count12), t12, TextView.CENTER, font, g);

        TextView label1 = new TextView(table1.name + " (" + table1.table.getSize() + ")", new Point(0, r - r1), TextView.LEFT, font, g);
        TextView label2 = new TextView(table2.name + " (" + table2.table.getSize() + ")", new Point(r1 + r + r2, r - r2), TextView.RIGHT,
                font, g);

        CompositeView result = new CompositeView();

        result.add(new EllipseView(null, table1.brush, 0, r - r1 + labelSpace, 2 * r1, 2 * r1), CompositeView.REL, new Point(0, 0));
        result.add(new EllipseView(null, table2.brush, r1 - r2 + r, r - r2 + labelSpace, 2 * r2, 2 * r2), CompositeView.REL, new Point(0, 0));
        result.add(new EllipseView(pen, null, 0, r - r1 + labelSpace, 2 * r1, 2 * r1), CompositeView.REL, new Point(0, 0));
        result.add(new EllipseView(pen, null, r1 - r2 + r, r - r2 + labelSpace, 2 * r2, 2 * r2), CompositeView.REL, new Point(0, 0));
        result.add(text1, CompositeView.REL, new Point(0, 0));
        result.add(text2, CompositeView.REL, new Point(0, 0));
        result.add(text12, CompositeView.REL, new Point(0, 0));
        result.add(label1, CompositeView.REL, new Point(0, 0));
        result.add(label2, CompositeView.REL, new Point(0, 0));

        return result;
    }

    public static double distance(Point p1, Point p2)
    {
        double diffX = p1.x - p2.x;
        double diffY = p1.y - p2.y;
        return ( Math.sqrt(diffX * diffX + diffY * diffY) );
    }

    //finds Point p3 on the line (p1,p2) for which is true: distance(p2,p3) = distance
    public static Point shift(Point p1, Point p2, double distance, boolean fromStart)
    {
        Point p3 = new Point(p2.x, p1.y);

        double r = distance(p1, p2);
        double hX = distance(p1, p3);
        double hY = distance(p2, p3);

        double alpha = distance / r;

        double shiftX = alpha * hX;
        double shiftY = alpha * hY;

        double newX;
        double newY;

        if( fromStart )
        {
            newX = p1.x + Math.signum(p2.x - p1.x) * shiftX;
            newY = p1.y + Math.signum(p2.y - p1.y) * shiftY;
        }
        else
        {
            newX = p2.x + Math.signum(p2.x - p1.x) * shiftX;
            newY = p2.y + Math.signum(p2.y - p1.y) * shiftY;
        }


        return new Point((int)newX, (int)newY);
    }

    public static Point center(Point p1, Point p2)
    {
        Point result = new Point();
        result.x = (int) ( ( (double)p1.x + (double)p2.x ) / 2 );
        result.y = (int) ( ( (double)p1.y + (double)p2.y ) / 2 );
        return result;
    }

    public static Point shift(Point p1, Point p2, double sinFi, double distance)
    {

        double cosFi = Math.sqrt(1 - sinFi * sinFi);
        double x = ( p2.x - p1.x ) * cosFi - ( p2.y - p1.y ) * sinFi;
        double y = ( p2.x - p1.x ) * sinFi + ( p2.y - p1.y ) * cosFi;
        double t = distance / Math.sqrt(x * x + y * y);

        int newX = (int) ( p1.x + x * t );
        int newY = (int) ( p1.y + y * t );
        return new Point(newX, newY);
    }

    public static Point rotate(Point p1, Point p2, double sinFi)
    {
        double cosFi = Math.sqrt(1 - sinFi * sinFi);
        double x = p1.x + ( p2.x - p1.x ) * cosFi - ( p2.y - p1.y ) * sinFi;
        double y = p1.y + ( p2.x - p1.x ) * sinFi + ( p2.y - p1.y ) * cosFi;
        return new Point((int)x, (int)y);
    }

    //ax^2+bx+c=0 solving
    public static double[] solve(double a, double b, double c)
    {
        double[] result = new double[2];
        if( a == 0 )
        {
            result[0] = -c / b;
            result[1] = -c / b;
            return result;
        }

        double d = b * b - 4 * a * c;
        if( d < 0 )
        {
            return null;
        }

        result[0] = ( -b + Math.sqrt(d) ) / ( 2 * a );
        result[1] = ( -b - Math.sqrt(d) ) / ( 2 * a );

        return result;
    }

    public static Point getInCenter(Point p1, Point p2, Point p3)
    {
        double a = distance(p2, p3);
        double b = distance(p1, p2);
        double c = distance(p1, p3);

        Point p4 = shift(p2, p3, a * b / ( b + c ), true);

        double bissectr = Math.sqrt(b * c * ( 1 - Math.pow(a / ( b + c ), 2) ));
        return shift(p1, p4, bissectr * ( b + c ) / ( a + b + c ), true);
    }

}

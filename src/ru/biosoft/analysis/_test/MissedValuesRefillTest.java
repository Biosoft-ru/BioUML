package ru.biosoft.analysis._test;

import java.util.Iterator;
import java.util.Vector;

import org.jgraph.util.Spline;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

import ru.biosoft.access.core.Environment;
import ru.biosoft.access.security.BiosoftClassLoading;


public class MissedValuesRefillTest extends TestCase
{
    public MissedValuesRefillTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(MissedValuesRefillTest.class.getName());
        suite.addTest(new MissedValuesRefillTest("test"));
        return suite;
    }

    /**
     * @throws Exception
     */
    public void test() throws Exception
    {
        Environment.setClassLoading( new BiosoftClassLoading() );

        CollectionFactory.createRepository("../data_resources");

        TransformedDataCollection microarray = (TransformedDataCollection)CollectionFactory.getDataElement("data/microarray/");

        TableDataCollection target = (TableDataCollection)microarray.get("cellcycleCut");

        TableDataCollection result = new StandardTableDataCollection(microarray, "filledValues");

        double[][] v = TableDataCollectionUtils.getMatrix(target);

        for( TableColumn column : target.getColumnModel() )
        {
            result.getColumnModel().addColumn(column.getName(), column.getType());
        }


        String[] keys = target.names().toArray( String[]::new );

        double[] t = {0, 7, 14, 21, 28, 35, 42, 49, 56, 63, 70, 77, 84, 91, 98, 105, 112, 119};

        for( String key : keys )
        {
            Object[] row = TableDataCollectionUtils.getRowValues(target, key);

            Vector<Double> values = new Vector<>();
            Vector<Double> times = new Vector<>();

            for( int i = 0; i < row.length; i++ )
            {
                try
                {
                    String temp = row[i].toString();
                    double value = Double.parseDouble(temp);
                    if( Double.isNaN(value) )
                        continue;
                    values.add(value);
                    times.add(t[i]);
                }
                catch( Exception ex )
                {
                }
            }

            double[] f = new double[values.size()];
            double[] x = new double[times.size()];

            int i = 0;
            Iterator<Double> iter = times.iterator();
            while( iter.hasNext() )
            {
                x[i++] = iter.next();
            }

            iter = values.iterator();
            i = 0;
            while( iter.hasNext() )
            {
                f[i++] = iter.next();
            }

            Spline spline = new Spline(x, f);

            Object[] reconstructedRow = new Object[t.length];
            for( int j = 0; j < t.length; j++ )
            {
                reconstructedRow[j] = spline.getValue(t[j]);
            }

            TableDataCollectionUtils.addRow(result, key, reconstructedRow);
        }

        microarray.put(result);
    }
}

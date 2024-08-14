package biouml.plugins.modelreduction._test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.standard.diagram.Util;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

public class SplitTest extends AbstractBioUMLTest
{
    public Diagram getDiagram(String path, String name) throws Exception
    {
        CollectionFactory.createRepository("../data_resources");
        DataCollection collection = CollectionFactory.getDataCollection(path);
        Diagram d = (Diagram)collection.get(name);
        return d;
    }


        public void test() throws Exception
        {
            Diagram d = getDiagram("data/Collaboration/test/Data", "MM_v411");
    
            int[] limits = new int[] {11, 37, 42, 47, 52, 57, 62, 72, 87, 97, 109, 126, 137, 145, 153, 161, 173, 187, 198, 206, 214, 222, 226, 228};
            String[] name = new String[] {"Myeloid cells", "B cells", "Th0 cells", "Th1 cells", "Th2 cells", "Th17 cells", "Tregs", "CD8+ T cells", "NK", "DC", "IFNg", "TNFa",
                    "TGFb", "IL-2", "IL-4", "IL-5", "IL-6", "IL-10", "IL-12", "IL-13", "IL-17A", "IL-21", "CXCL12", "Specific Lysis"};
    
            int counter = 1;
            int seriesCounter = 0;
            List<String> series = new ArrayList<>();
            for( String rName : d.recursiveStream().select(Node.class).filter(n -> Util.isReaction(n)).map(r -> r.getName()).sorted(new ReactionComparator()) )
            {
                if( counter > limits[seriesCounter] )
                {
                    System.out.println("moduleName = \""+name[seriesCounter]+"\";");
                    System.out.println("species = [ "+StreamEx.of(series).map(n -> "\"" + n + "\"").joining(" , ")+" ];");
                    System.out.println("model.split(diagram, species, resultPath, moduleName);");
                    System.out.println();
                    seriesCounter++;
                    
                    if (seriesCounter == limits.length )
                        break;
                    
                    series.clear();
                }
                series.add(rName);
                counter++;
            }
        }

    private class ReactionComparator implements Comparator<String>
    {
        @Override
        public int compare(String o1, String o2)
        {
            int i1 = Integer.parseInt(o1.substring(1, o1.indexOf("_")));
            int i2 = Integer.parseInt(o2.substring(1, o2.indexOf("_")));
            return i1 > i2 ? 1 : i1 < i2 ? -1 : 0;
        }
    }

}

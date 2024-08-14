package biouml.plugins.simulation_test._test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.DiagramUtility;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class BioModelsUtil
{
    
    /**
     * Script automatically adds first 5 Species to Plot on diagrams on selected collection
     */
    public static void main(String...args ) throws Exception
    {
//        extract("C:/Users/Ilya/Desktop/BioModels_Database-r31_pub-all_files/BioModels_Database-r31_pub-all_files/curated", "C:/curated");
//        extract("C:/Users/Ilya/Desktop/BioModels_Database-r31_pub-all_files/BioModels_Database-r31_pub-all_files/non_curated", "C:/non_curated");
        
//        layout();
        
        
        addPlot("../data", "databases/SBML tests 3.3.0/l1v2");//"databases/Virtual Human/");
        addPlot("../data", "databases/SBML tests 3.3.0/l2v1");
        addPlot("../data", "databases/SBML tests 3.3.0/l2v2");
        addPlot("../data", "databases/SBML tests 3.3.0/l2v3");
        addPlot("../data", "databases/SBML tests 3.3.0/l2v4");
        addPlot("../data", "databases/SBML tests 3.3.0/l3v1");
        addPlot("../data", "databases/SBML tests 3.3.0/l3v2");
        
//        addPlot("../data", "databases/Biomodels/Diagrams");
    }
    
    public static void extract(String path, String resultPath) throws IOException
    {
        File dir = new File(path);
        File resultDir = new File(resultPath);
        resultDir.mkdirs();
        for( File innerDir : dir.listFiles() )
        {
            for( File f : innerDir.listFiles() )
            {
                if( f.getName().endsWith("_urn.xml") )
                {
                    File f2 = new File(resultDir, f.getName().replaceAll("_urn", ""));
                    f2.createNewFile();
                    ApplicationUtils.copyFile(f2, f);
                }
            }

        }
    }
    
    public static void layout() throws Exception
    {
        String collectionName = "SBML Tests 3.3.0";
        CollectionFactory.createRepository("../data");
//        DataCollection collection = CollectionFactory.getDataCollection("databases/SBML tests 3.3.0/l1v2");
        DataCollection collection2 = CollectionFactory.getDataCollection("databases/SBML tests 3.3.0/l1v2");
//        Diagram d = (Diagram)collection.get("00001-sbml-l1v2");
        System.out.println(collection2.getName());
//        d.save();
    }
    
    private Diagram getDiagram(String collectionPath, String name) throws Exception
    {
      
        DataCollection collection = CollectionFactory.getDataCollection(collectionPath);
        DataElement de = collection.get(name);
        return (Diagram)de;
    }
    
    public static void addPlot(String repositoryPath, String collectionPath) throws Exception
    {
        CollectionFactory.createRepository(repositoryPath);
        DataCollection<Diagram> collection = CollectionFactory.getDataCollection(collectionPath);
        
        
        Iterator<Diagram> iter = collection.iterator();
        while(iter.hasNext())
        {
            Object de = iter.next();
            if( de instanceof Diagram )
            {
                addPlot((Diagram)de);
                ( (Diagram)de ).save();
            }
        }
    }
    
    
    public static void addPlot(Diagram diagram) throws Exception
    {
        EModel emodel = diagram.getRole(EModel.class);
        PlotInfo plotInfo = new PlotInfo(emodel);
        List<Curve> curves = new ArrayList<>();
        int i=0;
        for( VariableRole var : emodel.getVariableRoles() )
        {
            if (var.isConstant() || var.isBoundaryCondition())
                continue;
            if (i > 4)
                break;
            curves.add(new Curve("", var.getName(), var.getTitle(), emodel));
            i++;
        }

        if( i < 4 )
        {
            for( Variable var : emodel.getVariables() )
            {
                if( var.isConstant() || var.getName().startsWith("$") || var.getName().equals("time"))
                    continue;
                if( i > 4 )
                    break;
                curves.add(new Curve("", var.getName(), var.getTitle(), emodel));
                i++;
            }
        }
        Curve[] curveArr = new Curve[curves.size()];
        curveArr = StreamEx.of(curves).toArray(Curve[]::new);
        plotInfo.setYVariables(curveArr);
        DiagramUtility.setPlotsInfo(diagram, new PlotsInfo(emodel, new PlotInfo[] {plotInfo}));
    }
}

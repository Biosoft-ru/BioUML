package biouml.plugins.pharm._test;

import java.io.File;
import java.util.List;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;

public class TestFitComplex extends AbstractBioUMLTest
{
    public TestFitComplex(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestFitComplex.class.getName());
        suite.addTest(new TestFitComplex("test"));
        return suite;
    }


    private Diagram getDiagram(String collectionName, String name) throws Exception
    {
        CollectionFactory.createRepository("../data_resources");
        DataCollection collection = CollectionFactory.getDataCollection(collectionName);
        DataElement de = collection.get(name);//"Hallow full plain");
        return (Diagram)de;
    }
    
    private Diagram getDiagram(DataElementPath path) throws Exception
    {
        String name = path.getName();
        String parentPath = path.getParentPath().toString();
        CollectionFactory.createRepository("../data_resources");
        DataCollection collection = CollectionFactory.getDataCollection(parentPath);
        DataElement de = collection.get(name);
        return (Diagram)de;
    }
    
    
    private DataCollection getCollection() throws Exception
    {
        CollectionFactory.createRepository("../data_resources");
        DataCollection collection = CollectionFactory.getDataCollection("data/Collaboration (git)/Cardiovascular system/Complex model/");
       
        return collection;
    }
    
    private DataCollection getCollection(String name) throws Exception
    {
        CollectionFactory.createRepository("../data_resources");
        DataCollection collection = CollectionFactory.getDataCollection(name);
       
        return collection;
    }

//    public void test() throws Exception
//    {
//        Diagram d = getDiagram("Complex model Hallow");
//        DataCollection collection = getCollection("data/Collaboration/Ilya/Data/Muscle/"); 
////                
//        CompositeModelPreprocessor  preprocessor = new CompositeModelPreprocessor();
////        
//        Diagram plain = preprocessor.preprocess(d, collection, d.getName()+" plain");
             
        
//        Diagram d = getDiagram("data/Collaboration/Ilya/Data/Muscle/", "aa");
//        CompositeModelPreprocessor  preprocessor = new CompositeModelPreprocessor();
//        Diagram plain = preprocessor.preprocess(d, collection, d.getName()+" plain");
        
//        Diagram plain = getDiagram("Complex model Hallow plain");
//        
//        String filePath = "C:/Users/Ilya/git/biouml/test/biouml.plugins.pharm/Results Hallow Population 2/One.txt";   
//        List<String> data = ApplicationUtils.readAsList(new File(filePath));  
//        String header = data.get(0);
//        String[] names = header.split("\t");
//        String[] vals = data.get(1).split("\t");        
//        for (int i =0; i<names.length; i++)
//        {
//            Variable variable = plain.getRole(EModel.class).getVariable(names[i]);
//            if (variable == null)
//                System.out.println("Parameter "+names[i]+" not found");
//            else 
//            variable.setInitialValue(Double.parseDouble(vals[i]));
//        }    
        
//        plain.save();
//    }
    
    
    public void test() throws Exception
    {
        DataElementPath path = DataElementPath.create("data/Collaboration (git)/Cardiovascular system/Complex model/Complex model new test");
        Diagram d = getDiagram(path);      
        
        assertNotNull(d);
        File f = new File("C:/Results_Abstract/1/Sel.txt");
        List<String> list = ApplicationUtils.readAsList(f);
        
        String[] header = list.get(0).split("\t");
        double[] values = StreamEx.of(list.get(1).split("\t")).mapToDouble(s->Double.parseDouble(s)).toArray();
        
        
        for (int i=0; i<header.length; i++)
        {
            
            String[] pathElements = header[i].contains("\\")? header[i].split("\\\\"): new String[] {header[i]};
            
            if (pathElements.length > 1)
            {
                SubDiagram subdiagram = (SubDiagram)d.get(pathElements[0]);
                String diagramPath = subdiagram.getDiagramPath();
                DataElementPath dep = DataElementPath.create(diagramPath);
                Diagram innerDiagram = getDiagram(dep);
                assertNotNull(innerDiagram);
                EModel emodel = innerDiagram.getRole(EModel.class);
                if( emodel.containsVariable(pathElements[1]) )
                {
                    if( !pathElements[1].contains("time") )
                        emodel.getVariable(pathElements[1]).setInitialValue(values[i]);
                }
                else 
                {
                    System.out.println("Variable "+pathElements[1]+" was not found");
                }
                innerDiagram.save();
            }
        }
            
       
    }
}

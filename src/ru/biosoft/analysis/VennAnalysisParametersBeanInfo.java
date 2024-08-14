package ru.biosoft.analysis;

import com.developmentontheedge.beans.editors.ColorEditor;

import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class VennAnalysisParametersBeanInfo extends BeanInfoEx2<VennAnalysisParameters>
{
    public VennAnalysisParametersBeanInfo()
    {
        super(VennAnalysisParameters.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        property( "table1Path" ).inputElement( TableDataCollection.class ).canBeNull().add();
        addExpert("table1Name");
        addExpert("circle1Color", ColorEditor.class);
        property( "table2Path" ).inputElement( TableDataCollection.class ).canBeNull().add();
        addExpert("table2Name");
        addExpert("circle2Color", ColorEditor.class);
        property( "table3Path" ).inputElement( TableDataCollection.class ).canBeNull().add();
        addExpert("table3Name");
        addExpert("circle3Color", ColorEditor.class);
        add("simple");
        property( "output" ).outputElement( FolderCollection.class ).auto( "$table1Path/parent$/Venn analysis" ).add();
    }
}

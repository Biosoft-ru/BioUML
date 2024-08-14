package ru.biosoft.analysis.diagram;

import biouml.model.Diagram;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;

public class DiagramAnnotationAnalysisParametersBeanInfo extends BeanInfoEx2<DiagramAnnotationAnalysisParameters>
{
    public DiagramAnnotationAnalysisParametersBeanInfo()
    {
        super(DiagramAnnotationAnalysisParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "inputDiagram" ).inputElement( Diagram.class ).add();
        property( "table" ).inputElement( TableDataCollection.class ).add();
        add(ColumnNameSelector.registerSelector( "column", beanClass, "table" ));
        add(OptionEx.makeAutoProperty( DataElementPathEditor.registerOutput( "outputDiagram", beanClass, Diagram.class ), "$inputDiagram$ annotated" ));
    }
}

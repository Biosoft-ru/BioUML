package biouml.plugins.bionetgen.diagram;

import ru.biosoft.graphics.editor.FontEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class BionetgenDiagramViewOptionsBeanInfo extends BeanInfoEx2<BionetgenDiagramViewOptions>
{
    public BionetgenDiagramViewOptionsBeanInfo()
    {
        super(BionetgenDiagramViewOptions.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("gridOptions");
        add("autoLayout");
        add("nodeTitleLimit");
        add( "moleculeTitleFont", FontEditor.class );
        add( "observableTitleFont", FontEditor.class );
        add( "moleculeTypeTitleFont", FontEditor.class );
        add( "moleculeComponentTitleFont", FontEditor.class );
        add("edgePen");
        add("speciesBrush");
        add("moleculeBrush");
        add("observableBrush");
        add("moleculeTypeBrush");
        add("edgeTipBrush");
    }
}
package biouml.plugins.physicell;

import java.beans.IntrospectionException;
import java.util.stream.Stream;

import ru.biosoft.util.bean.BeanInfoEx2;

public class PhysicellOptionsBeanInfo extends BeanInfoEx2<PhysicellOptions>
{
    public PhysicellOptionsBeanInfo()
    {
        super( PhysicellOptions.class );
    }

    @Override
    public void initProperties() throws IntrospectionException
    {
        add( "resultPath" );
        add( "finalTime" );
        add( "parallelDiffusion" );
        property( "cellUpdateType" ).tags( bean -> Stream.of( bean.getCellUpdateTypes() ) ).add();
        add( "diffusionDt" );
        add( "mechanicsDt" );
        add( "phenotypeDt" );
        add( "reportInterval" );
        add( "imageInterval" );
        add( "saveReport" );
        add("saveCellsText");
        add("saveCellsTable");
        add("saveDensity");
        add( "saveImage" );
        add( "saveGIF" );
        add( "saveVideo" );
        add( "useManualSeed" );
        add( "seed" );
        add("calculateGradient");
        add("trackInnerSubstrates");
        property("modelType").tags( PhysicellOptions.DEFAULT_MODEL, PhysicellOptions.COVID_MODEL ).add();
    }
}
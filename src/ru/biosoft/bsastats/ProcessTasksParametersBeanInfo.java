package ru.biosoft.bsastats;

import ru.biosoft.access.FileDataElement;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

/**
 * @author lan
 *
 */
public class ProcessTasksParametersBeanInfo extends BeanInfoEx2<ProcessTasksParameters>
{
    public ProcessTasksParametersBeanInfo()
    {
        super( ProcessTasksParameters.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("libraryType", LibraryTypeSelector.class);
        add("inputType", InputTypeSelector.class);

        //Single end fastq
        property( "singleEndFastq" ).inputElement( FileDataElement.class ).hidden( "isSingleEndFastqHidden" ).add();

        //Single end solid
        property( "singleEndCSFasta" ).inputElement( FileDataElement.class ).hidden( "isSingleEndCSFastaHidden" ).add();

        property( "singleEndQual" ).inputElement( FileDataElement.class ).hidden( "isSingleEndCSFastaHidden" ).add();

        //Paired end fastq
        property( "pairedEndFastqFirst" ).inputElement( FileDataElement.class ).hidden( "isPairedEndFastqHidden" ).add();

        property( "pairedEndFastqSecond" ).inputElement( FileDataElement.class ).hidden( "isPairedEndFastqHidden" ).add();

        //Paired end solid
        property( "pairedEndCSFastaFirst" ).inputElement( FileDataElement.class ).hidden( "isPairedEndCSFastaHidden" ).add();
        property( "pairedEndQualFirst" ).inputElement( FileDataElement.class ).hidden( "isPairedEndCSFastaHidden" ).add();
        property( "pairedEndCSFastaSecond" ).inputElement( FileDataElement.class ).hidden( "isPairedEndCSFastaHidden" ).add();
        property( "pairedEndQualSecond" ).inputElement( FileDataElement.class ).hidden( "isPairedEndCSFastaHidden" ).add();
        //

        addHidden("encoding", EncodingSelector.class, "isEncodingHidden");

        add("taskProcessorsDPS");

        //Outputs

        //single end fastq output
        property( "singleEndFastqOutput" ).outputElement( FileDataElement.class ).auto( "$singleEndFastq$ processed" )
                .hidden( "isSingleEndFastqHidden" ).add();

        //single end solid output
        property( "singleEndCSFastaOutput" ).outputElement( FileDataElement.class ).auto( "$singleEndCSFasta$ processed" )
                .hidden( "isSingleEndCSFastaHidden" ).add();

        property( "singleEndQualOutput" ).outputElement( FileDataElement.class ).auto( "$singleEndQual$ processed" )
                .hidden( "isSingleEndCSFastaHidden" ).add();

        //paired end fastq output
        property( "pairedEndFastqFirstOutput" ).outputElement( FileDataElement.class ).auto( "$pairedEndFastqFirst$ processed" )
                .hidden( "isPairedEndFastqHidden" ).add();

        property( "pairedEndFastqSecondOutput" ).outputElement( FileDataElement.class ).auto( "$pairedEndFastqFirst$ processed" )
                .hidden( "isPairedEndFastqHidden" ).add();

        //paired end solid output
        property( "pairedEndCSFastaFirstOutput" ).outputElement( FileDataElement.class ).auto( "$pairedEndCSFastaFirst$ processed" )
                .hidden( "isPairedEndCSFastaHidden" ).add();

        property( "pairedEndQualFirstOutput" ).outputElement( FileDataElement.class ).auto( "$pairedEndQualFirst$ processed" )
                .hidden( "isPairedEndCSFastaHidden" ).add();

        property( "pairedEndCSFastaSecondOutput" ).outputElement( FileDataElement.class ).auto( "$pairedEndCSFastaSecond$ processed" )
                .hidden( "isPairedEndCSFastaHidden" ).add();

        property( "pairedEndQualSecondOutput" ).outputElement( FileDataElement.class ).auto( "$pairedEndQualSecond$ processed" )
                .hidden( "isPairedEndCSFastaHidden" ).add();
    }

    public static class LibraryTypeSelector extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return ProcessTasksParameters.LIBRARY_TYPES;
        }
    }

    public static class InputTypeSelector extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return ProcessTasksParameters.INPUT_TYPES;
        }
    }
}

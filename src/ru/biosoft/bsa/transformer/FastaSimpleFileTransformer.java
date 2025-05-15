package ru.biosoft.bsa.transformer;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.file.FileTypePriority;
import ru.biosoft.access.core.PriorityTransformer;

public class FastaSimpleFileTransformer  extends AbstractFileTransformer<FastaSimpleSequenceCollection> implements PriorityTransformer{

	@Override
	public Class<? extends FastaSimpleSequenceCollection> getOutputType() {
		return FastaSimpleSequenceCollection.class;
	}

	@Override
	public int getInputPriority(Class<? extends DataElement> inputClass, DataElement output) {
		return 1;
	}

	@Override
	public int getOutputPriority(String name) {
		 if(name.endsWith( ".fa" ) || name.endsWith( ".fasta" ) || name.endsWith( ".fna" ))
	            return 2;
	        return 0;
	}

	@Override
	public FastaSimpleSequenceCollection load(File input, String name, DataCollection<FastaSimpleSequenceCollection> origin)
			throws Exception {
		return new FastaSimpleSequenceCollection(origin, name, input);
	}

	@Override
	public void save(File output, FastaSimpleSequenceCollection fasta) throws Exception {
		ApplicationUtils.linkOrCopyFile( output, fasta.getFile(), null );
	}

}

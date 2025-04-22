package ru.biosoft.bsa.transformer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.Index.IndexEntry;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.ErrorLetterPolicy;
import ru.biosoft.bsa.MapAsVector;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceCollection;
import ru.biosoft.bsa.SequenceFactory;

public class FastaSimpleSequenceCollection extends AbstractDataCollection<AnnotatedSequence> implements SequenceCollection {

	private File file;
	//seq name -> file offset
	private Map<String, IndexEntry> index = new LinkedHashMap<>();
	
	public FastaSimpleSequenceCollection(DataCollection<?> parent, Properties properties) throws IOException {
		super(parent, properties);
		String filePath = properties.getProperty(DataCollectionConfigConstants.FILE_PROPERTY);
		if(filePath == null)
			throw new IllegalArgumentException();
		file = new File(filePath);
		initIndex();
	}
	
	public FastaSimpleSequenceCollection(DataCollection<?> parent, String name, File file) throws IOException
	{
		this(parent, createProperties(name, file));
	}


	private static Properties createProperties(String name, File file) {
		Properties properties = new Properties();
		properties.put(DataCollectionConfigConstants.NAME_PROPERTY, name);
		properties.put(DataCollectionConfigConstants.FILE_PROPERTY, file.getAbsolutePath());
		return properties;
	}
	
	public File getFile()
	{
		return file;
	}

	public void initIndex() throws IOException, FileNotFoundException {
		try(BufferedInputStream is = new BufferedInputStream(new FileInputStream(file)))
		{
			int c = is.read();
			long pos = 1;
			byte[] nameBuffer = new byte[1024];
			while(c != -1)
			{
				if(c!='>')
					throw new IOException();
				int iNameBuffer = 0;
				while((c=is.read()) != '\n')
				{
					if(c < 0 || c > 255)
						throw new IOException();
					nameBuffer[iNameBuffer++] = (byte)c;
					pos++;
				}
				String name = new String(nameBuffer, StandardCharsets.UTF_8);
				long offset = pos;
				long length = 0;
				
				while((c=is.read()) != -1)
				{
					length++;
					if(c == '>')
						break;
				}
				pos += length;
				index.put(name, new IndexEntry(offset, length));	
			}
		}
	}
	
	
	
	@Override
	public List<String> getNameList() {
		return new ArrayList<>(index.keySet());
	}
	
	@Override
	protected AnnotatedSequence doGet(String name) throws Exception {
		IndexEntry e = index.get(name);
		if(e==null)
			return null;
		FileInputStream fis = new FileInputStream(file);
		fis.getChannel().position(e.from);
		Reader reader = new InputStreamReader(fis);
		Sequence seq =  SequenceFactory.createSequence(reader, 0, -1, Nucleotide15LetterAlphabet.getInstance(),
                ErrorLetterPolicy.REPLACE_BY_ANY, false);
		return new MapAsVector(name, this, seq, null);
	}

}

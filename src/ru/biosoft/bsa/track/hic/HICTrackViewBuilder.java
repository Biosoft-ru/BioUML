package ru.biosoft.bsa.track.hic;

import java.awt.Color;
import java.awt.Graphics;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

import ru.biosoft.javastraw.reader.Dataset;
import ru.biosoft.javastraw.reader.basics.Chromosome;
import ru.biosoft.javastraw.reader.block.Block;
import ru.biosoft.javastraw.reader.block.ContactRecord;
import ru.biosoft.javastraw.reader.mzd.Matrix;
import ru.biosoft.javastraw.reader.mzd.MatrixZoomData;
import ru.biosoft.javastraw.reader.type.HiCZoom;
import ru.biosoft.javastraw.reader.type.NormalizationType;
import ru.biosoft.javastraw.tools.HiCFileTools;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.view.MapJobControl;
import ru.biosoft.bsa.view.SequenceView;
import ru.biosoft.bsa.view.SiteViewOptions;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.PolygonView;

public class HICTrackViewBuilder extends TrackViewBuilder {

	private HICTrack track;
	private Dataset ds;
	
	public HICTrackViewBuilder(HICTrack track)
	{
		this.track = track;
		boolean useCache = true;
		try {
			ds = HiCFileTools.extractDatasetForCLT(track.getFilePath(), false, useCache, false);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
	public HICViewOptions createViewOptions() {
		HICViewOptions options = new HICViewOptions();
		
		List<HiCZoom> zooms = ds.getAllPossibleResolutions();
		options.allZooms = new String[zooms.size()];
		for(int i = 0; i < zooms.size(); i++)
			options.allZooms[i] = zooms.get(i).toString();
		options.setZoom(options.allZooms[0]);
		
		options.allNormalizations = new LinkedHashSet<String>( ds.getNormalizationTypesMap().keySet() );
		options.setNormalization(options.allNormalizations.stream().findFirst().orElse(null));
		
		return options;
	}
	    
	 
	@Override
	public CompositeView createTrackView(SequenceView sequenceView, DataCollection<Site> sites,
			SiteViewOptions siteViewOptions, int start, int end, int direction, Graphics graphics,
			MapJobControl control) {
		
		HICViewOptions viewOptions = (HICViewOptions) siteViewOptions;
		CompositeView view = new CompositeView();
		
		String zoomStr = viewOptions.getZoom();
		HiCZoom zoom = null;
		for(HiCZoom cur : ds.getAllPossibleResolutions() )
			if(zoomStr.equals(cur.toString()))
				zoom = cur;
		if(zoom == null)
			return view;
		int binSize = zoom.getBinSize();
		
		String normStr = viewOptions.getNormalization();
		if(normStr == null)
			return view;
        NormalizationType norm = ds.getNormalizationTypesMap().get(normStr);

        String chrName = sequenceView.getSequence().getName();
        chrName = track.externalToInternalName(chrName);
        Chromosome chr = ds.getChromosomeHandler().getChromosomeFromName(chrName);//will trim chr prefix sometimes!!! TODO: fix it in javastraw library
        
        Matrix matrix = ds.getMatrix(chr, chr);
        if (matrix == null) return view;
        MatrixZoomData zd = matrix.getZoomData(zoom);
        if (zd == null) return view;
        
        
        double tan = 2 * viewOptions.getImageHeight() / ((double)sequenceView.getEndPoint(end-(start-1), graphics).x - sequenceView.getStartPoint(1, graphics).x);
        // our bounds will be binXStart, binYStart, binXEnd, binYEnd
        // these are in BIN coordinates, not genome coordinates
        int binXStart = start/binSize, binXEnd = end/binSize;
		boolean getDataUnderTheDiagonal = false;
		List<Block> blocks = zd.getNormalizedBlocksOverlapping(binXStart, binXStart, binXEnd, binXEnd, norm,
				getDataUnderTheDiagonal);
		for (Block b : blocks) {
			if (b != null) {
				for (ContactRecord rec : b.getContactRecords()) {
					if (rec.getCounts() > 0) { // will skip NaNs
						// can choose to use the BIN coordinates
						int binX = rec.getBinX();
						int binY = rec.getBinY();
						if(binX < binXStart || binX > binXEnd || binY < binXStart || binY > binXEnd)
							continue;
						int genomeX = binX*binSize;
						int genomeY = binY*binSize;
						int genomeX1px = sequenceView.getStartPoint(genomeX-(start-1), graphics).x;
						int genomeX2px = sequenceView.getStartPoint(genomeX+binSize-(start-1), graphics).x;
						int genomeY1px = sequenceView.getStartPoint(genomeY-(start-1), graphics).x;
						int genomeY2px = sequenceView.getStartPoint(genomeY+binSize-(start-1), graphics).x;
						
						double p1x = (genomeX1px + genomeY1px)/2d;
						double p1y = (genomeY1px - genomeX1px)*tan/2d;
						double p2x = (genomeX1px + genomeY2px)/2d;
						double p2y = (genomeY2px - genomeX1px)*tan/2d;
						double p3x = (genomeX2px + genomeY2px)/2d;
						double p3y = (genomeY2px-genomeX2px)*tan/2d;
						double p4x = (genomeX2px + genomeY1px)/2d;
						double p4y = (genomeY1px-genomeX2px)*tan/2d;
						
						int alpha = (int) (rec.getCounts()*255/viewOptions.getMaxValue());
						if(alpha > 255)
							alpha = 255;
						Color color = new Color(255,0,0,alpha);
						Pen pen = new Pen(1, color);
						Brush brush = new Brush(color);
						PolygonView polygon = new PolygonView(pen, brush);
						polygon.addPoint((int)Math.round(p1x), (int)Math.round(p1y));
						polygon.addPoint((int)Math.round(p2x), (int)Math.round(p2y));
						polygon.addPoint((int)Math.round(p3x), (int)Math.round(p3y));
						polygon.addPoint((int)Math.round(p4x), (int)Math.round(p4y));
						view.add(polygon);
					}
				}
			}
		}
		
		return view;
	}
}

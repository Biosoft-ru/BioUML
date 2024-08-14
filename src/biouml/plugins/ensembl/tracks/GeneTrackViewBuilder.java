package biouml.plugins.ensembl.tracks;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Paint;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.view.MapJobControl;
import ru.biosoft.bsa.view.SequenceView;
import ru.biosoft.bsa.view.SiteViewOptions;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.util.TextUtil;
import biouml.plugins.ensembl.analysis.SiteData.Location;

/**
 * @author lan
 *
 */
public class GeneTrackViewBuilder extends TrackViewBuilder
{
    private static class Feature
    {
        int start;
        int end;
        Site gene;
        Location what;
        int anchor;

        public Feature(int start, int end, Location what, Site gene, int anchor)
        {
            super();
            this.start = start;
            this.end = end;
            this.gene = gene;
            this.what = what;
            this.anchor = anchor;
        }
        
        public Feature shrinkLeft(int newStart)
        {
            if(newStart >= end) return null;
            if(newStart <= start) return this;
            return new Feature(newStart, end, what, gene, anchor);
        }

        public Feature shrinkRight(int newEnd)
        {
            if(newEnd <= start) return null;
            if(newEnd >= end) return this;
            return new Feature(start, newEnd, what, gene, anchor);
        }
        
        @Override
        public String toString()
        {
            return gene.getProperties().getValue("symbol")+":"+what+"("+start+","+end+")";
        }
    }
    
    @SuppressWarnings ( "serial" )
    private static class FeatureMap extends TreeMap<Integer, Feature>
    {
        void put(Feature feature)
        {
            if(feature != null)
                put(feature.start, feature);
        }
    }
    
    @Override
    public CompositeView doCreateTrackView(SequenceView sequenceView, DataCollection<Site> sites, SiteViewOptions siteViewOptions,
            int start, int end, int direction, Graphics graphics, MapJobControl control, int size)
    {
        FeatureMap features = new FeatureMap();
        for(Site site: sites)
        {
            updateFeatures(site, start, end, sequenceView, graphics, features);
        }
        CompositeView result = new CompositeView();
        Pen pen = new Pen(1, Color.BLACK);
        int height = graphics.getFontMetrics(siteViewOptions.getFont().getFont()).getHeight()+2;
        int pixelStep = 100;
        for(Feature feature: features.values())
        {
            Site gene = feature.gene;
            Brush brush = siteViewOptions.getColorScheme().getBrush(gene);
            int fivePrime = 0;
            int threePrime = 0;
            int geneFrom = gene.getStrand() == StrandType.STRAND_MINUS?gene.getTo()-fivePrime:gene.getFrom()+fivePrime;
            int geneTo = gene.getStrand() == StrandType.STRAND_MINUS?gene.getFrom()+threePrime:gene.getTo()-threePrime;
            int featureFrom = sequenceView.getPosition(feature.start, graphics) + start - 1;
            int featureTo = sequenceView.getPosition(feature.end, graphics) + start - 1;
            int sign = (gene.getStrand() == StrandType.STRAND_MINUS ^ feature.what == Location.FIVE_PRIME)?-1:1;
            if(feature.what == Location.FIVE_PRIME || feature.what == Location.THREE_PRIME)
            {
                CompositeView view = new CompositeView();
                view.setActive(true);
                view.setModel(feature.gene);
                //view.add(new LineView(pen, feature.start, height/2, feature.end-1, height/2));
                if(feature.end - feature.start > pixelStep)
                {
                    String prefix = "3'+";
                    int anchor = geneTo;
                    if(feature.what == Location.FIVE_PRIME)
                    {
                        prefix = "5'-";
                        anchor = geneFrom;
                    }
                    int coordFrom = Math.abs(featureFrom-anchor);
                    int coordTo = Math.abs(featureTo-anchor);
                    if(coordTo < coordFrom)
                    {
                        int tmp = coordTo;
                        coordTo = coordFrom;
                        coordFrom = tmp;
                    }
                    double step = roundStep((coordTo-coordFrom)*(double)pixelStep/(feature.end-feature.start));
                    for(int tickPos = (int)(Math.ceil(coordFrom/step)*step); tickPos < coordTo; tickPos += (int)step)
                    {
                        Point pt = sequenceView.getStartPoint(( tickPos*sign+anchor-start+1 ), graphics);
                        view.add(new LineView(pen, pt.x, height/4f, pt.x, 3*height/4f));
                        TextView textView = new TextView(prefix+tickPos, new Point(pt.x+2, height), TextView.LEFT|TextView.BOTTOM, siteViewOptions.getFont(), graphics);
                        if(textView.getBounds().width+10 < pixelStep && textView.getBounds().getMaxX() < feature.end)
                            view.add(textView);
                    }
                }
                result.add(view);
            } else if(feature.what == Location.INTRON)
            {
                CompositeView view = new CompositeView();
                Object symbolObj = feature.gene.getProperties().getValue("symbol");
                String symbol = symbolObj == null ? feature.gene.getType() : symbolObj.toString();
                symbol = "..."+symbol+"...";
                TextView textView = new TextView(symbol, new Point((feature.start+feature.end)/2, height), TextView.CENTER|TextView.BOTTOM, siteViewOptions.getFont(), graphics);
                if(textView.getBounds().width*3 < feature.end - feature.start)
                {
                    view.add(new LineView(pen, feature.start, height/2.0f, (float) ( textView.getBounds().getMinX()-1 ), height/2.0f));
                    view.add(new LineView(pen, feature.end-1, height/2.0f, (float) ( textView.getBounds().getMaxX()+1 ), height/2.0f));
                    view.add(textView);
                } else
                {
                    view.add(new LineView(pen, feature.start, height/2.0f, feature.end-1, height/2.0f));
                }
                view.setActive(true);
                view.setModel(feature.gene);
                result.add(view);
            } else
            {
                CompositeView view = new CompositeView();
                view.setActive(true);
                view.setModel(feature.gene);
                view.add(new BoxView(null, brush, feature.start, 0, feature.end - feature.start, height));
                Object symbolObj = feature.gene.getProperties().getValue("symbol");
                String symbol = symbolObj == null ? feature.gene.getType() : symbolObj.toString();
                ColorFont colorFont = new ColorFont(siteViewOptions.getFont().getFont(), isBright(brush.getPaint())?Color.BLACK:Color.WHITE);
                TextView textView = new TextView(symbol, new Point(feature.start, height), TextView.LEFT|TextView.BOTTOM, colorFont, graphics);
                if(textView.getBounds().width < view.getBounds().width)
                {
                    view.add(textView);
/*                    int newStart = (int)(textView.getBounds().getMaxX()+10);
                    featureFrom = sequenceView.getPosition(newStart, graphics) + start - 1;
                    if(feature.end - newStart > pixelStep)
                    {
                        int coordFrom = Math.abs(featureFrom-geneFrom);
                        int coordTo = Math.abs(featureTo-geneFrom);
                        if(coordTo < coordFrom)
                        {
                            int tmp = coordTo;
                            coordTo = coordFrom;
                            coordFrom = tmp;
                        }
                        double step = roundStep((coordTo-coordFrom)*(double)pixelStep/(feature.end-feature.start));
                        for(int tickPos = (int)(Math.ceil(coordFrom/step)*step); tickPos < coordTo; tickPos += (int)step)
                        {
                            Point pt = sequenceView.getStartPoint((int) ( tickPos*sign+geneFrom-start+1 ), graphics);
                            view.add(new LineView(pen, pt.x, height/4, pt.x, 3*height/4));
                            textView = new TextView("+"+tickPos, new Point(pt.x+2, height), TextView.LEFT|TextView.BOTTOM, siteViewOptions.getFont(), graphics);
                            if(textView.getBounds().width+10 < pixelStep && textView.getBounds().getMaxX() < feature.end)
                                view.add(textView);
                        }
                    }*/
                }
                result.add(view);
            }
        }
        return result;
    }

    /**
     * @param paint
     * @return true if color is bright enough to write with white black pen over it
     */
    private boolean isBright(Paint paint)
    {
        Color color = null;
        if(paint instanceof Color) color = (Color)paint;
        else if(paint instanceof GradientPaint) color = ((GradientPaint)paint).getColor1();
        if(color == null) return true;
        return color.getRed()*0.299+color.getGreen()*0.587+color.getBlue()*0.114 > 127;
    }

    /**
     * @param step
     * @return
     */
    private double roundStep(double step)
    {
        int power = 1;
        while(step > 10)
        {
            step /= 10;
            power *= 10;
        }
        if(step > 5) step = 10*power;
        else if(step > 2) step = 5*power;
        else step = 2*power;
        return step;
    }

    private void updateFeatures(Site site, int start, int end, SequenceView sequenceView, Graphics graphics, FeatureMap features)
    {
        int startX = site.getFrom();
        int endX = site.getTo();
        if( startX > end || endX < start )
        {
            return;
        }
        int fivePrime = 0;
        int threePrime = 0;
        int geneStartX = site.getStrand() == StrandType.STRAND_MINUS?startX+threePrime:startX+fivePrime;
        int geneEndX = site.getStrand() == StrandType.STRAND_MINUS?endX-fivePrime:endX-threePrime;
        int geneFrom = site.getStrand() == StrandType.STRAND_MINUS?geneEndX:geneStartX;
        int geneTo = site.getStrand() == StrandType.STRAND_MINUS?geneStartX:geneEndX;
        if( geneStartX < start ) geneStartX = start;
        if( geneStartX > end ) geneStartX = end;
        if( geneEndX > end ) geneEndX = end;
        if( geneEndX < start ) geneEndX = start;
        if( startX < start ) startX = start;
        if( endX > end ) endX = end;
        // Beginning drawing sites from 1
        startX -= start - 1;
        endX -= start - 1;
        geneStartX -= start - 1;
        geneEndX -= start - 1;
        geneFrom -= start - 1;
        geneTo -= start - 1;

        Point startPoint = sequenceView.getStartPoint(startX, graphics);
        Point endPoint = sequenceView.getEndPoint(endX, graphics);
        Point geneStartPoint = sequenceView.getStartPoint(geneStartX, graphics);
        Point geneEndPoint = sequenceView.getEndPoint(geneEndX, graphics);
        if(geneEndPoint.x == geneStartPoint.x && geneEndX != geneStartX) geneEndPoint.x++;
        if(endPoint.x < geneEndPoint.x) endPoint.x = geneEndPoint.x;
        Point geneFromPoint = sequenceView.getStartPoint(geneFrom, graphics);
        Point geneToPoint = sequenceView.getStartPoint(geneTo, graphics);
        if(geneStartPoint.x > startPoint.x)
        {
            Location location = site.getStrand() == StrandType.STRAND_MINUS ? Location.THREE_PRIME : Location.FIVE_PRIME;
            Feature feature = new Feature(startPoint.x, geneStartPoint.x, location, site, location == Location.THREE_PRIME ? geneToPoint.x
                    : geneFromPoint.x);
            while(true)
            {
                SortedMap<Integer, Feature> headMap = features.headMap(geneStartPoint.x);
                Feature lastFeature = headMap.isEmpty()?null:headMap.get(headMap.lastKey());
                if(lastFeature != null && lastFeature.end > feature.start)
                {
                    if(lastFeature.what == Location.GENE || lastFeature.what == Location.INTRON || lastFeature.what == Location.EXON)
                    {
                        feature = feature.shrinkLeft(lastFeature.end);
                    } else
                    {
                        if(lastFeature.anchor >= lastFeature.end)
                        {
                            if(lastFeature.anchor < feature.anchor)
                                feature = null;
                            else
                            {
                                features.remove(lastFeature.start);
                                features.put(lastFeature.shrinkLeft(feature.anchor));
                                continue;
                            }
                        } else
                        {
                            int middlePoint = ( feature.anchor + lastFeature.anchor ) / 2;
                            if(middlePoint < feature.start) middlePoint = feature.start;
                            feature = feature.shrinkLeft(middlePoint);
                            features.remove(lastFeature.start);
                            features.put(lastFeature.shrinkRight(middlePoint));
                        }
                    }
                }
                break;
            }
            features.put(feature);
        }
        if(endPoint.x > geneEndPoint.x)
        {
            Location location = site.getStrand() == StrandType.STRAND_MINUS ? Location.FIVE_PRIME : Location.THREE_PRIME;
            Feature feature = new Feature(geneEndPoint.x, endPoint.x, location, site, location == Location.THREE_PRIME ? geneToPoint.x
                    : geneFromPoint.x);
            while(true)
            {
                SortedMap<Integer, Feature> tailMap = features.tailMap(geneEndPoint.x);
                SortedMap<Integer, Feature> headMap = features.headMap(geneEndPoint.x);
                Feature nextFeature = null;
                if(!headMap.isEmpty())
                {
                    nextFeature = headMap.get(headMap.lastKey());
                    if(nextFeature.end <= geneEndPoint.x) nextFeature = null;
                }
                if(nextFeature == null && !tailMap.isEmpty()) nextFeature = tailMap.get(tailMap.firstKey());
                if(nextFeature != null && nextFeature.start < feature.end)
                {
                    if(nextFeature.what == Location.INTRON || nextFeature.what == Location.EXON)
                    {
                        feature = feature.shrinkRight(nextFeature.start);
                    } else
                    {
                        if(nextFeature.anchor <= nextFeature.start)
                        {
                            if(nextFeature.anchor > feature.anchor)
                                feature = null;
                            else
                            {
                                features.remove(nextFeature.start);
                                features.put(nextFeature.shrinkRight(feature.anchor));
                                continue;
                            }
                        } else
                        {
                            int middlePoint = ( feature.anchor + nextFeature.anchor ) / 2;
                            if(middlePoint > feature.end) middlePoint = feature.end;
                            feature = feature.shrinkRight(middlePoint);
                            features.remove(nextFeature.start);
                            features.put(nextFeature.shrinkLeft(middlePoint));
                        }
                    }
                }
                break;
            }
            features.put(feature);
        }
        if(geneEndPoint.x > geneStartPoint.x)
        {
            List<Feature> exons = new ArrayList<>();
            if(geneEndPoint.x - geneStartPoint.x > 2)
            {
                for( String blockStr : TextUtil.split( site.getProperties().getProperty("exons").getValue().toString(), ';' ) )
                {
                    Interval interval;
                    try
                    {
                        interval = new Interval(blockStr);
                    }
                    catch( IllegalArgumentException e )
                    {
                        continue;
                    }
                    int from = interval.getFrom();
                    int to = interval.getTo();
                    int startBlock, endBlock;
                    Point startBlockPoint, endBlockPoint;
                    if( site.getStrand() == Site.STRAND_MINUS )
                    {
                        startBlock = site.getStart() - to;
                        endBlock = site.getStart() - from;
                    }
                    else
                    {
                        startBlock = site.getStart() + from;
                        endBlock = site.getStart() + to;
                    }
                    if( endBlock < start || startBlock > end )
                        continue;
                    if( startBlock < start )
                        startBlock = start;
                    if( endBlock > end )
                        endBlock = end;
                    startBlockPoint = sequenceView.getStartPoint(startBlock - start + 1, graphics);
                    endBlockPoint = sequenceView.getEndPoint(endBlock - start + 1, graphics);
                    if( startBlockPoint.x >= endBlockPoint.x )
                    {
                        endBlockPoint.x = startBlockPoint.x + 1;
                    }
                    Feature exon = new Feature(startBlockPoint.x, endBlockPoint.x, Location.EXON, site, geneFromPoint.x);
                    if(!exons.isEmpty())
                    {
                        Feature lastExon = exons.get(exons.size()-1);
                        if(lastExon.end == exon.start)
                        {
                            exons.remove(exons.size()-1);
                            exon = new Feature(lastExon.start, endBlockPoint.x, Location.EXON, site, geneFromPoint.x);
                        } else if(lastExon.start == exon.end)
                        {
                            exons.remove(exons.size()-1);
                            exon = new Feature(startBlockPoint.x, lastExon.end, Location.EXON, site, geneFromPoint.x);
                        } else if(lastExon.start == exon.start)
                        {
                            exons.remove(exons.size()-1);
                            exon = new Feature(lastExon.start, Math.max(lastExon.end, endBlockPoint.x), Location.EXON, site, geneFromPoint.x);
                        }
                    }
                    exons.add(exon);
                }
                Feature feature = new Feature(geneStartPoint.x, geneEndPoint.x, Location.INTRON, site, geneFromPoint.x);
                Feature rightFeature = feature;
                SortedMap<Integer, Feature> subMap = features.subMap(feature.start, feature.end);
                if(!subMap.isEmpty())
                {
                    int lastKey = subMap.lastKey();
                    for(int key: subMap.keySet().toArray(new Integer[subMap.size()]))
                    {
                        Feature inFeature = features.get(key);
                        if(inFeature.what == Location.EXON)
                        {
                            if(rightFeature != null)
                            {
                                Feature leftFeature = rightFeature.shrinkRight(inFeature.start);
                                if(feature != null && rightFeature.start == feature.start) feature = leftFeature;
                                else features.put(leftFeature);
                                rightFeature = rightFeature.shrinkLeft(inFeature.end);
                            }
                        } else
                        {
                            features.remove(inFeature.start);
                            if(key == lastKey)
                            {
                                features.put(inFeature.shrinkLeft(geneEndPoint.x));
                            }
                        }
                    }
                }
                features.put(rightFeature);
                if(feature != null)
                {
                    SortedMap<Integer, Feature> headMap = features.headMap(feature.start);
                    if(!headMap.isEmpty())
                    {
                        Feature removedFeature = features.remove( headMap.lastKey() );
                        if( ( removedFeature.what == Location.EXON || removedFeature.what == Location.INTRON )
                                && removedFeature.end > feature.end )
                        {
                            feature = null;
                            features.put(removedFeature);
                        } else
                        {
                            features.put(removedFeature.shrinkRight(feature.start));
                        }
                    }
                }
                features.put(feature);
            } else
            {
                exons.add(new Feature(geneStartPoint.x, geneEndPoint.x, Location.EXON, site, geneFromPoint.x));
            }
            for(Feature feature : exons)
            {
                SortedMap<Integer, Feature> subMap = features.subMap(feature.start, feature.end);
                if(!subMap.isEmpty())
                {
                    int lastKey = subMap.lastKey();
                    for(int key: subMap.keySet().toArray(new Integer[subMap.size()]))
                    {
                        Feature removedFeature = features.remove(key);
                        if(key == lastKey)
                        {
                            features.put(removedFeature.shrinkLeft(feature.end));
                        }
                    }
                }
                SortedMap<Integer, Feature> headMap = features.headMap(feature.start);
                if(!headMap.isEmpty())
                {
                    Feature removedFeature = features.remove( headMap.lastKey() );
                    if((removedFeature.what == Location.EXON || removedFeature.what == Location.GENE) && removedFeature.end > feature.end)
                    {
                        feature = null;
                        features.put(removedFeature);
                    } else if(removedFeature.what == Location.INTRON)
                    {
                        features.put(removedFeature.shrinkLeft(feature.end));
                        features.put(removedFeature.shrinkRight(feature.start));
                    } else
                    {
                        features.put(removedFeature.shrinkRight(feature.start));
                    }
                }
                features.put(feature);
            }
        }
    }
}

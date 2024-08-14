package ru.biosoft.bsa.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.BAMTrack.SitesCollection.BAMSite;
import ru.biosoft.bsa.LimitedSizeSitesCollection;
import ru.biosoft.bsa.PileupElement;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.view.colorscheme.AbstractSiteColorScheme;
import ru.biosoft.bsa.view.colorscheme.ConstantColorScheme;
import ru.biosoft.bsa.view.colorscheme.TagBasedColorScheme;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.PolylineView;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.font.ColorFont;

public class BamTrackViewBuilder extends TrackViewBuilder
{
    public static final int SITE_COUNT_LIMIT = 5000;
    
    @SuppressWarnings ( "serial" )
    public static class BamTrackViewOptions extends SiteViewOptions
    {
        private boolean showAlignment = true;
        private boolean showPhredQual = true;
        private boolean profileView = false;
        private Color phredQualColor = Color.LIGHT_GRAY;
        private boolean showContig = false;
        private boolean showOnlyMismatchesInContig = true;
        private boolean showPassingThroughIntrons = true;
        
        
        private static AbstractSiteColorScheme BAM_SITE_COLOR_SCHEME;
        private static ViewTagger BAM_SITE_TAGGER;
        static {
            ViewTagger tagger = model -> {
                if(! (model instanceof BAMSite) )
                    return null;
                BAMSite site = (BAMSite)model;
                if(!site.isPaired())
                    return "not paired";
                return site.isFirstOfPair() ? "first of pair" : "second of pair";
            };
            Map<String, Brush> tagColors = new HashMap<>();
            tagColors.put( "not paired", new Brush( new Color( 0, 191, 95 ) ));
            tagColors.put( "first of pair", new Brush( new Color( 255, 86, 86 ) ));
            tagColors.put( "second of pair", new Brush( new Color( 86, 170, 255 ) ));
            BAM_SITE_COLOR_SCHEME = new TagBasedColorScheme( new ConstantColorScheme(Color.GREEN), tagger, tagColors );
        }
        
        public BamTrackViewOptions()
        {
            schemes = Collections.singletonMap( BAM_SITE_COLOR_SCHEME.getName(), BAM_SITE_COLOR_SCHEME );
            setColorScheme( BAM_SITE_COLOR_SCHEME );
            setViewTagger(BAM_SITE_TAGGER);
            setMinProfileHeight( 0 );
        }

        public boolean isShowAlignment()
        {
            return showAlignment;
        }
        
        public void setShowAlignment(boolean showAlignment)
        {
            boolean oldValue = this.showAlignment;
            this.showAlignment = showAlignment;
            firePropertyChange("showAlignment", oldValue, showAlignment);
        }
        
        public boolean isShowPhredQual()
        {
            return showPhredQual;
        }
        
        public void setShowPhredQual(boolean showPhredQual)
        {
            boolean oldValue = this.showPhredQual;
            this.showPhredQual = showPhredQual;
            firePropertyChange("showPhredQual", oldValue, showPhredQual);
        }

        public boolean isProfileView()
        {
            return profileView;
        }

        public void setProfileView(boolean profileView)
        {
            Object oldValue = this.profileView;
            this.profileView = profileView;
            firePropertyChange("profileView", oldValue, profileView);
        }

        public Color getPhredQualColor()
        {
            return phredQualColor;
        }

        public void setPhredQualColor(Color phredQualColor)
        {
            Object oldValue = this.phredQualColor;
            this.phredQualColor = phredQualColor;
            firePropertyChange("phredQualColor", oldValue, phredQualColor);
        }

        public boolean isShowContig()
        {
            return showContig;
        }

        public void setShowContig(boolean showContig)
        {
            Object oldValue = this.showContig;
            this.showContig = showContig;
            firePropertyChange( "showContig", oldValue, showContig );
        }

        public boolean isShowOnlyMismatchesInContig()
        {
            return showOnlyMismatchesInContig;
        }

        public void setShowOnlyMismatchesInContig(boolean showOnlyMismatchesInContig)
        {
            Object oldValue = this.showOnlyMismatchesInContig;
            this.showOnlyMismatchesInContig = showOnlyMismatchesInContig;
            firePropertyChange( "showOnlyMismatchesInContig", oldValue, showOnlyMismatchesInContig );
        }

        public boolean isShowPassingThroughIntrons()
        {
            return showPassingThroughIntrons;
        }
        
        public void setShowPassingThroughIntrons(boolean value)
        {
            Object oldValue = this.showPassingThroughIntrons;
            this.showPassingThroughIntrons = value;
            firePropertyChange( "showPassingThroughIntrons", oldValue, showOnlyMismatchesInContig );
        }
    }
    
    public static class BamTrackViewOptionsBeanInfo extends SiteViewOptionsBeanInfo
    {
        public BamTrackViewOptionsBeanInfo()
        {
            super(BamTrackViewOptions.class);
        }
        
        @Override
        public void initProperties() throws Exception
        {
            //super.initProperties();
            add(new PropertyDescriptorEx("showAlignment", beanClass), getResourceString("PN_SITE_VIEW_OPTIONS_SHOW_ALIGNMENT"), getResourceString("PD_SITE_VIEW_OPTIONS_SHOW_ALIGNMENT"));
            add(new PropertyDescriptorEx("showPhredQual", beanClass), getResourceString("PN_SITE_VIEW_OPTIONS_SHOW_PHRED_QUAL"), getResourceString("PD_SITE_VIEW_OPTIONS_SHOW_PHRED_QUAL"));
            add(new PropertyDescriptorEx("phredQualColor", beanClass), getResourceString("PN_SITE_VIEW_OPTIONS_PHRED_QUAL_COLOR"), getResourceString("PD_SITE_VIEW_OPTIONS_PHRED_QUAL_COLOR"));
            add(new PropertyDescriptorEx("profileView", beanClass), getResourceString("PN_SITE_VIEW_OPTIONS_PROFILE_VIEW"), getResourceString("PD_SITE_VIEW_OPTIONS_PROFILE_VIEW"));
            add(new PropertyDescriptorEx("maxProfileHeight", beanClass), getResourceString("PN_SITE_VIEW_OPTIONS_MAX_PROFILE_HEIGHT"), getResourceString("PD_SITE_VIEW_OPTIONS_MAX_PROFILE_HEIGHT"));
            add(new PropertyDescriptorEx("showContig", beanClass), getResourceString("PN_SITE_VIEW_OPTIONS_SHOW_CONTIG"), getResourceString("PD_SITE_VIEW_OPTIONS_SHOW_CONTIG"));
            add(new PropertyDescriptorEx("showOnlyMismatchesInContig", beanClass), getResourceString("PN_SITE_VIEW_OPTIONS_SHOW_ONLY_MISMATCHES_IN_CONTIG"), getResourceString("PD_SITE_VIEW_OPTIONS_SHOW_ONLY_MISMATCHES_IN_CONTIG"));
            add(new PropertyDescriptorEx("showPassingThroughIntrons", beanClass), getResourceString("PN_SITE_VIEW_OPTIONS_SHOW_PASSING_THROUGH_INTRONS"), getResourceString("PD_SITE_VIEW_OPTIONS_SHOW_PASSING_THROUGH_INTRONS"));
            
            
            initColorSchemeProperties();
        }
    }
    
    @Override
    public SiteViewOptions createViewOptions()
    {
        return new BamTrackViewOptions();
    }
    
    @Override
    public CompositeView createTrackView(SequenceView sequenceView, DataCollection<Site> sites, SiteViewOptions siteViewOptions,
            int start, int end, int direction, Graphics graphics, MapJobControl control)
    {
        int size = ((LimitedSizeSitesCollection)sites).getSizeLimited(SITE_COUNT_HARD_LIMIT+1);
        boolean profileView = ((BamTrackViewOptions)siteViewOptions).isProfileView();
        if(size > SITE_COUNT_HARD_LIMIT)
        {
            CompositeView trackView = new CompositeView();
            TextView siteCountLabel = new TextView(">"+SITE_COUNT_HARD_LIMIT+" sites", siteViewOptions.getTrackTitleFont(), ApplicationUtils.getGraphics());
            trackView.add(siteCountLabel, CompositeView.X_CC,
                    new Point(sequenceView.getStartPoint( ( start + end ) / 2 - start + 1, graphics).x, 0));
            return trackView;
        }
        if(profileView || size>SITE_COUNT_LIMIT)
        {
            return createProfileView(sequenceView, sites, siteViewOptions, start, end, direction, graphics, control);
        }
        CompositeView result = new CompositeView();
        if(((BamTrackViewOptions)siteViewOptions).isShowContig() && sequenceView.getNucleotideWidth() >= 5)
            result.add( createContigView( sequenceView, sites, (BamTrackViewOptions)siteViewOptions, start, end, direction, graphics, control ), CompositeView.Y_BT);
        result.add( doCreateTrackView( sequenceView, sites, siteViewOptions, start, end, direction, graphics, control, size ), CompositeView.Y_BT );
        return result;
    }

    @Override
    protected View createSiteView(SequenceView sequenceView, Site site, SiteViewOptions siteViewOptions, int start, int end,
            Graphics graphics, int sitesCount)
    {
        BamTrackViewOptions bamViewOptions = ((BamTrackViewOptions)siteViewOptions);

        CompositeView result = new CompositeView();
        if( site == null || site.getFrom() > end || site.getTo() < start )
            return null;
        
        if( !bamViewOptions.isShowPassingThroughIntrons() && isPassingThroughIntron(site, start, end) )
            return null;

        
        int siteHeight = graphics.getFontMetrics(siteViewOptions.getSequenceFont().getFont()).getHeight();
        
        int siteStartLocation = sequenceView.getStartPoint(site.getFrom() - start + 1, graphics).x;
        siteStartLocation -= sequenceView.getNucleotideWidth() / 2;
        
        if(bamViewOptions.isShowPhredQual() && sequenceView.getNucleotideWidth() >= 3)
        {
            View qualView = createPhredQualView((BAMSite)site, (float)sequenceView.getNucleotideWidth(), siteHeight, ((BamTrackViewOptions)siteViewOptions).getPhredQualColor());
            qualView.move(siteStartLocation, 0);
            result.add(qualView);
        }

        if( bamViewOptions.isShowAlignment() && sequenceView.getNucleotideWidth() >= 5 )
        {
            View alignmentView = createAlignmentView((BAMSite)site, sequenceView.getSequence(), sequenceView.getNucleotideWidth(), siteHeight, siteViewOptions.getSequenceFont(), graphics);
            alignmentView.move(siteStartLocation, 0);
            result.add(alignmentView);
        }

        View baseView = createBaseView(sequenceView.getNucleotideWidth(), (BAMSite)site, (BamTrackViewOptions)siteViewOptions);
        baseView.move(siteStartLocation, 0);
        result.add(baseView);
        
        result.setModel(site);
        result.setActive(true);
        return result;
    }
    
    private boolean isPassingThroughIntron(Site site, int start, int end)
    {
        BAMSite bamSite = (BAMSite)site;
        Cigar cigar = bamSite.getCigar();
        int refPos = bamSite.getFrom();
        for( CigarElement e : cigar.getCigarElements() )
        {
            CigarOperator op = e.getOperator();
            
            if(op == CigarOperator.N && start >= refPos && end < refPos + e.getLength())
                return true;
            
            if(op.consumesReferenceBases())
                refPos += e.getLength();
        }
        return false;
    }

    private View createAlignmentView(BAMSite site, Sequence originalSequence, double nucleotideWidth, int nucleotideHeight, ColorFont font, Graphics graphics)
    {
        CompositeView result = new CompositeView();
        
        Sequence refSequence = new SequenceRegion(originalSequence, site.getFrom(), site.getLength(), false, false);
        Sequence readSequence = site.getReadSequence();
        
        Cigar cigar = site.getCigar();
        int readOffset = 0;
        int refOffset = 0;
        for( CigarElement e : cigar.getCigarElements() )
        {
            CigarOperator op = e.getOperator();
            switch( op )
            {
                case M:
                case X:
                case EQ:
                {
                    for( int i = 0; i < e.getLength(); i++ )
                    {
                        char readBase = Character.toUpperCase((char)readSequence.getLetterAt(i + readOffset + readSequence.getStart()));
                        char refBase = Character.toUpperCase((char)refSequence.getLetterAt(i + refOffset + refSequence.getStart()));
                        if( readBase != refBase )
                        {
                            TextView mismatchView = new TextView(new String(new char[] {readBase}), new Point((int) ( (refOffset + i)*nucleotideWidth ), 0), View.BOTTOM, font, graphics);
                            result.add(mismatchView);
                        }
                    }
                    break;
                }
                case I:
                {
                    int pos = (int) (refOffset * nucleotideWidth);
                    View insertionView = createInsertionView( pos, nucleotideHeight );
                    Sequence insertedSequence = new SequenceRegion(readSequence, readOffset + readSequence.getStart(), e.getLength(),
                            false, false);
                    insertionView.setDescription("Insertion " + insertedSequence.toString());
                    insertionView.setSelectable(true);
                    result.add(insertionView);
                    break;
                }
                case D:
                    for( int i = 0; i < e.getLength(); i++ )
                    {
                        int start = (int) ((refOffset + i + 0.25) * nucleotideWidth);
                        int end   = (int) ((refOffset + i + 0.75) * nucleotideWidth);
                        View gapView = new LineView(new Pen(1, Color.BLACK), start, -nucleotideHeight/2.0f, end, -nucleotideHeight/2.0f);
                        result.add(gapView);
                    }
                    break;
                default:
                    break;
            }
            if( op.consumesReadBases() )
                readOffset += e.getLength();
            if( op.consumesReferenceBases() )
                refOffset += e.getLength();
        }
        
        return result;
    }
    
    private static View createInsertionView(int screenPos, int siteHeight)
    {
        return new PolylineView( new Pen(1, Color.BLACK), new int[] {screenPos - 1, screenPos, screenPos + 1}, new int[] {-siteHeight, 0, -siteHeight} );
    }
    
    private View createPhredQualView(BAMSite site, float width, int height, Color color)
    {
        Pen pen = new Pen(1, color);
        Brush brush = new Brush(color);
        
        CompositeView result = new CompositeView();
        
        Cigar cigar = site.getCigar();
        byte[] phredQuals = site.getBaseQualities();
        
        int readOffset = 0, refOffset = 0;
        
        for( CigarElement e : cigar.getCigarElements() )
        {
            CigarOperator op = e.getOperator();
            switch( op )
            {
                case M:
                case X:
                case EQ:
                {
                    int lastHeight = 0;
                    int lastPos = -1;
                    for( int i = 0; i <= e.getLength(); i++ )
                    {
                        int boxHeight = -1;
                        if(i < e.getLength())
                        {
                            int phredQual = phredQuals.length > readOffset + i ? phredQuals[readOffset + i] : 0;
                            if(phredQual > SAMUtils.MAX_PHRED_SCORE)
                                phredQual = SAMUtils.MAX_PHRED_SCORE;
                            if(phredQual < 0)
                                phredQual = 0;
                            boxHeight = (int)Math.round(height * (double)phredQual / SAMUtils.MAX_PHRED_SCORE);
                        }
                        if(boxHeight != lastHeight)
                        {
                            if(lastPos >= 0)
                            {
                                View qualView = new BoxView(pen, brush,  (refOffset + lastPos) * width, (float)(-lastHeight), (i-lastPos)*width, (float)lastHeight);
                                result.add(qualView);
                            }
                            lastHeight = boxHeight;
                            lastPos = i;
                        }
                    }
                    break;
                }
                default:
                    break;
            }
            if( op.consumesReadBases() )
                readOffset += e.getLength();
            if( op.consumesReferenceBases() )
                refOffset += e.getLength();
        }
        return result;
    }

    private View createBaseView(double nucleotideWidth, BAMSite site, BamTrackViewOptions viewOptions)
    {
        CompositeView result = new CompositeView();

        Pen dashedPen = new Pen(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[] {6, 6}, 0), Color.GRAY);
        Pen mainPen = new Pen(1, (Color)viewOptions.getColorScheme().getBrush(site).getPaint());
        
        Cigar cigar = site.getCigar();

        int minArrowDistanceInPixels = 50;
        int nucleotidesPerArrow = prettyIntGreaterOrEqualThen((int)Math.ceil(minArrowDistanceInPixels / nucleotideWidth));
        int arrowOffset = site.getStrand() == StrandType.STRAND_MINUS ? 0 : ((site.getLength() - 1) % nucleotidesPerArrow);

        int position = 0;
        for(CigarElement e : cigar.getCigarElements())
        {
            CigarOperator op = e.getOperator();
            if(!op.consumesReferenceBases())
                continue;

            result.add(new LineView(op == CigarOperator.N ? dashedPen : mainPen,
                    (float) ( position * nucleotideWidth ), 0f, (float)nucleotideWidth * ( position + e.getLength() ), 0f));
            
            if(op != CigarOperator.N && e.getLength() * nucleotideWidth >= 3)//plot arrows
            {
                for(; arrowOffset < position + e.getLength(); arrowOffset += nucleotidesPerArrow)
                if( site.getStrand() == StrandType.STRAND_PLUS )
                {
                    result.add(new LineView(mainPen, (int) ( (arrowOffset + 1) * nucleotideWidth ), 0, (int) ( (arrowOffset + 1) * nucleotideWidth ) - 3, 3));
                    result.add(new LineView(mainPen, (int) ( (arrowOffset + 1) * nucleotideWidth ), 0, (int) ( (arrowOffset + 1) * nucleotideWidth ) - 3, -3));
                }
                else if( site.getStrand() == StrandType.STRAND_MINUS )
                {
                    result.add(new LineView(mainPen, (int) ( arrowOffset * nucleotideWidth ), 0, (int) ( arrowOffset * nucleotideWidth ) + 3, 3));
                    result.add(new LineView(mainPen, (int) ( arrowOffset * nucleotideWidth ), 0, (int) ( arrowOffset * nucleotideWidth ) + 3, -3));
                }

            }
            
            position += e.getLength();
        }
        
        return result;
    }
    
    /**
     * Pretty integers: 1,2,5,10,20,50,100,200,500,...
     */
    private int prettyIntGreaterOrEqualThen(int x)
    {
       if(x <= 1)
           return 1;
       int a = (int)Math.pow(10, (int)Math.log10(x));
       int r = x % a;
       int b = x / a;
       if(r != 0) b++;
       if(b <= 1)
           return a;
       if(b <= 2)
           return 2*a;
       if(b <= 5)
           return 5*a;
       return 10*a;
    }
    
    @Override
    protected int getIntersectionLength(Site site, int from2, int to2)
    {
        Cigar cigar = ( (BAMSite)site ).getCigar();
        int position = site.getFrom();
        int result = 0;
        for( CigarElement e : cigar.getCigarElements() )
        {
            CigarOperator op = e.getOperator();
            if( !op.consumesReferenceBases() )
                continue;
            if( op != CigarOperator.N )
                result += getIntersectionLength(position, position + e.getLength() - 1, from2, to2);
            position += e.getLength();
        }
        return result;
    }


    private static View createContigView(SequenceView sequenceView, DataCollection<Site> sites, BamTrackViewOptions siteViewOptions,
            int start, int end, int direction, Graphics graphics, MapJobControl control)
    {
        CompositeView result = new CompositeView();

        List<BAMSite> alignments = new ArrayList<>();
        for( Site s : sites )
            alignments.add( (BAMSite)s );

        PileupElement[] pileup = PileupElement.getElements( alignments, start, end );

        ColorFont mainSeqFont = siteViewOptions.getSequenceFont();
        ColorFont mismatchSeqFont = new ColorFont( mainSeqFont.getFont(), Color.RED );
        int siteHeight = graphics.getFontMetrics( mainSeqFont.getFont() ).getHeight();
        Sequence refSequence = sequenceView.getSequence();

        for( int pos = start; pos <= end; pos++ )
        {
            PileupElement pileupElement = pileup[pos - start];
            byte bestLetter = pileupElement.getMostPresentLetterOrDeletion();
            if( bestLetter != -1 )
            {
                char refBase = Character.toUpperCase( (char)refSequence.getLetterAt( pos ) );
                char contigBase = Character.toUpperCase( (char)bestLetter );
                if( ( !siteViewOptions.isShowOnlyMismatchesInContig() ) || ( refBase != contigBase ) )
                {
                    Point pt = new Point( (int) ( ( pos - start ) * sequenceView.getNucleotideWidth() ), 0 );
                    TextView positionView = new TextView( new String( new byte[] {bestLetter} ).toUpperCase(), pt, View.BOTTOM, refBase == contigBase ? mainSeqFont : mismatchSeqFont,
                            graphics );
                    positionView.setDescription( pileupElement.getDescription() );
                    positionView.setSelectable( true );
                    result.add( positionView );
                }
            }
            String bestInsertion = pileupElement.getMostPresentInsertion();
            if( bestInsertion != null )
            {
                int screenPos = (int) ( ( pos - start ) * sequenceView.getNucleotideWidth() );
                View insertionView = createInsertionView( screenPos, siteHeight );
                insertionView.setDescription( pileupElement.getInsertionDescription() );
                insertionView.setSelectable( true );
                result.add( insertionView );
            }

        }

        return result;
    }
}

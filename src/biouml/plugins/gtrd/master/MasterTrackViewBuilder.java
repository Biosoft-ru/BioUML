package biouml.plugins.gtrd.master;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.gtrd.master.sites.MasterSite;
import biouml.plugins.gtrd.master.sites.PWMMotif;
import biouml.plugins.gtrd.master.sites.chipseq.ChIPSeqPeak;
import biouml.plugins.gtrd.master.sites.dnase.DNaseCluster;
import biouml.plugins.gtrd.master.sites.dnase.DNaseFootprint;
import biouml.plugins.gtrd.master.sites.dnase.DNasePeak;
import biouml.plugins.gtrd.master.sites.dnase.FootprintCluster;
import biouml.plugins.gtrd.master.sites.histones.HistonesCluster;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.view.SequenceView;
import ru.biosoft.bsa.view.SiteViewOptions;
import ru.biosoft.bsa.view.TrackView;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.bsa.view.sitelayout.OptimizedListLayoutAlgorithm;
import ru.biosoft.bsa.view.sitelayout.SiteLayoutAlgorithm;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.util.bean.BeanInfoEx2;

public class MasterTrackViewBuilder extends TrackViewBuilder
{
    @Override
    protected View createSiteView(SequenceView sequenceView, Site site, SiteViewOptions siteViewOptions, int start, int end,
            Graphics graphics, int sitesCount)
    {
        CompositeView cv = new CompositeView();

        MasterSite ms = (MasterSite)site;
        Options viewOptions = (Options)siteViewOptions;
        
        CompositeView msView = createMasterSiteView( ms, sequenceView, graphics, start, end );
        cv.add(msView);
        
        if(sequenceView.getNucleotideWidth() > 1)
            showExtraData( sequenceView, start, end, graphics, cv, ms, viewOptions, msView );

        cv.updateBounds();
        return cv;
    }

    public void showExtraData(SequenceView sequenceView, int start, int end, Graphics graphics, CompositeView cv, MasterSite ms,
            Options viewOptions, CompositeView msView)
    {
        if(viewOptions.isShowMotifs() && !ms.getMotifs().isEmpty())
        {
            CompositeView pwmTrackView = createPWMTrackView(ms, sequenceView, graphics, start, end, viewOptions);
            if(pwmTrackView.size() > 0)
                msView.add(pwmTrackView);
        }
        msView.updateBounds();
        
        int y = msView.getBounds().height;
        
        if( viewOptions.isShowChIPSeqPeaks() && !ms.getChipSeqPeaks().isEmpty() )
        {
            View chipSeqView = createChIPSeqView( ms, sequenceView, graphics, start, end, viewOptions );
            chipSeqView.move( 0, y + 10 );
            y += chipSeqView.getBounds().height + 10;
            cv.add( chipSeqView );
        }

        if(viewOptions.isShowDNaseClusters() && !ms.getDnaseClusters().isEmpty())
        {
            View dnaseView = createDNaseClusterView( ms, sequenceView, graphics, start, end, viewOptions );
            dnaseView.move( 0, y + 10 );
            y += dnaseView.getBounds().height + 10;
            cv.add( dnaseView );
        }
        
        if( viewOptions.isShowDNasePeaks() && !ms.getDnasePeaks().isEmpty() )
        {
            View dnaseView = createDNaseView( ms, sequenceView, graphics, start, end, viewOptions );
            dnaseView.move( 0, y + 10 );
            y += dnaseView.getBounds().height + 10;
            cv.add( dnaseView );
        }
        
        if(viewOptions.isShowATACClusters() && !ms.getAtacClusters().isEmpty())
        {
            View dnaseView = createATACClusterView( ms, sequenceView, graphics, start, end, viewOptions );
            dnaseView.move( 0, y + 10 );
            y += dnaseView.getBounds().height + 10;
            cv.add( dnaseView );
        }
        
        if(viewOptions.isShowFAIREClusters() && !ms.getFaireClusters().isEmpty())
        {
            View dnaseView = createFAIREClusterView( ms, sequenceView, graphics, start, end, viewOptions );
            dnaseView.move( 0, y + 10 );
            y += dnaseView.getBounds().height + 10;
            cv.add( dnaseView );
        }
        
        if(viewOptions.isShowHistoneClusters() && !ms.getHistonesClusters().isEmpty())
        {
            View histonesView = createHistonesClusterView( ms, sequenceView, graphics, start, end, viewOptions );
            histonesView.move( 0, y + 10 );
            y += histonesView.getBounds().height + 10;
            cv.add( histonesView );
        }
    }
    
    private CompositeView createMasterSiteView(MasterSite ms, SequenceView sequenceView, Graphics graphics, int start, int end)
    {
        CompositeView cv = new CompositeView();
        
        //Meta cluster box
        final int msBoxHeight = 20;
        View msSiteView = boxView( ms.getFrom(), ms.getTo(), msBoxHeight, new Pen( 1, Color.DARK_GRAY ), new Brush( Color.LIGHT_GRAY ), sequenceView, graphics, start, end );
        msSiteView.setSelectable( true );
        msSiteView.setModel( ms );
        cv.add( msSiteView );
        
        //summit
        Pen summitPen = new Pen( 2, Color.BLACK );
        Point summitPoint = sequenceView.getStartPoint( ms.getFrom() + ms.getSummit() - start + 1, graphics );
        cv.add( new LineView( summitPen, summitPoint.x, summitPoint.y, summitPoint.x, summitPoint.y + msBoxHeight ) );
        
        return cv;
    }

    private View createDNaseClusterView(MasterSite ms, SequenceView sequenceView, Graphics graphics, int start, int end, Options viewOptions)
    {
        return createDNaseLikeClusterView(ms, ms.getDnaseClusters(), ms.getFootprintClusters(), sequenceView, graphics, start, end, viewOptions);
    }
    private View createATACClusterView(MasterSite ms, SequenceView sequenceView, Graphics graphics, int start, int end, Options viewOptions)
    {
        return createDNaseLikeClusterView(ms, ms.getAtacClusters(), Collections.emptyList(), sequenceView, graphics, start, end, viewOptions);
    }
    private View createFAIREClusterView(MasterSite ms, SequenceView sequenceView, Graphics graphics, int start, int end, Options viewOptions)
    {
        return createDNaseLikeClusterView(ms, ms.getFaireClusters(), Collections.emptyList(), sequenceView, graphics, start, end, viewOptions);
    }
    
    private View createDNaseLikeClusterView(MasterSite ms, List<DNaseCluster> clusters, List<FootprintCluster> footprints,
            SequenceView sequenceView, Graphics graphics, int start, int end, Options viewOptions)
    {
        CompositeView result = new CompositeView();
        
        Map<String, FootprintCluster> footprintsByCell = new HashMap<>();
        for(FootprintCluster fp : footprints)
            footprintsByCell.put(fp.getCell().getName(), fp);
     
        Color lightGreen = new Color(185, 215, 166);
        Color darkGreen = new Color(113, 168, 74);
        ColorFont font  = new ColorFont(new Font("SansSerif", Font.PLAIN, 8), Color.black);;

        int y = 0;
        for(DNaseCluster cluster : clusters)
        {
            CompositeView cv = new CompositeView();
            int cutFrom = Math.max( cluster.getFrom(), ms.getFrom()  - viewOptions.getMasterSiteFlanks() );
            int cutTo = Math.min( cluster.getTo(), ms.getTo() + viewOptions.getMasterSiteFlanks() );
            
            View peakView = boxView( cutFrom, cutTo, 8, new Pen(1, Color.DARK_GRAY), new Brush( darkGreen ), sequenceView, graphics, start, end );
            peakView.move( 0, y );
            cv.add( peakView );
            
            String text = cluster.getCell().getTitle();
            Rectangle rect = peakView.getBounds();
            Point p = rect.getLocation();//upper left corner
            p.x += rect.width / 2;//center horizontally
            TextView textView = new TextView( text, p, View.TOP | View.CENTER, font, graphics );
            cv.add( textView );
            
            peakView.move( 0, textView.getBounds().height + 2 );//move under text
            
            FootprintCluster f = footprintsByCell.get( cluster.getCell().getName() );
            if( f != null )
            {
                if( f.getFrom() <= cutTo && f.getTo() >= cutFrom )//intersects
                {
                    int from = Math.max( f.getFrom(), cutFrom );
                    int to = Math.min( f.getTo(), cutTo );

                    View fView = boxView( from, to, 8, new Pen( 1, lightGreen ), new Brush( lightGreen ), sequenceView, graphics, start,
                            end );
                    fView.move( 0, peakView.getBounds().y );
                    cv.add( fView );
                }
            }
            
            cv.updateBounds();
            cv.setSelectable( true );
            cv.setModel( cluster );
            result.add( cv );
            
            y += cv.getBounds().height + 2;
        }
        
        result.updateBounds();
        return result;
    }

    private View createHistonesClusterView(MasterSite ms, SequenceView sequenceView, Graphics graphics, int start, int end, Options viewOptions)
    {
        CompositeView result = new CompositeView();
        
        Pen pen = new Pen(1, Color.DARK_GRAY);
        Brush brush = new Brush( new Color(237, 226, 53) );
        ColorFont font  = new ColorFont(new Font("SansSerif", Font.PLAIN, 8), Color.black);;

        int y = 0;
        for(HistonesCluster cluster : ms.getHistonesClusters())
        {
            CompositeView cv = new CompositeView();
            int cutFrom = Math.max( cluster.getFrom(), ms.getFrom()  - viewOptions.getMasterSiteFlanks() );
            int cutTo = Math.min( cluster.getTo(), ms.getTo() + viewOptions.getMasterSiteFlanks() );
            
            View peakView = boxView( cutFrom, cutTo, 8, pen, brush, sequenceView, graphics, start, end );
            peakView.move( 0, y );
            cv.add( peakView );
            
            String text = cluster.getTarget() + " in " + cluster.getCell().getTitle();
            Rectangle rect = peakView.getBounds();
            Point p = rect.getLocation();//upper left corner
            p.x += rect.width / 2;//center horizontally
            TextView textView = new TextView( text, p, View.TOP | View.CENTER, font, graphics );
            cv.add( textView );
            
            peakView.move( 0, textView.getBounds().height + 2 );//move under text
            
            cv.updateBounds();
            cv.setSelectable( true );
            cv.setModel( cluster );
            result.add( cv );
            
            y += cv.getBounds().height + 2;
        }
        
        result.updateBounds();
        return result;
    }
    
    private View createDNaseView(MasterSite ms, SequenceView sequenceView, Graphics graphics, int start, int end, Options viewOptions)
    {
        CompositeView result = new CompositeView();
        
        Map<String, List<DNaseFootprint>> footprints = new HashMap<>();
        for(DNaseFootprint fp : ms.getDnaseFootprints())
        {
            String dataset = fp.getExp().getName() + "_" + fp.getReplicate();
            footprints.computeIfAbsent( dataset, x->new ArrayList<>() ).add( fp );
        }
     
        Color lightGreen = new Color(185, 215, 166);
        Color darkGreen = new Color(113, 168, 74);
        ColorFont font  = new ColorFont(new Font("SansSerif", Font.PLAIN, 8), Color.black);;

        int y = 0;
        for(DNasePeak peak : ms.getDnasePeaks())
        {
            CompositeView cv = new CompositeView();
            int cutFrom = Math.max( peak.getFrom(), ms.getFrom() - viewOptions.getMasterSiteFlanks() );
            int cutTo = Math.min( peak.getTo(), ms.getTo() + viewOptions.getMasterSiteFlanks() );
            
            View peakView = boxView( cutFrom, cutTo, 8, new Pen(1, Color.DARK_GRAY), new Brush( darkGreen ), sequenceView, graphics, start, end );
            peakView.move( 0, y );
            cv.add( peakView );
            
            String text = peak.getExp().getName() + "_" + peak.getReplicate() + "_" + peak.getPeakCaller() + "_" + peak.getExp().getCell().getTitle();
            Rectangle rect = peakView.getBounds();
            Point p = rect.getLocation();//upper left corner
            p.x += rect.width / 2;//center horizontally
            TextView textView = new TextView( text, p, View.TOP | View.CENTER, font, graphics );
            cv.add( textView );
            
            peakView.move( 0, textView.getBounds().height + 2 );//move under text
            
            List<DNaseFootprint> fs = footprints.get( peak.getExp().getName() + "_" + peak.getReplicate() );
            if(fs != null)
                for(DNaseFootprint f : fs)
                {
                    if(f.getFrom() <= cutTo && f.getTo() >= cutFrom)//intersects
                    {
                        int from = Math.max( f.getFrom(), cutFrom );
                        int to = Math.min( f.getTo(), cutTo );
                       
                        View fView = boxView( from, to, 8, new Pen(1, lightGreen), new Brush( lightGreen ), sequenceView, graphics, start, end );
                        fView.move( 0, peakView.getBounds().y );
                        cv.add( fView );
                    }
                }
            
            cv.updateBounds();
            cv.setSelectable( true );
            cv.setModel( peak );
            result.add( cv );
            
            y += cv.getBounds().height + 2;
        }
        
        result.updateBounds();
        return result;
    }
    
    private View createChIPSeqView(MasterSite ms, SequenceView sequenceView, Graphics graphics, int start, int end, Options viewOptions)
    {
        CompositeView result = new CompositeView();
        
        ColorFont font  = new ColorFont(new Font("SansSerif", Font.PLAIN, 8), Color.black);;
        Pen pen = new Pen(1, Color.DARK_GRAY);
        Brush summitBrush = new Brush( Color.DARK_GRAY );
        Brush siteBrush = new Brush( new Color(187, 64, 53) );

        int y = 0;
        for(ChIPSeqPeak peak : ms.getChipSeqPeaks())
        {
            CompositeView cv = new CompositeView();
            int cutFrom = Math.max( peak.getFrom(), ms.getFrom() - viewOptions.getMasterSiteFlanks() );
            int cutTo = Math.min( peak.getTo(), ms.getTo() + viewOptions.getMasterSiteFlanks() );
            
            View peakView = boxView( cutFrom, cutTo, 8, pen, siteBrush, sequenceView, graphics, start, end );
            peakView.move( 0, y );
            cv.add( peakView );
            
            View summitView = null;
            if(peak.hasSummit())
            {
                int absSummit = peak.getFrom() + peak.getSummit();
                summitView = boxView( absSummit, absSummit, 8, pen, summitBrush, sequenceView, graphics, start, end );
                summitView.move( 0, y );
                cv.add( summitView );
            }
            
            String text = peak.getExp().getName() + "_" + peak.getPeakCaller() + "_" + peak.getExp().getCell().getTitle();
            Rectangle rect = peakView.getBounds();
            Point p = rect.getLocation();//upper left corner
            p.x += rect.width / 2;//center horizontally
            TextView textView = new TextView( text, p, View.TOP | View.CENTER, font, graphics );
            cv.add( textView );
            
            peakView.move( 0, textView.getBounds().height + 2 );//move under text
            if(summitView != null)
                summitView.move( 0, textView.getBounds().height + 2 );//move under text
            
            cv.updateBounds();
            cv.setSelectable( true );
            cv.setModel( peak );
            result.add( cv );
            
            y += cv.getBounds().height + 2;
        }
        
        result.updateBounds();
        return result;
    }
    

    private CompositeView createPWMTrackView(MasterSite ms, SequenceView sequenceView, Graphics graphics, int start, int end, Options viewOptions)
    {
        TrackView pwmTrackView = new TrackView();
        Pen pwmPen = new Pen(1, Color.PINK);
        Brush pwmBrush = new Brush( Color.PINK );
        Interval bounds = ms.getInterval().grow(viewOptions.getMasterSiteFlanks());
        for(PWMMotif motif : ms.getMotifs())
        {
            if(!motif.getInterval().intersects( bounds ))
                continue;
            View motifView = boxView( motif.getFrom(), motif.getTo(), 8, pwmPen, pwmBrush,  sequenceView, graphics, start, end);
            motifView.setModel( motif ); 
            motifView.setSelectable( true );
            motifView.move( 0, 4 );
            pwmTrackView.add( motifView  );
        }
        SiteLayoutAlgorithm layout = new OptimizedListLayoutAlgorithm();
        layout.layout( pwmTrackView, 1 );
        return pwmTrackView;
    }

    public static View boxView(int start, int end, int height, Pen pen, Brush brush, SequenceView sequenceView, Graphics graphics, int seqStart, int seqEnd )
    {
        // Beginning drawing sites from 1
        start -= seqStart - 1;
        end -= seqStart - 1;
        Point startPoint = sequenceView.getStartPoint( start, graphics );
        Point endPoint = sequenceView.getEndPoint( end, graphics );
        return new BoxView( pen, brush, startPoint.x, 0, endPoint.x - startPoint.x, height );
    }
    
    @Override
    public SiteViewOptions createViewOptions()
    {
        return new Options();
    }
    

    public static class Options extends SiteViewOptions
    {
        private boolean showMotifs = true;
        @PropertyName("Show motifs")
        public boolean isShowMotifs()
        {
            return showMotifs;
        }
        public void setShowMotifs(boolean showMotifs)
        {
            boolean oldValue = this.showMotifs;
            this.showMotifs = showMotifs;
            firePropertyChange( "showMotifs", oldValue, showMotifs );
        }


        private boolean showChIPSeqPeaks = true;
        @PropertyName("Show ChIP-seq peaks")
        public boolean isShowChIPSeqPeaks()
        {
            return showChIPSeqPeaks;
        }
        public void setShowChIPSeqPeaks(boolean showChIPSeqPeaks)
        {
            boolean oldValue = this.showChIPSeqPeaks;
            this.showChIPSeqPeaks = showChIPSeqPeaks;
            firePropertyChange( "showChIPSeqPeaks", oldValue, showChIPSeqPeaks );
        }
        

        private boolean showDNaseClusters = true;
        @PropertyName("Show DNase-seq clusters")
        public boolean isShowDNaseClusters()
        {
            return showDNaseClusters;
        }
        public void setShowDNaseClusters(boolean showDNaseClusters)
        {
            boolean oldValue = this.showDNaseClusters;
            this.showDNaseClusters = showDNaseClusters;
            firePropertyChange( "showDNaseClusters", oldValue, showDNaseClusters );
        }
        
        private boolean showATACClusters = true;
        @PropertyName("Show ATAC-seq clusters")
        public boolean isShowATACClusters()
        {
            return showATACClusters;
        }
        public void setShowATACClusters(boolean showATACClusters)
        {
            boolean oldValue = this.showATACClusters;
            this.showATACClusters = showATACClusters;
            firePropertyChange( "showATACClusters", oldValue, showATACClusters );
        }
        
        private boolean showFAIREClusters = true;
        @PropertyName("Show FAIRE-seq clusters")
        public boolean isShowFAIREClusters()
        {
            return showFAIREClusters;
        }
        public void setShowFAIREClusters(boolean showFAIREClusters)
        {
            boolean oldValue = this.showFAIREClusters;
            this.showFAIREClusters = showFAIREClusters;
            firePropertyChange( "showFAIREClusters", oldValue, showFAIREClusters );
        }

        private boolean showDNasePeaks;
        @PropertyName("Show DNase-seq peaks")
        public boolean isShowDNasePeaks()
        {
            return showDNasePeaks;
        }
        public void setShowDNasePeaks(boolean showDNasePeaks)
        {
            boolean oldValue = this.showDNasePeaks;
            this.showDNasePeaks = showDNasePeaks;
            firePropertyChange( "showDNasePeaks", oldValue, showDNasePeaks );
        }
        
        private boolean showHistoneClusters = true;
        @PropertyName("Show histone modification clusters")
        public boolean isShowHistoneClusters()
        {
            return showHistoneClusters;
        }
        public void setShowHistoneClusters(boolean showHistoneClusters)
        {
            boolean oldValue = this.showHistoneClusters;
            this.showHistoneClusters = showHistoneClusters;
            firePropertyChange( "showHistoneClusters", oldValue, showHistoneClusters );
        }

        private int masterSiteFlanks = 50;
        @PropertyName("Flanks")
        public int getMasterSiteFlanks()
        {
            return masterSiteFlanks;
        }
        public void setMasterSiteFlanks(int masterSiteFlanks)
        {
            int oldValue = this.masterSiteFlanks;
            this.masterSiteFlanks = masterSiteFlanks;
            firePropertyChange( "masterSiteFlanks", oldValue, masterSiteFlanks );
        }
        
        
        
    }

    public static class OptionsBeanInfo extends BeanInfoEx2<Options>
    {
        public OptionsBeanInfo()
        {
            super( Options.class );
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("showMotifs");
            add("showChIPSeqPeaks");
            add("showDNaseClusters");
            add("showDNasePeaks");
            add("showATACClusters");
            add("showFAIREClusters");
            add("showHistoneClusters");
            add("masterSiteFlanks");
        }
    }
}

package ru.biosoft.bsa.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import java.util.logging.Logger;

import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.DataElementExporterRegistry.ExporterInfo;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackRegion;
import ru.biosoft.bsa.project.Project;
import ru.biosoft.bsa.project.Region;
import ru.biosoft.bsa.project.TrackInfo;
import biouml.workbench.ProcessElementDialog;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.application.Application;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;

@SuppressWarnings ( "serial" )
public class ExportTrackDialog extends ProcessElementDialog implements JobControlListener
{
    private static final String[] BSA_CATEGORY_LIST = {"ru.biosoft.bsa"};
    protected Project model;
    protected JTextField tfPath = new JTextField(30);
    protected JComboBox<String> formatComboBox = new JComboBox<>();
    protected JComboBox<TrackItem> trackComboBox = new JComboBox<>();
    protected JComboBox<RegionItem> sequenceComboBox = new JComboBox<>();
    protected JRadioButton wholeSequenceRadioButton = null;
    protected JRadioButton visibleRangeRadioButton = null;
    protected JRadioButton customRangeRadioButton = null;
    protected JTextField customFrom = null;
    protected JTextField customTo = null;
    protected JProgressBar progressBar = new JProgressBar();

    protected static final String PREFERENCES_EXPORT_DIRECTORY = "exportDialog.exportDirectory";
    protected String exportDirectory;
    protected Track track;
    protected Interval seq, vis;
    FunctionJobControl jobControl;

    public ExportTrackDialog(JFrame parent, Project project, Track track)
    {
        super(parent, "");
        messageBundle = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());
        log = Logger.getLogger(ExportTrackDialog.class.getName());
        this.track = track;
        this.model = project;
        setTitle(messageBundle.getResourceString("EXPORT_TRACK_DIALOG_TITLE"));

        JPanel contentPane = new JPanel(new GridBagLayout());
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPane.add(new JLabel(messageBundle.getResourceString("TRACK_DIALOG_FILE")), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        contentPane.add(tfPath, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(5, 5, 0, 0), 0, 0));

        JButton browseButton = new JButton("...");
        contentPane.add(browseButton, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(5, 5, 0, 0), 0, 0));
        tfPath.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                update();
            }
        });

        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(new File(exportDirectory));
            int res = chooser.showOpenDialog(Application.getApplicationFrame());
            if( res == JFileChooser.APPROVE_OPTION )
            {
                File file = chooser.getSelectedFile();
                tfPath.setText(file.getPath());
                update();
                exportDirectory = chooser.getCurrentDirectory().getAbsolutePath();
            }
        });

        //--- format ---
        contentPane.add(new JLabel(messageBundle.getResourceString("TRACK_DIALOG_FORMAT")), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        contentPane.add(formatComboBox, new GridBagConstraints(1, 2, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

        contentPane.add(new JLabel(messageBundle.getResourceString("TRACK_DIALOG_TRACK")), new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        contentPane.add(trackComboBox, new GridBagConstraints(1, 4, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(5, 5, 0, 0), 0, 0));

        if( model != null )
        {
            contentPane.add(new JLabel(messageBundle.getResourceString("TRACK_DIALOG_SEQUENCE")), new GridBagConstraints(0, 3, 1, 1, 0.0,
                    0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
            contentPane.add(sequenceComboBox, new GridBagConstraints(1, 3, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

            initFormats();
            initRanges();

            ButtonGroup radioGroup = new ButtonGroup();

            visibleRangeRadioButton = new JRadioButton(messageBundle.getResourceString("EXPORT_TRACK_VISIBLE_RANGE") + " "+vis, true);
            contentPane.add(visibleRangeRadioButton, new GridBagConstraints(0, 5, 3, 1, 0.0, 0.0, GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            radioGroup.add(visibleRangeRadioButton);

            wholeSequenceRadioButton = new JRadioButton(messageBundle.getResourceString("EXPORT_TRACK_WHOLE_SEQUENCE") + " "+seq, true);
            contentPane.add(wholeSequenceRadioButton, new GridBagConstraints(0, 6, 3, 1, 0.0, 0.0, GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            radioGroup.add(wholeSequenceRadioButton);

            JPanel customPane = new JPanel(new GridBagLayout());
            customRangeRadioButton = new JRadioButton(messageBundle.getResourceString("EXPORT_TRACK_CUSTOM_RANGE"), true);
            customPane.add(customRangeRadioButton, new GridBagConstraints(0, 0, 1, 1, 2.0, 0.0, GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            customFrom = new JTextField(String.valueOf(vis.getFrom()));
            customPane.add(customFrom, new GridBagConstraints(1, 0, 1, 1, 3.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                    new Insets(5, 5, 0, 0), 0, 0));
            customTo = new JTextField(String.valueOf(vis.getTo()));
            customPane.add(new JLabel("-"), new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                    GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
            customPane.add(customTo, new GridBagConstraints(3, 0, 1, 1, 3.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                    new Insets(5, 5, 0, 0), 0, 0));
            contentPane.add(customPane, new GridBagConstraints(0, 7, 3, 1, 0.0, 0.0, GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            radioGroup.add(customRangeRadioButton);
        }
        else
        {
            initFormats();
        }

        progressBar.setMaximum(100);
        contentPane.add(progressBar, new GridBagConstraints(0, 8, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 10, 0, 0), 0, 0));

        //--- logging settings ---
        initAppender("Track export log", "");
        appender.addToCategories(BSA_CATEGORY_LIST);

        contentPane.add(new JLabel(messageBundle.getResourceString("TRACK_DIALOG_INFO")), new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));

        contentPane.add(appender.getLogTextPanel(), new GridBagConstraints(0, 10, 3, 3, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.BOTH, new Insets(5, 0, 0, 0), 0, 0));

        //--- dialog settings ---
        setContent(contentPane);

        okButton.setText(messageBundle.getResourceString("EXPORT_TRACK_DIALOG_EXPORT"));
        okButton.setPreferredSize(okButton.getMinimumSize());
        okButton.setEnabled(false);
        cancelButton.setText(messageBundle.getResourceString("TRACK_DIALOG_CLOSE"));

        loadPreferences();
    }

    protected void initRanges()
    {
        Region region = ( (RegionItem)sequenceComboBox.getSelectedItem() ).getRegion();
        seq = region.getSequence().getInterval();
        vis = region.getInterval();
    }

    public static class RegionItem
    {
        DataElementPath completeName;
        String shortName;
        Region region;
        public RegionItem(Region region)
        {
            this.region = region;
            this.completeName = region.getSequencePath();
            this.shortName = this.completeName.getName();
        }
        @Override
        public String toString()
        {
            return this.shortName;
        }
        public DataElementPath getCompletePath()
        {
            return this.completeName;
        }
        public Region getRegion()
        {
            return region;
        }
    }

    public static class TrackItem
    {
        Track tr;
        String name;
        public TrackItem(Track tr)
        {
            this.tr = tr;
            this.name = tr.getName();
        }
        @Override
        public String toString()
        {
            return this.name;
        }
        public Track getTrack()
        {
            return this.tr;
        }
    }

    protected void initFormats()
    {
        TrackRegion tr;

        if( model != null )
        {
            Region[] reglist = model.getRegions();
            TrackInfo[] tinfo = model.getTracks();
            if( reglist.length == 0 || tinfo.length == 0 )
            {
                info("EXPORT_TRACK_DIALOG_NO_TRACKS");
                return;
            }
            tr = new TrackRegion(tinfo[0].getTrack(), reglist[0].getSequenceName());
            for( Region r : reglist )
                sequenceComboBox.addItem(new RegionItem(r));
            for( TrackInfo t : tinfo )
                trackComboBox.addItem(new TrackItem(t.getTrack()));
        }
        else
        {
            trackComboBox.addItem(new TrackItem(track));
            tr = new TrackRegion(track);
        }

        List<String> formats = DataElementExporterRegistry.getExporterFormats(tr);
        if( formats.size() == 0 )
        {
            throw new UnsupportedOperationException(messageBundle.getResourceString("EXPORT_TRACK_DIALOG_NO_EXPORTERS"));
        }
        for( String format : formats )
        {
            formatComboBox.addItem(format);
        }
    }

    protected void loadPreferences()
    {
        String key = Preferences.DIALOGS + "/" + PREFERENCES_EXPORT_DIRECTORY;
        exportDirectory = Application.getPreferences().getStringValue(key, ".");
    }

    protected void savePreferences()
    {
        String key = Preferences.DIALOGS + "/" + PREFERENCES_EXPORT_DIRECTORY;
        if( Application.getPreferences().getProperty(key) != null )
            Application.getPreferences().setValue(key, exportDirectory);
        else
        {
            try
            {
                Preferences preferences = Application.getPreferences().getPreferencesValue(Preferences.DIALOGS);
                preferences.add(new DynamicProperty(messageBundle.getResourceString(PREFERENCES_EXPORT_DIRECTORY), messageBundle
                        .getResourceString("EXPORT_TRACK_DIALOG_PREFERENCES_DIR_PN"), messageBundle
                        .getResourceString("EXPORT_TRACK_DIALOG_PREFERENCES_DIR_PD"), String.class, exportDirectory));
            }
            catch( Exception e )
            {
            }
        }
    }

    protected void update()
    {
        String fileName = tfPath.getText();
        okButton.setEnabled(fileName != null && fileName.length() > 0 && formatComboBox.getItemCount() > 0);
    }

    @Override
    protected void cancelPressed()
    {
        if( jobControl != null && jobControl.getStatus() == FunctionJobControl.RUNNING )
        {
            jobControl.terminate();
        }
        else
        {
            super.cancelPressed();
        }
    }

    @Override
    protected void okPressed()
    {
        final String format = (String)formatComboBox.getSelectedItem();
        String fileName = tfPath.getText();
        DataElementPath sequence = null;
        Track track = ( (TrackItem)trackComboBox.getSelectedItem() ).getTrack();
        Interval interval = new Interval(0, 0);
        if( model != null )
        {
            sequence = ( (RegionItem)sequenceComboBox.getSelectedItem() ).getCompletePath();
            initRanges();
            if( wholeSequenceRadioButton.isSelected() )
            {
                interval = seq;
            }
            else if( visibleRangeRadioButton.isSelected() )
            {
                interval = vis;
            }
            else
            {
                try
                {
                    interval = new Interval(Integer.parseInt(customFrom.getText()), Integer.parseInt(customTo.getText()));
                }
                catch( NumberFormatException e )
                {
                    info("EXPORT_TRACK_INVALID_RANGE");
                    return;
                }
            }
        }

        final TrackRegion tr = new TrackRegion(track, sequence, interval);
        final long start = System.currentTimeMillis();
        savePreferences();

        try
        {
            ExporterInfo[] exporterInfo = DataElementExporterRegistry.getExporterInfo(format, tr);

            // info should be not empty because previously we have selected only suitable formats
            if( exporterInfo == null )
            {
                info("EXPORT_TRACK_DIALOG_NO_EXPORTER", format, tr.getName());
                return;
            }

            /**
             * TODO Implement suitable dialog for users ability to choose
             * one of feasible exporters
             */
            if( exporterInfo.length > 1 )
            {
                info("There are more than one exporters available for the given format:");
                for( int i = 0; i < exporterInfo.length; i++ )
                    info( ( i + 1 ) + ". " + exporterInfo[i].getExporter().getClass().getName());
                info("The first one " + exporterInfo[0].getExporter().getClass().getName() + " will be used.");
            }

            String suffix = exporterInfo[0].getSuffix();
            if( suffix.indexOf('.') == -1 )
                suffix = "." + suffix;

            if( fileName.endsWith(suffix) )
                suffix = "";

            final File file = new File(fileName + suffix);

            final DataElementExporter exporter = exporterInfo[0].cloneExporter();

            for( JComponent c : new JComponent[] {tfPath, formatComboBox, trackComboBox, sequenceComboBox, wholeSequenceRadioButton,
                    visibleRangeRadioButton, customRangeRadioButton, customFrom, customTo, okButton} )
                if( c != null )
                    c.setEnabled(false);
            cancelButton.setText(messageBundle.getResourceString("TRACK_DIALOG_CANCEL"));
            info("EXPORT_TRACK_EXPORT_STARTED");

            jobControl = new FunctionJobControl(null);
            jobControl.addListener(this);
            ( new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        exporter.doExport(tr, file, jobControl);
                        switch( jobControl.getStatus() )
                        {
                            case JobControl.COMPLETED:
                                String time = "" + ( System.currentTimeMillis() - start );
                                info("EXPORT_TRACK_DIALOG_SUCCESS", tr.getName(), format, file.getAbsolutePath(), time);
                                break;
                            case JobControl.TERMINATED_BY_REQUEST:
                                info("EXPORT_TRACK_EXPORT_CANCELLED");
                                break;
                            default:
                                log.info(jobControl.getTextStatus());
                        }
                    }
                    catch( Throwable t )
                    {
                        error("EXPORT_TRACK_FAILED", t.toString());
                    }
                    for( JComponent c : new JComponent[] {tfPath, formatComboBox, trackComboBox, sequenceComboBox,
                            wholeSequenceRadioButton, visibleRangeRadioButton, customRangeRadioButton, customFrom, customTo, okButton} )
                        if( c != null )
                            c.setEnabled(true);
                    cancelButton.setText(messageBundle.getResourceString("TRACK_DIALOG_CLOSE"));
                }
            } ).start();
        }
        catch( Exception e )
        {
            info("EXPORT_TRACK_DIALOG_ERROR", tr.getName(), format, e.getMessage());
        }
    }

    @Override
    public void valueChanged(JobControlEvent event)
    {
        progressBar.setValue(event.getPreparedness());
    }

    @Override
    public void jobPaused(JobControlEvent event)
    {
    }

    @Override
    public void jobResumed(JobControlEvent event)
    {
    }

    @Override
    public void jobStarted(JobControlEvent event)
    {
    }

    @Override
    public void jobTerminated(JobControlEvent event)
    {
    }

    @Override
    public void resultsReady(JobControlEvent event)
    {
    }
}

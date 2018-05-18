package uk.ac.starlink.topcat.activate;

import java.awt.Component;
import java.io.IOException;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.LocationTableLoadDialog;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.topcat.plot2.PlotWindowType;
import uk.ac.starlink.topcat.plot2.TablePlotDisplay;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Activation type that opens a plot window for use with a table whose
 * location is supplied.  The table is not actually loaded into the
 * TOPCAT application.
 *
 * @author   Mark Taylor
 * @since    10 May 2018
 */
public class PlotTableActivationType implements ActivationType {

    public String getName() {
        return "Plot Table";
    }

    public String getDescription() {
        return "Open a plot window for the table referenced by"
             + " a file or URL column, without loading the table";
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new PlotColumnConfigurator( tinfo );
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return tinfo.getUrlSuitability();
    }

    /**
     * Configurator for use with this class.
     */
    private static class PlotColumnConfigurator extends UrlColumnConfigurator {
        private final StarTableFactory tfact_;
        private TablePlotDisplay plotDisplay_;
        private final JComboBox formatSelector_;
        private final JComboBox ptypeSelector_;
        private final JCheckBox paramsSelector_;
        private static final String FORMAT_KEY = "format";
        private static final String PLOTTYPE_KEY = "plotType";
        private static final String IMPORTPARAMS_KEY = "importParams";

        /**
         * Constructor.
         *
         * @param  tinfo   table info
         */
        PlotColumnConfigurator( TopcatModelInfo tinfo ) {
            super( tinfo, "Table", new ColFlag[] { ColFlag.URL, } );
            tfact_ = ControlWindow.getInstance().getTableFactory();
            JComponent queryPanel = getQueryPanel();
            ActionForwarder forwarder = getActionForwarder();
            ptypeSelector_ = new JComboBox( PlotWindowType.values() );
            ptypeSelector_.setSelectedItem( PlotWindowType.PLANE );
            ptypeSelector_.setRenderer( new BasicComboBoxRenderer() {
                @Override
                public Component
                        getListCellRendererComponent( JList list, Object value,
                                                      int index, boolean isSel,
                                                      boolean hasFocus ) {
                    Component c =
                        super.getListCellRendererComponent( list, value, index,
                                                            isSel, hasFocus );
                    if ( c instanceof JLabel &&
                         value instanceof PlotWindowType  ) {
                        JLabel label = (JLabel) c;
                        ((JLabel) c).setIcon( ((PlotWindowType) value)
                                             .getIcon() );
                    }
                    return c;
                }
            } );
            ptypeSelector_.addActionListener( forwarder );
            LocationTableLoadDialog locTld = new LocationTableLoadDialog();
            locTld.configure( tfact_, null );
            formatSelector_ = locTld.createFormatSelector();
            formatSelector_.addActionListener( forwarder );
            paramsSelector_ = new JCheckBox();
            paramsSelector_.setSelected( true );
            paramsSelector_.addActionListener( forwarder );
            queryPanel.add( new LineBox( "Plot Type",
                                      new ShrinkWrapper( ptypeSelector_ ) ) );
            queryPanel.add( Box.createVerticalStrut( 5 ) );
            queryPanel.add( new LineBox( "Table Format",
                                      new ShrinkWrapper( formatSelector_ ) ) );
            queryPanel.add( Box.createVerticalStrut( 5 ) );
            queryPanel.add( new LineBox( "Import Parameters",
                                         paramsSelector_ ) );
            queryPanel.add( Box.createVerticalStrut( 5 ) );
        }

        protected Activator createActivator( ColumnData cdata ) {
            final String format = (String) formatSelector_.getSelectedItem();
            final boolean importParams = paramsSelector_.isSelected();
            PlotWindowType ptype =
                (PlotWindowType) ptypeSelector_.getSelectedItem();
            final TablePlotDisplay plotDisplay;
            if ( plotDisplay_ != null &&
                 plotDisplay_.getPlotWindowType() == ptype ) {
                plotDisplay = plotDisplay_;
            }
            else {
                if ( plotDisplay_ != null ) {
                    JFrame window = plotDisplay_.getWindow();
                    if ( window != null ) {
                        window.dispose();
                    }
                }
                plotDisplay = new TablePlotDisplay( getQueryPanel(), ptype,
                                                    "Activated", false );
                plotDisplay_ = plotDisplay;
            }
            return new LocationColumnActivator( cdata, false ) {
                final TopcatModel parentTable = getTopcatModel();
                public Outcome activateLocation( final String loc, long lrow ) {
                    final StarTable table;
                    try {
                        table = tfact_.makeStarTable( loc, format );
                    }
                    catch ( IOException e ) {
                        return Outcome.failure( e );
                    }
                    if ( importParams ) {
                        table.getParameters()
                             .addAll( TopcatUtils
                                     .getRowAsParameters( parentTable, lrow ) );
                    }
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            plotDisplay.showPlotWindow( table );
                        }
                    } );
                    return Outcome.success( loc );
                }
            };
        }

        protected String getConfigMessage( ColumnData cdata ) {
            return null;
        }

        public ConfigState getState() {
            ConfigState state = getUrlState();
            state.saveSelection( PLOTTYPE_KEY, ptypeSelector_ );
            state.saveSelection( FORMAT_KEY, formatSelector_ );
            state.saveFlag( IMPORTPARAMS_KEY, paramsSelector_.getModel() );
            return state;
        }

        public void setState( ConfigState state ) {
            setUrlState( state );
            state.restoreSelection( PLOTTYPE_KEY, ptypeSelector_ );
            state.restoreSelection( FORMAT_KEY, formatSelector_ );
            state.restoreFlag( IMPORTPARAMS_KEY, paramsSelector_.getModel() );
        }
    }
}

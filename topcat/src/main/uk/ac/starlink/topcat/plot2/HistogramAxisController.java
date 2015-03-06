package uk.ac.starlink.topcat.plot2;

import java.util.Arrays;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.geom.PlaneAspect;
import uk.ac.starlink.ttools.plot2.geom.PlaneNavigator;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurfaceFactory;
import uk.ac.starlink.ttools.plot2.layer.BinSizer;
import uk.ac.starlink.ttools.plot2.layer.HistogramPlotter;
import uk.ac.starlink.ttools.plot2.layer.KernelDensityPlotter;
import uk.ac.starlink.ttools.plot2.layer.Normalisation;
import uk.ac.starlink.ttools.plot2.layer.Pixel1dPlotter;

/**
 * Axis control for histogram window.
 *
 * @author   Mark Taylor
 * @since    21 Jan 2014
 */
public class HistogramAxisController
        extends CartesianAxisController<PlaneSurfaceFactory.Profile,
                                        PlaneAspect> {

    /**
     * Constructor.
     *
     * @param  stack  control stack
     */
    public HistogramAxisController( ControlStack stack ) {
        super( new HistogramSurfaceFactory(), "histogramNavigation",
               PlaneAxisController.createAxisLabelKeys(), stack );
        SurfaceFactory surfFact = getSurfaceFactory();
        ConfigControl mainControl = getMainControl();

        /* Log/flip tab. */
        mainControl.addSpecifierTab( "Coords",
                                     new ConfigSpecifier( new ConfigKey[] {
            PlaneSurfaceFactory.XLOG_KEY,
            PlaneSurfaceFactory.YLOG_KEY,
            PlaneSurfaceFactory.XFLIP_KEY,
            PlaneSurfaceFactory.YFLIP_KEY,
            PlaneSurfaceFactory.XYFACTOR_KEY,
        } ) );

        /* Navigator tab. */
        addNavigatorTab();

        /* Range tab. */
        addAspectConfigTab( "Range",
                            new ConfigSpecifier( surfFact.getAspectKeys() ) {
            @Override
            protected void checkConfig( ConfigMap config )
                    throws ConfigException {
                checkRangeSense( config, "X",
                                 PlaneSurfaceFactory.XMIN_KEY,
                                 PlaneSurfaceFactory.XMAX_KEY );
                checkRangeSense( config, "Y",
                                 PlaneSurfaceFactory.YMIN_KEY,
                                 PlaneSurfaceFactory.YMAX_KEY );
            }
        } );

        /* Grid tab. */
        mainControl.addSpecifierTab( "Grid",
                                     new ConfigSpecifier( new ConfigKey[] {
            PlaneSurfaceFactory.GRID_KEY,
            StyleKeys.GRID_COLOR,
            StyleKeys.AXLABEL_COLOR,
            StyleKeys.MINOR_TICKS,
            PlaneSurfaceFactory.XCROWD_KEY,
            PlaneSurfaceFactory.YCROWD_KEY,
        } ) );

        /* Labels tab. */
        addLabelsTab();
        AutoSpecifier<String> ySpecifier = 
            getLabelSpecifier()
           .getAutoSpecifier( PlaneSurfaceFactory.YLABEL_KEY );
        ySpecifier.setAuto( false );
        ySpecifier.setSpecifiedValue( null );

        /* Font tab. */
        mainControl.addSpecifierTab( "Font",
                                     new ConfigSpecifier( StyleKeys.CAPTIONER
                                                         .getKeys() ) );

        /* Bars control. */
        ConfigSpecifier hbarSpecifier = new ConfigSpecifier( new ConfigKey[] {
            HistogramPlotter.BINSIZER_KEY,
            HistogramPlotter.PHASE_KEY,
        } );
        ConfigSpecifier kbinSpecifier = new ConfigSpecifier( new ConfigKey[] {
            Pixel1dPlotter.SMOOTH_KEY,
            Pixel1dPlotter.KERNEL_KEY,
        } );
        ConfigSpecifier genSpecifier = new ConfigSpecifier( new ConfigKey[] {
            StyleKeys.CUMULATIVE,
            StyleKeys.NORMALISE,
        } );
        ConfigControl barControl =
            new ConfigControl( "Bars", ResourceIcon.HISTOBARS );
        barControl.addSpecifierTab( "Histogram", hbarSpecifier );
        barControl.addSpecifierTab( "KDE", kbinSpecifier );
        barControl.addSpecifierTab( "General", genSpecifier );
        addControl( barControl );

        assert assertHasKeys( surfFact.getProfileKeys() );
    }

    @Override
    protected boolean logChanged( PlaneSurfaceFactory.Profile prof1,
                                  PlaneSurfaceFactory.Profile prof2 ) {
        return ! Arrays.equals( prof1.getLogFlags(), prof2.getLogFlags() );
    }

    @Override
    protected boolean clearRange( PlaneSurfaceFactory.Profile oldProfile,
                                  PlaneSurfaceFactory.Profile newProfile,
                                  PlotLayer[] oldLayers, PlotLayer[] newLayers,
                                  boolean lock ) {
        if ( super.clearRange( oldProfile, newProfile, oldLayers, newLayers,
                               lock ) ) {
            return true;
        }
        else if ( lock ) {
            return false;
        }

        /* Look at the bar style changes which ought to cause a re-range.
         * If any of them have changed between all the old and new layers,
         * it's a global change which is worth a re-range. */
        else {
            BarState state0 = getBarState( oldLayers );
            BarState state1 = getBarState( newLayers );
            if ( state0 == null && state1 == null ) {
                return false;
            }
            else if ( state0 == null || state1 == null ) {
                return true;
            }
            else {
                if ( state0.isCumulative_ != state1.isCumulative_ ||
                     state0.norm_ != state1.norm_ ) {
                    return true;
                }
                else if ( ! PlotUtil.equals( state0.sizer_, state1.sizer_ ) &&
                          ! state0.isCumulative_ ) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
    }

    /**
     * Returns the common state of the histogram layers concerning
     * how the bars are laid out.
     *
     * @param  layers   plot layers
     * @return  bar state if one can be determined, null if no histogram layers
     */
    private static BarState getBarState( PlotLayer[] layers ) {

        /* Get the state from one representative layer, since at present
         * configuration of these items is per-window not per-layer.
         * There is code here to assert that this is still the case,
         * i.e. that the BarState is consistent across all layers;
         * subsequent changes to the GUI could invalidate those assertions
         * in which case we have to rethink the role of the bar state. */
        BinSizer sizer = null;
        Boolean cumul = null;
        Normalisation norm = null;
        boolean hasBars = false;
        for ( PlotLayer layer : layers ) {
            BinSizer sizer1 = null;
            Boolean cumul1 = null;
            Normalisation norm1 = null;
            Style style = layer.getStyle();
            if ( style instanceof HistogramPlotter.HistoStyle ) {
                hasBars = true;
                HistogramPlotter.HistoStyle hstyle =
                    (HistogramPlotter.HistoStyle) style;
                sizer1 = hstyle.getBinSizer();
                cumul1 = hstyle.isCumulative();
                norm1 = hstyle.getNormalisation();
            }
            else if ( style instanceof KernelDensityPlotter.KDenseStyle ) {
                hasBars = true;
                KernelDensityPlotter.KDenseStyle dstyle =
                    (KernelDensityPlotter.KDenseStyle) style;
                cumul1 = dstyle.isCumulative();
                norm1 = dstyle.getNormalisation();
            }
            assert sizer == null || sizer1 == null || sizer.equals( sizer1 );
            assert cumul == null || cumul.equals( cumul1 );
            assert norm == null || norm.equals( norm1 );
            sizer = sizer1;
            cumul = cumul1;
            norm = norm1;
        }
        return hasBars ? new BarState( sizer, cumul, norm ) : null;
    }

    /**
     * Surface factory for histogram.
     */
    private static class HistogramSurfaceFactory extends PlaneSurfaceFactory {
        
        private static final ConfigKey<Boolean> HIST_XANCHOR_KEY =
            createAxisAnchorKey( "X", true );
        private static final ConfigKey<Boolean> HIST_YANCHOR_KEY =
            createAxisAnchorKey( "Y", false );

        @Override
        public ConfigKey[] getNavigatorKeys() {
            return new ConfigKey[] {
                NAVAXES_KEY,
                HIST_XANCHOR_KEY,
                HIST_YANCHOR_KEY,
                StyleKeys.ZOOM_FACTOR,
            };
        }

        @Override
        public Navigator<PlaneAspect> createNavigator( ConfigMap navConfig ) {
            double zoom = navConfig.get( StyleKeys.ZOOM_FACTOR );
            boolean[] navFlags = navConfig.get( NAVAXES_KEY );
            boolean xnav = navFlags[ 0 ];
            boolean ynav = navFlags[ 1 ];
            double xAnchor = navConfig.get( HIST_YANCHOR_KEY ) ? 0.0
                                                               : Double.NaN;
            double yAnchor = navConfig.get( HIST_XANCHOR_KEY ) ? 0.0
                                                               : Double.NaN;
            return new PlaneNavigator( zoom, xnav, ynav, xnav, ynav,
                                       xAnchor, yAnchor );
        }
    }

    /**
     * Characterises how histogram bars are laid out on the plot surface.
     */
    private static class BarState {
        final BinSizer sizer_;
        final boolean isCumulative_;
        final Normalisation norm_;
        BarState( BinSizer sizer, boolean isCumulative, Normalisation norm ) {
            sizer_ = sizer;
            isCumulative_ = isCumulative;
            norm_ = norm;
        }
    }
}

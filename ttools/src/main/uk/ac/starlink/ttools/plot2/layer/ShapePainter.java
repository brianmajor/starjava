package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.paper.Paper;

/**
 * Interface to define the actual shape drawing done by an Outliner.
 * It contains a single method, {@link #paintPoint paintPoint},
 * which is called once for each point.
 *
 * @author   Mark Taylor
 * @since    6 Nov 2015
 */
public interface ShapePainter {

    /**
     * Paints a point given the current state.
     * If the supplied <code>color</code> is non-null,
     * then this painter must take steps to colour its painting.
     * Otherwise, it should use the defaults for the graphics context
     * on which it's painting.
     *
     * <p>Note, iteration over the supplied tuple sequence is done
     * outside of this method, this method should only examine the
     * values as the current position, not call its <code>next</code> method.
     *
     * @param  tseq  tuple sequence positioned at the row of interest
     * @param  color  colour, or null for default
     * @param  paper  graphics destination
     */
    void paintPoint( TupleSequence tseq, Color color, Paper paper );
}

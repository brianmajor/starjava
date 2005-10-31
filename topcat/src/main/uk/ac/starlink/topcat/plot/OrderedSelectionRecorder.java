package uk.ac.starlink.topcat.plot;

import java.util.ArrayList;
import java.util.List;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * ListSelectionListener implementation which can tell you what order 
 * items have been selected in.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Jun 2004
 */
public abstract class OrderedSelectionRecorder
        implements ListSelectionListener {

    private boolean[] lastState_;
    private List orderedSelection_;

    /**
     * Constructs a new recorder with no items selected.
     */
    public OrderedSelectionRecorder() {
        lastState_ = new boolean[ 0 ];
        orderedSelection_ = new ArrayList();
    }

    /**
     * Returns a list of the currently-selected indices in the selection
     * model in the order in which they were (most recently) added to the
     * selection.
     *
     * @return  ordered selection model indices
     */
    public int[] getOrderedSelection() {
        int nsel = orderedSelection_.size();
        int[] sel = new int[ nsel ];
        for ( int i = 0; i < nsel; i++ ) {
            sel[ i ] = ((Integer) orderedSelection_.get( i )).intValue();
        }
        return sel;
    }

    public void valueChanged( ListSelectionEvent evt ) {
        updateState( getModelState( evt.getSource() ) );
    }

    /**
     * Sets the new selection state.
     *
     * @param  state   mask of flags, one true for each selected item
     */
    public void updateState( boolean[] state ) {
        boolean[] oldState = lastState_;
        boolean[] newState = state;
        lastState_ = newState;
        for ( int i = 0; i < Math.max( oldState.length, newState.length );
              i++ ) {
            Integer item = new Integer( i );
            boolean oldFlag = i < oldState.length ? oldState[ i ] : false;
            boolean newFlag = i < newState.length ? newState[ i ] : false;
            if ( ! oldFlag && newFlag ) {
                assert ! orderedSelection_.contains( item );
                orderedSelection_.add( item );
            }
            else if ( oldFlag && ! newFlag ) {
                assert orderedSelection_.contains( item );
                orderedSelection_.remove( item );
            }
            else {
                assert oldFlag == newFlag;
            }
        }
    }

    /**
     * Returns the state of the selection model given the source of a
     * selection event.
     *
     * @param  source  ListSelectionEvent source object
     * @return   mask of flags, one true for each selected item
     */
    protected abstract boolean[] getModelState( Object source );
}

package uk.ac.starlink.topcat;

import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JToggleButton;
import javax.swing.JToggleButton;

/**
 * Provides all information about a toggle button.  This is not only
 * it's current on/off status (selection state in swing talk), but
 * also the button's name, tooltip etc.  Swing doesn't provide a
 * model/action for this, so this class does it instead. 
 * Factory methods are provided to create Swing components that use
 * this as their model.
 *
 * @author   Mark Taylor
 * @since    3 Nov 2005
 */
public class ToggleButtonModel extends JToggleButton.ToggleButtonModel {

    private String text_;
    private Icon icon_;
    private String shortdesc_;
 
    /**
     * Constructor.
     *
     * @param   text  text to be used on buttons etc
     * @param   icon  icon to be used on buttons etc
     * @param   shortdesc  short description to be used for tool tips etc
     */
    public ToggleButtonModel( String text, Icon icon, String shortdesc ) {
        setText( text );
        setIcon( icon );
        setDescription( shortdesc );
    }

    /**
     * Sets the text associated with this model.
     *
     * @param  text  button name
     */
    public void setText( String text ) {
        text_ = text;
    }

    /**
     * Sets the icon associated with this model.
     *
     * @param  icon  button icon
     */
    public void setIcon( Icon icon ) {
        icon_ = icon;
    }

    /**
     * Sets the description (for tooltips etc) associated with this model.
     *
     * @param   shortdesc  description
     */
    public void setDescription( String shortdesc ) {
        shortdesc_ = shortdesc;
    }

    /**
     * Creates and returns a normal button using this model.
     *
     * @return  button
     */
    public JToggleButton createButton() {
        JToggleButton button = new JToggleButton( text_, icon_ );
        button.setModel( this );
        button.setToolTipText( shortdesc_ );
        return button;
    }

    /**
     * Creates and returns a button suitable for use in a toolbar using
     * this model.   The button has no text.
     *
     * @return  button 
     */
    public JToggleButton createToolbarButton() {
        JToggleButton button = createButton();
        button.setText( null );
        return button;
    }

    /**
     * Creates and returns a menu item using this model.
     *
     * @return  checkbox menu item
     */
    public JCheckBoxMenuItem createMenuItem() {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem( text_, icon_ );
        menuItem.setModel( this );
        menuItem.setToolTipText( shortdesc_ );
        return menuItem;
    }

    /**
     * Sets the state of this model.
     * 
     * @param  state  on/off status
     */
    public void setSelected( boolean state ) {
        super.setSelected( state );
    }

    /**
     * Returns the state of this model.
     *
     * @return  on/off status
     */
    public boolean isSelected() {
        return super.isSelected();
    }
}

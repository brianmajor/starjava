package uk.ac.starlink.treeview;

import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;

/** 
 * This interface represents a node suitable for use in a tree viewer.
 * From a data point of view, it represents a node which may have zero
 * or more children - the node must be able to supply its own children
 * on request.  From a GUI point of view it supplies 
 * methods which can be used to represent the node, such as a name, 
 * an icon and perhaps a JComponent displaying fuller information about
 * itself.
 *
 * Implementing classes will normally also supply one or more constructors
 * based on other classes (for instance <code>String</code>, 
 * <code>File</code> or other types of <code>DataNode</code>) -
 * such constructors should throw a <code>NoSuchDataException</code>
 * if the construction fails.
 * When a <code>DataNodeFactory</code> instance is informed of the 
 * existence of this new <code>DataNode</code> implementation, it can
 * try to construct a new object of this type from child nodes generated
 * by this or other classes.  In this way a <code>DataNode</code> of
 * a given type does not need to know all about the kinds of child
 * nodes it can have.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public interface DataNode {

    /** 
     * DataNode representing the notional root of node space.
     * Any node whose parent is <tt>ROOT</tt> name which meaningful without
     * further qualification.
     */
    public static final DataNode ROOT = new DefaultDataNode();

    /**
     * Indicates whether the node can in principle have child nodes.
     * Note this does not actually mean that it has any children.
     *
     * @return   <code>true</code> if the node is of a type which can have
     *           child nodes, <code>false</code> otherwise
     */
    public boolean allowsChildren();

    /**
     * Sets a label for this object.  This is like a name but is not
     * intrinsic to the object, so users of implementing classes may call
     * this method to impose a label of their own.
     *
     * @param  label  the label to be given to this object
     */
    public void setLabel( String label );

    /**
     * Gets an Iterator over the children of the object, each of which 
     * should itself be a <code>DataNode</code>.  Should only be
     * called if <code>hasChildren</code> returns <code>true</code>.
     * It is preferred that this method completes quickly, so if
     * constructing the whole list of children may be time-consuming,
     * implementing classes should avoid constructing the whole list
     * before returning the iterator.
     * <p>
     * Implementing classes should in general follow this strategy when
     * generating children: the class should get the list of children
     * in whatever way is appropriate for the type of node in question.
     * It should then make use of its childMaker (the 
     * {@link DataNodeFactory} returned by the
     * <code>getChildMaker</code> method) 
     * to turn these into <code>DataNode</code> objects to return as
     * the children rather than using a particular constructor, such
     * as one of its own, to generate them.  In this way, children
     * may turn out to be more specific objects of a type known about
     * by <code>DataNodeFactory</code> but not by the implementing class.
     *
     * @return  an <code>Iterator</code> over the children.  Each object
     *          iterated over should be a <code>DataNode</code>.
     *          Behaviour is undefined if this method is called on an
     *          object for which <code>allowsChildren</code> returns 
     *          <code>false</code>.
     */
    public Iterator getChildIterator();

    /**
     * Indicates whether this node has an object it can call a parent.
     * This is regardless of its position in the tree - it should not 
     * keep track of how it was created to be able to answer yes to this.
     *
     * @return   true iff this node can present a parent object
     * @see   #getParentObject
     */
    public boolean hasParentObject();

    /**
     * Returns an object which is in some sense the parent of the one
     * this node is based on.  The parent is <em>not</em> a <tt>DataNode</tt>,
     * it is something which may get fed to a <tt>DataNodeFactory</tt> 
     * to create <tt>DataNode</tt>.
     * This method should only be called if the {@link #hasParentObject}
     * returns <tt>true</tt>.
     *
     * @return  an object which is the parent of this one
     */
    public Object getParentObject();

    /**
     * Gets the label of this object.  This ought to return the same value
     * <code>getName</code> unless the user of the implementing class
     * has previously called <code>setLabel</code> to change it.
     *
     * @return  the label of the object
     */
    public String getLabel();

    /**
     * Gets the name of this object.  This is an intrinsic property of the
     * object.
     *
     * @return  the name of the object
     */
    public String getName();

    /**
     * Returns a short string indicating what kind of node this is.
     * The return value should preferably be a Three Letter Acronym.
     * As a rough guideline the return value should indicate what the
     * implementing class is.
     *
     * @return  an abbreviated description of the type of this 
     *          <code>DataNode</code>
     */
    public String getNodeTLA();

    /**
     * Returns a short sentence indicating what kind of node this is.
     * The return value should be just a few words.  As a rough
     * guideline it should indicate what the implementing class is.
     * 
     * @return  a short description of the type of this <code>DataNode</code>
     */
    public String getNodeType();

    /**
     * Gets a concise description of this object.  The form of the
     * description will depend on
     * the type of node, but it might detail the shape or type of data
     * represented, or otherwise give some information additional to the
     * name.  It should not include the return value of the 
     * <code>getName</code> method, since they may be presented together.
     * It should be on one line, and preferably no longer than around 
     * 70 characters.  The <code>null</code> value may be returned if
     * there is nothing to say.
     *
     * @return  a short string describing this object
     */
    public String getDescription();

    /**
     * Gets a (preferably full) pathname to this node.
     * May return <tt>null</tt> if the pathname is not defined or cannot
     * be determined.
     *
     * @return   pathname to this node
     */
    public String getPath();

    /**
     * Sets the (preferably full) pathname to this node.
     *
     * @param  path  pathname to this node
     */
    public void setPath( String path );

    /**
     * Gets an <code>Icon</code> which can be used when displaying this node.
     * This should return an icon suitable for displaying in a JTree,
     * ideally about 16x16 pixels.  It should give some indication of
     * the type of node.  
     * <p>
     * Implementations are encouraged to construct this icon lazily 
     * (i.e. not do it until this method is called), since using Icons
     * usually causes a large number of Swing classes to be loaded,
     * and in some circumstances (such as treeview -text) these may
     * never be used.
     *
     * @return  an <code>Icon</code> for display
     */
    public Icon getIcon();

    /**
     * Gets the contribution of this node to a pathname.
     * The return value should be the name of this node as it forms part
     * of a path name.  This may or may not be equal to the <tt>name</tt>
     * member variable.  Can be null to indicate the no pathname can
     * be formed from this node.
     *
     * @return   pathname name of this node
     */
    public String getPathElement();

    /**
     * Gets the delimiter string which separates the name of this node from
     * the name of one of its children when constructing a pathname.
     * If <tt>null</tt> is returned it indicates that no pathname 
     * can be formed from this node and one of its children (for instance
     * if it has no children).
     *
     * @return  short delimiter string
     */
    public String getPathSeparator();

    /**
     * Indicates whether there is a more detailed view of this object available.
     *
     * @return  <code>true</code> if a call to <code>getFullView</code> 
     *          may be made to return a <code>JComponent</code> representing
     *          the data in the underlying object, <code>false</code> 
     *          otherwise.
     */
    public boolean hasFullView();

    /**
     * If the <code>hasFullView</code> method returns <code>true</code>,
     * this method returns a <code>JComponent</code> giving a full view 
     * representing this object.
     * This complements the (one-line) representation used by the 
     * tree cell renderer (which in turn probably
     * calls <code>getName</code> and <code>getDescription</code>).
     * The returned JComponent will be displayed in a viewport of
     * limited size, so should be packed in a JScrollPane if it is
     * likely to be large.
     * Behaviour is undefined if this method is called when 
     * <code>hasFullView</code> returns false.
     *
     * @return  a <code>JComponent</code> which gives a detailed, but not 
     *          arbitrarily large, representation of the underlying data.
     *          May be <code>null</code>.
     */
    public JComponent getFullView();

    /**
     * Sets the factory which should in general be used to generate 
     * child nodes.
     * This is not necessarily the factory which is used for generating
     * children of this node, since this node may have special procedures
     * for generating children.  However it is the factory which should 
     * in general be used for creating descendants of this node.
     * <p>
     * This method should only be used by applications which wish to
     * restrict the type of node which can appear in a whole subtree
     * of the node hierarchy.  The childMaker is normally inherited
     * from parent to child, so for instance customising the childMaker
     * of the tree root by removing certain builders will prevent 
     * such nodes from appearing anywhere in the tree.
     *
     * @param  factory  the factory to use for generating children
     */
    public void setChildMaker( DataNodeFactory factory );

    /**
     * Gets the factory which should in general be used to generate 
     * descendant nodes.
     *
     * @return  the factory used for generating children
     */
    public DataNodeFactory getChildMaker();

    /**
     * Stores information about how this node was created.
     * 
     * @param  state  an object encapsulating the means by which this node
     *                was created
     */
    public void setCreator( CreationState state );

    /**
     * Retrieves information about how this node was created.
     *
     * @return  an object encapsulating the means by which this node was
     *          created.  May be null if no information is available
     */
    public CreationState getCreator();
}

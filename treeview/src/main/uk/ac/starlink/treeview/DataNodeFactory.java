package uk.ac.starlink.treeview;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Node;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.URLDataSource;

/**
 * Factory class for constructing {@link DataNode} objects.
 * Instances of this class can be used to construct a <code>DataNode</code>
 * from a generic input - for instance a <code>String</code> or a
 * <code>File</code>.
 * It tries to find the most appropriate existing object of one of the
 * classes which implements <code>DataNode</code>.
 * The classes it knows about, in rough order of preference, are:
 * <ul>
 * <li> {@link NDFDataNode}
 * <li> {@link WCSDataNode}
 * <li> {@link ARYDataNode}
 * <li> {@link HistoryDataNode}
 * <li> {@link HDSDataNode}
 * <li> {@link FITSFileDataNode}
 * <li> {@link NdxDataNode}
 * <li> {@link VOTableDataNode}
 * <li> {@link ZipFileDataNode}
 * <li> {@link TarStreamDataNode}
 * <li> {@link NDArrayDataNode}
 * <li> {@link FITSStreamDataNode}
 * <li> {@link JDBCDataNode}
 * <li> {@link StarTableDataNode}
 * <li> {@link HDXDataNode}
 * <li> {@link XMLDataNode}
 * <li> {@link CompressedDataNode}
 * <li> {@link FileDataNode}
 * <li> {@link PlainDataNode}
 * </ul>
 * The factory will churn out a <code>DataNode</code> object based on
 * the object it is given for construction, the constructors available
 * from the known implementing objects (the above list), and optionally
 * a list of preferences which may be examined and modified using
 * supplied methods.
 * <p>
 * The factory has a list of DataNodeBuilder objects which it uses
 * to try to construct nodes from any given object, be it a filename,
 * string, XML source, or whatever.  The {@link #makeDataNode} method
 * passes the object to each suitable builder to see if it can turn
 * it into a DataNode, and returns the first successful result.
 * Thus the list of DataNodeBuilders and its order determines what kind
 * of DataNode you will get.
 * <p>
 * There are two types of builder in the list.  The first is generated
 * by reflection on a number of DataNode-implementing classes as listed
 * above.  These are made out of suitable (one-argument) constructors
 * supplied by those classes.  The second type is a special one of
 * type {@link DataNodeBuilder}.  This is smart and fast and
 * can make clever decisions about what kind of data node a given file
 * should be turned into.
 * <p>
 * Initially a newly constructed DataNodeFactory has a
 * <tt>FileDataNodeBuilder</tt>,
 * <tt>StringDataNodeBuilder</tt>,
 * <tt>SourceDataNodeBuilder</tt> and
 * <tt>XMLDataNodeBuilder</tt>
 * at the head of the list, followed by
 * ones got from constructors of the known DataNode implementations.
 * This means that a file or string will get tackled first by
 * the clever classes, but if that fails it will trawl through all the
 * other possibilities.
 */
public class DataNodeFactory {

    /* Instance members - make sure these are cloned in the copy constructor. */
    boolean debug;
    private List builders;
    private Set shunnedClasses;

    private static List defaultClassList;
    private static Logger logger = 
        Logger.getLogger( "uk.ac.starlink.treeview" );
    private static Pattern pathPattern = Pattern.compile( "([^ /\\\\:]+)$" );

    /**
     * Constructs a new factory with a default list of node builders.
     */
    public DataNodeFactory() {
        builders = new ArrayList();
        shunnedClasses = new HashSet();
        builders.addAll( getSpecialBuilders() );
        for ( Iterator it = getDefaultClassList().iterator(); it.hasNext(); ) {
            Class clazz = (Class) it.next();
            DataNodeBuilder[] bbatch = DataNodeBuilder.getBuilders( clazz );
            if ( bbatch.length == 0 ) {
                logger.warning( "No builders from class " + clazz );
            }
            builders.addAll( Arrays.asList( bbatch ) );
        }
    }

    /**
     * Copy constructor.  Creates a clone of <tt>orig</tt> which has identical
     * characteristics to it but its own copies of the data structures,
     * so that modifying the resulting factory will not affect the original.
     *
     * @param  orig  the original factory on which to base this one
     */
    public DataNodeFactory( DataNodeFactory orig ) {
        builders = new ArrayList( orig.builders );
        shunnedClasses = new HashSet( orig.shunnedClasses );
        debug = orig.debug;
    }

    /**
     * Ensures that the factory will not generate nodes of a given class.
     * Following this call, calls to {@link #makeDataNode} will not 
     * return any nodes of class <tt>clazz</tt>.  Note this does not
     * affect the construction of classes of subtypes of <tt>clazz</tt>.
     *
     * @param  clazz  the shunned class (presumably a subtype of DataNode)
     */
    public void removeNodeClass( Class clazz ) {

        /* Go through each builder in the list trying to stop doing work
         * which attempts to build nodes of the shunned class. */
        for ( Iterator it = builders.iterator(); it.hasNext(); ) {
            DataNodeBuilder builder = (DataNodeBuilder) it.next();

            /* If it's a builder which is known to build items of the
             * shunned class, just remove it. */
            if ( builder.getNodeClass().equals( clazz ) ) {
                it.remove();
            }
        }

        /* Remember that this class is unwelcome. */
        shunnedClasses.add( clazz );
    }

    /**
     * Sets the class you would most like to see generated by this factory.
     * This raises any constructor-based builders for the named class
     * to the head of the builder list, so if it is possible to build
     * a node of this type, that is what subsequent calls of 
     * {@link #makeDataNode} will do.
     *
     * @param  clazz  the preferred class (presumably a subtype of DataNode)
     */
    public void setPreferredClass( Class clazz ) {

        /* Put together a set of builders based on one-arg constructors
         * which can make nodes of this class. */
        DataNodeBuilder[] cBuilders = DataNodeBuilder.getBuilders( clazz );
        List cbList = Arrays.asList( cBuilders );

        /* Remove any builders which look like the ones we've just come 
         * up with which are already in the list.  The way this is done 
         * isn't absolutely watertight, we might just end up throwing 
         * out the baby with the bathwater, but it's not very likely. */
        Set prefBuilderClasses = new HashSet();
        for ( Iterator it = cbList.iterator(); it.hasNext(); ) {
            prefBuilderClasses.add( ((DataNodeBuilder) it.next()).getClass() );
        }
        for ( Iterator it = builders.iterator(); it.hasNext(); ) {
            DataNodeBuilder builder = (DataNodeBuilder) it.next();
            if ( builder.getNodeClass().equals( clazz ) &&
                 prefBuilderClasses.contains( builder.getClass() ) ) {
                it.remove();
            }
        }

        /* Add the preferred builders to the top of the list. */
        builders.addAll( 0, cbList );
    }

    /**
     * Returns the list of {@link DataNodeBuilder}s which this factory uses to
     * construct data nodes.  This may be modified by code which reckons
     * it knows what it's doing, but beware that by modifying this 
     * list in strange ways the behaviour of this factory may be
     * compromised.  In particular, don't put anything in here which
     * is not a <tt>DataNodeBuilder</tt> object.
     *
     * @return   a mutable list of {@link DataNodeBuilder} objects
     */
    public List getBuilders() {
        return builders;
    }

    /**
     * Generates a new DataNode from a given object.
     * It goes through this factory's list of builder objects and
     * tries each one with the given object until one of the builders
     * can turn it into a DataNode.
     * <p>
     * Ideally, all data node construction (except perhaps the root of
     * a tree) should be done using this method, since it keeps track
     * of various data structures which invoking constructors
     * directly may not.
     *
     * @param  parent  the DataNode whose child the new node will be in
     *          the node hierarchy.  May be <tt>null</tt> for a hierarchy root
     * @param  obj  an object which is to be turned into a DataNode by 
     *          one of the builders
     * @return  a new DataNode object based on <tt>obj</tt>
     * @throws  NoSuchDataException  if none of the builders in the list
     *          could turn <tt>obj</tt> into a <tt>DataNode</tt>
     */
    public DataNode makeDataNode( DataNode parent, Object obj )
            throws NoSuchDataException {

        /* Try to get a new node using one of the builders. */
        Class objClass = obj.getClass();
        List tried = new ArrayList();
        DataNode newNode = null;
        DataNodeBuilder successfulBuilder = null;
        StringBuffer trace = null;
        if ( debug ) {
            trace = new StringBuffer()
                   .append( "Object: " )
                   .append( obj )
                   .append( '\n' )
                   .append( "Class: " )
                   .append( objClass.getName() )
                   .append( '\n' );
        }
        for ( Iterator it = builders.iterator(); 
              it.hasNext() && newNode == null; ) {
            DataNodeBuilder builder = (DataNodeBuilder) it.next();
            if ( builder.suitable( objClass ) ) {
                if ( debug ) {
                    trace.append( "\nBuilder: " )
                         .append( builder )
                         .append( '\n' );
                }
                tried.add( builder );
                try {
                    DataNode node = builder.buildNode( obj );
                    if ( ! shunnedClasses.contains( node.getClass() ) ) {
                        newNode = node;
                        if ( debug ) {
                            trace.append( "   SUCCESS (" )
                                 .append( newNode.getClass().getName() )
                                 .append( ")\n" );
                        }
                        successfulBuilder = builder;
                    } 
                    else {
                        if ( debug ) {
                            trace.append( "   Class " )
                                 .append( node.getClass().getName() )
                                 .append( " shunned.\n" );
                        }
                    }
                }
                catch ( NoSuchDataException e ) {
                    if ( debug ) {
                        trace.append( "   " )
                             .append( e.getMessage() )
                             .append( '\n' );
                        StackTraceElement[] frames = e.getStackTrace();
                        for ( int i = 0; i < frames.length; i++ ) {
                            trace.append( "      " ) 
                                 .append( frames[ i ] )
                                 .append( "\n" );
                        }
                    }
                }
            }
        }

        /* Dropped off the end of the loop - no success, explain what 
         * happened. */
        if ( newNode == null ) {
            StringBuffer msg = new StringBuffer()
                .append( "No DataNode could be constructed from " )
                .append( obj )
                .append( " of class " )
                .append( objClass.getName() )
                .append( "\n" )
                .append( "Tried:\n" );
            for ( Iterator it = tried.iterator(); it.hasNext(); ) {
                msg.append( "    " )
                   .append( it.next() )
                   .append( "\n" );
            }
            throw new NoSuchDataException( msg.toString() );
        }

        /* We have successfully created a new node.  Do some additional
         * configuration before returning it to the caller. */
        assert newNode != null;
        assert successfulBuilder != null;
        configureDataNode( newNode, parent, obj );
        CreationState creator = newNode.getCreator();
        creator.setFactory( this );
        creator.setBuilder( successfulBuilder );
        if ( debug ) {
            creator.setFactoryTrace( trace.toString() );
        }
        return newNode;
    }

    /**
     * Performs some of the desirable configuration on a new DataNode which
     * is about to get inserted into the tree.  This method is called
     * by {@link #makeDataNode} and in most cases should
     * not be called by nodes creating children.  However, if a node
     * is creating children other than using <tt>makeDataNode</tt>
     * (for instance because their constructors are not suitable for
     * the generic node creation system on which this class is based)
     * then this method should be called on the new child before it is
     * returned from the child iterator.  Configuring new-born nodes
     * using this method is not essential, but it is likely to ensure
     * that the node is as far as possible a well-behaved member of
     * the node tree; not doing it can lead to some impairment of
     * functionality for the nodes in question.
     *
     * @param   the new node to configure
     * @param   parentNode   <tt>node</tt>'s parent data node 
     *          (may be <tt>null</tt>) if it's at the top of the tree)
     * @param   the object on which <tt>node</tt> is based 
     *          (may be <tt>null</tt>) if nothing suitable applies
     */
    public void configureDataNode( DataNode node, DataNode parentNode, 
                                   Object obj ) {

        /* Inherit child node creation factory from parent. */
        node.setChildMaker( parentNode == null ? new DataNodeFactory()
                                               : parentNode.getChildMaker() );

        /* Set up information about how the node was created. */
        node.setCreator( new CreationState( parentNode, obj ) );

        /* Try to determine some additional information about the node
         * based on some generic base object types. */
        Object parentObj = null;
        String label = null;
        if ( obj instanceof FileDataSource ) {
            File file = ((FileDataSource) obj).getFile();
            label = file.getName();
            parentObj = file.getAbsoluteFile().getParent();
        }
        else if ( obj instanceof File ) {
            File file = (File) obj;
            label = file.getName();
            parentObj = file.getAbsoluteFile().getParent();
        }
        else if ( obj instanceof DOMSource ) {
            DOMSource dsrc = (DOMSource) obj;
            String sysid = dsrc.getSystemId();
            Node pnode = dsrc.getNode().getParentNode();
            if ( pnode != null ) {
                parentObj = new DOMSource( pnode, sysid );
            }
            else if ( sysid != null && sysid.trim().length() > 0 ) {
                parentObj = sysid;
            }
        }
            
        /* Get a suitable label from a source name if we have one.  The format
         * of a DataSource name is not defined, but it may be some sort of
         * path - try to pick the last element of it. */
        if ( label == null && obj instanceof DataSource ) {
            String name = ((DataSource) obj).getName();
            if ( name != null ) {
                Matcher match = pathPattern.matcher( name );
                if ( match.lookingAt() ) {
                    label = match.group( 1 );
                }
            }
        }

        if ( parentObj != null && node.getParentObject() == null ) {
            node.setParentObject( parentObj );
        }
        if ( label != null && node.getLabel() == null ) {
            node.setLabel( label );
        }
    }

    /**
     * Makes a DataNode from a Throwable.  This behaves the same as
     * <tt>makeDataNode</tt> but for convenience it doesn't throw a
     * NoSuchDataException, since it can guarantee to make a DataNode
     * from the throwable.
     *
     * @param  th  the Throwable object from which to construct the node
     * @param  parent   the DataNode whose child this is
     * @param  a DataNode (probably an ErrorDataNode) representing <tt>th</tt>
     */
    public DataNode makeErrorDataNode( DataNode parent, Throwable th ) {
        try {
            return makeDataNode( parent, th );
        }
        catch ( NoSuchDataException e ) {
            return new ErrorDataNode( th );
        }
    }

    /**
     * Convenience method which invokes {@link #makeDataNode} but does not
     * throw a <tt>NoSuchDataException</tt>.  If the node construction
     * fails, then {@link #makeErrorDataNode} will be called to construct
     * the node for return instead.
     *
     * @param  parent  the DataNode whose child the new node will be in
     *          the node hierarchy.  May be <tt>null</tt> for a hierarchy root
     * @param  obj  an object which is to be turned into a DataNode by 
     *          one of the builders
     * @return  a new DataNode object based on <tt>obj</tt>
     */
    public DataNode makeChildNode( DataNode parent, Object obj ) {
        try {
            return makeDataNode( parent, obj );
        }
        catch ( NoSuchDataException e ) {
            return makeErrorDataNode( parent, e );
        }
    }

    /**
     * Returns a string representation of this factory.
     * The returned string is a comprehensive list of which constructors
     * will be tried.
     *
     * @return  an ordered list of the constructors that the
     *          <code>makeDataNode</code> will try to use.
     */
    public String toString() {
        StringBuffer buf =
            new StringBuffer( "DataNodeFactory with builders:\n" );
        for ( Iterator bit = builders.iterator(); bit.hasNext(); ) {
            buf.append( "    " )
               .append( bit.next().toString() )
               .append( '\n' );
        }
        return buf.toString();
    }

    /**
     * Returns a list of builders which, for a default DataNodeFactory,
     * are invoked before any of the constructor-based ones.
     * These allow cleverer decisions to be made about what kind of
     * node is generated from what object: if a given object
     * could make either a FooDataNode or a BarDataNode but would be
     * better off as a BarDataNode, this can be done even if FooDataNode
     * was higher in the class preference list for this factory.
     * For the default configuration, this list should come first in
     * the list of builders, if the user reconfigures the preferred
     * class list it may slip lower down or disappear from the hierarchy.
     * <p>
     * This method is called in the constructor to initialise the
     * factory's default state.
     *
     * @return  a list of {@link DataNodeBuilder} objects 
     */
    public static List getSpecialBuilders() {
        List specials = new ArrayList( 4 );
        specials.add( FileDataNodeBuilder.getInstance() );
        specials.add( StringDataNodeBuilder.getInstance() );
        specials.add( SourceDataNodeBuilder.getInstance() );
        specials.add( XMLDataNodeBuilder.getInstance() );
        return specials;
    }

    /**
     * Returns the default class list from which the list of 
     * constructor-based builders is initialised.
     * <p>
     * This method is called in the constructor to initialise the
     * factory's default state.
     *
     * @return  a list of {@link Class} objects representing concrete 
     *          implementations of the {@link DataNode} interface
     *          
     */
    public static List getDefaultClassList() {
        if ( defaultClassList == null ) {
            Class[] classes = new Class[] {
                NDFDataNode.class,
                WCSDataNode.class,
                ARYDataNode.class,
                HistoryDataNode.class,
                HDSDataNode.class,
                FITSFileDataNode.class,
                NdxDataNode.class,
                VOTableDataNode.class,
                ZipFileDataNode.class,
                TarStreamDataNode.class,
                NDArrayDataNode.class,
                FITSStreamDataNode.class,
                JDBCDataNode.class,
                StarTableDataNode.class,
                HDXDataNode.class,
                XMLDataNode.class,
                VOTableTableDataNode.class,
                VOComponentDataNode.class,
                CompressedDataNode.class,
                FileDataNode.class,
                PlainDataNode.class,
            };
            defaultClassList = new ArrayList( Arrays.asList( classes ) );

            /* Some of the classes are contingent on having working HDS and
             * AST subsystems available, which may not be the case if the
             * corresponding native libraries are not present.  Remove classes
             * we know that we will not be able to deal with. */
            if ( ! TreeviewUtil.hasHDS() ) {
                defaultClassList.remove( NDFDataNode.class );
                defaultClassList.remove( ARYDataNode.class );
                defaultClassList.remove( HistoryDataNode.class );
                defaultClassList.remove( HDSDataNode.class );
            }
            if ( ! TreeviewUtil.hasAST() ) {
                defaultClassList.remove( WCSDataNode.class );
            }
        }
        return defaultClassList;
    }

}

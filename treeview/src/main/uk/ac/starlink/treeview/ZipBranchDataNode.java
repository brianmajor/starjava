package uk.ac.starlink.treeview;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import javax.swing.JComponent;

public class ZipBranchDataNode extends DefaultDataNode {

    private final ZipArchiveDataNode ziparchivenode;
    private final ZipEntry zipentry;
    private final String path;
    private String name;
    private JComponent fullView;

    /**
     * Constructs a ZipBranchDataNode from a ZipEntry and ZipArchive.
     *
     * @param  ziparchivenode  DataNode representing the zip file within which
     *         this entry lives
     * @param  entry  the ZipEntry object represented by this node
     */
    public ZipBranchDataNode( ZipArchiveDataNode ziparchivenode,
                              ZipEntry entry ) {
        this.ziparchivenode = ziparchivenode;
        this.zipentry = entry;
        this.path = entry.getName();
        this.name = path.substring( path.substring( 0, path.length() - 1 ) 
                                   .lastIndexOf( '/' ) + 1 );
        if ( name.endsWith( "/" ) ) {
            name = name.substring( 0, name.length() - 1 );
        }
        setLabel( name );
        setIconID( IconFactory.ZIPBRANCH );
    }

    public String getName() {
        return name;
    }

    public String getPathSeparator() {
        return "/";
    }

    public String getNodeTLA() {
        return "ZPD";
    }

    public String getNodeType() {
        return "Directory in Zip archive";
    }

    public boolean allowsChildren() {
        return true;
    }

    public Iterator getChildIterator() {
        try {
            return ziparchivenode.getChildIteratorAtLevel( path, this );
        }
        catch ( IOException e ) {
            return Collections.singleton( makeErrorChild( e ) ).iterator();
        }
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            try {
                int nent = ziparchivenode.getEntriesAtLevel( path ).size();
                dv.addSeparator();
                dv.addKeyedItem( "Number of entries", nent );
            }
            catch ( IOException e ) {
                // too bad
            }
        }
        return fullView;
    }

}

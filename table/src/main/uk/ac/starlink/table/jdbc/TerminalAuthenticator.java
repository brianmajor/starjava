package uk.ac.starlink.table.jdbc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Provides JDBC authentication using the terminal; assumes that someone
 * is sitting at <tt>System.in</tt>/<tt>System.out</tt>.
 */
public class TerminalAuthenticator implements JDBCAuthenticator {

    public String[] authenticate() throws IOException {
        BufferedReader rdr = 
            new BufferedReader( new InputStreamReader( System.in ) );
        System.out.print( "JDBC User: " );
        String user = rdr.readLine();
        String password = getMaskedString( "JDBC Password: " );
        return new String[] { user, password };
    }

    private String getMaskedString( String prompt ) throws IOException {
        StringBuffer sbuf = new StringBuffer();
        int linesep0 = (int) System.getProperty( "line.separator" ).charAt( 0 );
        Thread masker = new MaskingThread( prompt );
        masker.start();
        try {
            while ( true ) {
                int c = System.in.read();
                if ( c == linesep0 || c < 0 ) {
                    masker.interrupt();
                    if ( c < 0 ) {
                        System.out.println();
                    }
                    break;
                }
                sbuf.append( c );
            }
        }
        catch ( IOException e ) {
            masker.interrupt();
            System.out.println();
            throw e;
        }
        return sbuf.toString();
    }

    private static class MaskingThread extends Thread {
        private String out;
        public MaskingThread( String prompt ) {
            out = '\r' + prompt + " \r" + prompt;
        }
        public void run() {
            while ( ! interrupted() ) {
                System.out.print( out );
                System.out.flush();
                try {
                    this.sleep( 1 );
                }
                catch ( InterruptedException e ) {
                    interrupt();
                }
            }
        }
    }
}

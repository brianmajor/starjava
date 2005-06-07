package uk.ac.starlink.ttools.func;

import java.util.Random;
import uk.ac.starlink.pal.Pal;
import uk.ac.starlink.pal.mjDate;
import uk.ac.starlink.pal.palError;
import uk.ac.starlink.util.TestCase;

public class TimesTest extends TestCase {

    private static Random RANDOM = new Random( 1234567L );
    private static int NTEST = 10000;

    public TimesTest( String name ) {
        super( name );
    }

    public void testFixed() {
        assertEquals( 53303.5, Times.isoToMjd( "2004-10-25T12:00:00.00" ) );
        assertEquals( 53303.5, Times.isoToMjd( "2004-10-25T12:00:00." ) );
        assertEquals( 53303.5, Times.isoToMjd( "2004-10-25T12:00:00" ) );
        assertEquals( 53303.5, Times.isoToMjd( "2004-10-25 12:00:00" ) );
        assertEquals( 53303.5, Times.isoToMjd( "2004-10-25T12:00" ) );
        assertEquals( 53303.5, Times.isoToMjd( "2004-10-25T12" ) );
        assertEquals( 53303.0, Times.isoToMjd( "2004-10-25" ) );

        assertEquals( "2004-10-25T12:00:00", Times.mjdToIso( 53303.5 ) );
        assertEquals( "18:00:00", Times.mjdToTime( -0.25 ) );
        assertEquals( "1858-11-17", Times.mjdToDate( 0.1 ) );
    } 

    public void testFormat() {
        double frac;
        double milliTolerance = 1.0 / ( 24 * 60 * 60 * 1000 );
        double secTolerance = 1.0 / ( 24 * 60 * 60 );
        String mfmt = "yyyy-MM-dd'T'HH:mm:ss.SSS";
        for ( int i = 0; i < NTEST; i++ ) {
            double mjd = 1e5 * rnd();
            assertEquals( mjd, Times.isoToMjd( Times.mjdToIso( mjd ) ), 
                          secTolerance );
            assertEquals( mjd, Times.isoToMjd( Times.formatMjd( mjd, mfmt ) ),
                          milliTolerance );
            assertEquals( mjd, Times.dateToMjd( Times.mjdYear( mjd ),
                                                Times.mjdMonth( mjd ),
                                                Times.mjdDayOfMonth( mjd ),
                                                Times.mjdHour( mjd ),
                                                Times.mjdMinute( mjd ),
                                                Times.mjdSecond( mjd ) ),
                          milliTolerance );
            assertEquals( Times.mjdToIso( mjd ), Times.mjdToDate( mjd ) + 
                                                 "T" +
                                                 Times.mjdToTime( mjd ) );
        }
    }

    public void testPalCmp() throws palError {
        Pal pal = new Pal();
        for ( int i = 0; i < NTEST; i++ ) {
            double mjd = 1e5 * rnd();
            mjDate palMjd = pal.Djcal( mjd );
            assertEquals( (int) mjd, Times.dateToMjd( palMjd.getYear(), 
                                                      palMjd.getMonth() - 1, 
                                                      palMjd.getDay() ) );
        }
    }

    public void testBlanks() {
        assertNull( Times.mjdToIso( Double.NaN ) );
        assertNull( Times.mjdToDate( Double.NaN ) );
        assertNull( Times.mjdToTime( Double.NaN ) );
        assertTrue( Double.isNaN( Times.isoToMjd( "" ) ) );
        assertTrue( Double.isNaN( Times.isoToMjd( null ) ) );
        try {
            Times.isoToMjd( "not-an-iso8601-epoch" );
            fail();
        }
        catch ( IllegalArgumentException e ) {
        }
    }

    private static double rnd() {
        return RANDOM.nextDouble();
    }

}

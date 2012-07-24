/* ********************************************************
 * This file automatically generated by RateMap.pl.
 *                   Do not edit.                         *
 **********************************************************/

package uk.ac.starlink.ast;


/**
 * Java interface to the AST RateMap class
 *  - mapping which represents differentiation. 
 * A RateMap is a Mapping which represents a single element of the
 * Jacobian matrix of another Mapping. The Mapping for which the
 * Jacobian is required is specified when the new RateMap is created, 
 * and is referred to as the "encapsulated Mapping" below.
 * <p>
 * The number of inputs to a RateMap is the same as the number of inputs 
 * to its encapsulated Mapping. The number of outputs from a RateMap
 * is always one. This one output equals the rate of change of a
 * specified output of the encapsulated Mapping with respect to a
 * specified input of the encapsulated Mapping (the input and output 
 * to use are specified when the RateMap is created).
 * <p>
 * A RateMap which has not been inverted does not define an inverse 
 * transformation. If a RateMap has been inverted then it will define
 * an inverse transformation but not a forward transformation.
 * <h4>Licence</h4>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public Licence as
 * published by the Free Software Foundation; either version 2 of
 * the Licence, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be
 * useful,but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public Licence for more details.
 * <p>
 * You should have received a copy of the GNU General Public Licence
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place,Suite 330, Boston, MA
 * 02111-1307, USA
 * 
 * 
 * @see  <a href='http://star-www.rl.ac.uk/cgi-bin/htxserver/sun211.htx/?xref_RateMap'>AST RateMap</a>  
 */
public class RateMap extends Mapping {
    /** 
     * Create a RateMap.   
     * @param  map  Pointer to the encapsulated Mapping.
     * 
     * @param  ax1  Index of the output from the encapsulated Mapping for which the 
     * rate of change is required. This corresponds to the delta
     * quantity forming the numerator of the required element of the 
     * Jacobian matrix. The first axis has index 1.
     * 
     * @param  ax2  Index of the input to the encapsulated Mapping which is to be
     * varied. This corresponds to the delta quantity forming the 
     * denominator of the required element of the Jacobian matrix.
     * The first axis has index 1.
     * 
     * @throws  AstException  if an error occurred in the AST library
    */
    public RateMap( Mapping map, int ax1, int ax2 ) {
        construct( map, ax1, ax2 );
    }
    private native void construct( Mapping map, int ax1, int ax2 );

}

/* ==========================================
 * JGraphT : a free Java graph-theory library
 * ==========================================
 *
 * Project Info:  http://jgrapht.sourceforge.net/
 * Project Creator:  Barak Naveh (http://sourceforge.net/users/barak_naveh)
 *
 * (C) Copyright 2003-2008, by Barak Naveh and Contributors.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */
/* ------------------
 * StringNameProvider.java
 * ------------------
 * (C) Copyright 2005-2008, by Trevor Harmon.
 *
 * Original Author:  Trevor Harmon
 *
 */
package edu.nd.nina.ext;

/**
 * Generates edge names by invoking {@link #toString()} on them. This assumes
 * that the edge's {@link #toString()} method returns a unique String
 * representation for each edge.
 *
 * @author Trevor Harmon
 */
public class StringEdgeNameProvider<E>
    implements EdgeNameProvider<E>
{
    //~ Constructors -----------------------------------------------------------

    public StringEdgeNameProvider()
    {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Returns the String representation an edge.
     *
     * @param edge the edge to be named
     */
    public String getEdgeName(E edge)
    {
        return edge.toString();
    }
}

// End StringEdgeNameProvider.java
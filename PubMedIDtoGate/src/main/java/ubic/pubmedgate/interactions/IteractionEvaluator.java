/*
 * The WhiteText project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubic.pubmedgate.interactions;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.connection.Connection;
import ubic.pubmedgate.ConnectionsDocument;

public class IteractionEvaluator {
    private static Log log = LogFactory.getLog( Cooccurance.class.getName() );

    // compares connections to a set of connections to see how they match up
    // check direction - boolean
    boolean checkDirection;

    public IteractionEvaluator() {
        checkDirection = true;
    }

    public Map<String, String> compare( ConnectionList aList, ConnectionList bList, boolean usePositions ) {
        Map<String, String> results = new HashMap<String, String>();
        results.put( "A Name", aList.getName() );
        results.put( "B Name", bList.getName() );
        results.put( "A size", aList.size() + "" );
        results.put( "B size", bList.size() + "" );
        results.put( "usePositions", usePositions + "" );

        int overlapDirect = 0;
        int overlapUndirected = 0;
        for ( Connection a : aList ) {
            boolean undirected = false;
            ConnectionsDocument aDoc = aList.getDocFromConnection( a );
            if ( usePositions && bList.containsByPartners( a, undirected, aDoc ) ) {
                overlapDirect++;
            }
            if ( !usePositions && bList.containsByText( a, undirected, aDoc ) ) {
                overlapDirect++;
            }
            undirected = true;
            if ( usePositions && bList.containsByPartners( a, true, aDoc ) ) {
                overlapUndirected++;
            }
            if ( !usePositions && bList.containsByText( a, true, aDoc ) ) {
                overlapUndirected++;
            }
        }
        results.put( "Directed overlap", overlapDirect + "" );
        results.put( "Undirected overlap", overlapUndirected + "" );
        // duplicates?
        return results;

    }

    // need to go by PMIDs
    public List<Connection> getMissingConnections( ConnectionList aList, ConnectionList bList ) {
        List<Connection> result = new LinkedList<Connection>();
        for ( Connection a : aList ) {
            ConnectionsDocument aDoc = aList.getDocFromConnection( a );

            boolean undirected = true;
            if ( !bList.containsByPartners( a, undirected, aDoc ) ) {
                result.add( a );
            }
        }
        return result;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // TODO Auto-generated method stub

    }

}

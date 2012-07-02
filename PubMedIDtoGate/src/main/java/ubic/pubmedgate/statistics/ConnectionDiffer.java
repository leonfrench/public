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

package ubic.pubmedgate.statistics;

import gate.Annotation;
import gate.Document;
import gate.FeatureMap;
import gate.util.AnnotationDiffer;
import gate.util.AnnotationDiffer.PairingImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import ubic.connection.Connection;

public class ConnectionDiffer {
    private String annotatorA;

    private String annotatorB;

    public ConnectionDiffer( String annotatorA, String annotatorB ) {
        this.annotatorA = annotatorA;
        this.annotatorB = annotatorB;
    }

    /*
     * given two documents find how many connections are shared, using the gate annotation matching
     */
    public Map<Connection, Connection> getMatches( Document doc, boolean lenient ) {
        Map<Connection, Connection> result = new HashMap<Connection, Connection>();
        FeatureMap fMap = doc.getFeatures();
        List<Connection> connections = Connection.getConnections( fMap );
        List<Connection> connectionsA = new CopyOnWriteArrayList<Connection>();
        List<Connection> connectionsB = new CopyOnWriteArrayList<Connection>();

        if ( connections == null ) {
            return new HashMap<Connection, Connection>();
        }
        for ( Connection c : connections ) {
            if ( c.getAuthor().equalsIgnoreCase( annotatorA ) ) {
                connectionsA.add( c );
            }
            if ( c.getAuthor().equalsIgnoreCase( annotatorB ) ) {
                connectionsB.add( c );
            }
        }

        // no matches if one is empty
        if ( connectionsA.size() == 0 || connectionsB.size() == 0 ) {
            return new HashMap<Connection, Connection>();
        }
        // get the annotation mapping
        AnnotationDiffer annDiffer = new AnnotationDiffer();

        List diffResults = annDiffer.calculateDiff( doc.getAnnotations( annotatorA ), doc.getAnnotations( annotatorB ) );

        Map<Annotation, Annotation> correctPairs = new HashMap<Annotation, Annotation>();
        for ( Object o : diffResults ) {
            PairingImpl pairing = ( PairingImpl ) o;
            int type = pairing.getType();
            if ( type == AnnotationDiffer.CORRECT_TYPE || type == AnnotationDiffer.PARTIALLY_CORRECT_TYPE ) {
                correctPairs.put( pairing.getKey(), pairing.getResponse() );
            }
        }

        for ( Connection cA : connectionsA ) {
            // if the connections match before mapping
            Annotation cAPartnerA = cA.getPartnerA();
            Annotation cAPartnerB = cA.getPartnerB();
            // after mapping
            cAPartnerA = correctPairs.get( cAPartnerA );
            cAPartnerB = correctPairs.get( cAPartnerB );

            // if there is a mapping then continue
            if ( cAPartnerA != null && cAPartnerB != null ) {
                for ( Connection cB : connectionsB ) {
                    Annotation cBPartnerA = cB.getPartnerA();
                    Annotation cBPartnerB = cB.getPartnerB();
                    // if they match or symetrically match
                    if ( lenient && ( cAPartnerA.coextensive( cBPartnerA ) && cAPartnerB.coextensive( cBPartnerB ) )
                            || ( ( cAPartnerA.coextensive( cBPartnerB ) ) && cAPartnerB.coextensive( cBPartnerA ) ) ) {
                        // its a match
                        result.put( cA, cB ); // System.out.println("one");
                    } // end match if
                } // end b connection for
            } // end checking for mapping
        }
        return result;
    }

}

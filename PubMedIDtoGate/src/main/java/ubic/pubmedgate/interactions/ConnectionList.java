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

import gate.Annotation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.connection.Connection;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.interactions.predicates.GeneratePredicateSpreadSheet;
import ubic.pubmedgate.interactions.predicates.InteractionPredicateNegationReader;

public class ConnectionList extends LinkedList<Connection> {
    private static Log log = LogFactory.getLog( ConnectionList.class.getName() );
    static InteractionPredicateNegationReader negationFilter;
    static {
        try {
            negationFilter = new InteractionPredicateNegationReader();
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    // remove symetric connections
    // count connections
    String name;

    // connection to document map
    Map<Connection, ConnectionsDocument> conToDocMap;

    public ConnectionList( String name ) {
        super();
        this.name = name;
        conToDocMap = new HashMap<Connection, ConnectionsDocument>();
    }

    public void addAllFromDocument( ConnectionsDocument doc ) {
        List<Connection> conList = doc.getConnections( name );
        if ( conList == null ) return;
        for ( Connection connection : conList ) {
            String predicate = GeneratePredicateSpreadSheet.getPredicateString( doc, connection );
            if ( negationFilter.filter( predicate ) ) {
                log.info( "Rejected predicate:" + predicate );
            } else {
                add( connection, doc );
            }
        }
    }

    public int getSize() {
        return size();
    }

    public ConnectionsDocument getDocFromConnection( Connection c ) {
        return conToDocMap.get( c );
    }

    public void add( Connection connection, ConnectionsDocument doc ) {
        add( connection );
        conToDocMap.put( connection, doc );
    }

    public ConnectionList( List<Connection> list, String name ) {
        super( list );
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void removeSymetrics() {
        ConnectionList toRemove = new ConnectionList( "remove" );
        for ( int i = 0; i < this.size(); i++ ) {
            for ( int j = 0; j < this.size(); j++ ) {
                if ( i > j ) {
                    // TODO add check for same document?

                    Connection a = this.get( i );
                    Connection b = this.get( j );

                    Annotation aA = a.getPartnerA();
                    Annotation bA = a.getPartnerB();

                    Annotation aB = b.getPartnerA();
                    Annotation bB = b.getPartnerB();

                    if ( aA.coextensive( bB ) && bA.coextensive( aB ) ) {
                        toRemove.add( a );
                    }
                }
            }
        }
        if ( toRemove.size() != 0 ) {
            log.info( "Removed " + toRemove.size() + " connections that were bidirectional duplicates" );
            log.info( "Before:" + this.size() );
            this.removeAll( toRemove );
            log.info( "After:" + this.size() );
        }
        // this removes too many!
    }

    public void removeSymetric( Connection removeMe, ConnectionsDocument doc ) {
        Connection target = findSymetric( removeMe, doc );
        if ( target != null ) remove( target );
    }

    private Connection findSymetric( Connection removeMe, ConnectionsDocument doc ) {
        Connection switched = new Connection( null, removeMe.getPartnerB(), removeMe.getPartnerA(), ( String ) null );
        Connection target = findByPartners( switched, doc );
        return target;
    }

    // TODO
    // public void containsDuplicates() {
    //
    // }

    public Connection findByPartners( Connection query, ConnectionsDocument doc ) {
        Annotation queryPartnerA = query.getPartnerA();
        Annotation queryPartnerB = query.getPartnerB();
        if ( queryPartnerA == null ) {
            log.info( "queryPartnerA is null, is the annotatset in the corpus?" );
        }
        if ( queryPartnerB == null ) {
            log.info( "queryPartnerB is null, is the annotatset in the corpus?" );
        }

        for ( Connection connection : this ) {
            ConnectionsDocument testDocument = conToDocMap.get( connection );
            if ( !doc.equals( testDocument ) ) continue;

            Annotation partnerA = connection.getPartnerA();
            Annotation partnerB = connection.getPartnerB();

            // log.info(partnerA.toString());
            if ( partnerA == null ) {
                log.info( "partnerA is null, is the annotatset in the corpus? " + getName() );
            }
            if ( partnerB == null ) {
                log.info( "partnerB is null, is the annotatset in the corpus? " + getName() );
            }

            // allows same positions in different documents, erg
            if ( partnerA.coextensive( queryPartnerA ) && partnerB.coextensive( queryPartnerB ) ) {
                return connection;
            }

        }
        return null;
    }

    /**
     * Code reuse from findbypartners
     * 
     * @param query
     * @param doc
     * @return
     */
    public Connection findByText( Connection query, ConnectionsDocument doc ) {
        Annotation queryPartnerA = query.getPartnerA();
        Annotation queryPartnerB = query.getPartnerB();

        String queryPartnerAText = doc.getAnnotationText( queryPartnerA );
        String queryPartnerBText = doc.getAnnotationText( queryPartnerB );

        if ( queryPartnerA == null ) {
            log.info( "queryPartnerA is null, is the annotatset in the corpus?" );
        }
        if ( queryPartnerB == null ) {
            log.info( "queryPartnerB is null, is the annotatset in the corpus?" );
        }

        for ( Connection connection : this ) {
            ConnectionsDocument testDocument = conToDocMap.get( connection );
            if ( !doc.equals( testDocument ) ) continue;

            Annotation partnerA = connection.getPartnerA();
            Annotation partnerB = connection.getPartnerB();

            String partnerAText = doc.getAnnotationText( partnerA );
            String partnerBText = doc.getAnnotationText( partnerB );

            // log.info(partnerA.toString());
            if ( partnerA == null ) {
                log.info( "partnerA is null, is the annotatset in the corpus? " + getName() );
            }
            if ( partnerB == null ) {
                log.info( "partnerB is null, is the annotatset in the corpus? " + getName() );
            }

            // allows same positions in different documents, erg
            if ( partnerAText.equals( queryPartnerAText ) && partnerBText.equals( queryPartnerBText ) ) {
                return connection;
            }
        }
        return null;
    }

    public boolean containsByPartners( Connection query, ConnectionsDocument doc ) {
        boolean undirected = false;
        return containsByPartners( query, undirected, doc );
    }

    public boolean containsByText( Connection query, boolean undirected, ConnectionsDocument doc ) {
        if ( findByText( query, doc ) != null ) return true;

        if ( undirected ) {
            Connection switched = new Connection( null, query.getPartnerB(), query.getPartnerA(), ( String ) null );
            if ( findByText( switched, doc ) != null ) return true;
        }
        return false;
    }

    public boolean containsByPartners( Connection query, boolean undirected, ConnectionsDocument doc ) {
        if ( findByPartners( query, doc ) != null ) return true;

        if ( undirected ) {
            Connection switched = new Connection( null, query.getPartnerB(), query.getPartnerA(), ( String ) null );
            if ( findByPartners( switched, doc ) != null ) return true;
        }
        return false;
    }

}

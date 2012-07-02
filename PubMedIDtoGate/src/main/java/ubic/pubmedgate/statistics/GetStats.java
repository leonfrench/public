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
import gate.AnnotationSet;
import gate.Document;
import gate.FeatureMap;
import gate.Node;
import gate.corpora.DocumentContentImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ubic.basecode.dataStructure.CountingMap;
import ubic.connection.Connection;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.loader.PubMedIDtoGate;

public class GetStats {
    /**
     * Retrives annotation statistics for an entire gate datastore
     * 
     * @param args
     * @version $Id:
     */
    GateInterface p2g;

    public GetStats() throws Exception {
        p2g = new PubMedIDtoGate();
    }

    public GetStats( GateInterface p2g ) {
        this.p2g = p2g;
    }

    public void printStats( boolean verbose ) {
        printStats( null, verbose );
    }

    // given an Annotation return the string it covers
    /*
     * public String getAnnotationText( Annotation ann ) { int start = ann.getStartNode().getOffset().intValue(); int
     * offset = ann.getEndNode().getOffset().intValue() - start; try { ann.get return textPane.getText( start, offset ); }
     * catch ( BadLocationException e ) { e.printStackTrace(); return ""; } }
     */

    public void getCommas() {
        int listMention = 0;
        int connMention = 0;
        int totalAnnotations = 0;
        int docLists = 0;
        for ( Document doc : p2g.getDocuments() ) {
            boolean inDoc = false;
            FeatureMap fMap = doc.getFeatures();

            // make a map of changed connections
            DocumentContentImpl documentContent = ( DocumentContentImpl ) doc.getContent();

            Set<Annotation> inConnection = new HashSet<Annotation>();
            List<Connection> connections = Connection.getConnections( fMap );
            if ( connections != null ) {
                for ( Connection connection : connections ) {
                    inConnection.add( connection.getPartnerA() );
                    inConnection.add( connection.getPartnerB() );
                    if ( connection.getRelAnn() != null ) inConnection.add( connection.getRelAnn() );
                }
            }

            // get number of brain regions and connection predicates
            Map m = doc.getNamedAnnotationSets();
            for ( Object key : m.keySet() ) { // goes through all annotators
                // System.out.println(key); -key is the name of the annotator
                // if its not the matching annotator name or set to blank then
                // move on
                if ( !key.equals( "UnionMerge" ) ) continue;
                AnnotationSet annotSet = ( AnnotationSet ) m.get( key );

                // convert it to an array first so we don't get concurrent
                // modification issues
                for ( Object o : annotSet.toArray() ) {
                    Annotation ann = ( Annotation ) o;

                    // make sure we have a brain region or connectionpred type
                    if ( !( ann.getType().equals( "BrainRegion" ) ) || ( ann.getType().equals( "ConnectionPredicate" ) ) ) {
                        continue;
                    }
                    totalAnnotations++;
                    Node startNode = ann.getStartNode();
                    Node endNode = ann.getEndNode();
                    long start = startNode.getOffset();
                    long end = endNode.getOffset();
                    // we trim it then put it in the map
                    try {
                        String cont = documentContent.getContent( start, end ).toString();

                        if ( checkForList( cont ) ) {
                            // if its connected
                            if ( inConnection.contains( ann ) ) {
                                connMention++;
                                // denote it
                                cont += " [C]";
                            }
                            System.out.println( cont );
                            listMention++;
                            inDoc = true;
                        }
                    } catch ( Exception e ) {
                        e.printStackTrace();
                    }
                    // Annotation newAnn =

                } // end of annotations

            } // end of annotation sets
            if ( inDoc ) docLists++;
        } // end of documents
        System.out.println( "Number of lists:" + listMention );
        System.out.println( "Number of docs:" + docLists );
        System.out.println( "Number of lists in a connection:" + connMention );
        System.out.println( "Number of annotations:" + totalAnnotations );

    }

    public static boolean checkForList( String phrase ) {
        return phrase.contains( "," ) || phrase.contains( "\\" ) || phrase.contains( "/" ) || phrase.contains( ";" )
                || phrase.contains( " and " ) || phrase.contains( " or " );
    }

    public void printRandomCorpusAbstractsWithConnections( String annotatorName ) {
        printAbstractsWithConnections( p2g.getRandomSubsetDocuments(), annotatorName );
    }

    private void printAbstractsWithConnections( List<ConnectionsDocument> documents, String annotatorName ) {
        for ( int i = 0; i < documents.size(); i++ ) {
            Document doc = documents.get( i );
            FeatureMap fMap = doc.getFeatures();
            int connectionsSize = getConnectionsCount( fMap, annotatorName );
            if ( connectionsSize != 0 ) {
                System.out.println( i );
            }
        }
    }

    public void printStats( String annotatorName, boolean verbose ) {
        // header line
        Set<String> uniqPreds = new HashSet<String>();
        if ( verbose ) System.out.println( "PMID, connections, brain region mentions, connection predicate mentions" );
        int totalAbs, totalConn, totalBM, totalPr;
        totalAbs = totalConn = totalBM = totalPr = 0;

        for ( Document doc : p2g.getDocuments() ) {
            FeatureMap fMap = doc.getFeatures();
            if ( verbose ) System.out.print( fMap.get( "PMID" ) + "," );
            totalAbs++;

            int connectionsSize = getConnectionsCount( fMap, annotatorName );
            if ( verbose ) System.out.print( connectionsSize + "," );
            totalConn += connectionsSize;

            // get number of brain regions and connection predicates
            Map m = doc.getNamedAnnotationSets();
            int brainRegs = 0;
            int connectionPreds = 0;

            for ( Object key : m.keySet() ) { // goes through all annotators
                // System.out.println(key); -key is the name of the annotator
                // if its not the matching annotator name or set to blank then
                // move on
                if ( !( annotatorName == null || annotatorName.equalsIgnoreCase( ( String ) key ) ) ) continue;
                AnnotationSet annotSet = ( AnnotationSet ) m.get( key );
                for ( Object o : annotSet ) {
                    Annotation ann = ( Annotation ) o;
                    // brain region mentions
                    if ( ann.getType().equals( "BrainRegion" ) ) {
                        brainRegs++;
                    }
                    // connection type mentions
                    if ( ann.getType().equals( "ConnectionPredicate" ) ) {
                        connectionPreds++;
                        ConnectionsDocument cDdoc = new ConnectionsDocument( doc );
                        uniqPreds.add( cDdoc.getAnnotationText( ann ) );
                    }
                }
            }
            totalPr += connectionPreds;
            totalBM += brainRegs;

            if ( verbose ) System.out.print( brainRegs + "," + connectionPreds );
            if ( verbose ) System.out.println();
        }
        System.out.println( annotatorName + ":\n*" + totalAbs + " abstracts\n*" + totalConn + " connections\n*"
                + totalBM + " brain region mentions\n*" + totalPr + " connection predicate mentions\n*"
                + uniqPreds.size() + " unique preds\n" );
    }

    /**
     * Counts number of connections a document has.
     * 
     * @param fMap document feature map, used to extract connection list
     * @param name is the annotator name, pass null for all annotators.
     * @return
     */
    private int getConnectionsCount( Document d ) {
        return getConnectionsCount( d.getFeatures(), null );
    }

    private int getConnectionsCount( Document d, String name ) {
        return getConnectionsCount( d.getFeatures(), name );
    }

    private int getConnectionsCount( FeatureMap fMap, String name ) {
        List<Connection> connections = Connection.getConnections( fMap );
        if ( connections == null ) return 0;

        // get number of connections
        int connectionsSize = 0;
        for ( Connection c : connections ) {
            if ( name == null || c.getAuthor().equalsIgnoreCase( name ) ) connectionsSize++;
        }
        return connectionsSize;
    }

    public void printAnimalsCSV() {
        Map<String, Integer> animals = getAnnotatedSpecies();
        for ( String animal : animals.keySet() ) {
            System.out.println( "\"" + animal + "\"," + animals.get( animal ) );
        }
    }

    public void printConnectionMatchCount( boolean lenient ) {
        String annotatorA = "Suzanne";
        String annotatorB = "Lydia";

        ConnectionDiffer conDiff = new ConnectionDiffer( annotatorA, annotatorB );
        // for (Document doc : p2g.getRandomSubsetDocuments()) {
        List<ConnectionsDocument> documents = p2g.getRandomSubsetDocuments();
        int totalConnections = 0;
        int totalHits = 0;
        int totalA = 0;
        int totalB = 0;
        for ( int i = 0; i < documents.size(); i++ ) {
            // //////////////////////
            if ( i == 230 ) break;
            Document doc = documents.get( i );

            int matches = conDiff.getMatches( doc, lenient ).size();
            int total = getConnectionsCount( doc );
            if ( total == 0 ) continue;
            total = total - matches;
            System.out.println( i + " has " + matches + " matched of a total " + total + " (" + annotatorA + ":"
                    + getConnectionsCount( doc, annotatorA ) + ", " + annotatorB + ":"
                    + getConnectionsCount( doc, annotatorB ) + ")" );

            totalA += getConnectionsCount( doc, annotatorA );
            totalB += getConnectionsCount( doc, annotatorB );
            totalConnections += total;
            totalHits += matches;
        }
        System.out.println( "Overall: " + totalHits + " of " + totalConnections );
        System.out.println( annotatorA + " has " + totalA + " connections " );
        System.out.println( annotatorB + " has " + totalB + " connections " );
    }

    public CountingMap<String> getAnnotatedSpecies() {
        CountingMap<String> annotatedSpecies = new CountingMap<String>();
        for ( Document doc : p2g.getDocuments() ) {
            FeatureMap fMap = doc.getFeatures();
            List<Connection> connections = Connection.getConnections( fMap );
            if ( connections != null ) {
                // get all the connection orgamisms
                for ( Connection c : connections ) {
                    String key = c.getComment();
                    annotatedSpecies.increment( key );
                }
            }
        }
        return annotatedSpecies;
    }

    public static void main( String[] args ) throws Exception {
        GetStats stats = new GetStats();
        // stats.printAnimalsCSV();
        boolean verbose = false;
        // stats.printStats( "Suzanne", verbose );
        // stats.printStats( "Lydia", verbose );
        // stats.printStats( "UnionMerge", verbose );
        // stats.printStats( "IntersectMerge", verbose );
        // stats.printStats( verbose );
        boolean leienent = false;
        stats.printConnectionMatchCount( leienent );
        // stats.printAnimalsCSV();

        // stats.getCommas();
        // stats.printRandomCorpusAbstractsWithConnections("Lydia");
        // stats.printStatsCSV( null );
        // stats.getCommas();
    }
}

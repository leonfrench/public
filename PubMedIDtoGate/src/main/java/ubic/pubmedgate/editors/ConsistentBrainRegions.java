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

package ubic.pubmedgate.editors;

import gate.Annotation;
import gate.AnnotationSet;
import gate.util.FMeasure;
import gate.util.SimpleFeatureMapImpl;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.statistics.AnnotationComparator;
/*
 * I wrote this to test how consistent the annotations are, that is given one abstract and all terms, are those terms always annotated?
 */
public class ConsistentBrainRegions {
    protected static Log log = LogFactory.getLog( ConsistentBrainRegions.class );
    String annotationSet;
    String tokenAnnotationSet;
    List<ConnectionsDocument> docs;
    boolean save;

    public ConsistentBrainRegions( String annotationSet, String tokenAnnotationSet, List<ConnectionsDocument> docs,
            boolean save ) {
        this.annotationSet = annotationSet;
        this.tokenAnnotationSet = tokenAnnotationSet;
        this.docs = docs;
        this.save = save;
    }

    public void run( List<ConnectionsDocument> docs ) {
        int totalHits = 0;
        int documents = 0;
        for ( ConnectionsDocument doc : docs ) {
            int hits = run( doc );
            totalHits += hits;
            if ( hits != 0 ) {
                log.info( doc.getName() + " hits:" + hits );
                documents ++;
            }
        }
        log.info("Total Hits:"+ totalHits);
        log.info("Documents:"+ documents + " of " + docs.size());
    }

    public int run( ConnectionsDocument doc ) {
        // use treetager/gate, and match those tokens to annotations?
        List<Annotation> regions = doc.getAnnotationsByType( annotationSet, "BrainRegion" );
        List<Annotation> tokens = doc.getAnnotationsByType( tokenAnnotationSet,
                gate.creole.ANNIEConstants.TOKEN_ANNOTATION_TYPE );
        Set<String> regionStrings = new HashSet<String>();
        int hits = 0;

        for ( Annotation region : regions ) {
            String regionText = doc.getAnnotationText( region );
            regionText = regionText.toLowerCase();
            regionStrings.add( regionText );
        }
        for ( Annotation token : tokens ) {
            String tokenText = doc.getAnnotationText( token );
            tokenText = tokenText.toLowerCase();
            if ( regionStrings.contains( tokenText ) ) {
                AnnotationSet saveTo = doc.getAnnotations( annotationSet );
                long start = token.getStartNode().getOffset();
                long end = token.getEndNode().getOffset();
                if ( saveTo.get( start, end ).get( "BrainRegion" ).size() == 0 ) {
                    log.info( "HIT:" + tokenText );
                    hits++;
                    if ( save ) {
                        try {
                            // add it
                            saveTo.add( start, end, "BrainRegion", new SimpleFeatureMapImpl() );
                            doc.sync();
                        } catch ( Exception e ) {
                            throw new RuntimeException( e );
                        }
                    }
                }

            }

        }
        return hits;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // TODO Auto-generated method stub
        GateInterface p2g = new GateInterface();
        List<ConnectionsDocument> docs = new LinkedList<ConnectionsDocument>();
        docs = p2g.getDocuments();
        //docs.add( p2g.getByPMID( "9779940" ) );
        AnnotationComparator ac = new AnnotationComparator( "Suzanne", "Mallet", "BrainRegion" );
        FMeasure f = ac.computeFMeasure( docs );
        System.out.println( f.f1 );
        System.out.println( f.printResults() );

        ConsistentBrainRegions cons = new ConsistentBrainRegions( "Suzanne", "GATETokens", docs, false );
        cons.run( docs );
        
        System.exit(1);
        

         cons = new ConsistentBrainRegions( "Suzanne", "TreeTagger", docs, true );
        cons.run( docs );
        
        ac = new AnnotationComparator( "Suzanne", "Mallet", "BrainRegion" );
        f = ac.computeFMeasure( docs );
        System.out.println( f.f1 );
        System.out.println( f.printResults() );
        
        List<ConnectionsDocument> withOutConnect = new LinkedList<ConnectionsDocument>();
        for ( ConnectionsDocument d : docs ) {
            if ( !( d.getConnections() == null || d.getConnections().size() == 0 ) ) {
                withOutConnect.add( d );
            }
        }
        log.info( "----------------Without connections documents" );
        f = ac.computeFMeasure( withOutConnect );
        System.out.println( f.f1 );
        System.out.println( f.printResults() );

    }
}

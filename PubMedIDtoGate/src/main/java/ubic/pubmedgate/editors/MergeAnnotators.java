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

import gate.AnnotationSet;
import gate.Document;
import gate.DocumentContent;
import gate.util.SimpleFeatureMapImpl;

import java.util.List;

import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.mallet.GateReseter;
import ubic.pubmedgate.mallet.JoinMalletPredictions;
import ubic.pubmedgate.ner.TokenTargetLabeller;

/*
 * 
 * Merges annotations at a character by character level (something GATE doesn't do) 
 * 
 */
public class MergeAnnotators {
    public static int UNION = 0;
    public static int INTERSECT = 1;
    public static String UNIONSTRING = "UnionMerge";
    public static String INTERSTRING = "IntersectMerge";

    public void merge( Document doc, int method, String annotatorA, String annotatorB ) throws Exception {
        String resultSet = null;
        if ( method == UNION ) resultSet = UNIONSTRING;
        if ( method == INTERSECT ) resultSet = INTERSTRING;
        merge( doc, method, annotatorA, annotatorB, resultSet );
    }

    public void merge( Document doc, int method, String annotatorA, String annotatorB, String resultSet )
            throws Exception {
        String type = "BrainRegion";

        doc.removeAnnotationSet( "temp" );
        AnnotationSet temp = doc.getAnnotations( "temp" );

        AnnotationSet aSetA = doc.getAnnotations( annotatorA );
        AnnotationSet aSetB = doc.getAnnotations( annotatorB );
        aSetA = aSetA.get( type );
        aSetB = aSetB.get( type );

        DocumentContent cont = doc.getContent();

        // go through each character
        for ( long i = 0; i < cont.size(); i++ ) {
            try {
                long start = i;
                long end = i + 1;
                boolean inA = aSetB.get( start, end ).size() > 0;
                boolean inB = aSetA.get( start, end ).size() > 0;
                if ( ( method == UNION && ( inA || inB ) ) || ( method == INTERSECT && ( inA && inB ) ) ) {
                    temp.add( start, end, TokenTargetLabeller.BRAIN_TARGET, new SimpleFeatureMapImpl() );
                    // System.out.print( cont.getContent( start, end ) );
                } else {
                    temp.add( start, end, TokenTargetLabeller.OUTSIDE_TARGET, new SimpleFeatureMapImpl() );
                    // System.out.print( " " );
                }
            } catch ( Exception e ) {
                throw new RuntimeException( e );
            }
        }
        // clean out the old
        doc.removeAnnotationSet( resultSet );

        // join
        JoinMalletPredictions joiner = new JoinMalletPredictions( Config.config
                .getString( "whitetext.malletJoin.jape.location" ), temp.getName(), resultSet, null );
        joiner.execute( doc );

        // delete temp
        doc.removeAnnotationSet( "temp" );
        doc.sync();

    }

    public static void main( String[] args ) throws Exception {
        GateInterface p2g = new GateInterface();

        //clean out the old
        GateReseter reset = new GateReseter( p2g.getCorp(), UNIONSTRING );
        reset.reset();
        
        reset = new GateReseter( p2g.getCorp(), INTERSTRING );
        reset.reset();

        MergeAnnotators merger = new MergeAnnotators();

        List<ConnectionsDocument> docs = p2g.getRandomSubsetDocuments();
        for ( ConnectionsDocument cdoc : docs ) {
            Document doc = cdoc.getDocument();
            merger.merge( doc, UNION, "Suzanne", "Lydia" );
            merger.merge( doc, INTERSECT, "Suzanne", "Lydia" );
        }
        System.out.println( "Done random subset" );

        docs = p2g.getTrainingDocuments();
        for ( ConnectionsDocument cdoc : docs ) {
            Document doc = cdoc.getDocument();
            // just to make things consistent
            merger.merge( doc, UNION, "Suzanne", "Suzanne" );
            merger.merge( doc, INTERSECT, "Suzanne", "Suzanne" );
        }
        System.out.println( "Done training" );
    }
}

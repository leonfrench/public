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

package ubic.pubmedgate.ner;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.util.OffsetComparator;

import java.util.Collections;
import java.util.List;

import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.editors.AbbreviationLoader;
import ubic.pubmedgate.mallet.features.TargetFromGATE;

/**
 * Given a gate document, this can label it's tokens based on Gold standard annotations
 * 
 * @author leon
 */
public class TokenTargetLabeller {
    private GateInterface p2g;
    Corpus corp;
    private String annotationSet;
    String sourceSet;
    public static final String BRAIN_TARGET = "brain";
    public static final String START_TARGET = "brain";
    public static final String INSIDE_TARGET = "inside";
    public static final String OUTSIDE_TARGET = "outside";

    public TokenTargetLabeller() {
        this( ConnectionsDocument.GATETOKENS );
        p2g = new GateInterface();
    }

    public TokenTargetLabeller( String annotationSet ) {
        p2g = new GateInterface();
        this.annotationSet = annotationSet;
        this.sourceSet = "Suzanne";
    }

    public TokenTargetLabeller( GateInterface p2g, String annotationSet, String sourceSet, Corpus corp ) {
        this.p2g = p2g;
        this.annotationSet = annotationSet;
        this.corp = corp;
        this.sourceSet = sourceSet;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // SET the CORPUS and the SOURCESET
        GateInterface p2g = new GateInterface();
        Corpus corp = p2g.getCorp();
        //Corpus corp = p2g.getNoAbbrevCorp();
        System.out.println(corp.size());
        // String[] annotationSets = { "TreeTagger", ConnectionsDocument.GATETOKENS, "TreeTaggerGATETokens" };
        String[] annotationSets = { "TreeTaggerGATETokens" };

        // String annotationSet = "GATETokens";
        // String sourceSet = "Suzanne";
        // String sourceSet = "Lydia";
        // String sourceSet = "IntersectMerge";
        String sourceSet = "UnionMerge";

        for ( String annotationSet : annotationSets ) {
            System.out.println( "Labelling " + annotationSet + " using " + sourceSet );
            TokenTargetLabeller sentIt = new TokenTargetLabeller( p2g, annotationSet, sourceSet, corp );
            // sentIt.printSentences();
            System.out.println( "Simple Labels" );
            sentIt.generateSimpleTokenTargets();
            System.out.println( "BIO Labels" );
            sentIt.generateSIOTokenTargets();
        }

    }

    public void printSentences() {
        for ( ConnectionsDocument doc : p2g.getDocuments() ) {
            AbbreviationLoader abbrevInterface = new AbbreviationLoader( doc );
            System.out.println( doc.getName() );
            for ( Annotation sentence : doc.getGATESentences( annotationSet ) ) {
                System.out.println( doc.getAnnotationText( sentence ) );
                for ( Annotation token : doc.getTokens( sentence, annotationSet ) ) {
                    if ( abbrevInterface.hasShortFormOverlap( token ) ) {
                        System.out.print( "SKIP" + "|" );
                    } else {
                        System.out.print( doc.getAnnotationText( token ) + "|" );
                    }
                }
                System.out.println();
            }

        }
    }

    public void generateSimpleTokenTargets() {
        // the inside and start targets are the same, so it's a simple target method
        generateSIOTokenTargets( BRAIN_TARGET, BRAIN_TARGET, OUTSIDE_TARGET, TargetFromGATE.GATE_SIMPLE_TARGET_FEATURE );
    }

    public void generateSIOTokenTargets() {
        generateSIOTokenTargets( START_TARGET, INSIDE_TARGET, OUTSIDE_TARGET, TargetFromGATE.GATE_BIO_TARGET_FEATURE );
    }

    public void generateSIOTokenTargets( String start, String inside, String outside, String targetFeature ) {
        int brainCount = 0;
        int outsideCount = 0;
        for ( ConnectionsDocument doc : GateInterface.getDocuments( corp ) ) {
            // System.out.println( doc.getName() );

            // get brain regions
            AnnotationSet brainRegions = doc.getBrainRegionAnnotations( sourceSet );

            List<Annotation> tokens = doc.getAnnotationsByType( annotationSet,
                    gate.creole.ANNIEConstants.TOKEN_ANNOTATION_TYPE );

            // sort the tokens
            Collections.sort( tokens, new OffsetComparator() );

            String lastTarget = null;

            for ( Annotation token : tokens ) {
                AnnotationSet brainInside = brainRegions.get( token.getStartNode().getOffset(), token.getEndNode()
                        .getOffset() );

                String target = "";
                if ( brainInside.size() > 0 ) {
                    brainCount++;
                    // it's in a brain region annotation
                    if ( lastTarget != null && ( lastTarget.equals( start ) | lastTarget.equals( inside ) ) ) {
                        target = inside;
                    } else {
                        target = start;
                    }

                } else {
                    // no brain regions involved
                    outsideCount++;
                    target = outside;
                }
                // System.out.println( target + ":" + + doc.getAnnotationText( token ).length() );
                // //doc.getAnnotationText( token ) );
                // System.out.println( target + ":" + doc.getAnnotationText( token ) );
                token.getFeatures().put( targetFeature, target );
                lastTarget = target;
            }
            // save it
            try {
                // TOGGLE TODO
                doc.sync();
            } catch ( Exception e ) {
                e.printStackTrace();
                throw new RuntimeException();
            }

        }
        System.out.println( "Brain:" + brainCount + " Outside:" + outsideCount );
    }
}

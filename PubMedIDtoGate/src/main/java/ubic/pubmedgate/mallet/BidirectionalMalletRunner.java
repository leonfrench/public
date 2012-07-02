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

package ubic.pubmedgate.mallet;

import gate.Corpus;
import gate.util.FMeasure;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.mallet.mergers.BestSumOrFirstMerge;
import ubic.pubmedgate.mallet.mergers.BidirectionalMerge;
import ubic.pubmedgate.statistics.AnnotationComparator;
import cc.mallet.types.Instance;
import cc.mallet.types.Sequence;
import cc.mallet.types.SequencePairAlignment;

public class BidirectionalMalletRunner extends MalletRunner {
    protected static Log log = LogFactory.getLog( BidirectionalMalletRunner.class );

    SimpleMalletRunner aRunner;
    SimpleMalletRunner bRunner;

    public BidirectionalMalletRunner( SimpleMalletRunner a, SimpleMalletRunner b ) {
        assert ( a.getCorpus().equals( b.getCorpus() ) );
        this.corpus = a.getCorpus();

        assert ( a.getDocuments().equals( b.getDocuments() ) );
        documents = a.getDocuments();

        sentenceLevel = false;
        numFolds = 8;

        // String inputTokenSet = "TreeTagger";

        // false then 1 before and 0 after
        // true then 0 before and 1 after
        skipAbbrev = false;

        annotationSet = "MalletBi";

        aRunner = a;
        bRunner = b;
        try {
            aRunner.reset();
            bRunner.reset();
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

    }

    public void run() throws Exception {
        log.info( "======================== Going forward ======================== " );
        aRunner.run();
        log.info( "======================== Going backward ======================== " );
        bRunner.run();
    }

    public void joinAndEvaluate() throws Exception {
        BidirectionalMerge merger;
        // join them
        // merger = new UnionMerge( false );
        // merger = new IntersectMerge( false );
        merger = new BestSumOrFirstMerge();

        // put the result in gate
        MalletPredictionsToGate putInGate = new MalletPredictionsToGate( annotationSet );
        outer: for ( Instance iForward : aRunner.getAllInstances() ) {
            for ( Instance iBackward : bRunner.getAllInstances() ) {
                String forwardName = iForward.getName().toString();
                String backwardName = iBackward.getName().toString();
                if ( aRunner.isReverse() ) {
                    forwardName = forwardName.replace( "REVERSE-", "" );
                }
                if ( bRunner.isReverse() ) {
                    backwardName = backwardName.replace( "REVERSE-", "" );
                }

                if ( forwardName.equals( backwardName ) ) {
                    // log.info( "Match:" + iBackward.getName() + " " + iForward.getName() );
                    List<SequencePairAlignment<Object, Object>> forward10 = aRunner.getTop10( iForward );
                    List<SequencePairAlignment<Object, Object>> reverse10 = bRunner.getTop10( iBackward );
                    List<SequencePairAlignment<Object, Object>> reverseForward10 = new LinkedList<SequencePairAlignment<Object, Object>>();
                    List<SequencePairAlignment<Object, Object>> forwardForward10 = new LinkedList<SequencePairAlignment<Object, Object>>();

                    // reverse the reverse 10
                    if ( aRunner.isReverse() ) {
                        for ( SequencePairAlignment<Object, Object> reverseMe : reverse10 ) {
                            forwardForward10.add( ReverseTools.reverseSequencePairAlignment( reverseMe ) );
                        }
                        forward10 = forwardForward10;
                    }
                    // reverse the reverse 10
                    if ( bRunner.isReverse() ) {
                        for ( SequencePairAlignment<Object, Object> reverseMe : reverse10 ) {
                            reverseForward10.add( ReverseTools.reverseSequencePairAlignment( reverseMe ) );
                        }
                        reverse10 = reverseForward10;
                    }

                    Sequence result = merger.merge( forward10, reverse10 );
                    putInGate.saveTargets( iForward, result );
                    continue outer;
                }
            }
        }

        log.info( "======================== Forward ======================== " );
        aRunner.joinAndEvaluate();

        log.info( "======================== Backward ======================== " );
        bRunner.joinAndEvaluate();

        log.info( "======================== Eachother ======================== " );
        AnnotationComparator ac = new AnnotationComparator( aRunner.getAnnotationSet(), bRunner.getAnnotationSet(),
                "BrainRegion" );
        FMeasure f = ac.computeFMeasure( documents );
        System.out.println( f.f1 );
        System.out.println( f.printResults() );

        log.info( "======================== Merged ======================== " );
        super.joinAndEvaluate();
        log.info( "Merge stats:" + merger );
    }

    public void writeOutResults() {
        // two sets of params!
        log.info( "not written" );
    }

    public Corpus getCorpus() {
        return corpus;
    }

    public List<ConnectionsDocument> getDocuments() {
        return documents;
    }

    public int getNumFolds() {
        return numFolds;
    }

    public boolean isSentenceLevel() {
        return sentenceLevel;
    }

    public boolean isSkipAbbrev() {
        return skipAbbrev;
    }

}

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
import gate.Gate;
import gate.util.FMeasure;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.statistics.AnnotationComparator;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.InstanceList;

// targets do not exist for test
// no folds, just train and test
// no evaluation
public class UnseenTestMalletRunner extends SimpleMalletRunner {
    Corpus testCorpus;
    

    public UnseenTestMalletRunner( Corpus trainCorpus, Corpus testCorpus, String truthSet, boolean reverse,
            int windowBefore, int windowAfter ) {
        super( "Mallet", truthSet, "TreeTagger", trainCorpus, reverse, windowBefore, windowAfter, 1 );
        this.testCorpus = testCorpus;
    }

    public UnseenTestMalletRunner( String annotationSet, String truthSet, String tokenSet, Corpus corpus,
            Corpus testCorpus, boolean reverse, int windowBefore, int windowAfter, 
            BrainRegionPipes brainPipes ) {
        super( annotationSet, truthSet, tokenSet, corpus, reverse, windowBefore, windowAfter, 1, brainPipes );
        this.testCorpus = testCorpus;
    }

    public void join() throws Exception {
        join( testCorpus );
    }

    public FMeasure evaluate() {
        return evaluate( truthSet );
        // log.info( "View in Gate to evaluate" );
        // return null;
    }

    public FMeasure evaluate( String truthSet ) {
        AnnotationComparator ac = new AnnotationComparator( truthSet, annotationSet, "BrainRegion" );
        FMeasure f = ac.computeFMeasure( GateInterface.getDocuments( testCorpus ) );
        System.out.println( f.f1 + " Truth set:" + truthSet );
        System.out.println( f.printResults() );
        return f;
    }

    public void run() throws Exception {
        // training
        InstanceList trainingInstanceList = getInstanceList( documents );

        // testing (a different corpus)
        InstanceList testingInstanceList = getInstanceList( GateInterface.getDocuments( testCorpus ) );

        Pipe pipes = getPipe();

        log.info( "Training size:" + trainingInstanceList.size() + " Testing size:" + testingInstanceList.size() );

        InstanceList training = new InstanceList( pipes );
        training.addThruPipe( trainingInstanceList.iterator() );

        InstanceList testing = new InstanceList( pipes );
        testing.addThruPipe( testingInstanceList.iterator() );

        foldRunners = new FoldRunner[1];
        FoldRunner foldRunner = new FoldRunner( 0, training, testing, null, this );
        foldRunners[0] = foldRunner;
        foldRunner.run();

        // log.info( "Abstract cache size:" + pipes.cacheSize() );
        log.info( "Before window size = " + windowBefore + " after window size = " + windowAfter );

    }

    /*
     * Test sequences are not labbeled, so do not extract targets (non-Javadoc)
     * 
     * @see ubic.pubmedgate.mallet.MalletRunner#isTestUnlabelled()
     */
    public boolean isTestUnlabelled() {
        return true;
    }

    public void reset() throws Exception {
        // reset docs (both)
        // GateReseter reset = new GateReseter( documents, annotationSet );
        // reset.reset();
        // only reset test corpus
        GateReseter reset = new GateReseter( GateInterface.getDocuments( testCorpus ), annotationSet );
        reset.reset();
    }
}

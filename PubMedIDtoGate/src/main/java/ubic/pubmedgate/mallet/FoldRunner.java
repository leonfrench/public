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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArraySet;

import ubic.basecode.dataStructure.DoubleAddingMap;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrixFactory;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.mallet.features.TargetFromGATE;
import ubic.pubmedgate.ner.TokenTargetLabeller;
import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFTrainerByLabelLikelihood;
import cc.mallet.fst.MaxLatticeDefault;
import cc.mallet.fst.MultiSegmentationEvaluator;
import cc.mallet.fst.Transducer;
import cc.mallet.fst.TransducerEvaluator;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.share.upenn.ner.FeatureWindow;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Sequence;
import cc.mallet.types.SequencePairAlignment;
import cc.mallet.types.SparseVector;

public class FoldRunner implements Runnable {
    protected static Log log = LogFactory.getLog( FoldRunner.class );

    final static int nBestCacheSize = 300000;
    int fold;
    boolean done;
    InstanceList training;
    InstanceList testing;
    Object notifyWhenDone;
    SimpleMalletRunner runner;
    CRF crf = null;

    /*
     * Use state sequence as input, for example outside,brain
     */
    public DoubleAddingMap<String> weightsTest( String states ) {
        DoubleAddingMap<String> weights = new DoubleAddingMap<String>();
        SparseVector[] vecs = crf.getWeights();
        Alphabet a = training.getAlphabet();

        for ( int i = 0; i < vecs.length; i++ ) {
            if ( crf.getWeightsName( i ).equals( states ) ) {
                double[] values = vecs[i].getValues();
                int[] indices = vecs[i].getIndices();
                for ( int j = 0; j < values.length; j++ ) {
                    int index = indices[j];
                    double value = values[j];
                    String featureName = a.lookupObject( index ).toString();
                    weights.addPut( featureName, value );
                }
            }
        }
        return weights;
    }

    public void weightsTestOld() {
        // Map<String, Double> weights;
        // DoubleMatrix<String, String> weights;
        // DoubleMatrix<String, String> weights = new DenseDoubleMatrix<String, String>( totalGenes, brainRegions );

        SparseVector[] vecs = crf.getWeights();
        SparseVector out2Brain = null;

        for ( int i = 0; i < vecs.length; i++ ) {
            System.out.println( "Weight of:" + crf.getWeightsName( i ) );
            if ( crf.getWeightsName( i ).equals( "brain,brain" ) ) {
                out2Brain = vecs[i];
            }
            SparseVector v = vecs[i];
            System.out.println( "Size:" + v.singleSize() );
            System.out.println( "Size:" + v.getIndices().length );
            // use the alphabet.lookup to get name
        }
        // now deal with out2brain
        Alphabet a = training.getAlphabet();
        // out2Brain.print();
        double[] values = out2Brain.getValues();
        int[] indices = out2Brain.getIndices();
        List<Double> valuesD = new ArrayList<Double>();
        // out2Brain.
        // DenseDoubleMatrix x;

        // DoubleMatrix x;
        DenseDoubleMatrix<String, String> matrix = DoubleMatrixFactory.dense( out2Brain.singleSize(), 1 );

        // matrix.setRowNames( a.iterator(). )
        for ( int i = 0; i < values.length; i++ ) {
            valuesD.add( indices[i], values[i] );

        }
        Collections.sort( valuesD );
        Collections.reverse( valuesD );
        // get the 20th value
        double threshold = valuesD.get( 50 );

        for ( int i = 0; i < values.length; i++ ) {
            int index = indices[i];
            double value = values[i];
            if ( value < threshold ) {
                log.info( a.lookupObject( index ) + " => " + value );
            }
        }
    }

    public FoldRunner( int fold, InstanceList training, InstanceList testing, Object notifyWhenDone,
            SimpleMalletRunner runner ) {
        super();
        this.runner = runner;
        this.fold = fold;

        List<ConnectionsDocument> trainingDocs = new ArrayList<ConnectionsDocument>( training.size() );
        for ( Instance i : training ) {
            Object source = i.getSource();
            MalletToGateLink link = ( MalletToGateLink ) source;
            trainingDocs.add( link.getDoc() );
        }

        SerialPipes finalPipe = new SerialPipes( getFinalPipes( trainingDocs ) );

        log.info( "number of fold specific pipes:" + finalPipe.size() );

        // run the instances through the final pipes
        this.training = new InstanceList( finalPipe );
        this.training.addThruPipe( training.iterator() );

        this.testing = new InstanceList( finalPipe );
        this.testing.addThruPipe( testing.iterator() );

        this.notifyWhenDone = notifyWhenDone;
        done = false;

        Collections.sort( training, new InstanceSorter() );
        Collections.sort( testing, new InstanceSorter() );

    }

    /*
     * This a big ugly, its because I have to squeeze in the inferred dictionary Pipe in a strange location in the
     * pipelist
     */
    public Collection<Pipe> getFinalPipes( List<ConnectionsDocument> trainingDocs ) {
        Collection<Pipe> pipeList = new LinkedList<Pipe>();

        // make a pipe to deep copy
        pipeList.add( new DataDeepCopyPipe() );

        // make the targets, either simple target or BIO
        pipeList.add( new TargetFromGATE( runner.getInputTargetFeature() ) );

        try {
            // get an inferred dictionary
            // pipeList.addAll( InferredDictionaryPipeFactory.getInferredPipes( trainingDocs) );
        } catch ( Exception e ) {
            e.printStackTrace();
            System.exit( 1 );
        }

        // window features, should be last or second last, args are window left and right
        if ( runner.getWindowBefore() != 0 || runner.getWindowAfter() != 0 ) {
            pipeList.add( new FeatureWindow( runner.getWindowBefore(), runner.getWindowAfter() ) );
        }

        // pipeList.add( new FeaturesOfFirstMention( "FirstMention" ) );

        // lastly convert to vector sequence
        boolean augmentable = runner.isInduceFeatures();
        pipeList.add( new TokenSequence2FeatureVectorSequence( true, augmentable ) );

        // // reverse
        if ( runner.isReverse() ) {
            pipeList.add( new ReversePipe() );
        }

        return pipeList;

    }

    public void run() {
        train( training );
        StopWatch watch = new StopWatch();
        watch.start();
        test( testing );
        watch.stop();
        log.info( "Testing classifcation took " + watch.toString() + " for " + testing.size() );
        done = true;
        if ( notifyWhenDone != null ) {
            synchronized ( notifyWhenDone ) {
                notifyWhenDone.notifyAll();
            }
        }
    }

    public boolean isDone() {
        return done;
    }

    public boolean isInTestSet( Instance i ) {
        return testing.contains( i );
    }

    public List<SequencePairAlignment<java.lang.Object, java.lang.Object>> getTop10( Instance i ) {
        if ( !testing.contains( i ) ) throw new RuntimeException( "Error this is not in the fold test set" );
        return getTopN( i, 10 );
    }

    /*
     * Returns at most N pairs with weights, in some cases it can't return N so it goes lower
     */
    private List<SequencePairAlignment<java.lang.Object, java.lang.Object>> getTopN( Instance instance, int n ) {
        Sequence input = ( Sequence ) instance.getData();
        MaxLatticeDefault lattice = new MaxLatticeDefault( crf, input, null, nBestCacheSize );
        List<SequencePairAlignment<java.lang.Object, java.lang.Object>> alignments;
        try {
            alignments = lattice.bestOutputAlignments( n );
        } catch ( IndexOutOfBoundsException e ) {
            // try one less
            // log.info( "TopN returning one less (" + ( n - 1 ) + ")" );
            return getTopN( instance, n - 1 );
        }
        return alignments;
    }

    public Set<String> getTestFeatures() {
        // for each sentence
        Set<String> result = new HashSet<String>( 1000 );
        Alphabet a = testing.getAlphabet();

        for ( int i = 0; i < testing.size(); i++ ) {
            Instance instance = testing.get( i );
            Sequence input = ( Sequence ) instance.getData();
            // for each token
            for ( int j = 0; j < input.size(); j++ ) {
                FeatureVector inputFV = ( FeatureVector ) input.get( j );
                // log.info( instance.getName() + " token:" + j + " " + inputFV.toString( true ) );
                // iterate features of the token
                int indicesLength = inputFV.numLocations();
                int[] indices = inputFV.getIndices();

                for ( int iF = 0; iF < indicesLength; iF++ ) {
                    int index = indices[iF];
                    String featureName = a.lookupObject( index ).toString();
                    result.add( featureName );
                }
            }
        }
        return result;
    }

    public void test( InstanceList testing ) {
        if ( crf == null ) throw new RuntimeException( "Classifier not trained" );
        // the below class should probably extend TransducerEvaluator
        MalletPredictionsToGate putInGate = new MalletPredictionsToGate( runner.getAnnotationSet() );
        int testSetSize = testing.size();
        for ( int i = 0; i < testSetSize; i++ ) {
            Instance instance = testing.get( i );
            Sequence input = ( Sequence ) instance.getData();
            Sequence output = crf.transduce( input );

            // put the predictions back in the corpus
            putInGate.saveTargets( instance, output );

            if ( i % 10000 == 0 && i != 0 ) {
                log.info( i + " of " + testSetSize + " instances have been ran" );
            }
            // if ( i % 10 == 0 ) {
            // // code to print results
            // for ( int j = 0; j < input.size(); j++ ) {
            // FeatureVector inputFV = ( FeatureVector ) input.get( j );
            // log.info( instance.getName() + " token:" + j + " " + inputFV.toString(true) + " " + output.get( j ) );
            // }
            // }

        }
    }

    public CRF train( InstanceList training ) {
        String outside = TokenTargetLabeller.OUTSIDE_TARGET;
        String inside = TokenTargetLabeller.INSIDE_TARGET;
        String start = TokenTargetLabeller.START_TARGET;

        Pattern forbiddenPat;
        Pattern allowedPat;
        if ( runner.isBio() ) {
            forbiddenPat = Pattern.compile( "(" + outside + "," + inside + ")|(" + inside + "," + start + ")|(" + start
                    + "," + start + ")" );
            log.info( "Forbidden patterns:" + forbiddenPat );
            allowedPat = Pattern.compile( ".*" );
        } else {
            forbiddenPat = Pattern.compile( "\\s" );
            allowedPat = Pattern.compile( ".*" );
        }

        // setup the testing dataset
        InstanceList testing;
        if ( !runner.isTestUnlabelled() ) {
            testing = this.testing;
        } else {
            testing = null;
        }

        // markov orders
        int[] orders = new int[] { 1 };
        CRF crf = new CRF( training.getPipe(), ( Pipe ) null );
        String startName = crf.addOrderNStates( training, orders, null, outside, forbiddenPat, allowedPat, true );
        CRFTrainerByLabelLikelihood crft = new CRFTrainerByLabelLikelihood( crf );
        crft.setGaussianPriorVariance( 1 );
        for ( int i = 0; i < crf.numStates(); i++ )
            crf.getState( i ).setInitialWeight( Transducer.IMPOSSIBLE_WEIGHT );
        crf.getState( startName ).setInitialWeight( 0.0 );

        log.info( "Training on " + training.size() + " sentences." );
        if ( testing != null ) log.info( "Testing on " + testing.size() + " sentences" );

        boolean viterbiOutputOption = false;

        // MultiSegmentationEvaluator TokenAccuracyEvaluator
        String[] tags = new String[] { TokenTargetLabeller.BRAIN_TARGET };
        String[] continueTags;
        if ( runner.isBio() ) {
            continueTags = new String[] { TokenTargetLabeller.INSIDE_TARGET };
        } else {
            continueTags = tags;
        }

        TransducerEvaluator eval;
        // eval = new TokenAccuracyEvaluator( new InstanceList[] { training, testing }, new String[] {
        // "Training", "Testing" } );
        eval = new MultiSegmentationEvaluator( new InstanceList[] { training, testing }, new String[] { "Training",
                "Testing" }, tags, continueTags );

        boolean converged;

        // default of simpletagger is 100, TUI uses 99999
        int iterations = 250;
        if ( runner.isInduceFeatures() ) {
            // Number of maximizer iterations between each call to the Feature Inducer. (10 in simpletagger and TUI)
            int numIterationsBetweenFeatureInductions = 10;

            // Maximum number of rounds of feature induction. (20 in simpleTagger, 99 in TUI)
            int numFeatureInductions = 20;

            // Maximum number of features to induce at each round of induction. (500 in simpletagger, 200 in TUI)
            int numFeaturesPerFeatureInduction = 300;
            // splits = new double[] {.1, .2, .5, .7}

            crft.trainWithFeatureInduction( training, null, testing, eval, iterations,
                    numIterationsBetweenFeatureInductions, numFeatureInductions, numFeaturesPerFeatureInduction, 0.5,
                    false, null );
        } else {
            // before
            converged = crft.train( training ); // , iterations );
        }

        // for ( int i = 1; i <= iterations; i++ ) {
        // crft.train(training);
        // // if ( i % 5 == 0 && eval != null ) // Change the 1 to higher integer to evaluate less often
        // // eval.evaluate( crft );
        //
        // if ( viterbiOutputOption && i % 10 == 0 )
        // new ViterbiWriter( "", new InstanceList[] { training, testing }, new String[] { "training", "testing" } )
        // .evaluate( crft );
        // if ( converged ) break;
        // }
        eval.evaluate( crft );

        this.crf = crf;
        return crf;
    }

    public List<Instance> getTestInstances() {
        return testing;
    }
}

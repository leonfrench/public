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

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.FeatureMap;
import gate.Node;
import gate.corpora.CorpusImpl;
import gate.util.FMeasure;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.Util;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.connection.Connection;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.SubSetUtil;
import ubic.pubmedgate.editors.MergeAnnotators;
import ubic.pubmedgate.ner.TokenTargetLabeller;
import ubic.pubmedgate.statistics.AnnotationComparator;
import ubic.pubmedgate.statistics.GetStats;
import cc.mallet.fst.CRFOptimizableByLabelLikelihood;
import cc.mallet.fst.CRFTrainerByLabelLikelihood;
import cc.mallet.fst.CRFTrainerByValueGradients;
import cc.mallet.fst.ThreadedOptimizable;
import cc.mallet.types.FeatureInducer;
import cc.mallet.util.MalletLogger;

public class MalletQuick {
    protected static Log log = LogFactory.getLog( MalletQuick.class );

    private GateInterface p2g;

    public MalletQuick( String dataStore ) {
        if ( dataStore == null ) {
            p2g = new GateInterface();
        } else {
            log.info( "Using datastore" + dataStore );
            p2g = new GateInterface( dataStore );
        }
        silenceLogger( MalletLogger.getLogger( CRFTrainerByLabelLikelihood.class.getName() ) );
        silenceLogger( MalletLogger.getLogger( CRFOptimizableByLabelLikelihood.class.getName() ) );
        silenceLogger( MalletLogger.getLogger( ThreadedOptimizable.class.getName() ) );
        silenceLogger( MalletLogger.getLogger( CRFTrainerByValueGradients.class.getName() ) );
        silenceLogger( MalletLogger.getLogger( FeatureInducer.class.getName() ) );
    }

    public void runBidirect() throws Exception {
        BrainRegionPipes aPipes = new BrainRegionPipes();
        aPipes.addAllGoodPipes();

        BrainRegionPipes bPipes = new BrainRegionPipes();
        bPipes.addTextPipe();
        bPipes.addBrainRegionLexicons( true );

        SimpleMalletRunner a = new SimpleMalletRunner( "MalletBi1", "UnionMerge", "TreeTagger", p2g.getTrainingCorp(),
                false, 2, 2, 8, aPipes );
        SimpleMalletRunner b = new SimpleMalletRunner( "MalletBi2", "UnionMerge", "TreeTagger", p2g.getTrainingCorp(),
                false, 2, 2, 8, bPipes );

        MalletRunner runBi = new BidirectionalMalletRunner( a, b );
        runBi.reset();
        runBi.run();
        runBi.joinAndEvaluate();
    }

    public Corpus getNoAbbrevCorpus() throws Exception {
        Corpus corpus = new CorpusImpl();

        outer: for ( Object o : p2g.getCorp() ) {
            Document doc = ( Document ) ( o );
            // get annotations
            AnnotationSet anots = doc.getAnnotations( "UnionMerge" );
            for ( Annotation ann : anots ) {
                Node startNode = ann.getStartNode();
                Node endNode = ann.getEndNode();
                long start = startNode.getOffset();
                long end = endNode.getOffset();
                // we trim it then put it in the map
                String cont = doc.getContent().getContent( start, end ).toString();

                // check for commas, if it has one move on to another document
                if ( GetStats.checkForList( cont ) ) {
                    continue outer;
                }
            }
            corpus.add( o );
        }
        log.info( "Filtered corpus size:" + corpus.size() );
        return corpus;
    }

    public Corpus getCommonAnimalsCorpus() {
        Corpus corpus = new CorpusImpl();
        Set<String> keepers = new HashSet<String>();
        keepers.add( "mouse" );
        keepers.add( "mice" );
        keepers.add( "rhesus monkey" );
        keepers.add( "primate" );
        keepers.add( "squirrel monkey" );
        keepers.add( "monkey" );

        outer: for ( Object o : p2g.getTrainingCorp() ) {
            Document doc = ( Document ) ( o );
            FeatureMap fMap = doc.getFeatures();
            List<Connection> connections = Connection.getConnections( fMap );
            if ( connections != null ) {
                // get all the connection orgamisms
                for ( Connection c : connections ) {
                    String key = c.getComment();
                    if ( keepers.contains( key ) || key.startsWith( "rat" ) || key.startsWith( "macaque" )
                            || key.startsWith( "Macaque" ) /* || key.startsWith( "cat" ) */) {
                        corpus.add( doc );
                        continue outer;
                    }
                }
            }
        }
        log.info( "Filtered corpus size:" + corpus.size() );
        return corpus;
    }

    public void runCommonAnimals() throws Exception {
        Corpus animals = getCommonAnimalsCorpus();

        BrainRegionPipes testPipes = new BrainRegionPipes();
        // testPipes.addAllGoodPipes();
        testPipes.addTextPipe();

        log.info( "Doing random comparison" );
        Corpus random = SubSetUtil.getCorpusSubset( p2g.getTrainingCorp(), animals.size(), 1 );
        SimpleMalletRunner test = new SimpleMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens", random,
                false, 1, 1, 8, testPipes );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.compareFeatures();
        test.writeOutResults();

        log.info( "Doing animals comparison" );
        test = new SimpleMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens", animals, false, 1, 1, 8,
                testPipes );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.compareFeatures();
        test.writeOutResults();
    }

    public void runNoCommas() throws Exception {
        Corpus animals = getNoAbbrevCorpus();

        BrainRegionPipes testPipes = new BrainRegionPipes();
        testPipes.addAllGoodPipes();
        testPipes.addTextPipe();

        log.info( "Doing random comparison" );
        Corpus random = SubSetUtil.getCorpusSubset( p2g.getCorp(), animals.size(), 1 );
        SimpleMalletRunner test = new SimpleMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens", random,
                false, 2, 2, 8, testPipes );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.compareFeatures();
        test.writeOutResults();

        log.info( "Doing no commas comparison" );
        test = new SimpleMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens", animals, false, 2, 2, 8,
                testPipes );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.compareFeatures();
        test.writeOutResults();
    }

    public void labelUnSeenCorpus() throws Exception {
        int window = 2;
        BrainRegionPipes testPipes = new BrainRegionPipes();
        testPipes.addAllGoodPipes();
        testPipes.addTextPipe();

        Corpus allLabelled = p2g.getCorp();
        // allLabelled = p2g.getTrainingCorp();

        Corpus unSeen = p2g.getUnseenCorp();
        // unSeen = p2g.getRandomSubsetCorp();
        String annotationSet = "Mallet";
        String truthSet = "UnionMerge";
        String tokens = "TreeTaggerGATETokens";

        UnseenTestMalletRunner runner = new UnseenTestMalletRunner( annotationSet, truthSet, tokens, allLabelled,
                unSeen, false, window, window, testPipes );
        runner.reset();
        runner.run();
        runner.join();

    }

    public void runSemiTestBigCo() throws Exception {
        String semiLabels = "UnionMerge";
        int window = 2;
        int folds = 8;

        Corpus allLabelled = p2g.getTrainingCorp();
        List<ConnectionsDocument> allLabelledList = p2g.getDocuments( allLabelled );
        Collections.shuffle( allLabelledList, new Random( 1 ) );

        for ( int fold = 0; fold < folds; fold++ ) {
            BrainRegionPipes testPipes = new BrainRegionPipes();
            testPipes.addAllGoodPipes( false );
            // hacked up crossvalidation
            int splitSize = allLabelled.size() / folds;
            int start = fold * splitSize;
            int end = ( fold + 1 ) * splitSize;
            Corpus test = new CorpusImpl();
            for ( int j = start; j < end; j++ ) {
                test.add( allLabelledList.get( j ).getDocument() );
            }

            Corpus train = new CorpusImpl();
            train.addAll( allLabelled );
            train.removeAll( test );

            Corpus semi = p2g.getUnseenCorp();
            log.info( "Fold:" + fold );
            log.info( "train size:" + train.size() );
            log.info( "test size:" + test.size() );
            log.info( "semi size:" + semi.size() );

            log.info( "next - label semi" );
            // train, label unseen
            UnseenTestMalletRunner runner = new UnseenTestMalletRunner( semiLabels, "UnionMerge",
                    "TreeTaggerGATETokens", train, semi, false, window, window, testPipes );
            runner.reset();
            runner.run();
            runner.join();

            // train + unseen, test
            Corpus trainPlusSemi = new CorpusImpl();
            trainPlusSemi.addAll( train );
            trainPlusSemi.addAll( semi );

            // expand the pipes
            testPipes.addTextPipe();
            testPipes.addTreeTaggerLemmaPipe();

            // ** token target label using SemiLabels
            log.info( "Changing labels on semi set" );
            TokenTargetLabeller labeller = new TokenTargetLabeller( p2g, "TreeTaggerGATETokens", semiLabels, semi );
            labeller.generateSimpleTokenTargets();
            log.info( "Done labelling" );

            // train on first and second (semi labels), test on third
            log.info( "Second last test, train on two thirds like normal" );
            runner = new UnseenTestMalletRunner( "MalletSemi", "UnionMerge", "TreeTaggerGATETokens", trainPlusSemi,
                    test, false, window, window, testPipes );
            runner.reset();
            runner.run();
            runner.joinAndEvaluate();
            runner.compareFeatures();
            runner.writeOutResults();
            log.info( "Second last test above" );

            log.info( "Second last test, train on two thirds like normal" );
            testPipes = new BrainRegionPipes();
            testPipes.addTextPipe();
            testPipes.addTreeTaggerLemmaPipe();
            runner = new UnseenTestMalletRunner( "MalletSemi", "UnionMerge", "TreeTaggerGATETokens", trainPlusSemi,
                    test, false, window, window, testPipes );
            runner.reset();
            runner.run();
            runner.joinAndEvaluate();
            runner.compareFeatures();
            runner.writeOutResults();
            log.info( "Final test above" );

        }

    }

    public void runSemiTestBig() throws Exception {
        String semiLabels = "UnionMerge";
        int window = 2;
        BrainRegionPipes testPipes = new BrainRegionPipes();
        testPipes.addAllGoodPipes();
        testPipes.addTextPipe();
        int folds = 8;

        Corpus allLabelled = p2g.getTrainingCorp();
        List<ConnectionsDocument> allLabelledList = p2g.getDocuments( allLabelled );
        Collections.shuffle( allLabelledList, new Random( 1 ) );

        for ( int fold = 0; fold < folds; fold++ ) {
            // hacked up crossvalidation -may have issues with one or two at the end of the list
            int splitSize = allLabelled.size() / folds;
            int start = fold * splitSize;
            int end = ( fold + 1 ) * splitSize;
            Corpus test = new CorpusImpl();
            for ( int j = start; j < end; j++ ) {
                test.add( allLabelledList.get( j ).getDocument() );
            }

            Corpus train = new CorpusImpl();
            train.addAll( allLabelled );
            train.removeAll( test );

            // OLD Way
            // int splitSize = allLabelled.size() / 8;
            // Corpus train = Util.getCorpusSubset( allLabelled, splitSize * 7, 2 );
            //
            // Corpus test = new CorpusImpl();
            // test.addAll( allLabelled );
            // test.removeAll( train );

            Corpus semi = p2g.getUnseenCorp();
            log.info( "Fold:" + fold );
            log.info( "train size:" + train.size() );
            log.info( "test size:" + test.size() );
            log.info( "semi size:" + semi.size() );

            // train, test normal, for comparison only
            UnseenTestMalletRunner runner;
            runner = new UnseenTestMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens", train, test, false,
                    window, window, testPipes );
            runner.reset();
            runner.run();
            runner.joinAndEvaluate();
            runner.compareFeatures();
            runner.writeOutResults();
            log.info( "above was train/test normal" );
            log.info( "next - label semi" );

            // train, label unseen
            runner = new UnseenTestMalletRunner( semiLabels, "UnionMerge", "TreeTaggerGATETokens", train, semi, false,
                    window, window, testPipes );
            runner.reset();
            runner.run();
            runner.join();

            // train + unseen, test
            Corpus trainPlusSemi = new CorpusImpl();
            trainPlusSemi.addAll( train );
            trainPlusSemi.addAll( semi );

            // ** token target label using SemiLabels
            log.info( "Changing labels on semi set" );
            TokenTargetLabeller labeller = new TokenTargetLabeller( p2g, "TreeTaggerGATETokens", semiLabels, semi );
            labeller.generateSimpleTokenTargets();
            log.info( "Done labelling" );

            // train on first and second (semi labels), test on third
            log.info( "Second last test, train on two thirds like normal" );
            runner = new UnseenTestMalletRunner( "MalletSemi", "UnionMerge", "TreeTaggerGATETokens", trainPlusSemi,
                    test, false, window, window, testPipes );
            runner.reset();
            runner.run();
            runner.joinAndEvaluate();
            runner.compareFeatures();
            runner.writeOutResults();
            log.info( "Final test above" );
        }
        // put back labels, not needed
        // log.info( "putting back labels" );
        // labeller = new TokenTargetLabeller( p2g, "TreeTaggerGATETokens", "UnionMerge", trainPlusSemi );
        // labeller.generateSimpleTokenTargets();

    }

    public void runSemiTest() throws Exception {
        String semiLabels = "SemiLabels";
        int window = 1;
        BrainRegionPipes testPipes = new BrainRegionPipes();
        testPipes.addAllGoodPipes();
        testPipes.addTextPipe();

        // set up three sets
        Corpus all = p2g.getTrainingCorp();
        int splitSize = all.size() / 3;

        Corpus first = SubSetUtil.getCorpusSubset( all, splitSize, 1 );
        Corpus remaining = new CorpusImpl();
        remaining.addAll( all );
        remaining.removeAll( first );
        // split the remainder
        Corpus second = SubSetUtil.getCorpusSubset( remaining, splitSize, 1 );
        remaining.removeAll( second );
        Corpus third = remaining;
        log.info( "First size:" + first.size() );
        log.info( "Second size:" + second.size() );
        log.info( "Third size:" + third.size() );
        AnnotationComparator ac = new AnnotationComparator( semiLabels, "UnionMerge", "TreeTaggerGATETokens" );
        FMeasure f = ac.computeFMeasure( GateInterface.getDocuments( second ) );
        log.info( f.f1 );
        UnseenTestMalletRunner test;
        // train on first, test on third
        test = new UnseenTestMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens", first, third, false, window,
                window, testPipes );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.writeOutResults();
        log.info( "above was train on first and test on third" );
        log.info( "next - train on first, test on second -> label semi" );

        // train on first, test on second -> label semi
        test = new UnseenTestMalletRunner( semiLabels, "UnionMerge", "TreeTaggerGATETokens", first, second, false,
                window, window, testPipes );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.writeOutResults();

        log.info( "Label copy to first" );
        // first: copy truth labels from truth to semi
        MergeAnnotators merger = new MergeAnnotators();
        List<ConnectionsDocument> docs = GateInterface.getDocuments( first );
        for ( ConnectionsDocument cdoc : docs ) {
            Document doc = cdoc.getDocument();
            merger.merge( doc, MergeAnnotators.INTERSECT, "UnionMerge", "UnionMerge", semiLabels );
        }

        Corpus firstAndSecond = new CorpusImpl();
        firstAndSecond.addAll( first );
        firstAndSecond.addAll( second );

        // train on first and second (semi labels), test on third
        log.info( "Second last test, train on two thirds like normal" );
        test = new UnseenTestMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens", firstAndSecond, third,
                false, window, window, testPipes );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.writeOutResults();
        log.info( "Final test" );

        // ** token target label using SemiLabels
        log.info( "Changing labels" );
        TokenTargetLabeller labeller = new TokenTargetLabeller( p2g, "TreeTaggerGATETokens", semiLabels, firstAndSecond );
        labeller.generateSimpleTokenTargets();

        // here the truth for training is different from truth for testing
        test = new UnseenTestMalletRunner( semiLabels, semiLabels, "TreeTaggerGATETokens", firstAndSecond, third,
                false, window, window, testPipes );
        test.reset();
        test.run();
        test.join();
        test.evaluate( "UnionMerge" );
        test.writeOutResults( "UnionMerge" );
        log.info( "Done" );

        // put back labels
        log.info( "putting back labels" );
        labeller = new TokenTargetLabeller( p2g, "TreeTaggerGATETokens", "UnionMerge", firstAndSecond );
        labeller.generateSimpleTokenTargets();
    }

    public void runOriginal() throws Exception {
        // this should be 0.7884655 on the 100 sized database
        BrainRegionPipes testPipes = new BrainRegionPipes();
        testPipes.addAllGoodPipesOld();
        SimpleMalletRunner test = new SimpleMalletRunner( "Mallet", "UnionMerge", "TreeTagger", p2g.getTrainingCorp(),
                false, 1, 0, 8, testPipes );
        test.reset();
        test.run();
        // should be 0.7884655
        test.joinAndEvaluate();
    }

    public void runTestTrainFeatures() throws Exception {
        BrainRegionPipes testPipes = new BrainRegionPipes();
        // testPipes.addAllGoodPipesOld();
        testPipes.addTextPipe();
        Corpus train = p2g.getTrainingCorp();
        // train = Util.getCorpusSubset( p2g.getTrainingCorp(), 200, 1 );

        SimpleMalletRunner test = new SimpleMalletRunner( "MalletText", "UnionMerge", "TreeTaggerGATETokens", train,
                false, 0, 0, 8, testPipes );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        // test.writeOutResults();

        test.compareFeatures();
    }

    public void runWeights() throws Exception {
        BrainRegionPipes testPipes = new BrainRegionPipes();
        testPipes.addTextPipe();
        SimpleMalletRunner test = new SimpleMalletRunner( "MalletText", "UnionMerge", "TreeTaggerGATETokens", p2g
                .getCorp(), false, 2, 2, 8, testPipes );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.writeOutResults();

        FeatureCounter counter = new FeatureCounter( test, true );
        counter.run();
        DoubleMatrix<String, String> matrix = counter.getMatrix( test.getStatePairs().size() );
        for ( String statePair : test.getStatePairs() ) {
            matrix.addColumnName( statePair );
            log.info( "Doing transition " + statePair );
            HashMap<String, Double> weights = test.weightsTest( statePair );
            for ( String key : weights.keySet() ) {
                // if context is on some keys won't exist in matrix
                if ( counter.containsKey( key ) ) {
                    matrix.setByKeys( key, statePair, weights.get( key ) );
                }
            }
        }

        Util.writeRTable( "FeatureMatrix.csv", matrix );
    }

    public void runVanbug() throws Exception {
        BrainRegionPipes testPipes = new BrainRegionPipes();
        // testPipes.addAllGoodPipesOld();
        testPipes.addTextPipe();
        SimpleMalletRunner test = new SimpleMalletRunner( "MalletTextW0", "UnionMerge", "TreeTagger", p2g
                .getTrainingCorp(), false, 2, 2, 8, testPipes );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.writeOutResults();

        // BrainRegionPipes testPipes = new BrainRegionPipes();
        // testPipes.addAllGoodPipesOld();
        // SimpleMalletRunner test = new SimpleMalletRunner( "MalletFull", p2g.getTrainingCorp(), false, 2, 2, 8,
        // testPipes );
        // test.reset();
        // test.run();
        // test.joinAndEvaluate();
        // test.writeOutResults();

    }

    public void runMerge() throws Exception {
        BrainRegionPipes testPipes = new BrainRegionPipes();
        testPipes.addAllGoodPipesOld();
        SimpleMalletRunner test = new SimpleMalletRunner( "Mallet", "UnionMerge", "GATETokens", p2g.getNoAbbrevCorp(),
                false, 2, 2, 8, testPipes );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.writeOutResults();
    }

    public void noAbbrev2() throws Exception {
        BrainRegionPipes testPipes = new BrainRegionPipes();
        testPipes.addAllGoodPipes( true );
        testPipes.addTextPipe();
        SimpleMalletRunner test = new SimpleMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens", p2g
                .getNoAbbrevCorp(), false, 2, 2, 8, testPipes );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.compareFeatures();
        test.writeOutResults();

        testPipes = new BrainRegionPipes();
        testPipes.addAllGoodPipes( true );
        testPipes.addTextPipe();
        testPipes.addAbbreviationLexiconPipes();
        test = new SimpleMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens", p2g.getNoAbbrevCorp(), false, 2,
                2, 8, testPipes );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.compareFeatures();
        test.writeOutResults();

        testPipes = new BrainRegionPipes();
        testPipes.addAllGoodPipes( true );
        testPipes.addTextPipe();
        testPipes.addAbbreviationLexiconPipes();
        test = new SimpleMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens", p2g.getNoAbbrevCorp(), false, 2,
                2, 8, testPipes );
        test.setSentenceLevel( true );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.compareFeatures();
        test.writeOutResults();
    }

    public void runFeatureExp() throws Exception {
        BrainRegionPipes testPipes;
        int window = 2;

        testPipes = new BrainRegionPipes();
        testPipes.addTextPipe();
        testPipes.addAllGoodPipes();
        SimpleMalletRunner test = new SimpleMalletRunner( "MalletAll", "UnionMerge", "TreeTaggerGATETokens", p2g
                .getCorp(), false, window, window, 8, testPipes );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.compareFeatures();
        test.writeIndividualFMeasures();
        test.writeOutResults();

        testPipes = new BrainRegionPipes();
        testPipes.addAllGoodPipes( false );
        test = new SimpleMalletRunner( "MalletFeatures", "UnionMerge", "TreeTaggerGATETokens", p2g.getCorp(), false,
                window, window, 8, testPipes );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.compareFeatures();
        test.writeIndividualFMeasures();
        test.writeOutResults();

        testPipes = new BrainRegionPipes();
        testPipes.addTreeTaggerLemmaPipe();
        test = new SimpleMalletRunner( "MalletLemma", "UnionMerge", "TreeTaggerGATETokens", p2g.getCorp(), false,
                window, window, 8, testPipes );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.compareFeatures();
        test.writeIndividualFMeasures();
        test.writeOutResults();

    }

    public void runForPaper2() throws Exception {
        BrainRegionPipes testPipes;
        int window = 2;

        // lemma
        testPipes = new BrainRegionPipes();
        testPipes.addTreeTaggerLemmaPipe();
        runPaperOne( testPipes, window );

        // text
        testPipes = new BrainRegionPipes();
        testPipes.addTextPipe();
        runPaperOne( testPipes, window );

        // features
        testPipes = new BrainRegionPipes();
        testPipes.addAllGoodPipes( false );
        runPaperOne( testPipes, window );

        // all - order matters
        testPipes = new BrainRegionPipes();
        testPipes.addAllGoodPipes( true );
        testPipes.addTextPipe();
        runPaperOne( testPipes, window );

    }

    public void runForPaper() throws Exception {
        BrainRegionPipes testPipes;

        // Windowcontext
        testPipes = new BrainRegionPipes();
        testPipes.addTextPipe();
        testPipes.addAllGoodPipes();
        // testPipes.addFixes();
        testPipes.addWindowContext();
        // testPipes.addMMtxPipes();
        runPaperOne( testPipes, 2 );
        // System.exit( 1 );

        // all - order matters
        testPipes = new BrainRegionPipes();
        testPipes.addAllGoodPipes( true );
        testPipes.addTextPipe();
        runPaperOne( testPipes, 2 );

        // System.exit( 1 );

        // without POS
        testPipes = new BrainRegionPipes();
        testPipes.addTextPipe();
        testPipes.addTreeTaggerPOSPipe();
        runPaperOne( testPipes, 2 );
        // System.exit( 1 );

        // lemma
        testPipes = new BrainRegionPipes();
        testPipes.addTreeTaggerLemmaPipe();
        runPaperOne( testPipes, 2 );

        // text + lemma
        testPipes.addTextPipe();
        runPaperOne( testPipes, 2 );

        // Features + text
        testPipes = new BrainRegionPipes();
        testPipes.addAllGoodPipes( false );
        testPipes.addTextPipe();
        runPaperOne( testPipes, 2 );

        // all
        testPipes = new BrainRegionPipes();
        testPipes.addAllGoodPipes( true );
        testPipes.addTextPipe();
        runPaperOne( testPipes, 2 );

        AnnotationComparator ac = new AnnotationComparator( "UnionMerge", "Mallet", "BrainRegion" );
        DoubleMatrix<String, String> matrix = ac.getMatrix( p2g.getDocuments() );
        Util.writeRTable( "Docs.txt.csv", matrix );

        SimpleMalletRunner test = new SimpleMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens",
                p2g.getCorp(), false, 2, 2, 8, testPipes );
        test.setSentenceLevel( true );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.compareFeatures();
        test.writeOutResults();

        // testPipes.addTextPipe();
        // for ( int window = 0; window < 3; window++ ) {
        // runPaperOne( testPipes, window );
        // }
        //        
        // testPipes = new BrainRegionPipes();
        // testPipes.addAllGoodPipes( false );
        // runPaperOne( testPipes, 2 );
        //
        // testPipes = new BrainRegionPipes();
        // testPipes.addAllGoodPipes( true );
        // runPaperOne( testPipes, 2 );
        //
        // testPipes = new BrainRegionPipes();
        // testPipes.addAllGoodPipes( true );
        // testPipes.addTextPipe();
        // runPaperOne( testPipes, 2 );
    }

    public void runPaperOne( BrainRegionPipes pipes, int window ) throws Exception {
        SimpleMalletRunner test = new SimpleMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens",
                p2g.getCorp(), false, window, window, 8, pipes );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.compareFeatures();
        test.writeOutResults();
    }

    public void runLabMeeting() throws Exception {
        BrainRegionPipes testPipes = new BrainRegionPipes();
        // testPipes.addAllGoodPipes();
        testPipes.addTextPipe();

        SimpleMalletRunner test = new SimpleMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens", p2g
                .getTrainingCorp(), false, 1, 1, 8, testPipes );
        test.setBio( true );
        test.setSeed( 399 );
        test.reset();
        test.run();
        test.joinAndEvaluate();
        test.writeOutResults();
    }

    public void runInputSetsNoAbbrev() throws Exception {
        BrainRegionPipes testPipes = new BrainRegionPipes();
        testPipes.addAllGoodPipesOld();
        // testPipes.addTextPipe();
        // testPipes.addAbbreviationLexiconPipes();

        // String[] inputSets = { ConnectionsDocument.GATETOKENS, "TreeTaggerGATETokens" };
        String[] inputSets = { "TreeTaggerGATETokens" };
        for ( String inputSet : inputSets ) {
            log.info( "Using " + inputSet );

            SimpleMalletRunner test = new SimpleMalletRunner( "Mallet", "UnionMerge", inputSet, p2g.getNoAbbrevCorp(),
                    false, 2, 2, 8, testPipes );
            test.reset();
            test.run();
            test.joinAndEvaluate();
            test.writeOutResults();
        }
    }

    public void runInputSetsMerged() throws Exception {
        BrainRegionPipes testPipes = new BrainRegionPipes();
        testPipes.addAllGoodPipesOld();
        // testPipes.addTextPipe();
        // testPipes.addAbbreviationLexiconPipes();

        String[] inputSets = { "TreeTagger", ConnectionsDocument.GATETOKENS, "TreeTaggerGATETokens" };
        for ( String inputSet : inputSets ) {
            log.info( "Using " + inputSet );
            SimpleMalletRunner test = new SimpleMalletRunner( "Mallet", "UnionMerge", inputSet, p2g.getTrainingCorp(),
                    false, 2, 2, 8, testPipes );
            test.reset();
            test.run();
            test.joinAndEvaluate();
            test.writeOutResults();
        }
    }

    public void runMergedTest() throws Exception {
        MalletRunner runA = new UnseenTestMalletRunner( p2g.getTrainingCorp(), p2g.getRandomSubsetCorp(),
                "IntersectMerge", false, 2, 2 );
        runA.reset();
        runA.run();
        runA.joinAndEvaluate();
    }

    public void runIndividualFMeasures() throws Exception {
        BrainRegionPipes testPipes = new BrainRegionPipes();
        testPipes.addAllGoodPipes();

        MalletRunner runA = new SimpleMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens", p2g
                .getTrainingCorp(), false, 2, 2, 8, testPipes );
        // setting sentence
        runA.setSentenceLevel( true );
        runA.reset();
        runA.run();
        runA.joinAndEvaluate();
        runA.writeIndividualFMeasures();
        runA.writeOutResults();
    }

    public void runMany() throws Exception {
        BrainRegionPipes testPipes = new BrainRegionPipes();
        // testPipes.addAllGoodPipesOld();
        // testPipes.addAllGoodPipes();
        // testPipes.addTextPipe();
        // for ( int i = 0; i < 5; i++ ) {
        // SimpleMalletRunner test = new SimpleMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens", p2g
        // .getCorp(), false, 2, 2, 8, testPipes );
        // test.reset();
        // test.setSeed( i );
        // test.run();
        // test.joinAndEvaluate();
        // test.writeOutResults();
        // }
        //
        // testPipes.addTreeTaggerPOSPipe();
        // for ( int i = 0; i < 5; i++ ) {
        // SimpleMalletRunner test = new SimpleMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens", p2g
        // .getCorp(), false, 2, 2, 8, testPipes );
        // test.reset();
        // test.setSeed( i );
        // test.run();
        // test.joinAndEvaluate();
        // test.writeOutResults();
        // }

        // testPipes = new BrainRegionPipes();
        // testPipes.addTextPipe();
        // testPipes.addAllGoodPipes();
        // for ( int i = 0; i < 5; i++ ) {
        // SimpleMalletRunner test = new SimpleMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens", p2g
        // .getCorp(), false, 2, 2, 8, testPipes );
        // test.reset();
        // test.setSeed( i );
        // test.run();
        // test.joinAndEvaluate();
        // test.writeOutResults();
        // }

        testPipes = new BrainRegionPipes();
        testPipes.addTextPipe();
        testPipes.addAllGoodPipes();
        testPipes.addFixes();
        testPipes.addWindowContext();
        testPipes.addMMtxPipes();
        for ( int i = 0; i < 5; i++ ) {
            SimpleMalletRunner test = new SimpleMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens", p2g
                    .getCorp(), false, 2, 2, 8, testPipes );
            test.reset();
            test.setSeed( i );
            test.run();
            test.joinAndEvaluate();
            test.writeOutResults();
        }

        // testPipes = new BrainRegionPipes();
        // testPipes.addTreeTaggerLemmaPipe();
        // testPipes.addAllGoodPipes(false);
        // for ( int i = 0; i < 5; i++ ) {
        // SimpleMalletRunner test = new SimpleMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens", p2g
        // .getCorp(), false, 2, 2, 8, testPipes );
        // test.reset();
        // test.setSeed( i );
        // test.run();
        // test.joinAndEvaluate();
        // test.writeOutResults();
        // }

    }

    public void runWindowTest() throws Exception {
        Corpus all = p2g.getTrainingCorp();

        BrainRegionPipes testPipes = new BrainRegionPipes();
        // testPipes.addAllGoodPipesOld();
        testPipes.addAllGoodPipes();
        testPipes.addTextPipe();

        int seed = 1;
        int[] sizes = { 100, 200, 400, 600, 800, 1000 };
        for ( int size : sizes ) {
            for ( int window = 0; window < 3; window++ ) {
                Corpus corp = SubSetUtil.getCorpusSubset( all, size, seed );
                log.info( "Size:" + corp.size() );
                log.info( "Window:" + window );

                SimpleMalletRunner test = new SimpleMalletRunner( "Mallet", "UnionMerge", "TreeTaggerGATETokens", corp,
                        false, window, window, 8, testPipes );
                test.reset();
                test.run();
                test.joinAndEvaluate();
                test.writeOutResults();
            }
        }
    }

    public void runOld( String[] args ) throws Exception {
        // runSemiTestBig();
        // runCommonAnimals();
        // runLabMeeting();
        // runTestTrainFeatures();
        // runIndividualFMeasures();
        // runInputSetsMerged();
        // runLabMeeting();
        // runMergedTest();
        // runMany();
        // /// FIX LEMMA ON TREETAGER PIPE
        // runMany();
        // runInputSetsNoAbbrev();
        // runBidirect();
        // / make a MalletRun dealie

        // false then 1 before and 0 after
        // true then 0 before and 1 after

        // try window sizes for reverse and forward
        // for ( int windowBefore = 0; windowBefore < 4; windowBefore++ ) {
        // for ( int windowAfter = 0; windowAfter < 4; windowAfter++ ) {
        // // boolean[] directions = { false };
        // boolean[] directions = { false };
        // // boolean[] directions = { true, false };
        // for ( boolean direction : directions ) {
        // MalletRunner forRunA = new SimpleMalletRunner( p2g.getTrainingCorp(), direction, windowBefore,
        // windowAfter, 8 );
        // forRunA.reset();
        // forRunA.run();
        // forRunA.joinAndEvaluate();
        // forRunA.writeOutResults();
        // }
        // }
        // }
        // System.exit( 1 );

        // MalletRunner runA = new UnseenTestMalletRunner( p2g.getTrainingCorp(), p2g.getUnseenCorp(), false, 1, 0, 8 );
        // runA.reset();
        // runA.run();
        // runA.joinAndEvaluate();

        // so it does all features minus one, then no features plus one
        // grid search
        // boolean[] both = { true, false };
        // boolean ignoreCase = true;
        // // int windowBefore = 2;
        // // int windowAfter = 2;
        // for ( int windowBefore = 0; windowBefore < 4; windowBefore++ ) {
        // for ( int windowAfter = 0; windowAfter < 4; windowAfter++ ) {
        // // boolean[] directions = { false };
        // // boolean[] directions = { false };
        // boolean[] directions = { true, false };
        // for ( boolean direction : directions ) {
        // for ( boolean all : both ) {
        // for ( int i = 0; i < 14; i++ ) {
        // BrainRegionPipes pipes = new BrainRegionPipes();
        // // uses xor
        // // always use the text pipe
        // // if i==0 it will do all or just text
        // pipes.addTextPipe();
        // int j = 1;
        // if ( ( i == j++ ) ^ all ) pipes.addTreeTaggerPipes();
        // if ( ( i == j++ ) ^ all ) pipes.addOriginalMarkupPipes();
        // if ( ( i == j++ ) ^ all ) pipes.addAreaRegexPipes();
        // if ( ( i == j++ ) ^ all ) pipes.addSubstringRegexPipes();
        //
        // // if ( ( i == j++ ) ) pipes.addLexiconPipes();
        // // expanded lexicons below
        // if ( ( i == j++ ) ^ all ) pipes.addSmallLexicons( ignoreCase );
        // if ( ( i == j++ ) ^ all ) pipes.addTextPressoPipes( ignoreCase );
        // if ( ( i == j++ ) ^ all ) pipes.addBrainRegionLexicons( ignoreCase );
        // if ( ( i == j++ ) ^ all ) pipes.addPigeonLexicon( ignoreCase );
        // if ( ( i == j++ ) ^ all ) pipes.addAbbreviationLexiconPipes();
        // if ( ( i == j++ ) ^ all ) pipes.addAreaLexicons( ignoreCase );
        //
        // if ( ( i == j++ ) ^ all ) pipes.addHandMadeRegexPipes();
        // if ( ( i == j++ ) ^ all ) pipes.addLengthPipes();
        // if ( ( i == j++ ) ^ all ) pipes.addMalletNEPipes();
        // // if ( i == j++ ) break;
        //
        // log.info( "Pipes(" + pipes.size() + "):" + pipes.toString() );
        // MalletRunner runA = new SimpleMalletRunner( "PipeTest", p2g.getTrainingCorp(), direction,
        // windowBefore, windowAfter, 8, pipes );
        // runA.reset();
        // runA.run();
        // runA.joinAndEvaluate();
        // runA.writeOutResults();
        // }
        // }
        // }
        // }
        // }

    }

    private void silenceLogger( java.util.logging.Logger logger ) {
        logger.setFilter( new MalletLogFilter() );
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {

        String dataStore = null;
        if ( args.length > 0 ) {
            dataStore = args[0];
        }

        StopWatch watch = new StopWatch();
        watch.start();
        MalletQuick it = new MalletQuick( dataStore );

        it.run();
        System.out.println( "Time: " + watch ); // print execution time
    }

    public void run() throws Exception {
        // noAbbrev2();
        // runWeights();
        // runMany();
        // runForPaper2(); //-- last uncommented after paper, can be used for cross-validation testing
        labelUnSeenCorpus();
        // runFeatureExp();
        // getNoAbbrevCorpus();
        // runNoCommas();
    }

}

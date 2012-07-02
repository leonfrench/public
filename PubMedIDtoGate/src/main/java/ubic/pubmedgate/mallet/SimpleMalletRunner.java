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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.dataStructure.DoubleAddingMap;
import ubic.basecode.dataStructure.params.ParameterGrabber;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.mallet.features.TargetFromGATE;
import ubic.pubmedgate.ner.TokenTargetLabeller;
import cc.mallet.pipe.Noop;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.SequencePairAlignment;

public class SimpleMalletRunner extends MalletRunner {
    protected static Log log = LogFactory.getLog( SimpleMalletRunner.class );

    String inputTargetFeature;
    boolean bio;
    boolean reverse;
    boolean induceFeatures;
    int windowBefore, windowAfter;
    FoldRunner[] foldRunners;
    BrainRegionPipes brainPipes;
    int seed;
    Map<String, String> otherResults;

    protected SimpleMalletRunner() {
        getParams();
    }

    static Logger resultLog;
    static {
        setupResultLogger();
    }

    public void writeOutResults() {
        writeOutResults( truthSet );
    }

    public void writeOutResults( String truthAnnSet ) {
        Map<String, String> params = new HashMap<String, String>();
        params.putAll( getParams() );
        params.put( "size", documents.size() + "" );
        params.put( "pipeNamesSize", brainPipes.size() + "" );
        params.put( "pipes", brainPipes.toString() );
        List<String> sortedKeys = new LinkedList<String>( params.keySet() );

        // sort before adding results
        Collections.sort( sortedKeys );

        FMeasure f = evaluate( truthAnnSet );
        Map<String, String> results = ParameterGrabber.getParams( f.getClass(), f );
        log.info( "Params for results size:" + results.size() );
        params.putAll( results );

        params.putAll( otherResults );

        // add to end (unsorted)
        sortedKeys.addAll( results.keySet() );
        sortedKeys.addAll( otherResults.keySet() );

        String line = "";
        for ( String key : sortedKeys ) {
            line += key + "=" + params.get( key ) + ", ";
        }
        line = line.substring( 0, line.length() - 2 );
        resultLog.info( line );
    }

    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<String, String>();
        params.putAll( getParams( this.getClass() ) );
        params.putAll( getParams( this.getClass().getSuperclass() ) );
        return params;
    }

    public Map<String, String> getParams( Class<?> c ) {
        return ParameterGrabber.getParams( c, this );
    }

    private static void setupResultLogger() {
        resultLog = Logger.getLogger( "results" );
        PatternLayout layout = new PatternLayout( "%d{dd MMM yyyy HH:mm:ss} %C{1} %p | %m%n" );

        try {
            FileAppender appender = new FileAppender( layout, "results" + System.currentTimeMillis() + ".csv" );
            resultLog.addAppender( appender );
            resultLog.setLevel( ( Level ) Level.DEBUG );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * null annotationSet
     */
    public SimpleMalletRunner( String truthSet, String tokenSet, Corpus corpus, boolean reverse, int windowBefore,
            int windowAfter, int numFolds ) {
        this( null, truthSet, tokenSet, corpus, reverse, windowBefore, windowAfter, numFolds );
    }

    public SimpleMalletRunner( String annotationSet, String truthSet, String tokenSet, Corpus corpus, boolean reverse,
            int windowBefore, int windowAfter, int numFolds ) {
        this( null, truthSet, tokenSet, corpus, reverse, windowBefore, windowAfter, numFolds, null );
    }

    public SimpleMalletRunner( String annotationSet, String truthSet, String tokenSet, Corpus corpus, boolean reverse,
            int windowBefore, int windowAfter, int numFolds, BrainRegionPipes brainPipes ) {

        // for loggin
        if ( corpus.getDataStore() == null ) {
            dataStoreLocation = "null";
        } else {
            dataStoreLocation = corpus.getDataStore().getStorageUrl();
        }

        otherResults = new HashMap<String, String>();
        this.corpus = corpus;
        documents = GateInterface.getDocuments( corpus );
        this.truthSet = truthSet;
        seed = 1;

        induceFeatures = false;

        foldRunners = null;
        sentenceLevel = false;
        setBio( false );
        // inputTokenSet = ConnectionsDocument.GATETOKENS;
        inputTokenSet = tokenSet;
        this.numFolds = numFolds;

        // false then 1 before and 0 after
        // true then 0 before and 1 after
        this.reverse = reverse;
        this.windowBefore = windowBefore;
        this.windowAfter = windowAfter;

        if ( annotationSet == null ) {
            if ( reverse )
                annotationSet = "MalletReverse";
            else
                annotationSet = "Mallet";
        }
        this.annotationSet = annotationSet;

        // if we don't give brain pipes, then make a new one with the 'good pipes'
        if ( brainPipes == null ) {
            this.brainPipes = new BrainRegionPipes();
            try {
                this.brainPipes.addAllGoodPipes();
            } catch ( Exception e ) {
                throw new RuntimeException( e );
            }
        } else {
            // pipes provided by constructor
            this.brainPipes = brainPipes;
        }

        skipAbbrev = false;

        // getParams();
        // resultLog.info( "working" );

    }

    public HashMap<String, Double> weightsTest( String states ) {
        DoubleAddingMap<String> weights = new DoubleAddingMap<String>();
        for ( FoldRunner foldRunner : foldRunners ) {
            // combine all the folds
            DoubleAddingMap<String> foldWeights = foldRunner.weightsTest( states );
            weights.addPutAll( foldWeights );
        }
        return weights;
    }

    public Set<String> getStatePairs() {
        if ( !bio ) {
            String[] states = { TokenTargetLabeller.BRAIN_TARGET, TokenTargetLabeller.OUTSIDE_TARGET };
            Set<String> result = new HashSet<String>();
            for ( String s1 : states ) {
                for ( String s2 : states ) {
                    String statePair = s1 + "," + s2;
                    result.add( statePair );
                }
            }
            return result;
        } else
            return null;
    }

    public void compareFeatures() throws Exception {
        float sum = 0;
        float sumTotal = 0;
        for ( FoldRunner foldRunner : foldRunners ) {
            // feature count on training
            FeatureCounter counter = new FeatureCounter( this, false );
            counter.run( foldRunner.training );
            Set<String> trainFeatures = counter.getMap().keySet();
            int trainSize = trainFeatures.size();

            // feature count on test
            counter = new FeatureCounter( this, false );
            counter.run( foldRunner.testing );
            CountingMap<String> testMap = counter.getMap();
            Set<String> testFeatures = testMap.keySet();
            int testSize = testFeatures.size();
            long testSizeTotal = counter.getMap().summation();

            // stupid shallow copy error here
            Set<String> intersect = new HashSet<String>( trainFeatures );
            intersect.retainAll( testFeatures );
            int intersectSize = intersect.size();
            for ( String key : intersect ) {
                testMap.remove( key );
            }
            long intersectTotal = testMap.summation();

            log.info( "Train Features=" + trainSize + " Test Features=" + testSize + " Intersect:" + intersectSize
                    + " %unseen" + ( float ) intersect.size() / ( float ) testFeatures.size() );
            sum += ( float ) intersectSize / ( float ) testSize;
            log.info( "test summation:" + testSizeTotal + " intersect summation:" + intersectTotal + " %unseen"
                    + intersectTotal / ( float ) testSizeTotal );
            sumTotal += intersectTotal / ( float ) testSizeTotal;

            // for ( int i = 0; i < 20; i++ ) {
            // log.info( testMap.keySet().toArray()[i] );
            // }
        }

        log.info( "Percent of features not seen in training set: " + ( 1 - sum / ( float ) numFolds ) );
        log.info( "Percent of words not seen in training set: " + sumTotal / ( float ) numFolds );
        otherResults.put( "%NewFeatures", ( 1 - sum / ( float ) numFolds ) + "" );
        otherResults.put( "%NewWords", ( sumTotal / ( float ) numFolds ) + "" );
    }

    public void printTopWeights( HashMap<String, Double> weights ) {
        // convert to a list
        List<Map.Entry<String, Double>> features = new LinkedList<Map.Entry<String, Double>>( weights.entrySet() );

        // sort by the value
        Comparator c = new Comparator<Map.Entry<String, Double>>() {
            public int compare( Map.Entry<String, Double> a, Map.Entry<String, Double> b ) {
                return a.getValue().compareTo( b.getValue() );
            }
        };
        Collections.sort( features, c );
        for ( int i = 0; i < 50; i++ ) {
            String feature = features.get( i ).getKey();
            log.info( i + " " + features.get( i ) );
            // if ( feature.matches( ".+[/][+-]\\d$" ) ) log.info( i + " " + features.get( i ) );
        }
        log.info( "______________________ OTHER DIRECTION ____________________" );
        Collections.reverse( features );
        for ( int i = 0; i < 50; i++ ) {
            String feature = features.get( i ).getKey();
            log.info( i + " " + features.get( i ) );
            // if ( feature.matches( ".+[/][+-]\\d$" ) ) log.info( i + " " + features.get( i ) );
        }
    }

    public Collection<Pipe> getAllFoldPipes() throws Exception {

        Collection<Pipe> pipeList = new LinkedList<Pipe>();

        // convert GATE document in to many sentence based instances
        if ( !sentenceLevel ) {
            pipeList.add( new DocumentToSentencesPipe( inputTokenSet ) );
        }

        // convert GATE sentence annotations to Mallet tokens
        // this also makes the targets (should be refactored to be a different pipe)
        pipeList.add( new Sentence2TokenSequence( skipAbbrev, inputTokenSet ) );

        pipeList.addAll( brainPipes.getCurrentPipes() );

        return pipeList;
    }

    public Pipe getPipe() throws Exception {
        Collection<Pipe> pipeList = getAllFoldPipes();
        SerialPipes pipeSerial = new SerialPipes( pipeList );

        log.info( "number of pipes for all folds:" + pipeSerial.size() );
        Pipe pipes;
        if ( sentenceLevel ) {
            // the pipeSerial should be null
            pipes = pipeSerial;
        } else { // use the cache for abstracts
            // pipes = pipeSerial;
            pipes = new DocCachePipe( pipeSerial );
        }
        return pipes;
    }

    public InstanceList getInstanceList( List<ConnectionsDocument> documents ) throws Exception {
        // are we dealing with sentences or abstracts?

        InstanceList instanceList = new InstanceList( new Noop() );

        // get documents
        int count = 1;
        for ( ConnectionsDocument doc : documents ) {
            if ( count > 1 ) {
                log.info( "Stopping at " + ( count - 1 ) + " documents" );
                break;
            }
            Instance instance = new Instance( doc, null, null, null );
            instanceList.addThruPipe( instance );
            // count++;
        }

        if ( sentenceLevel ) {
            InstanceList instanceListx = new InstanceList( new DocumentToSentencesPipe( inputTokenSet ) );
            instanceListx.addThruPipe( instanceList.iterator() );
            instanceList = instanceListx;
            log.info( instanceList.size() + " sentences" );
        } else {
            log.info( instanceList.size() + " abstracts" );
        }

        for ( Instance i : instanceList ) {
            // log.info( "Training data:" + i.getName() );
        }

        return instanceList;
    }

    public void setSeed( int seed ) {
        this.seed = seed;
    }

    public void run() throws Exception {
        InstanceList instanceList = getInstanceList( documents );
        Pipe pipes = getPipe();
        // send the docs through the folds, then expand and classify
        Iterator<InstanceList[]> folds = instanceList.crossValidationIterator( numFolds, seed );

        Object notifyWhenDone = new Object();
        int fold = 0;

        int foldRunnersSize = Config.config.getInt( "whitetext.max_threads" );
        if ( numFolds < foldRunnersSize ) foldRunnersSize = numFolds;
        foldRunners = new FoldRunner[foldRunnersSize];
        boolean allDone = false;
        // while there is folds to do, do em
        synchronized ( notifyWhenDone ) {
            while ( !allDone ) {
                // init threads
                for ( int i = 0; i < foldRunners.length; i++ ) {
                    FoldRunner foldRunner = foldRunners[i];
                    // if we have something to do, and this thread is empty or done, then do it
                    if ( folds.hasNext() && ( foldRunner == null || foldRunner.isDone() ) ) {
                        log.info( "Starting fold:" + ++fold + " of " + numFolds );

                        InstanceList[] foldInstances = folds.next();

                        log.info( "Training size:" + foldInstances[0].size() + " Testing size:"
                                + foldInstances[1].size() );

                        // TokenSequence tt;
                        InstanceList training = new InstanceList( pipes );
                        training.addThruPipe( foldInstances[0].iterator() );

                        InstanceList testing = new InstanceList( pipes );
                        testing.addThruPipe( foldInstances[1].iterator() );

                        foldRunners[i] = new FoldRunner( fold, training, testing, notifyWhenDone, this );
                        // start it going
                        new Thread( foldRunners[i], "Thread-" + i ).start();
                    }
                }

                int stillRunning = 0;
                for ( FoldRunner thread : foldRunners ) {
                    if ( thread != null && !thread.isDone() ) stillRunning++;
                }

                log.info( "Main thread is waiting on " + stillRunning + " threads" );
                if ( fold < numFolds ) log.info( "Next fold is " + ( fold + 1 ) + "of " + numFolds );

                notifyWhenDone.wait();

                // check for all done
                allDone = !folds.hasNext();
                // if we ran out of folds then check to see if everyone is done
                if ( allDone ) {
                    for ( FoldRunner thread : foldRunners ) {
                        allDone = allDone && thread.isDone();
                    }
                }

            }
            // log.info( "Abstract cache size:" + pipes.cacheSize() );
            log.info( "Before window size = " + windowBefore + " after window size = " + windowAfter );
            log.info( "Pipes(" + brainPipes.size() + "):" + brainPipes.toString() );

        }

    }

    public List<SequencePairAlignment<java.lang.Object, java.lang.Object>> getTop10( Instance i ) {
        if ( foldRunners == null ) throw new RuntimeException( "Classifiers not trained yet" );
        // find the fold it is a test for and get it's top ten alignments
        for ( FoldRunner foldRunner : foldRunners ) {
            if ( foldRunner.isInTestSet( i ) ) {
                return foldRunner.getTop10( i );
            }
        }
        return null;
    }

    /*
     * Gets all instances, since features vary based on fold, it returns the test instances
     */
    public List<Instance> getAllInstances() {
        List<Instance> instances = new LinkedList<Instance>();
        for ( FoldRunner foldRunner : foldRunners ) {
            instances.addAll( foldRunner.getTestInstances() );
        }
        return instances;
    }

    public static Log getLog() {
        return log;
    }

    public boolean isBio() {
        return bio;
    }

    public String getInputTargetFeature() {
        return inputTargetFeature;
    }

    public String getInputTokenSet() {
        return inputTokenSet;
    }

    public boolean isReverse() {
        return reverse;
    }

    public int getWindowAfter() {
        return windowAfter;
    }

    public int getWindowBefore() {
        return windowBefore;
    }

    public boolean isInduceFeatures() {
        return induceFeatures;
    }

    public void setBio( boolean bio ) {
        if ( bio ) {
            inputTargetFeature = TargetFromGATE.GATE_BIO_TARGET_FEATURE;
        } else {
            inputTargetFeature = TargetFromGATE.GATE_SIMPLE_TARGET_FEATURE;
        }

        this.bio = bio;
    }

    public Collection<Pipe> getPipes() {
        return brainPipes.getCurrentPipes();
    }

    public void setInduceFeatures( boolean induceFeatures ) {
        this.induceFeatures = induceFeatures;
    }

    public boolean getSkipAbbrev() {
        return skipAbbrev;
    }
}

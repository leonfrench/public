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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//import ubic.GEOMMTx.evaluation.MakeHistogramData;
import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import cc.mallet.pipe.Noop;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.share.upenn.ner.FeatureWindow;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Sequence;

public class FeatureCounter {
    protected static Log log = LogFactory.getLog( FeatureCounter.class );

    Collection<ConnectionsDocument> docs;
    LinkedList<Pipe> pipes;
    CountingMap<String> map;
    String inputTokenSet;
    boolean skipAbbrev;
    boolean contextOnly;

    public FeatureCounter( SimpleMalletRunner runner, boolean contextOnly ) {
        this( runner.getDocuments(), runner.getPipes(), runner.getInputTokenSet(), runner.getWindowBefore(), runner
                .getWindowBefore(), runner.getSkipAbbrev(), contextOnly );
    }

    public FeatureCounter( List<ConnectionsDocument> docs, Collection<Pipe> pipes, String inputTokenSet,
            int windowBefore, int windowAfter, boolean skipAbbrev, boolean contextOnly ) {
        this.docs = docs;
        this.pipes = new LinkedList<Pipe>( pipes );
        this.skipAbbrev = skipAbbrev;
        map = new CountingMap<String>();
        this.inputTokenSet = inputTokenSet;
        this.contextOnly = contextOnly;

        if ( windowAfter != 0 || windowAfter != 0 ) {
            this.pipes.addLast( new FeatureWindow( windowAfter, windowBefore ) );
        }
        this.pipes.addLast( new TokenSequence2FeatureVectorSequence( true, false ) );

    }

    public int get( String feature ) {
        return map.get( feature );
    }

    public CountingMap<String> getMap() {
        return map;
    }

    public void run() {
        run( docs );
    }

    public void run( InstanceList instanceList ) {
        for ( Instance instance : instanceList ) {
            addInstance( instance );
        }
    }

    public void run( Collection<ConnectionsDocument> docsToRun ) {
        InstanceList instanceList = processDocs( docsToRun );
        log.info( "instancelist size:" + instanceList.size() );
        run( instanceList );
    }

    public InstanceList processDocs( Collection<ConnectionsDocument> docsToRun ) {
        InstanceList instanceList = new InstanceList( new Noop() );
        int count = 0;
        for ( ConnectionsDocument doc : docsToRun ) {
            if ( count > 11 ) {
                log.info( "Stopping at " + ( count - 1 ) + " documents" );
                break;
            }
            Instance instance = new Instance( doc, null, null, null );
            instanceList.addThruPipe( instance );
            // count++;
        }

        pipes.addFirst( new Sentence2TokenSequence( skipAbbrev, inputTokenSet ) );
        pipes.addFirst( new DocumentToSentencesPipe( inputTokenSet ) );

        SerialPipes pipeSerial = new SerialPipes( pipes );
        InstanceList instanceListx = new InstanceList( pipeSerial );
        instanceListx.addThruPipe( instanceList.iterator() );
        instanceList = instanceListx;
        return instanceList;
    }

    public void addInstance( Instance instance ) {
        Sequence input = ( Sequence ) instance.getData();
        for ( int j = 0; j < input.size(); j++ ) {
            FeatureVector fv = ( FeatureVector ) input.get( j );
            addToken( fv );
            // log.info( "token:" + j + " " + fv.toString( true ) );
        }
    }

    public void addToken( FeatureVector fv ) {
        int[] indices = fv.getIndices();
        Alphabet dictionary = fv.getAlphabet();
        int indicesLength = fv.numLocations();
        for ( int i = 0; i < indicesLength; i++ ) {
            String key = dictionary.lookupObject( indices[i] ).toString();
            // log.info( key );
            if ( !contextOnly
                    || ( contextOnly && ( key.endsWith( "/+1" ) || key.endsWith( "/-1" ) || key.endsWith( "/-2" ) || key
                            .endsWith( "/+2" ) ) ) ) {
                map.increment( key );
            }
        }
    }

    public boolean containsKey( String key ) {
        return map.containsKey( key );
    }

    public String toString() {
        return map.toString();
    }

    public DoubleMatrix<String, String> getMatrix() {
        return getMatrix( 0 );
    }

    /*
     * blank columns for adding other information to the matrix, like weights
     */
    public DoubleMatrix<String, String> getMatrix( int blankColumns ) {
        String countColName = "count";
        List<String> keys = new LinkedList<String>( map.keySet() );
        Collections.sort( keys );
        DoubleMatrix<String, String> resultMatrix = new DenseDoubleMatrix<String, String>( keys.size(),
                1 + blankColumns );
        resultMatrix.addColumnName( countColName );

        for ( String key : keys ) {
            resultMatrix.addRowName( key );
            resultMatrix.setByKeys( key, countColName, ( double ) map.get( key ) );
        }
        return resultMatrix;
    }

    public int getSize() {
        List<String> keys = new LinkedList<String>( map.keySet() );
        return keys.size();
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        GateInterface p2g = new GateInterface();

        BrainRegionPipes pipes = new BrainRegionPipes();
        pipes.addTextPipe();
        // pipes.addBrainRegionLexicons( true );

        FeatureCounter fc = new FeatureCounter( p2g.getDocuments(), pipes.getCurrentPipes(), "TreeTaggerGATETokens", 0,
                0, false, false );
        fc.run();
        DoubleMatrix<String, String> matrix = fc.getMatrix();
        // log.info( fc.toString() );
//        MakeHistogramData.writeRTable( "FeatureCountsAllDocuments.csv", matrix );
    }
}

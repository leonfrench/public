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

package ubic.pubmedgate.interactions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.Util;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.pubmedgate.Config;
import au.com.bytecode.opencsv.CSVReader;

public class SLOutputReader {
    protected static Log log = LogFactory.getLog( SLOutputReader.class );

    boolean unseenSetup;
    File baseFolder;
    Map<String, Double> scores;
    Map<String, Double> truth;

    // requires properly named set
    public static SLOutputReader getCCSLReader( String testSet ) throws Exception {
        String trainingSet = "WhiteTextNegFixFull";

        String baseFolder = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Saved Results/SL/CC/NegFixFullOn" + testSet + "/";

        SLOutputReader SLReader = new SLOutputReader( trainingSet, testSet, baseFolder );
        return SLReader;
    }

    public Map<String, Double> getScores() {
        return scores;
    }

    public Map<String, Double> getTruth() {
        return truth;
    }

    public List<String> sortPairUsingScore( List<String> input ) {
        Collections.sort( input, SCORE_ORDER );
        return input;
    }

    final Comparator<String> SCORE_ORDER = new Comparator<String>() {
        public int compare( String a, String b ) {
            return ( scores.get( a ) < scores.get( b ) ) ? -1 : 1;
        }
    };

    public List<String> getTruePositives() {
        return filter( true, true );
    }

    public List<String> getFalsePositives() {
        return filter( false, true );
    }

    public List<String> getFalseNegatives() {
        return filter( true, false );
    }

    /**
     * Not in original order
     * 
     * @return
     */
    public List<String> getAll() {
        return new LinkedList( truth.keySet() );
    }

    // those annotated as positive
    public List<String> getPositives() {
        boolean truth = true;
        List<String> result = filter( truth, false );
        result.addAll( filter( truth, true ) );
        return result;
    }

    //
    public List<String> getPositivePredictions() {
        boolean score = true;
        List<String> result = filter( false, score );
        result.addAll( filter( true, score ) );
        return result;
    }

    public List<String> getNegativePredictions() {
        boolean score = false;
        List<String> result = filter( false, score );
        result.addAll( filter( true, score ) );
        return result;
    }

    public List<String> getNegatives() {
        boolean truth = false;
        List<String> result = filter( truth, false );
        result.addAll( filter( truth, true ) );
        return result;
    }

    public void writeRTable() throws Exception {
        Collection<String> pairs = scores.keySet();
        DoubleMatrix<String, String> matrix = new DenseDoubleMatrix<String, String>( scores.size(), 3 );
        matrix.addColumnName( "pair" );
        matrix.addColumnName( "prediction" );
        matrix.addColumnName( "label" );
        for ( String pair : pairs ) {
            matrix.addRowName( pair );
            matrix.setByKeys( pair, "prediction", scores.get( pair ) );
            matrix.setByKeys( pair, "label", truth.get( pair ) );
        }
        Util.writeRTable( baseFolder + System.getProperty( "file.separator" ) + "predictionsForR.txt", matrix );
    }

    public List<String> getTrueNegatives() {
        return filter( false, false );
    }

    private List<String> filter( boolean truthPostive, boolean scorePositive ) {
        List<String> result = new LinkedList<String>();
        for ( String pair : scores.keySet() ) {
            if ( ( scores.get( pair ) > 0 == scorePositive ) && ( truth.get( pair ) > 0 == truthPostive ) ) {
                result.add( pair );
            }
        }
        return result;
    }

    /**
     * For loading in unseen predictions
     * 
     * @param trainingSet
     * @param testingSet
     * @param baseFolder
     * @throws Exception
     */
    public SLOutputReader( String trainingSet, String testingSet, String baseFolder ) throws Exception {
        this.baseFolder = new File( baseFolder );
        unseenSetup = true;

        scores = new HashMap<String, Double>();
        truth = new HashMap<String, Double>();

        String predictionFile = baseFolder + trainingSet + "/" + testingSet + ".predict";
        String corpusFile = baseFolder + testingSet + "/" + "corpus.txt";

        log.info( "Prediction file:" + predictionFile );
        log.info( "Corpus file:" + corpusFile );

        CSVReader predictionReader = new CSVReader( new FileReader( predictionFile ), ' ' );
        CSVReader corpusReader = new CSVReader( new FileReader( corpusFile ), '\t' );
        List<String[]> predictionLines = predictionReader.readAll();
        List<String[]> corpusLines = corpusReader.readAll();
        int size = predictionLines.size();
        if ( corpusLines.size() != size ) {
            throw new RuntimeException( "Error, line count is not equal" );
        }

        for ( int i = 0; i < size; i++ ) {
            String pairID = corpusLines.get( i )[1];
            String predictionStringScore = predictionLines.get( i )[2];

            double score = Double.parseDouble( predictionStringScore );
            scores.put( pairID, score );

            // assume all false - for uncurated data
            truth.put( pairID, 0d );

        }

        log.info( "Done reading, pair count:" + size );

    }

    public SLOutputReader( File baseDirectory ) throws Exception {
        unseenSetup = false;
        baseFolder = baseDirectory;
        scores = new HashMap<String, Double>();
        truth = new HashMap<String, Double>();
        int pairCount = 0;
        for ( File subFolder : baseFolder.listFiles() ) {
            if ( subFolder.getName().endsWith( "predict" ) ) {
                pairCount += loadFile( pairCount, subFolder );
            }
            if ( subFolder.isDirectory() ) {
                log.info( subFolder.getName() );
                if ( subFolder.getName().equals( ".svn" ) ) {
                    log.info( "Skipping" );
                    continue;
                }

                for ( File predictionFile : subFolder.listFiles() ) {
                    pairCount += loadFile( pairCount, predictionFile );
                }
            }
        }
        log.info( "Done reading, pair count:" + pairCount );
    }

    private int loadFile( int pairCount, File predictionFile ) throws FileNotFoundException, IOException {
        log.info( predictionFile.toString() );
        CSVReader predictionReader = new CSVReader( new FileReader( predictionFile ), ' ' );
        List<String[]> lines = predictionReader.readAll();
        for ( String[] line : lines ) {
            pairCount++;
            String pairID = line[0];
            double score = Double.parseDouble( line[2] );
            scores.put( pairID, score );
            double truthValue = Double.parseDouble( line[1] );
            truth.put( pairID, truthValue );
        }
        return pairCount;
    }

    public boolean isUnseenSetup() {
        return unseenSetup;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        String testSet = "Annotated";
        String annotationSet = "Suzanne";

        // String airolaXML = Config.config.getString( "whitetextweb.airolaXML" );
        // System.setProperty( Gate.BUILTIN_CREOLE_DIR_PROPERTY_NAME, "file:" + System.getProperty( "user.dir" ) + "/"
        // );
        //
        // String SLFolder = Config.config.getString( "whitetextweb.SLResults" );
        String folder = "/home/leon/ppi-benchmark/Saved Results/SL/CV/WhiteTextNegFixFull/predict/WhiteTextNegFixFull";

        // GateInterface p2g = new GateInterface();
        // p2g.setUnSeenCorpNull();
        // p2g.setNamedCorpNull( "PubMedUnseenJNChem" );
        // p2g.setNamedCorpNull( "PubMedUnseenJCN" );
        // p2g.setNamedCorpNull( "PubMedUnseenMScan1" );

        SLOutputReader reader = new SLOutputReader( new File( folder ) );

        // AirolaXMLReader XMLReader = new AirolaXMLReader( airolaXML, p2g, annotationSet );

        log.info( "pos predictions:" + reader.getPositivePredictions().size() );
        log.info( "neg predictions:" + reader.getNegativePredictions().size() );
        for ( String pair : reader.getPositivePredictions() ) {
            log.info( pair );
            double score = reader.getScores().get( pair );
        }
        log.info( "DONE" );

    }

    public static void main2( String[] args ) throws Exception {

        String folder;
        folder = "/home/leon/ppi-benchmark/Experiments/SL/CV/predict/WhiteText";

        folder = "/home/leon/ppi-benchmark/Saved Results/SL/CV/WhiteTextNegFixFull/predict/WhiteTextNegFixFull";
        // SLOutputReader reader = new SLOutputReader( new File( folder ));

        // CC
        String trainingSet = "WhiteTextNegFixFull";
        String testSet = "WhiteTextUnseen";

        folder = "/home/leon/ppi-benchmark/Saved Results/SL/CC/NegFixFullOnUnseen/";

        SLOutputReader reader = new SLOutputReader( trainingSet, testSet, folder );
        log.info( "pos predictions:" + reader.getPositivePredictions().size() );
        log.info( "neg predictions:" + reader.getNegativePredictions().size() );
        // reader.writeRTable();
    }

}

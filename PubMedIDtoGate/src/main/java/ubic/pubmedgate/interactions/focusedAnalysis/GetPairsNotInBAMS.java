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

package ubic.pubmedgate.interactions.focusedAnalysis;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.AnalyzeBAMSandAllenGenes.Direction;
import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.interactions.AirolaXMLReader;
import ubic.pubmedgate.interactions.NormalizePairs;
import ubic.pubmedgate.interactions.NormalizeResult;
import ubic.pubmedgate.interactions.SLOutputReader;
import ubic.pubmedgate.interactions.evaluation.CreateInteractionSpreadsheet;
import ubic.pubmedgate.interactions.evaluation.NormalizedConnection;

public class GetPairsNotInBAMS {
    protected static Log log = LogFactory.getLog( GetPairsNotInBAMS.class );

    /**
     * Run on server (uses unseen set)
     * 
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        // TODO Auto-generated method stub
        getPredictionsMissingFromBAMS();
        // getAnnotatedMissingFromBAMS(); // takes 74 minutes
    }

    public static void getPredictionsMissingFromBAMS() throws Exception {
        boolean eraTest = false;
        boolean runAll = false;
        boolean notInBAMS = true;
        // gives all positive predicted rat pairs that are normalized in the unseen set

        String testSet = "WhiteTextUnseen";
        NormalizeResult normResult = NormalizePairs.runUnseen( eraTest, runAll, notInBAMS, testSet, NormalizePairs.RAT );
        Set<NormalizedConnection> normalizedPairs = normResult.normalizedPairs;

        // check against BAMS
        Direction direction = Direction.ANYDIRECTION;
        boolean propigated = true;
        DoubleMatrix<String, String> BAMSconnectionMatrix = NormalizePairs.getBAMSConnectionMatrix( propigated, direction );
        Set<NormalizedConnection> pairsNotInBAMS = new HashSet<NormalizedConnection>();
        Set<String> allPairs = new HashSet<String>();

        int disconnectedPairs = 0;
        for ( NormalizedConnection normC : normalizedPairs ) {
            String regionA = normC.regionA;
            String regionB = normC.regionB;
            String pairID = normC.pairID;

            allPairs.add( pairID );

            double BAMSValue = BAMSconnectionMatrix.getByKeys( regionA, regionB );
            if ( BAMSValue == 0 ) {
                disconnectedPairs++;
                pairsNotInBAMS.add( normC );
            }

        }

        log.info( "All pairs (by pairID):" + allPairs.size() );
        log.info( "All pair - normalized regions:" + normalizedPairs.size() );
        log.info( "Pairs not in BAMS:" + disconnectedPairs );
        log.info( "Pairs not in BAMS:" + pairsNotInBAMS.size() );

        // check against hand annotations

        // create spreadsheet
        GateInterface p2g = new GateInterface();

        String baseFolder = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Saved Results/SL/CC/NegFixFullOnUnseen/";
        String corpusFilename = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Corpora/Original-Modified/WhiteTextUnseen.orig.xml";
        String trainingSet = "WhiteTextNegFixFull";
        String annotationSet = "Mallet";
        AirolaXMLReader XMLReader = new AirolaXMLReader( corpusFilename, p2g, annotationSet );
        SLOutputReader SLReader = new SLOutputReader( trainingSet, testSet, baseFolder );

        String filename;
        filename = Config.config.getString( "whitetext.iteractions.results.folder" ) + "NotInBAMS.xls";
        CreateInteractionSpreadsheet test = new CreateInteractionSpreadsheet( filename, SLReader, XMLReader );
        test.populate( pairsNotInBAMS );
        test.save();

    }

    public static void getAnnotatedMissingFromBAMS() throws Exception {
        boolean eraTest = false;
        boolean runAll = false;
        boolean notInBAMS = true; // will run several times (rat+neg) because this flag isnt used
        boolean usePredictions = false;
        // gives all positive predicted rat pairs that are normalized in the unseen set

        // below code could be merged with this to save time
        NormalizeResult normResult = NormalizePairs.analyseTest( usePredictions, eraTest, runAll, NormalizePairs.RAT );
        Set<NormalizedConnection> normalizedPairs = normResult.normalizedPairs;

        // check against BAMS
        Direction direction = Direction.ANYDIRECTION;
        boolean propigated = true;
        DoubleMatrix<String, String> BAMSconnectionMatrix = NormalizePairs.getBAMSConnectionMatrix( propigated, direction );
        Set<NormalizedConnection> pairsNotInBAMS = new HashSet<NormalizedConnection>();
        Set<String> allPairs = new HashSet<String>();

        int disconnectedPairs = 0;
        for ( NormalizedConnection normC : normalizedPairs ) {
            String regionA = normC.regionA;
            String regionB = normC.regionB;
            String pairID = normC.pairID;

            allPairs.add( pairID );

            double BAMSValue = BAMSconnectionMatrix.getByKeys( regionA, regionB );
            if ( BAMSValue == 0 ) {
                disconnectedPairs++;
                pairsNotInBAMS.add( normC );
            }
        }

        log.info( "All pairs (by pairID):" + allPairs.size() );
        log.info( "All pair - normalized regions:" + normalizedPairs.size() );
        log.info( "Pairs not in BAMS:" + disconnectedPairs );
        log.info( "Pairs not in BAMS:" + pairsNotInBAMS.size() );

        // check against hand annotations

        // create spreadsheet
        GateInterface p2g = new GateInterface();
        p2g.setUnSeenCorpNull();

        String annotationSet = "Suzanne";

        String baseFolder = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Saved Results/SL/CV/WhiteTextNegFixFull/predict/WhiteTextNegFixFull";
        String corpusFilename = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Corpora/Original-Modified/WhiteTextNegFixFull.xml";

        AirolaXMLReader XMLReader = new AirolaXMLReader( corpusFilename, p2g, annotationSet );
        SLOutputReader SLReader = new SLOutputReader( new File( baseFolder ) );

        String filename = Config.config.getString( "whitetext.iteractions.results.folder" ) + "NotInBAMS.annotated.xls";
        CreateInteractionSpreadsheet test = new CreateInteractionSpreadsheet( filename, SLReader, XMLReader );
        test.populate( pairsNotInBAMS );
        test.save();
    }
}

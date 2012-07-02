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

package ubic.pubmedgate.interactions.evaluation;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.interactions.AirolaXMLReader;
import ubic.pubmedgate.interactions.SLOutputReader;

public class CreateSpreadSheetForRecall {
    private static Log log = LogFactory.getLog( CreateSpreadSheetForRecall.class.getName() );

    public static void main( String[] args ) throws Exception {
        sentencesWithNoPredictions();
    }

    public static void sentencesWithNoPredictions() throws Exception {

        String baseFolder = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Saved Results/SL/CC/NegFixFullOnUnseen/";
        String corpusFilename = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Corpora/Original-Modified/WhiteTextUnseen.orig.xml";
        String trainingSet = "WhiteTextNegFixFull";
        String testSet = "WhiteTextUnseen";
        String annotationSet = "Mallet";

        SLOutputReader SLReader = new SLOutputReader( trainingSet, testSet, baseFolder );
        log.info( "Score size:" + SLReader.getScores().size() );

        GateInterface p2g = new GateInterface();
        p2g.setUnSeenCorpNull();
        p2g.setNamedCorpNull( "PubMedUnseenJNChem" );
        // p2g.setNamedCorpNull("PubMedUnseenJCN");
        p2g.setNamedCorpNull( "PubMedUnseenMScan1" );
        p2g.setNamedCorpNull( "PubMedUnseenMScan2" );
        AirolaXMLReader XMLReader = new AirolaXMLReader( corpusFilename, p2g, annotationSet );

        Set<String> sentences = XMLReader.getAllSentences();
        log.info( "All sentences:" + sentences.size() );
        List<String> posPredictedPairs = SLReader.getPositivePredictions();
        StringToStringSetMap sentenceIDToPairs = XMLReader.getSentenceIDToPairs();
        int sentencesWithPosPrecitions = 0;
        int sentencesWithOnlyNegPrecitions = 0;
        int pairsFromSenPosPrecitions = 0;
        int pairsFromSenOnlyNegPrecitions = 0;
        Set<String> pairsToUse = new HashSet<String>();
        log.info( "All pairs:" + SLReader.getAll().size() );

        for ( String sentence : sentences ) {
            Set<String> pairs = new HashSet<String>( sentenceIDToPairs.get( sentence ) );
            int pairCount = pairs.size();
            pairs.retainAll( posPredictedPairs );
            if ( pairs.size() > 0 ) {
                sentencesWithPosPrecitions++;
                pairsFromSenPosPrecitions += pairCount;
            } else {
                sentencesWithOnlyNegPrecitions++;
                pairsFromSenOnlyNegPrecitions += pairCount;
                double d = Math.random();
                // this number is based on the ratio of sentencesWithPosPrecitions/sentencesWithOnlyNegPrecitions
                if ( d < ( 0.195d / 2 ) ) {
                    pairsToUse.addAll( sentenceIDToPairs.get( sentence ) );
                }
            }

        }
        log.info( "sentencesWithOnlyNegPrecitions:" + sentencesWithOnlyNegPrecitions );
        log.info( "pairsFromSenOnlyNegPrecitions:" + pairsFromSenOnlyNegPrecitions );
        log.info( "" );
        log.info( "sentencesWithPosPrecitions:" + sentencesWithPosPrecitions );
        log.info( "pairsFromSenPosPrecitions:" + pairsFromSenPosPrecitions );
        log.info( "pairsToUse:" + pairsToUse.size() );
        
//        System.exit(1);

        String filename = Config.config.getString( "whitetext.iteractions.results.folder" )
                + "EvalForRecall.negatives.xls";
        CreateInteractionSpreadsheet test = new CreateInteractionSpreadsheet( filename, SLReader, XMLReader );
        test.populate( new LinkedList<String>( pairsToUse ) );
        test.save();

    }

    /**
     * loads in airolaXML created by AirolaForEvaluations.java
     * 
     * @throws Exception
     */
    public static void negativePredictionsFrom2000Set() throws Exception {
        GateInterface p2g = new GateInterface();
        p2g.setUnSeenCorpNull();
        p2g.setNamedCorpNull( "PubMedUnseenJNChem" );
        // p2g.setNamedCorpNull("PubMedUnseenJCN");
        p2g.setNamedCorpNull( "PubMedUnseenMScan1" );
        p2g.setNamedCorpNull( "PubMedUnseenMScan2" );

        // modify this for the XML file being used
        String corpusFilename = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Corpora/Original-Modified/WhiteTextUnseenEval.justPos.xml";

        String baseFolder = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Saved Results/SL/CC/NegFixFullOnUnseen/";

        String trainingSet = "WhiteTextNegFixFull";
        String testSet = "WhiteTextUnseen";
        String annotationSet = "Mallet";
        AirolaXMLReader XMLReader = new AirolaXMLReader( corpusFilename, p2g, annotationSet );
        SLOutputReader SLReader = new SLOutputReader( trainingSet, testSet, baseFolder );

        String filename;
        filename = Config.config.getString( "whitetext.iteractions.results.folder" ) + "EvalForRecall.xls";

        CreateInteractionSpreadsheet test = new CreateInteractionSpreadsheet( filename, SLReader, XMLReader );

        log.info( "Number of pairs in XML:" + XMLReader.getPairCount() );
        test.populate( XMLReader.getAllPairs() );
        test.save();
    }
}

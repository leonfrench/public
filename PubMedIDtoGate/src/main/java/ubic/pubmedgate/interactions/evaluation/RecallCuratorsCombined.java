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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.basecode.io.excel.SpreadSheetSchema;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.interactions.AirolaXMLReader;
import ubic.pubmedgate.interactions.SLOutputReader;
import ubic.pubmedgate.interactions.focusedAnalysis.LoadInteractionRecallSpreadsheet;

public class RecallCuratorsCombined {
    protected static Log log = LogFactory.getLog( RecallCuratorsCombined.class );

    Set<LoadInteractionRecallSpreadsheet> curators;

    public void load6000Set() throws Exception {
        curators = new HashSet<LoadInteractionRecallSpreadsheet>();

        SpreadSheetSchema schema = new ForRecallEvaluationSchema();
        // row 2-1001 inclusive, Olivia and Artemis
        // 1002-2001, Luchia and Artemis
        // 2002-3001, Luchia and Olivia
        // 3002-4001, Tianna and Artemis
        // 4002-5001, Tianna and Olivia
        // 5002-6001, Tianna and Luchia
        String baseFolder = Config.config.getString( "whitetext.iteractions.evaluations.folder" )
                + "evaluations for Recall/second 6000 set/";

        Set<Integer> rows = new HashSet<Integer>();

        // 2-1001 2001-3001 4002-5001
        // /grp/java/workspace/PubMedIDtoGate/spreadsheets/interaction.evaluations/checkup on 6000
        // set/EvalForRecall.all2000.plusNegs.Olivia.xls
        for ( int i = 2; i <= 1001; i++ ) {
            rows.add( i );
        }

        for ( int i = 2002; i <= 3001; i++ ) {
            rows.add( i );
        }
        for ( int i = 4002; i <= 5001; i++ ) {
            rows.add( i );
        }
        curators.add( new LoadInteractionRecallSpreadsheet( baseFolder + "EvalForRecall.all2000.plusNegs.Olivia.xls",
                rows, "Olivia", schema ) );

        // 1002-3001
        // 5002-6001
        rows = new HashSet<Integer>();
        for ( int i = 1002; i <= 3001; i++ ) {
            rows.add( i );
        }
        for ( int i = 5002; i <= 6001; i++ ) {
            rows.add( i );
        }
        curators.add( new LoadInteractionRecallSpreadsheet( baseFolder + "Luchia - Whitetext Batch 2.xls", rows,
                "Luchia", schema ) );

        // 3002-6001
        rows = new HashSet<Integer>();
        for ( int i = 3002; i <= 6001; i++ ) {
            rows.add( i );
        }
        curators.add( new LoadInteractionRecallSpreadsheet( baseFolder + "WhiteText2.xls", rows, "Tianna", schema ) );

        // 2-2001
        // 3002-4001
        rows = new HashSet<Integer>();
        for ( int i = 2; i <= 2001; i++ ) {
            rows.add( i );
        }
        for ( int i = 3002; i <= 4001; i++ ) {
            rows.add( i );
        }
        curators.add( new LoadInteractionRecallSpreadsheet( baseFolder + "Artemis combined.xls", rows, "Artemis",
                schema ) );

    }

    public void loadFirst400Set() throws Exception {
        curators = new HashSet<LoadInteractionRecallSpreadsheet>();
        Set<Integer> rows = new HashSet<Integer>();
        for ( int i = 1; i < 401; i++ ) {
            rows.add( i );
        }

        SpreadSheetSchema schema = new ForRecallEvaluationSchema();

        String baseFolder = Config.config.getString( "whitetext.iteractions.evaluations.folder" )
                + "evaluations for Recall/";
        curators.add( new LoadInteractionRecallSpreadsheet(
                baseFolder + "EvalForRecall.all2000.edit.400.Olivia.v2.xls", rows, "Olivia", schema ) );
        curators.add( new LoadInteractionRecallSpreadsheet( baseFolder + "Luchia - Batch 1.xls", rows, "Luchia", schema ) );
        curators.add( new LoadInteractionRecallSpreadsheet( baseFolder + "WhiteText version1.xls", rows, "Tianna",
                schema ) );
        curators.add( new LoadInteractionRecallSpreadsheet( baseFolder + "EvalForRecall.all2000.edit.alai.xls", rows,
                "Artemis", schema ) );

    }

    public void printAllStats() throws Exception {
        for ( LoadInteractionSpreadsheet sheet : curators ) {
            sheet.printStats();
        }
    }

    public void retainPairs( Set<String> pairs ) throws Exception {
        for ( LoadInteractionRecallSpreadsheet sheet : curators ) {
            sheet.retainPairs( pairs );
        }
    }

    public void printDisagreements( String column ) throws Exception {
        List<LoadInteractionRecallSpreadsheet> curatorList = new LinkedList<LoadInteractionRecallSpreadsheet>( curators );
        int disagreements = 0;

        for ( int i = 0; i < curatorList.size(); i++ ) {
            LoadInteractionRecallSpreadsheet curatorA = curatorList.get( i );
            for ( int j = 0; j < i; j++ ) {
                LoadInteractionRecallSpreadsheet curatorB = curatorList.get( j );
                disagreements += curatorA.compareToOther( curatorB, column ).size();
            }
        }
        System.out.println();
        System.out.println( "Total pairwise disagreements:"
                + disagreements
                + " (average pairwise:"
                + LoadInteractionSpreadsheet.convertToPercent( disagreements, curatorList.iterator().next()
                        .getAllPairs().size()
                        * curatorList.size() * ( curatorList.size() - 1 ) / 2 ) + ")" );
        System.out.println();
    }

    public static void main( String args[] ) throws Exception {
        RecallCuratorsCombined combined = new RecallCuratorsCombined();
        // combined.loadFirst400Set();
        combined.load6000Set();

        combined.printAllStats();
        System.out.println();

        
        // combined.retainPairs( pairs )

        combined.printDisagreements( "Accept" );

//        splitBySentences();
    }

    public static void splitBySentences() throws Exception {
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
        int posPairsFromSenPosPrecitions = 0;
        Set<String> pairsFromSenPosPrecitionsSet = new HashSet<String>();
        int pairsFromSenOnlyNegPrecitions = 0;
        Set<String> pairsFromSenOnlyNegPrecitionsSet = new HashSet<String>();
        Set<String> pairsToUse = new HashSet<String>();
        log.info( "All pairs:" + SLReader.getAll().size() );

        for ( String sentence : sentences ) {
            Set<String> pairs = new HashSet<String>( sentenceIDToPairs.get( sentence ) );
            int pairCount = pairs.size();
            pairs.retainAll( posPredictedPairs );
            if ( pairs.size() > 0 ) {
                sentencesWithPosPrecitions++;
                pairsFromSenPosPrecitions += pairCount;
                // just positives
                posPairsFromSenPosPrecitions += pairs.size();
                pairsFromSenPosPrecitionsSet.addAll( sentenceIDToPairs.get( sentence ) );
            } else {
                sentencesWithOnlyNegPrecitions++;
                pairsFromSenOnlyNegPrecitions += pairCount;
                pairsFromSenOnlyNegPrecitionsSet.addAll( sentenceIDToPairs.get( sentence ) );
            }

        }
        log.info( "sentencesWithOnlyNegPrecitions:" + sentencesWithOnlyNegPrecitions );
        log.info( "pairsFromSenOnlyNegPrecitions:" + pairsFromSenOnlyNegPrecitions );
        log.info( "" );
        log.info( "sentencesWithPosPrecitions:" + sentencesWithPosPrecitions );
        log.info( "pairsFromSenPosPrecitions:" + pairsFromSenPosPrecitions );
        log.info( "posPairsFromSenPosPrecitions, positive pairs in sentences with at least one positive:" + posPairsFromSenPosPrecitions );
        log.info( "pairsToUse:" + pairsToUse.size() );

        RecallCuratorsCombined combined = new RecallCuratorsCombined();
        combined.load6000Set();

        combined.printAllStats();
        System.out.println();
        System.out.println();

        combined.retainPairs( pairsFromSenPosPrecitionsSet );
        combined.printAllStats();
        System.out.println();
        System.out.println();

        combined = new RecallCuratorsCombined();
        combined.load6000Set();
        combined.retainPairs( pairsFromSenOnlyNegPrecitionsSet );
        combined.printAllStats();

        // combined.printDisagreements( "Accept" );

    }
}

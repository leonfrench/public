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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;

import ubic.basecode.io.excel.CreateSpreadSheet;
import ubic.basecode.io.excel.ExcelUtil;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.interactions.AirolaXMLReader;
import ubic.pubmedgate.interactions.SLOutputReader;

public class CreateInteractionSpreadsheet extends CreateSpreadSheet {
    AirolaXMLReader XMLReader;
    SLOutputReader SLReader;
    GateInterface p2g;

    public CreateInteractionSpreadsheet( String filename, SLOutputReader SLReader, AirolaXMLReader XMLReader )
            throws Exception {
        super( filename, new InteractionSchema() );

        this.SLReader = SLReader;
        this.XMLReader = XMLReader;

        log.info( "SLReader size:" + SLReader.getAll().size() );
        log.info( "XMLReader size:" + XMLReader.getPairCount() );

    }

    public void populate( List<String> pairs ) throws Exception {
        // convert the list to just pairs, ugly - pretends its normalized
        List<NormalizedConnection> pairsConverted = new LinkedList<NormalizedConnection>();
        for ( String pair : pairs ) {
            NormalizedConnection nc = new NormalizedConnection();
            nc.pairID = pair;
            pairsConverted.add( nc );
        }
        populate( pairsConverted );
    }

    public void populate( Collection<NormalizedConnection> pairsNormalized ) throws Exception {
        int row = 1;
        boolean unseenSetup = SLReader.isUnseenSetup();

        HSSFFont textFont = workbook.createFont();
        textFont.setUnderline( HSSFFont.U_SINGLE );
        textFont.setBoldweight( HSSFFont.BOLDWEIGHT_BOLD );

        Map<String, String> pairToPMID = XMLReader.getPairIDToPMID();
        Map<String, Double> scores = SLReader.getScores();

        List<String> positivePredictions = SLReader.getPositivePredictions();
        List<String> positiveAnnotations = SLReader.getPositives();

        LoadInteractionSpreadsheet allSpreadSheet = AllCuratorsCombined.getFinal2000Results();
        Set<String> previousAccepts = allSpreadSheet.getAcceptedPairs();
        Collection<String> previousEvaluated = allSpreadSheet.getAllPairs();

        for ( NormalizedConnection nc : pairsNormalized ) {
            String pair = nc.pairID;
            if ( row > 65000 ) {
                save();
                init();
                filename += "overflow.xls";
                row = 1;
            }

            HSSFRow r = spreadsheet.getRow( row );
            if ( r == null ) {
                r = spreadsheet.createRow( row );
            }
            int col = schema.getPosition( "sentence" );
            HSSFCell c = r.createCell( col );
            c.setCellType( HSSFCell.CELL_TYPE_STRING );

            HSSFRichTextString richString = XMLReader.getRichStringSentence( pair, textFont );
            c.setCellValue( richString );

            String pmid = pairToPMID.get( pair );

            ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "PMID" ), pmid );
            ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "PairID" ), pair );

            String partnerAText = XMLReader.getPartnerAText( pair );
            String partnerBText = XMLReader.getPartnerBText( pair );

            ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "RegionAName" ), partnerAText );
            ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "RegionBName" ), partnerBText );

            ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Score" ), scores.get( pair ) );
            ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Prediction" ),
                    "" + positivePredictions.contains( pair ) );
            if ( !unseenSetup ) {
                ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Previous Annotation" ), ""
                        + positiveAnnotations.contains( pair ) );
            } else {
                ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Previous Annotation" ), "none" );
            }

            // if it was normalized
            if ( nc.regionA != null ) {
                ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "RegionAResolve" ), nc.regionA );
                // check for exact match
                if ( nc.regionA.equalsIgnoreCase( partnerAText ) ) {
                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Resolve Evaluation A" ), "Accept" );
                }
            }
            if ( nc.regionB != null ) {
                ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "RegionBResolve" ), nc.regionB );
                // check for exact match
                if ( nc.regionB.equalsIgnoreCase( partnerBText ) ) {
                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Resolve Evaluation B" ), "Accept" );
                }
            }

            String acceptString;
            if ( previousEvaluated.contains( pair ) ) {
                if ( previousAccepts.contains( pair ) ) {
                    acceptString = "Accept";
                } else {
                    acceptString = "Reject";
                }
            } else {
                acceptString = "Not done";
            }
            ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Previous Annotation" ), acceptString );

            ExcelUtil.setFormula( spreadsheet, row, schema.getPosition( "LinkToAbstract" ),
                    "HYPERLINK(\"http://www.ncbi.nlm.nih.gov/pubmed/" + pmid + "\", \"PubMed\")" );

            row++;
        }

    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        GateInterface p2g = new GateInterface();

        // cross validation on home machine
        String corpusFilename = "/home/leon/ppi-benchmark/Corpora/Original-Modified/WhiteText before negation preds/WhiteText.before.deleting.line.xml";
        String baseFolder = "/home/leon/ppi-benchmark/Saved Results/SL/CV/WhiteText/predict/WhiteText";
        p2g.setUnSeenCorpNull();
        AirolaXMLReader XMLReader = new AirolaXMLReader( corpusFilename, p2g, "Suzanne" );
        SLOutputReader SLReader = new SLOutputReader( new File( baseFolder ) );

        // cross validation on home machine
        // String baseFolder = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
        // + "Saved Results/SL/CV/WhiteTextNegFixFull/predict/WhiteTextNegFixFull";
        // String filename = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
        // + "Corpora/Original-Modified/WhiteTextNegFixFull.xml";
        // p2g.setUnSeenCorpNull();
        // AirolaXMLReader XMLReader = new AirolaXMLReader( corpusFilename, p2g, "Suzanne" );
        // SLOutputReader SLReader = new SLOutputReader( new File( baseFolder ) );

        // unSeen set
        // String baseFolder = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
        // + "Saved Results/SL/CC/NegFixFullOnUnseen/";
        // String corpusFilename = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
        // + "Corpora/Original-Modified/WhiteTextUnseen.orig.xml";
        // String trainingSet = "WhiteTextNegFixFull";
        // String testSet = "WhiteTextUnseen";
        // String annotationSet = "Mallet";
        // AirolaXMLReader XMLReader = new AirolaXMLReader( corpusFilename, p2g, annotationSet );
        // SLOutputReader SLReader = new SLOutputReader( trainingSet, testSet, baseFolder );

        String filename;
        filename = Config.config.getString( "whitetext.iteractions.results.folder" )
                + "TEMPEvalSheetTestPositivePredictions.xls";
        CreateInteractionSpreadsheet test = new CreateInteractionSpreadsheet( filename, SLReader, XMLReader );
        test.populate( SLReader.getPositivePredictions() );
        test.save();

        filename = Config.config.getString( "whitetext.iteractions.results.folder" )
                + "TEMPEvalSheetTestNegativePredictions.xls";
        test = new CreateInteractionSpreadsheet( filename, SLReader, XMLReader );
        test.populate( SLReader.getNegativePredictions() );
        test.save();
    }
}

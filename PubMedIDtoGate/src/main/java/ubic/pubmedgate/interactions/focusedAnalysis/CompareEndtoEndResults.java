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

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.params.ParamKeeper;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.interactions.AirolaXMLReader;
import ubic.pubmedgate.interactions.ConnectionList;
import ubic.pubmedgate.interactions.Cooccurance;
import ubic.pubmedgate.interactions.IteractionEvaluator;
import ubic.pubmedgate.interactions.SLOutputReader;

/**
 * used to compare the results from the complete pipeline to Suzannes results (for 231 abstracts)
 * @author leon
 *
 */
public class CompareEndtoEndResults {
    protected static Log log = LogFactory.getLog( SLOutputReader.class );

    public CompareEndtoEndResults() throws Exception {
        // USE THE MALLET annotated datastore!
        String trainingSet = "WhiteTextNegFixTrain";
        String testSet = "WhiteTextAnnotatedMalletRandom";
        String annotationSet = "Mallet";

        String baseFolder = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Saved Results/SL/CC/NegFixTrainOnRandom/";
        String filename = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Corpora/Original-Modified/WhiteTextAnnotatedMalletRandom.orig.xml";

        GateInterface p2g = new GateInterface();
        p2g.setUnSeenCorpNull();

        AirolaXMLReader XMLReader = new AirolaXMLReader( filename, p2g, annotationSet );
        SLOutputReader SLReader = new SLOutputReader( trainingSet, testSet, baseFolder );

        // hmm
        boolean sentenceLevel = true;
        Cooccurance coc = new Cooccurance( sentenceLevel );
        ConnectionList manual = coc.getAnnotatedConnections( "Suzanne" );

        List<String> predictedPairs = SLReader.getPositivePredictions();

        log.info( predictedPairs.size() + " predicted pairs" );
        ConnectionList predited = new ConnectionList( "Predicted pairs" );
        for ( String pair : predictedPairs ) {
            ConnectionsDocument document = XMLReader.getDocumentFromPairID( pair );
            predited.add( XMLReader.getPairIDToConnection( pair ), document );
        }

        ParamKeeper keeper = new ParamKeeper();
        IteractionEvaluator evaluator = new IteractionEvaluator();
        boolean usePositions = false;
        Map<String, String> params = evaluator.compare( manual, predited,usePositions);
        keeper.addParamInstance( params );
        keeper.writeExcel( Config.config.getString( "whitetext.iteractions.results.folder" )
                + "EndToEnd.random.sentence." + sentenceLevel + ".docCheck.xls" );

        // load in manual predictions
        // String annotationSet = "Mallet";
        // String baseFolder = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
        // + "Saved Results/SL/CV/WhiteTextNegFixFull/predict/WhiteTextNegFixFull";
        // String filename = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
        // + "Corpora/Original-Modified/WhiteTextNegFixFull.xml";
        //
        // GateInterface p2g = new GateInterface();
        // p2g.setUnSeenCorpNull();
        //
        // AirolaXMLReader XMLReader = new AirolaXMLReader( filename, p2g, annotationSet );
        // SLOutputReader SLReader = new SLOutputReader( new File( baseFolder ) );

        // compare to suzanne
        // use connectionlist class

    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        // TODO Auto-generated method stub
        CompareEndtoEndResults test = new CompareEndtoEndResults();
    }

}

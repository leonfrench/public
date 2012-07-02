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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.basecode.dataStructure.params.ParamKeeper;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.interactions.AirolaXMLReader;
import ubic.pubmedgate.interactions.NormalizePairs;
import ubic.pubmedgate.interactions.NormalizeResult;
import ubic.pubmedgate.interactions.SLOutputReader;
import ubic.pubmedgate.interactions.evaluation.AllCuratorsCombined;
import ubic.pubmedgate.interactions.evaluation.LoadInteractionSpreadsheet;
import ubic.pubmedgate.interactions.evaluation.NormalizedConnection;
import ubic.pubmedgate.organism.SpeciesUtil;

public class SplitConnectionsBySpecies {
    protected static Log log = LogFactory.getLog( SplitConnectionsBySpecies.class );

    public static void getCounts() throws Exception {
        // String testSet = "Annotated";
        // String annotationSet = "Suzanne";
        //
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
        String trainingSet = "WhiteTextNegFixFull";
        String testSet = "WhiteTextUnseen";
        String annotationSet = "Mallet";

        String baseFolder = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Saved Results/SL/CC/NegFixFullOnUnseen/";
        String filename = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Corpora/Original-Modified/WhiteTextUnseen.orig.xml";

        GateInterface p2g = new GateInterface();

        AirolaXMLReader XMLReader = new AirolaXMLReader( filename, p2g, annotationSet );
        SLOutputReader SLReader = new SLOutputReader( trainingSet, testSet, baseFolder );

        Map<String, String> pairIDtoPMID = XMLReader.getPairIDToPMID();

        List<String> posPredictions = SLReader.getPositivePredictions();

        CountingMap<String> speciesConCount = new CountingMap<String>();
        CountingMap<String> final2000Rows = new CountingMap<String>();
        CountingMap<String> final2000Accepts = new CountingMap<String>();

        LoadInteractionSpreadsheet final2000 = AllCuratorsCombined.getFinal2000Results();

        for ( String pairID : posPredictions ) {
            String PMID = pairIDtoPMID.get( pairID );
            ConnectionsDocument doc = p2g.getByPMID( PMID );
            Set<String> species = doc.getLinnaeusSpecies();
            speciesConCount.incrementAll( species );

            int final2000RowCount = final2000.getPairIDRowCount( pairID );
            int final2000AcceptCount = final2000.getPairIDAcceptCount( pairID );

            // ugly, but makes sense, increment the number of rows seen
            for ( String spec : species ) {
                for ( int i = 0; i < final2000RowCount; i++ ) {
                    final2000Rows.increment( spec );
                }
                for ( int i = 0; i < final2000AcceptCount; i++ ) {
                    final2000Accepts.increment( spec );
                }
            }
        }
        log.info( "Pos predictions:" + posPredictions.size() );

        log.info( "speciesConCount:" + speciesConCount.size() );

        StringToStringSetMap speciesStrings = SpeciesUtil.getSpeciesStrings( p2g, p2g.getUnseenCorp() ).strings;
        ParamKeeper keeper = new ParamKeeper();
        for ( String specieID : speciesConCount.keySet() ) {
            Map<String, String> result = new HashMap<String, String>();
            result.put( "speciesID", specieID );
            Set<String> speciesText = speciesStrings.get( specieID );
            if ( speciesText == null ) speciesText = new HashSet<String>();
            result.put( "species text", speciesText.toString() );
            result.put( "connection count", "" + speciesConCount.get( specieID ) );
            result.put( "final2000RowCount", "" + final2000Rows.get( specieID ) );
            result.put( "final2000AcceptCount", "" + final2000Accepts.get( specieID ) );

            keeper.addParamInstance( result );
        }
        keeper.writeExcel( Config.config.getString( "whitetext.iteractions.results.folder" )
                + "connectionsBySpecies.xls" );
        log.info( Config.config.getString( "whitetext.iteractions.results.folder" ) + "connectionsBySpecies.xls" );
        // p2g.get

    }


    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        getCounts();
    }

}

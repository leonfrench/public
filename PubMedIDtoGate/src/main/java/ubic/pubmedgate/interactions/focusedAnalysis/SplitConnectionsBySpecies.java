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
import ubic.pubmedgate.interactions.SLOutputReader;
import ubic.pubmedgate.interactions.evaluation.AllCuratorsCombined;
import ubic.pubmedgate.interactions.evaluation.AllUnseenEvaluationsCombined;
import ubic.pubmedgate.interactions.evaluation.LoadInteractionSpreadsheet;
import ubic.pubmedgate.organism.SpeciesLoader;
import ubic.pubmedgate.organism.SpeciesUtil;

public class SplitConnectionsBySpecies {
    protected static Log log = LogFactory.getLog( SplitConnectionsBySpecies.class );
    GateInterface p2g;
    SpeciesLoader filterLoader;
    Set<String> filtered;

    public SplitConnectionsBySpecies() throws Exception {
        p2g = new GateInterface();
        p2g.setNamedCorpNull( "PubMedUnseenJNChem" );

        filterLoader = new SpeciesLoader();
        filtered = filterLoader.getFilteredIDs();
    }

    public CountingMap<String> getSpeciesConCount( String testSet ) throws Exception {
        AirolaXMLReader XMLReader = getXMLReader( testSet );
        SLOutputReader SLReader = SLOutputReader.getCCSLReader( testSet );
        return getSpeciesConCount( SLReader, XMLReader );
    }

    public CountingMap<String> getSpeciesConCount( SLOutputReader reader, AirolaXMLReader XMLReader ) {

        Map<String, String> pairIDtoPMID = XMLReader.getPairIDToPMID();
        CountingMap<String> speciesConCount = new CountingMap<String>();
        List<String> posPredictions = reader.getPositivePredictions();
        for ( String pairID : posPredictions ) {
            String PMID = pairIDtoPMID.get( pairID );
            ConnectionsDocument doc = p2g.getByPMID( PMID );
            Set<String> species = doc.getLinnaeusSpecies();
            species.removeAll( filtered );

            speciesConCount.incrementAll( species );
        }
        return speciesConCount;

    }

    public void getCounts2() throws Exception {
        String testSet = "WhiteTextUnseen";

        AirolaXMLReader XMLReader = getXMLReader( testSet );
        SLOutputReader SLReader = SLOutputReader.getCCSLReader( testSet );

        Map<String, String> pairIDtoPMID = XMLReader.getPairIDToPMID();

        List<String> posPredictions = SLReader.getPositivePredictions();

        CountingMap<String> evaluationRows = new CountingMap<String>();
        CountingMap<String> evaluationAccepts = new CountingMap<String>();
        CountingMap<String> speciesConCount = new CountingMap<String>();

        CountingMap<String> speciesConCountMScan1 = getSpeciesConCount( "WhiteTextUnseenMScan" );
        CountingMap<String> speciesConCountMScan2 = getSpeciesConCount( "WhiteTextUnseenMScan2" );

        AllUnseenEvaluationsCombined allCombined = new AllUnseenEvaluationsCombined();
        Set<String> accepts = allCombined.getAllAcceptedPairs();
        Set<String> evaluated = allCombined.getAllEvaluatedPairs();

        for ( String pairID : posPredictions ) {
            String PMID = pairIDtoPMID.get( pairID );
            ConnectionsDocument doc = p2g.getByPMID( PMID );
            Set<String> species = doc.getLinnaeusSpecies();
            species.removeAll( filtered );

            speciesConCount.incrementAll( species );

            boolean final2000Accept = accepts.contains( pairID );
            boolean final2000Contains = evaluated.contains( pairID );

            for ( String spec : species ) {
                if ( final2000Accept ) evaluationAccepts.increment( spec );
                if ( final2000Contains ) evaluationRows.increment( spec );
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
            result.put( "MSCAN 1 connection count", "" + speciesConCountMScan1.get( specieID ) );
            result.put( "MSCAN 2 connection count", "" + speciesConCountMScan2.get( specieID ) );
            result.put( "EvaluationRowCount", "" + evaluationRows.get( specieID ) );
            result.put( "EvaluationAcceptCount", "" + evaluationAccepts.get( specieID ) );

            keeper.addParamInstance( result );
        }
        keeper.writeExcel( Config.config.getString( "whitetext.iteractions.results.folder" )
                + "connectionsBySpecies.2.xls" );
        log.info( Config.config.getString( "whitetext.iteractions.results.folder" ) + "connectionsBySpecies.2.xls" );
        // p2g.get
    }

    private AirolaXMLReader getXMLReader( String testSet ) throws Exception {
        String annotationSet = "Mallet";
        String filename = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Corpora/Original-Modified/" + testSet + ".orig.xml";
        AirolaXMLReader XMLReader = new AirolaXMLReader( filename, p2g, annotationSet );
        return XMLReader;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        AllUnseenEvaluationsCombined test = new AllUnseenEvaluationsCombined();

        log.info( "Evaluated:" + test.getAllEvaluatedPairs().size() );
        log.info( "Accepted:" + test.getAllAcceptedPairs().size() );

        SplitConnectionsBySpecies splitter = new SplitConnectionsBySpecies();
        splitter.getCounts2();
    }

}

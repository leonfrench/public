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
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.interactions.AirolaXMLGenerator;
import ubic.pubmedgate.interactions.AirolaXMLReader;
import ubic.pubmedgate.interactions.evaluation.AllCuratorsCombined;
import ubic.pubmedgate.interactions.evaluation.LoadInteractionSpreadsheet;

public class AirolaForEvaluations {
    protected static Log log = LogFactory.getLog( AirolaForEvaluations.class );

    Set<String> abstractIDs;
    Set<String> sentenceIDs;
    Set<String> enitityIDs;
    Set<String> acceptedPairIDs;
    Set<String> pairIDs;
    AirolaXMLReader reader;

    public Set<String> getAbstractsWithConnectionsIDs() {
        Set<String> result = new HashSet<String>();
        Map<String, String> pairIDtoPMID = reader.getPairIDToPMID();
        for ( String pairID : pairIDs ) {
            result.add( pairIDtoPMID.get( pairID ) );
        }
        return result;
    }

    public AirolaForEvaluations( AirolaXMLReader reader, LoadInteractionSpreadsheet sheet, boolean useAll )
            throws Exception {
        abstractIDs = new HashSet<String>();
        sentenceIDs = new HashSet<String>();
        enitityIDs = new HashSet<String>();
        acceptedPairIDs = new HashSet<String>();

        // from the sheet get accepted and rejected pairID's
        acceptedPairIDs = sheet.getAcceptedPairs();
        log.info( "Accepted pairs:" + acceptedPairIDs.size() );
        if ( useAll ) {
            pairIDs = sheet.getAllPairs();
        } else {
            pairIDs = sheet.getAcceptedPairs();
        }
        this.reader = reader;

        Map<String, String> pairIDToSentenceElementID = reader.getPairIDToSentenceElementID();
        Map<String, String> pairIDToAbstractElementID = reader.getPairIDToAbstractElementID();
        StringToStringSetMap sentenceIDToEntities = reader.getSentenceIDToEntities();
        HashMap<String, Set<String>> pairIDToEntities = reader.getPairIDToEntities();

        // get the associated entities, sentences and documents
        for ( String pairID : pairIDs ) {
            String abstractID = pairIDToAbstractElementID.get( pairID );
            Set<String> entitiyIDforPair = pairIDToEntities.get( pairID );
            String sentenceID = pairIDToSentenceElementID.get( pairID );

            if ( entitiyIDforPair != null ) enitityIDs.addAll( entitiyIDforPair );
            sentenceIDs.add( sentenceID );
            abstractIDs.add( abstractID );
            // log.info( "abstractID:" + abstractID );
            // log.info( "sentenceIDs:" + sentenceID );
        }

        // filter for sentences with two entities?
        int moreThanTwo = 0;
        for ( String sentenceID : sentenceIDs ) {
            Set<String> entitiesInSentence = sentenceIDToEntities.get( sentenceID );
            enitityIDs.addAll( entitiesInSentence );
            if ( entitiesInSentence.size() > 2 ) moreThanTwo++;
        }
        log.info( "Entities:" + enitityIDs.size() );
        log.info( "Pairs:" + pairIDs.size() );
        log.info( "Sentences:" + sentenceIDs.size() );
        log.info( "Abstracts:" + abstractIDs.size() );
        log.info( "Sentences with more than two:" + moreThanTwo + " of " + sentenceIDs.size() );
    }

    // generate XML file
    public void run( GateInterface p2g, String corpName ) throws Exception {
        String name = "WhiteTextUnseen";
        String annotatorSetName = "Mallet";
        AirolaXMLGenerator generator = new AirolaXMLGenerator( name, annotatorSetName, p2g );
        boolean abbrExtend = false;
        generator.run( p2g.getCorpusByName( corpName ), abbrExtend, this );
    }

    public boolean acceptSentence( String id ) {
        return sentenceIDs.contains( id );
    }

    public boolean acceptEntity( String id ) {
        return enitityIDs.contains( id );
    }

    public boolean acceptAbstract( String id ) {
        return abstractIDs.contains( id );
    }

    public boolean acceptPair( String id ) {
        return acceptedPairIDs.contains( id );
    }

    public boolean usedPair( String id ) {
        return pairIDs.contains( id );
    }

    public static void main( String[] args ) throws Exception {

        LoadInteractionSpreadsheet sheet = AllCuratorsCombined.getFinal2000Results();

        String corpName = "PubMedUnseenJCN";
        String annotationSet = "Mallet";
        
        // use all sentences/pairs? or ignore negative markings
        boolean usePosAndNeg = false;

        GateInterface p2g = new GateInterface();

        p2g.setUnSeenCorpNull();
        p2g.setNamedCorpNull( "PubMedUnseenJNChem" );
        // p2g.setNamedCorpNull("PubMedUnseenJCN");
        p2g.setNamedCorpNull( "PubMedUnseenMScan1" );
        p2g.setNamedCorpNull( "PubMedUnseenMScan2" );

        String corpusFilename = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Corpora/Original-Modified/WhiteTextUnseen.orig.xml";
        AirolaXMLReader reader = new AirolaXMLReader( corpusFilename, p2g, annotationSet );
        AirolaForEvaluations test = new AirolaForEvaluations( reader, sheet, usePosAndNeg );
        test.run( p2g, corpName );

    }

}

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

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.util.OffsetComparator;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import ubic.basecode.util.FileTools;
import ubic.connection.Connection;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.interactions.focusedAnalysis.AirolaForEvaluations;

public class AirolaXMLGenerator {
    private static Log log = LogFactory.getLog( Cooccurance.class.getName() );
    int folds = 10;
    int seed = 42;
    GateInterface p2g;
    String annotatorNameSet;
    String name;

    public AirolaXMLGenerator( String name, String annotatorSetName ) {
        this( name, annotatorSetName, new GateInterface() );
    }

    public AirolaXMLGenerator( String name, String annotatorSetName, GateInterface p2g ) {
        this.annotatorNameSet = annotatorSetName;
        this.name = name;
        this.p2g = p2g;
    }

    public Corpus getTrainingCorp() {
        return p2g.getTrainingCorp();
    }

    public Corpus getRandomCorp() {
        return p2g.getRandomSubsetCorp();
    }

    public Corpus getCorp() {
        return p2g.getCorp();
    }

    public Corpus getUnseen() {
        return p2g.getUnseenCorp();
    }

    public void run( Corpus corp ) throws Exception {
        boolean abbrExtend = false;
        run( ( Corpus ) corp, abbrExtend );
    }

    public void run( Corpus corp, boolean abbrExtend ) throws Exception {
        runOnDocs( p2g.getDocuments( corp ), abbrExtend );
    }

    // using the 2000 set we somehow need to filter the input sentences - match the sentences somehow
    // try matching origID
    public void runOnDocs( List<ConnectionsDocument> docs, boolean abbrExtend ) throws Exception {
        AirolaForEvaluations fromEvaluations = null;
        runOnDocs( docs, abbrExtend, fromEvaluations );
    }

    public void run( Corpus corp, boolean abbrExtend, AirolaForEvaluations fromEvaluations ) throws Exception {
        runOnDocs( p2g.getDocuments( corp ), abbrExtend, fromEvaluations );
    }

    public Set<String> getAbbreviations( ConnectionsDocument doc ) {
        AnnotationSet annotSet = doc.getAnnotations( "Original markups" );
        annotSet = annotSet.get( "AbbrevShort" );
        Set<Annotation> shortAbbrevs = new HashSet<Annotation>( annotSet );
        Set<String> result = new HashSet<String>();
        for ( Annotation annotation : shortAbbrevs ) {
            result.add( doc.getAnnotationText( annotation ) );
        }
        return result;
    }

    public void runOnDocs( List<ConnectionsDocument> docs, boolean abbrExtend, AirolaForEvaluations fromEvaluations )
            throws Exception {
        boolean undirected = true;
        List<String> docIDs = new LinkedList<String>();
        int totalTrueInteractions = 0;
        int endBrackets = 0;
        int endBracketShortMatch = 0;
        int regionsTestedForBrackets = 0;
        int totalFalseInteractions = 0;
        int badRegionsRemoved = 0;
        int sentenceCountWithRegions = 0;
        int sentenceCount = 0;
        Element corpus = new Element( "corpus" );
        String corpusStr = name;
        corpus.setAttribute( "source", corpusStr );
        Document myDocument = new Document( corpus );
        int docID = 0;
        for ( ConnectionsDocument doc : docs ) {
            // only use the document if it has a pair
            boolean hasPair = false;
            ConnectionList connectionList = new ConnectionList( annotatorNameSet );
            connectionList.addAllFromDocument( doc );

            Set<String> shortAbbreviations = getAbbreviations( doc );

            Element docElement = new Element( "document" );
            String docString = corpusStr + ".d" + docID;
            docElement.setAttribute( "id", docString );
            if ( !docIDs.contains( docString ) ) docIDs.add( docString ); // used for cross validation splits
            // log.info( "docString:" + docString );

            docElement.setAttribute( "origID", doc.getPMID() );
            // iterate sentences
            List<Annotation> sentences = doc.getGATESentences( doc.GATETOKENS );

            int sentenceID = 0;
            for ( Annotation sentence : sentences ) {
                sentenceCount++;
                long start = sentence.getStartNode().getOffset();
                long end = sentence.getEndNode().getOffset();

                AnnotationSet annotSet = doc.getAnnotations( annotatorNameSet );
                annotSet = annotSet.get( "BrainRegion" );
                annotSet = annotSet.getContained( start, end );
                // skip it if it has less than two brain regions?
                if ( annotSet.size() < 2 ) {
                    // sentenceID++;
                    continue;
                }
                sentenceCountWithRegions++;

                Element sentenceElement = new Element( "sentence" );
                // sentenceElement.setAttribute( "charOffset", start + "-" + end );
                String sentenceStrID = docString + ".s" + sentenceID;
                sentenceElement.setAttribute( "id", sentenceStrID );
                sentenceElement.setAttribute( "origId", sentence.getId() + "" );
                String sentenceText = doc.getAnnotationText( sentence );
                sentenceElement.setAttribute( "text", sentenceText );
                // log.info( "sentenceStr:" + sentenceStr );
                if ( fromEvaluations != null ) {
                    if ( !sentenceText.equals( fromEvaluations.getSentenceText( sentenceStrID ) ) ) {
                        log.info( "Error: sentenceID'd text does not match across evaluations and new corpus" );
                        log.info( "Generated:" + sentenceText );
                        log.info( "Evaluated:" + fromEvaluations.getSentenceText( sentenceStrID ) );
                        System.exit( 1 );
                    }
                }

                if ( fromEvaluations == null || fromEvaluations.acceptSentence( sentenceStrID ) ) {
                    docElement.addContent( sentenceElement );
                }

                // iterate entities
                // store map of entities
                // get brain regions in sentences

                List<Annotation> regionsSorted = new ArrayList<Annotation>( annotSet );
                Collections.sort( regionsSorted, new OffsetComparator() );

                Map<Annotation, String> entityMap = new HashMap<Annotation, String>();
                int entityID = 0;
                Set<Annotation> badRegions = new HashSet<Annotation>();
                for ( Annotation region : regionsSorted ) {
                    String entityString = sentenceStrID + ".e" + entityID;
                    Element entity = new Element( "entity" );
                    long entityStart = region.getStartNode().getOffset() - start;
                    long entityEnd = region.getEndNode().getOffset() - start;
                    entityEnd--;
                    if ( entityStart < 0 ) {
                        log.warn( "Entity start less than zero:" + doc.getPMID() + "\n" + sentenceText );
                        entityStart = 0;
                    }
                    if ( entityEnd < 0 ) {
                        log.warn( "Entity end less than zero:" + doc.getPMID() + "\n" + sentenceText );
                        badRegions.add( region );
                        // skip
                        continue;
                    }
                    String annotationText = doc.getAnnotationText( region );
                    if ( abbrExtend ) {
                        long regionToDocEnd = region.getEndNode().getOffset();
                        int extendAmount = 0;
                        try {
                            String endChar = doc.getContent().getContent( regionToDocEnd, regionToDocEnd + 2 )
                                    .toString();
                            if ( endChar.equals( " (" ) || endChar.startsWith( "(" ) ) {
                                String theRest = doc.getContent().getContent( regionToDocEnd, doc.getContent().size() )
                                        .toString();
                                log.info( "END:" + endChar );
                                log.info( "REST:" + theRest );

                                if ( theRest.startsWith( " " ) ) {
                                    theRest = theRest.substring( 1 );
                                    extendAmount++;
                                }

                                for ( String shortAbbrev : shortAbbreviations ) {
                                    if ( theRest.startsWith( shortAbbrev ) ) {
                                        extendAmount += shortAbbrev.length();
                                        annotationText = doc
                                                .getContent()
                                                .getContent( region.getStartNode().getOffset(),
                                                        regionToDocEnd + extendAmount ).toString();
                                        log.info( "NEW:" + annotationText );

                                        endBracketShortMatch++;
                                        entityEnd += extendAmount;
                                        break;
                                    }
                                }

                                endBrackets++;
                            }
                            regionsTestedForBrackets++;
                        } catch ( Exception e ) {
                            e.printStackTrace();
                        }

                    }

                    entity.setAttribute( "charOffset", entityStart + "-" + entityEnd );
                    entity.setAttribute( "id", entityString );
                    entity.setAttribute( "origId", region.getId() + "" );
                    entity.setAttribute( "text", annotationText );
                    entity.setAttribute( "type", "Individual_protein" );
                    if ( fromEvaluations == null || fromEvaluations.acceptEntity( entityString ) ) {
                        sentenceElement.addContent( entity );
                    }
                    entityMap.put( region, entityString );
                    entityID++;
                } // end entities
                regionsSorted.removeAll( badRegions ); // region with negative end span
                badRegionsRemoved += badRegions.size();
                // iterate pairs
                // similar to co-occurance code - requires mapping back to entities
                int pairID = 0;
                for ( Annotation anotA : regionsSorted ) {
                    for ( Annotation anotB : regionsSorted ) {
                        // dont do symetric relations
                        hasPair = true;
                        if ( anotA.getId() <= anotB.getId() ) continue;

                        String aID = entityMap.get( anotA );
                        String bID = entityMap.get( anotB );
                        Element pair = new Element( "pair" );
                        String pairStr = sentenceStrID + ".p" + pairID;
                        String interaction;

                        if ( connectionList.containsByPartners( new Connection( anotA, anotB ), undirected, doc )
                                || ( fromEvaluations != null && fromEvaluations.acceptPair( pairStr ) ) ) {
                            totalTrueInteractions++;
                            interaction = "True";
                        } else {
                            totalFalseInteractions++;
                            interaction = "False";
                        }

                        pair.setAttribute( "interaction", interaction );
                        pair.setAttribute( "id", pairStr );
                        pair.setAttribute( "e1", aID );
                        pair.setAttribute( "e2", bID );

                        // if ( fromEvaluations == null || fromEvaluations.usedPair( pairStr ) ) {
                        sentenceElement.addContent( pair );
                        // }
                        pairID++;
                    }
                }// end pairs

                sentenceID++;

            } // end sentence
              // only count this document if it has at least one interaction sentence
            if ( hasPair ) {
                docID++;
                if ( fromEvaluations == null || fromEvaluations.acceptAbstract( docString ) ) {
                    corpus.addContent( docElement );
                }
            }
            // if ( docID > 30 ) break;
        }

        log.info( "Total true interactions:" + totalTrueInteractions );
        log.info( "Total false interactions:" + totalFalseInteractions );
        if ( totalTrueInteractions == 0 ) log.info( "Warning: zero true interactions, unsupervised?" );
        log.info( "Total documents used:" + ( docID ) );
        log.info( "Total documents:" + docs.size() );
        log.info( "Total sentences:" + sentenceCount );
        log.info( "Total sentences with two or more regions:" + sentenceCountWithRegions );
        log.info( "End brakcets:" + endBrackets );
        log.info( "End brackets with short match:" + endBracketShortMatch );
        log.info( "Regions tested for ends:" + regionsTestedForBrackets );
        log.info( "Bad regions:" + badRegionsRemoved );
        // log.info( "Documents:" + docIDs );

        // update name for writing
        if ( fromEvaluations != null ) name = name + "Eval";

        XMLOutputter outputter = new XMLOutputter( Format.getPrettyFormat() );
        FileWriter fout = new FileWriter( Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Corpora/Original-Modified/" + name + ".xml" );
        outputter.output( myDocument, fout );
        fout.close();

        // write splits
        Random r = new Random( seed );
        Collections.shuffle( docIDs, r );
        File splitFolder = new File( Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Corpora/Splits/" + name + "/" );
        splitFolder.mkdir();
        log.info( "Writing splits to:" + splitFolder.toString() );
        splitFolder = new File( Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Corpora/splits-test-train/" + name );
        splitFolder.mkdir();
        log.info( "Writing splits to:" + splitFolder.toString() );

        for ( int fold = 0; fold < folds; fold++ ) {
            int splitSize = docIDs.size() / folds;
            int start = fold * splitSize;
            int end = ( fold + 1 ) * splitSize;
            // if it's last fold
            if ( fold == ( folds - 1 ) ) end = docIDs.size();
            List<String> output = new LinkedList<String>();
            for ( int i = start; i < end; i++ ) {
                output.add( docIDs.get( i ) );
            }
            FileTools.stringsToFile( output, Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                    + "Corpora/Splits/" + name + "/" + name + ( fold + 1 ) + ".txt" );
            FileTools.stringsToFile( output, Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                    + "Corpora/splits-test-train/" + name + "/test-" + ( fold + 1 ) );
            List<String> trainOutput = new LinkedList<String>( docIDs );
            trainOutput.removeAll( output );
            FileTools.stringsToFile( trainOutput, Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                    + "Corpora/splits-test-train/" + name + "/train-" + ( fold + 1 ) );
        }
    }

    public static void main( String[] args ) throws Exception {
        // AirolaXMLGenerator gen = new AirolaXMLGenerator( "WhiteTextNegFixTrain", "Suzanne" );
        // gen.run( gen.getTrainingCorp() );

        AirolaXMLGenerator gen = new AirolaXMLGenerator( "WhiteTextNegFixFullCountCheck2", "Suzanne" );
        gen.run( gen.getCorp() );

        // AirolaXMLGenerator gen = new AirolaXMLGenerator( "WhiteTextAnnotatedMalletRandom", "Mallet" );
        // gen.run( gen.getRandomCorp() );

        // AirolaXMLGenerator gen = new AirolaXMLGenerator( "WhiteTextUnseenMScan2", "Mallet" );
        // gen.run( gen.getUnseen() );

        // extend abbreviations? - may mess up normalization
        // AirolaXMLGenerator gen = new AirolaXMLGenerator( "WhiteTextUnseenAbbrFix", "Mallet" );
        // boolean abrExtend = true;
        // gen.run( gen.getUnseen(), abrExtend );

        // AirolaXMLGenerator gen = new AirolaXMLGenerator( "WhiteTextMalletAbbrFixTemp", "Suzanne" );
        // boolean abrExtend = true;
        // gen.run( gen.getCorp(), abrExtend );

        // with abbreviations
        // AirolaXMLGenerator gen = new AirolaXMLGenerator( "WhiteTextNegFixAbbrevFix", "Suzanne" );
        // boolean abrExtend = true;
        // gen.run( gen.getCorp(), abrExtend );

    }
}

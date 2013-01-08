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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.basecode.util.FileTools;
import ubic.connection.Connection;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;

public class AirolaXMLReader {
    protected static Log log = LogFactory.getLog( AirolaXMLReader.class );
    String filename;
    GateInterface p2g;
    String annotatorNameSet;

    Map<String, String> pairIDToPMID, pairIDToSentenceElementID, pairIDToAbstractElementID, sentenceIDToText;
    StringToStringSetMap sentenceIDToEntities;
    StringToStringSetMap sentenceIDToPairs;
    HashMap<String, Set<String>> pairIDToEntities;
    HashMap<String, String> entityIDtoText;
    List<String> pairList;
    Set<String> PMIDs;

    Map<String, Connection> pairIDToConnection;
    Map<String, Annotation> pairIDToSentence;

    public AirolaXMLReader( String filename, GateInterface p2g, String annotationNameSet ) throws Exception {
        this.filename = filename;
        this.p2g = p2g;
        this.annotatorNameSet = annotationNameSet;

        pairList = new LinkedList<String>();
        pairIDToPMID = new HashMap<String, String>();
        pairIDToConnection = new HashMap<String, Connection>();
        pairIDToSentence = new HashMap<String, Annotation>();
        pairIDToEntities = new HashMap<String, Set<String>>();
        pairIDToSentenceElementID = new HashMap<String, String>();
        pairIDToAbstractElementID = new HashMap<String, String>();
        sentenceIDToEntities = new StringToStringSetMap();
        sentenceIDToPairs = new StringToStringSetMap();
        sentenceIDToText = new HashMap<String, String>();
        entityIDtoText = new HashMap<String, String>();
        PMIDs = new HashSet<String>();

        // create maps
        log.info( "Reading XML" );
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read( filename );

        Map<String, Element> entitiyIDtoElement = new HashMap<String, Element>();
        String path = "//corpus/document/sentence/entity";
        List list = document.selectNodes( path );
        Iterator iter = list.iterator();
        while ( iter.hasNext() ) {
            Element entity = ( Element ) iter.next();
            String elementID = entity.attributeValue( "id" );
            entitiyIDtoElement.put( elementID, entity );
            Element sentence = entity.getParent();
            String sentenceID = sentence.attributeValue( "id" );
            sentenceIDToEntities.put( sentenceID, elementID );
        }

        path = "//corpus/document/sentence/pair";
        list = document.selectNodes( path );
        iter = list.iterator();
        int pairCount = 0;
        while ( iter.hasNext() ) {
            Element pair = ( Element ) iter.next();
            pairCount++;
            String pairID = pair.attributeValue( "id" );
            pairList.add( pairID );

            // get pairs sentence
            Element sentence = pair.getParent();
            String sentenceID = sentence.attributeValue( "id" );
            String sentenceOrigID = sentence.attributeValue( "origId" );

            // get pair's PMID
            Element abstractElement = sentence.getParent();
            String PMID = abstractElement.attributeValue( "origID" );
            // log.info( "PMID:" + PMID );
            PMIDs.add( PMID );

            ConnectionsDocument GATEdoc = p2g.getByPMID( PMID );
            if ( GATEdoc == null ) {
                log.info( PMID + " pmid not loaded from corpus" );
            }

            AnnotationSet sentenceSet = GATEdoc.getAnnotations( GATEdoc.GATETOKENS );

            AnnotationSet documentAnnotations = GATEdoc.getAnnotations( annotatorNameSet );

            // get pairs elements -> make connection
            String entitiy1 = pair.attributeValue( "e1" );
            String entitiy2 = pair.attributeValue( "e2" );

            Element entityElement1 = entitiyIDtoElement.get( entitiy1 );
            Element entityElement2 = entitiyIDtoElement.get( entitiy2 );

            Annotation entityAnnotation1 = documentAnnotations.get( Integer.parseInt( entityElement1
                    .attributeValue( "origId" ) ) );
            Annotation entityAnnotation2 = documentAnnotations.get( Integer.parseInt( entityElement2
                    .attributeValue( "origId" ) ) );

            entityIDtoText.put( entitiy1, GATEdoc.getAnnotationText( entityAnnotation1 ) );
            entityIDtoText.put( entitiy2, GATEdoc.getAnnotationText( entityAnnotation2 ) );

            Connection connection = new Connection( entityAnnotation1, entityAnnotation2 );

            Set<String> entities = new HashSet<String>();
            entities.add( entitiy1 );
            entities.add( entitiy2 );

            Annotation sentenceAnnotation = sentenceSet.get( Integer.parseInt( sentenceOrigID ) );

            sentenceIDToText.put( sentenceID, GATEdoc.getAnnotationText( sentenceAnnotation ) );
            pairIDToAbstractElementID.put( pairID, abstractElement.attributeValue( "id" ) );
            pairIDToSentenceElementID.put( pairID, sentence.attributeValue( "id" ) );
            pairIDToEntities.put( pairID, entities );
            pairIDToPMID.put( pairID, PMID );
            pairIDToSentence.put( pairID, sentenceAnnotation );
            pairIDToConnection.put( pairID, connection );
            sentenceIDToPairs.put( sentenceID, pairID );
        }
        log.info( "Done creating maps, " + pairCount + " pairs loaded" );
    }

    public StringToStringSetMap getSentenceIDToPairs() {
        return sentenceIDToPairs;
    }

    public Set<String> getPMIDs() {
        return PMIDs;
    }

    public Map<String, String> getSentenceIDToText() {
        return sentenceIDToText;
    }

    public String getEntityText( String entity ) {
        return entityIDtoText.get( entity );
    }

    public Set<String> getAllEntities() {
        return entityIDtoText.keySet();
    }

    public String getSentenceText( String sentenceID ) {
        return sentenceIDToText.get( sentenceID );
    }

    public int getPairCount() {
        return pairIDToPMID.size();
    }

    public List<String> getAllPairs() {
        return pairList;
    }

    public Set<String> getAllSentences() {
        return sentenceIDToEntities.keySet();
    }

    public String getPartnerAText( String pair ) {
        Connection connection = pairIDToConnection.get( pair );
        if ( connection == null ) {
            log.info( "Bad pair:" + pair );
            log.info( "Null connection:" + pairIDToPMID.get( pair ) );
            System.exit( 1 );
        }
        Annotation partner = connection.getPartnerA();
        String result = getConnectionRegionText( pair, partner );
        return result;
    }

    private String getConnectionRegionText( String pair, Annotation partner ) {
        ConnectionsDocument doc = getDocumentFromPairID( pair );
        String result = doc.getAnnotationText( partner );
        return result;
    }

    public ConnectionsDocument getDocumentFromPairID( String pair ) {
        String PMID = pairIDToPMID.get( pair );
        ConnectionsDocument doc = p2g.getByPMID( PMID );
        return doc;
    }

    public String getPartnerBText( String pair ) {
        Connection connection = pairIDToConnection.get( pair );
        Annotation partner = connection.getPartnerB();
        String result = getConnectionRegionText( pair, partner );
        return result;
    }

    public HSSFRichTextString getRichStringSentence( String pair, HSSFFont textFont ) {
        String PMID = pairIDToPMID.get( pair );
        ConnectionsDocument doc = p2g.getByPMID( PMID );
        Annotation sentence = pairIDToSentence.get( pair );
        Connection connection = pairIDToConnection.get( pair );

        if ( sentence == null ) {
            log.info( "Error null sentence" );
            System.exit( 1 );
        }

        HSSFRichTextString result = new HSSFRichTextString( doc.getAnnotationText( sentence ) );

        long start = sentence.getStartNode().getOffset();

        Annotation region1 = connection.getPartnerA();
        long entity1Start = region1.getStartNode().getOffset() - start;
        long entity1End = region1.getEndNode().getOffset() - start;
        if ( entity1Start < 0 ) {
            log.warn( "Entity start less than zero:" + doc.getPMID() + "\n" + doc.getAnnotationText( sentence ) );
            entity1Start = 0;
        }

        Annotation region2 = connection.getPartnerB();
        long entity2Start = region2.getStartNode().getOffset() - start;
        long entity2End = region2.getEndNode().getOffset() - start;
        if ( entity2Start < 0 ) {
            log.warn( "Entity start less than zero:" + doc.getPMID() + "\n" + doc.getAnnotationText( sentence ) );
            entity2Start = 0;
        }

        result.applyFont( ( int ) entity1Start, ( int ) entity1End, textFont );
        result.applyFont( ( int ) entity2Start, ( int ) entity2End, textFont );

        return result;
    }

    public String getUnderLineSentence( String pair ) {
        String result = "";
        String PMID = pairIDToPMID.get( pair );
        ConnectionsDocument doc = p2g.getByPMID( PMID );
        Annotation sentence = pairIDToSentence.get( pair );
        Connection connection = pairIDToConnection.get( pair );

        if ( sentence == null ) {
            log.info( "Error null sentence" );
            System.exit( 1 );
        }

        result = doc.getAnnotationText( sentence );
        // log.info( "Before:" + result );

        long start = sentence.getStartNode().getOffset();

        Annotation region1 = connection.getPartnerA();
        long entity1Start = region1.getStartNode().getOffset() - start;
        long entity1End = region1.getEndNode().getOffset() - start;
        if ( entity1Start < 0 ) {
            log.warn( "Entity start less than zero:" + doc.getPMID() + "\n" + doc.getAnnotationText( sentence ) );
            entity1Start = 0;
        }

        Annotation region2 = connection.getPartnerB();
        long entity2Start = region2.getStartNode().getOffset() - start;
        long entity2End = region2.getEndNode().getOffset() - start;
        if ( entity2Start < 0 ) {
            log.warn( "Entity start less than zero:" + doc.getPMID() + "\n" + doc.getAnnotationText( sentence ) );
            entity2Start = 0;
        }

        if ( entity1Start > entity2End ) {
            long tempStart = entity2Start;
            long tempEnd = entity2End;
            entity2Start = entity1Start;
            entity2End = entity1End;
            entity1Start = tempStart;
            entity1End = tempEnd;
        }

        // ugly
        String x = null;
        try {
            x = result.substring( 0, ( int ) entity1Start ) + "<b>"
                    + result.substring( ( int ) entity1Start, ( int ) entity1End ) + "</b>"
                    + result.substring( ( int ) entity1End, ( int ) entity2Start ) + "<b>"
                    + result.substring( ( int ) entity2Start, ( int ) entity2End ) + "</b>"
                    + result.substring( ( int ) entity2End );
        } catch ( Exception e ) {
            e.printStackTrace();
            x = "Error:" + e.getMessage();
        }

        return x;
    }

    public static void main( String[] args ) throws Exception {
        String filename = "/home/leon/ppi-benchmark/Corpora/Original-Modified/WhiteText.xml";
        GateInterface p2g = new GateInterface();

        AirolaXMLReader reader = new AirolaXMLReader( filename, p2g, "Suzanne" );

    }

    public Connection getPairIDToConnection( String pair ) {
        return pairIDToConnection.get( pair );
    }

    public Map<String, Connection> getPairIDToConnection() {
        return pairIDToConnection;
    }

    public Map<String, String> getPairIDToPMID() {
        return pairIDToPMID;
    }

    public Map<String, Annotation> getPairIDToSentence() {
        return pairIDToSentence;
    }

    public Map<String, String> getPairIDToAbstractElementID() {
        return pairIDToAbstractElementID;
    }

    public HashMap<String, Set<String>> getPairIDToEntities() {
        return pairIDToEntities;
    }

    public Map<String, String> getPairIDToSentenceElementID() {
        return pairIDToSentenceElementID;
    }

    public String getFilename() {
        return filename;
    }

    public String getNormalizedPairsFilename() {
        return filename + ".normalizedPairs";
    }

    public List<String> getNormalizedPairs() throws Exception {
        return FileTools.getLines( getNormalizedPairsFilename() );
    }

    public String getAnnotatorNameSet() {
        return annotatorNameSet;
    }

    public StringToStringSetMap getSentenceIDToEntities() {
        return sentenceIDToEntities;
    }

}

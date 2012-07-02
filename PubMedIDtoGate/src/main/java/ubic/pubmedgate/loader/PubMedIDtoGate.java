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

package ubic.pubmedgate.loader;

import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.persist.PersistenceException;
import gate.security.SecurityException;

import java.io.FileWriter;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import ubic.gemma.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.abbrev.ExtractAbbrev;

public class PubMedIDtoGate extends GateInterface {
    static PubMedSearch pSearch = new PubMedSearch();

    SAXParserFactory saxParserFactory;
    XMLReader newxmlParser;

    public PubMedIDtoGate() throws Exception {
        super();
        saxParserFactory = SAXParserFactory.newInstance();
        newxmlParser = saxParserFactory.newSAXParser().getXMLReader();
        processAbbrevs = true;
    }

    /**
     * @param args
     */
    public static Collection<BibliographicReference> getRefs( String PMIDs ) {
        Collection<BibliographicReference> refs = null;
        List<String> getme = new LinkedList<String>();
        // if ( PMIDs.contains( " " ) ) {
        getme = Arrays.asList( PMIDs.split( " " ) );
        // } else
        // getme.add( PMIDs );
        try {
            refs = pSearch.searchAndRetrieveIdByHTTP( getme );
        } catch ( Exception e ) {
            System.out.println( "Error on PMIDs=" + PMIDs + " - " + e.getMessage() );
        }
        return refs;
    }

    // needs refactoring
    public String convertReftoShortXML( BibliographicReference ref, boolean doAbbrev ) {
        String before, after;
        String PMID = ref.getPubAccession().getAccession();
        String abstractText = ref.getAbstractText();

        // XML escaping
        abstractText = abstractText.replaceAll( ">", "&gt;" );
        abstractText = abstractText.replaceAll( "<", "&lt;" );
        abstractText = abstractText.replaceAll( "&", "&amp;" );
        abstractText = abstractText.replaceAll( "[\"]", "&quot;" );
        abstractText = abstractText.replaceAll( "[']", "&apos;" );

        String result = "<?xml version=\"1.0\"?>";
        result += "<PubmedArticle>";
        result += "<PMID>" + PMID + "</PMID>\n";
        result += "<ArticleTitle>" + ref.getTitle() + "</ArticleTitle>\n";
        if ( doAbbrev ) {
            // process abstract text for abbreviations
            ExtractAbbrev extractAbbrev = new ExtractAbbrev();
            Map<String, String> pairs = extractAbbrev.extractAbbrPairs( new StringReader( abstractText ) );

            for ( String shortForm : pairs.keySet() ) {
                String longForm = pairs.get( shortForm );
                // System.out.println( shortForm + "=" + longForm );
                int afterFirstOccurance = abstractText.indexOf( shortForm ) + shortForm.length();
                before = abstractText.substring( 0, afterFirstOccurance );

                // chop out first occurance, call it after
                after = abstractText.substring( afterFirstOccurance );
                after = after.replaceAll( shortForm, "<Abbrev extractor=\"BerkeleyBiotext\" short=\"" + shortForm
                        + "\" long=\"" + longForm + "\">" + longForm + "(" + shortForm + ")" + "</Abbrev>" );
                abstractText = before + after;
            }
        }

        result += "<AbstractText>" + abstractText + "</AbstractText>";
        result += "</PubmedArticle>";
        return result;
    }

    boolean processAbbrevs;

    public PubMedIDtoGate( String datastore ) {
        super( datastore );
        processAbbrevs = true;
    }

    public void remove( String PMID ) throws Exception {
        for ( Document d : getDocuments() ) {
            if ( d.getFeatures().get( "PMID" ).equals( PMID ) ) {
                remove( d );
            }
        }
    }

    public void loadReftoGate( BibliographicReference ref ) throws Exception {
        loadReftoGate( ref, corp );
    }

    public void loadReftoGate( BibliographicReference ref, Corpus corp ) throws Exception {
        boolean sync = true;
        loadReftoGate( ref, corp, processAbbrevs, sync );
    }

    public void loadReftoGate( BibliographicReference ref, Corpus corp, boolean doAbbrev, boolean sync )
            throws Exception {
        String name, shortXML;
        String PMID = ref.getPubAccession().getAccession();
        FileWriter originalWriter;
        shortXML = convertReftoShortXML( ref, doAbbrev );
        // filename is the PMID, then title
        int titleLength = Math.min( 25, ref.getTitle().length() );
        name = "PM" + PMID + "-" + ref.getTitle().substring( 0, titleLength );
        name = name.replaceAll( "[^-A-Za-z0-9_ ]", "" );
        name = name.replaceAll( "[ ]", "_" );

        // check the XML
        try {
            newxmlParser.parse( new InputSource( new StringReader( shortXML ) ) );
        } catch ( SAXException e ) {
            if ( doAbbrev == true ) {
                System.out.println( "SAX problem, loading without abbreviations" );
                loadReftoGate( ref, corp, false, sync );
                return;
            } else {
                // failed to load
                System.out.println( ref.getId() + " Failed to load" );
                return;
            }
        }

        // org.apache.xerces.parsers.AbstractSAXParser.parse

        // write out original XML
        originalWriter = new FileWriter( originalsFile + name + ".xml" );
        originalWriter.write( shortXML );
        originalWriter.close();

        // GATE stuff
        FeatureMap params = Factory.newFeatureMap();
        params.put( "stringContent", shortXML );
        params.put( "encoding", "UTF-8" );

        FeatureMap feats = Factory.newFeatureMap();
        feats.put( "PMID", PMID );

        Document doc = ( Document ) Factory.createResource( "gate.corpora.DocumentImpl", params, feats );
        doc.setName( name );

        // create annotation sets
        doc.getAnnotations( "Leon" );
        doc.getAnnotations( "Paul" );
        doc.getAnnotations( "Suzanne" );
        // doc.getAnnotations( "Lydia" );

        corp.add( doc );
        if ( sync ) syncCorpus( corp );
    }

    public void syncCorpus( Corpus corp ) throws PersistenceException, SecurityException {
        dataStore.sync( corp );
    }
    
    

    public boolean isProcessAbbrevs() {
        return processAbbrevs;
    }

    public void setProcessAbbrevs( boolean processAbbrevs ) {
        this.processAbbrevs = processAbbrevs;
    }

    public static void main( String args[] ) throws Exception {
        PubMedIDtoGate pm2g = new PubMedIDtoGate();
        System.out.println( pm2g.getLoadedPMIDs().size() );
        // pm2g.getDocuments();
    }
}

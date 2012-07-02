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

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.util.FileTools;
import ubic.gemma.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.loader.entrez.pubmed.PubMedXMLParser;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.pubmedgate.Config;

public class PubMedLoadFromXML {
    private static Log log = LogFactory.getLog( PubMedLoadFromXML.class.getName() );
    PubMedXMLParser parser;
    PubMedIDtoGate p2g;
    File[] zippedXMLFiles;

    public PubMedLoadFromXML() throws Exception {
        p2g = new PubMedIDtoGate();
        parser = new PubMedXMLParser();
        File directory = new File( Config.config.getString( "whitetext.MEDLINE.home" ) );
        zippedXMLFiles = directory.listFiles( new FileFilter() {
            public boolean accept( File dir ) {
                return dir.getName().endsWith( ".gz" );
            }
        } );
    }

    public List<BibliographicReference> getRefs() throws Exception {
        List<BibliographicReference> result = new LinkedList<BibliographicReference>();
        for ( File file : zippedXMLFiles ) {
            log.info( "Loading: " + file );
            InputStream s = FileTools.getInputStreamFromPlainOrCompressedFile( file.getAbsolutePath() );
            Collection<BibliographicReference> refs = parser.parse( s );
            result.addAll( refs );
        }
        return result;
    }

    public List<BibliographicReference> getRefs( Collection<String> pmids ) throws Exception {
        List<BibliographicReference> result = new LinkedList<BibliographicReference>();
        for ( File file : zippedXMLFiles ) {
            log.info( "Loading: " + file );
            InputStream s = FileTools.getInputStreamFromPlainOrCompressedFile( file.getAbsolutePath() );
            Collection<BibliographicReference> refs = parser.parse( s );

            for ( BibliographicReference ref : refs ) {
                String PMID = ref.getPubAccession().getAccession();
                if ( pmids.contains( PMID ) ) {
                    result.add( ref );
                }
            }
        }
        return result;
    }

    // get PMID's from eUtils
    // filter all 2012 for the PMID's I want, use gzinputstream
    // load into gate - same object?

    public void loadRefs( List<BibliographicReference> refs, Corpus corp ) throws Exception {
        CountingMap<String> journals = new CountingMap<String>();

        int count = 0;
        boolean sync = false;
        for ( BibliographicReference ref : refs ) {
            if ( ++count % 500 == 0 ) {
                log.info( "Count:" + count + " of " + refs.size() );
            }

            String abstractText = ref.getAbstractText();

            // if it has no abstract then don't load it
            if ( abstractText == null ) {
                // String PMID = ref.getPubAccession().getAccession();
                // System.out.println( PMID + " has no abstract" );
                continue;
            }

            journals.increment( ref.getPublication() );

            boolean doAbbrev = true;
            try {
                p2g.loadReftoGate( ref, corp, doAbbrev, sync );
            } catch ( java.util.regex.PatternSyntaxException e ) {
                // its good, load it in
                doAbbrev = false;
                p2g.loadReftoGate( ref, corp, doAbbrev, sync );
            }
        }
        StopWatch watch = new StopWatch();
        watch.start();
        log.info( "Starting sync" );
        p2g.syncCorpus( corp );
        log.info( "Done sync:" + watch.getTime() );
        watch.stop();

        log.info( "Journal distribution:" );
        String journalString = "";
        for ( String key : journals.sortedKeyList() ) {
            journalString += key + " -> " + journals.get( key ) + "\n";
        }
        FileTools.stringToFile( journalString, new File( Config.config
                .getString( "whitetext.iteractions.results.folder" )
                + "journalDistroLoad" + System.currentTimeMillis() + ".txt" ) );
    }

    /**
     * Just loads all from the XML files (use on test XML directory)
     * 
     * @throws Exception
     */
    public void runAllTest() throws Exception {
        StopWatch watch = new StopWatch();
        watch.start();
        List<BibliographicReference> XMLloadedRefs = getRefs();
        log.info( "Got refs:" + watch.getTime() );
        watch.reset();
        watch.start();
        loadRefs( XMLloadedRefs, p2g.getUnseenCorp() );
        log.info( "Time to load refs:" + watch.getTime() );
        log.info( "Found in XML size:" + XMLloadedRefs.size() );
        log.info( "Corpus size:" + p2g.getUnseenCorp().size() );
    }

    public void runJNChem() throws Exception {
        PubMedSearch pSearch = new PubMedSearch();
        // 1471-4159 for Journal of Neurochemistry
        String query = "1471-4159";
        Collection<String> PMIDs = pSearch.searchAndRetrieveIdsByHTTP( query );
        List<BibliographicReference> loadedRefs = getRefs( PMIDs );
        loadRefs( loadedRefs, p2g.getUnseenCorp() );
        log.info( "query result size:" + PMIDs.size() );
        log.info( "Found in XML size:" + loadedRefs.size() );
        log.info( "Corpus size:" + p2g.getUnseenCorp().size() );
    }

    public void runMScanner() throws Exception {
        if ( !p2g.getUnseenCorp().isEmpty() ) {
            log.info( "Moving unseen to PubMedUnseenJNChem" );
            Corpus newCorp = p2g.createCorpus( "PubMedUnseenJNChem" );
            p2g.moveDataStoreDocuments( p2g.getUnseenCorp(), newCorp );
        }

        List<String> PMIDs = FileTools
                .getLines( "/home/lfrench/WhiteText/MScanner/mscanner.minus.loaded.inorder.all.features.txt" );

        List<BibliographicReference> loadedRefs = getRefs( PMIDs );
        loadRefs( loadedRefs, p2g.getUnseenCorp() );
        log.info( "query result size:" + PMIDs.size() );
        log.info( "Found in XML size:" + loadedRefs.size() );
        log.info( "Corpus size:" + p2g.getUnseenCorp().size() );
    }

    public void runMScanner2() throws Exception {
        if ( !p2g.getUnseenCorp().isEmpty() ) {
            log.info( "Moving unseen to PubMedUnseenMScan1" );
            Corpus newCorp = p2g.createCorpus( "PubMedUnseenMScan1" );
            p2g.moveDataStoreDocuments( p2g.getUnseenCorp(), newCorp );
        }
        

        List<String> PMIDs = FileTools
                .getLines( "/home/lfrench/WhiteText/MScanner/Mscanner2.results.PMIDs.union.minus.loaded.txt" );

        List<BibliographicReference> loadedRefs = getRefs( PMIDs );
        loadRefs( loadedRefs, p2g.getUnseenCorp() );
        log.info( "query result size:" + PMIDs.size() );
        log.info( "Found in XML size:" + loadedRefs.size() );
        log.info( "Corpus size:" + p2g.getUnseenCorp().size() );
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        PubMedLoadFromXML xmlLoader = new PubMedLoadFromXML();
        xmlLoader.runMScanner2();
    }

}

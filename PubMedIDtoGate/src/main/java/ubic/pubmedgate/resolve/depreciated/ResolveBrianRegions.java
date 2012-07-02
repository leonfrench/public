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

package ubic.pubmedgate.resolve.depreciated;

import gate.Annotation;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.connection.Connection;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.NeuroNamesLoader;
import ubic.pubmedgate.NeuroNamesMouseAndRatLoader;

public class ResolveBrianRegions {
    protected static Log log = LogFactory.getLog( ResolveBrianRegions.class );
    List<ConnectionsDocument> docs;
    String annotationSet;

    // all docs
    public ResolveBrianRegions( List<ConnectionsDocument> docs, String annotationSet ) {
        this.docs = docs;
        this.annotationSet = annotationSet;
    }

    @Deprecated
    public static void main( String[] args ) throws Exception {

        // BAMSDataLoader bams = new BAMSDataLoader();
        // StructureCatalogLoader allen = new StructureCatalogLoader();
        NeuroNamesLoader NN = new NeuroNamesLoader();
        NeuroNamesMouseAndRatLoader NNMR = new NeuroNamesMouseAndRatLoader();

        // log.info("BAMS");
        // BagOfWordsResolver.makeSetofBags(bams.getRegionsForLexicon());
        log.info( "Neuronames" );
        BagOfWordsResolver.makeSetofBags( NN.getRegionsForLexicon() );
        log.info( "Neuronames Rat" );
        BagOfWordsResolver.makeSetofBags( NNMR.getRegionsForLexicon() );
        // System.exit(1);

        GateInterface gateInt = new GateInterface();
        // use only training docs?
        List<ConnectionsDocument> docs = gateInt.getDocuments();

        ResolveBrianRegions regionGetter = new ResolveBrianRegions( docs, "UnionMerge" );
        log.info( "Test" );

        // Resolver BAMSResolver = new SimpleExactMatcher( bams.getRegionsForLexicon() );
        // Resolver allenResolver = new SimpleExactMatcher( allen.getRegionsForLexicon() );
        // Resolver NNResolver = new SimpleExactMatcher( NN.getRegionsForLexicon() );
        // Resolver NNMRResolver = new SimpleExactMatcher( NNMR.getRegionsForLexicon() );
        Resolver NNResolver = new LemmaResolver( NN.getRegionsForLexicon() );
        Resolver NNMRResolver = new LemmaResolver( NNMR.getRegionsForLexicon() );

        // Resolver BAMSResolver = new BagOfWordsResolver( bams.getRegionsForLexicon() );
        // Resolver allenResolver = new BagOfWordsResolver( allen.getRegionsForLexicon() );
        // Resolver NNResolver = new BagOfWordsResolver( NN.getRegionsForLexicon() );
        // Resolver NNMRResolver = new BagOfWordsResolver( NNMR.getRegionsForLexicon() );

        // Set<String> uniqueAnnotationText = new HashSet<String>();
        CountingMap<String> uniqueAnnotationText = new CountingMap<String>();

        int total = 0;
        int hits = 0;
        List<String> allBrainRegionText = regionGetter.getAllBrainRegionText();
        for ( String text : allBrainRegionText ) {
            // brain region mentions
            total++;
            String cleanText = text.trim().toLowerCase();
            uniqueAnnotationText.increment( cleanText );

            // String BAMSresolution = BAMSResolver.resolve( text );
            // String allenResolution = allenResolver.resolve( text );
            String NNResolution = NNResolver.resolve( text );
            String NNMRResolution = NNMRResolver.resolve( text );

            // if ( BAMSresolution != null ) hits++;

            if ( /* BAMSresolution != null || allenResolution != null || */NNResolution != null
                    || NNMRResolution != null ) {
                hits++;
                // System.out.println( text + "->" + BAMSresolution );
            }
        }

        System.out.println( "total=" + total );
        System.out.println( "hits=" + hits );

        // System.exit( 1 );

        // System.out.println( uniqueAnnotationText.toString() );

        hits = 0;
        for ( String text : uniqueAnnotationText.sortedKeyList() ) {
            System.out.print( text + " -> " + uniqueAnnotationText.get( text ) );

            // String BAMSresolution = BAMSResolver.resolve( text );
            // String allenResolution = allenResolver.resolve( text );
            String NNResolution = NNResolver.resolve( text );
            String NNMRResolution = NNMRResolver.resolve( text );

            // if ( BAMSresolution != null ) hits++;

            if ( /* BAMSresolution != null || allenResolution != null || */NNResolution != null
                    || NNMRResolution != null ) {
                hits++;
                System.out.println( " MATCHED" );
            } else {
                System.out.println( " NOT MATCHED" );
            }

        }

        System.out.println( "total=" + uniqueAnnotationText.keySet().size() );
        System.out.println( "hits=" + hits );

        // Set<String> all = BAMSResolver.getNames();
        // all.addAll( allenResolver.getNames() );
        // all.addAll( NNResolver.getNames() );
        // all.addAll( NNMRResolver.getNames() );
        // System.out.println( "Size of all names:" + all.size() );

    }

    // 
    /**
     * get the PMID's associated with each mention
     * 
     * @return
     */
    public StringToStringSetMap getAllBrainRegionTextToPMID() {
        StringToStringSetMap result = new StringToStringSetMap();
        for ( ConnectionsDocument doc : docs ) {
            for ( Annotation ann : doc.getBrainRegionAnnotations( annotationSet ) ) {
                String region = doc.getAnnotationText( ann ).toLowerCase().trim();
                String pmid = doc.getPMID();
                result.put( region, pmid );
            }
        }
        return result;
    }

    /*
     * Given a document and a string return how many times that string was annotated as a brain region in the docuement.
     * This ignores case.
     */
    public int getFrequenceInAbstract( ConnectionsDocument doc, String annotationText ) {
        int result = 0;
        for ( Annotation ann : doc.getBrainRegionAnnotations( annotationSet ) ) {
            String region = doc.getAnnotationText( ann ).trim();
            if ( region.equalsIgnoreCase( annotationText ) ) result++;
        }
        return result;
    }

    public CountingMap<String> getAllBrainRegionTextCounted() {
        CountingMap<String> uniqueAnnotationText = new CountingMap<String>();
        List<String> allBrainRegionText = getAllBrainRegionText();
        for ( String text : allBrainRegionText ) {
            String cleanText = text.trim().toLowerCase();
            uniqueAnnotationText.increment( cleanText );
        }
        return uniqueAnnotationText;
    }

    public Set<String> getAllBrainRegionTextSet() {
        return new HashSet<String>( getAllBrainRegionText() );
    }

    /**
     * Gets all the connection partner mentions for all connection annotators.
     * 
     * @return
     */
    public Set<String> getConnectionMentions() {
        Set<String> result = new HashSet<String>();
        for ( ConnectionsDocument doc : docs ) {
            List<Connection> connections = doc.getConnections();
            if ( connections != null ) {
                for ( Connection con : connections ) {
                    result.add( doc.getAnnotationText( con.getPartnerA() ).trim().toLowerCase() );
                    result.add( doc.getAnnotationText( con.getPartnerB() ).trim().toLowerCase() );
                }
            }
        }
        return result;
    }

    public List<String> getAllBrainRegionText() {
        List<String> l = new LinkedList<String>();

        for ( ConnectionsDocument doc : docs ) {
            for ( Annotation ann : doc.getBrainRegionAnnotations( annotationSet ) ) {
                // brain region mentions
                String region = doc.getAnnotationText( ann ).toLowerCase().trim();
                l.add( region );
            }
        }
        return l;
    }
}

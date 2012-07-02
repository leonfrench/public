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

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.CountingMap;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.NeuroNamesLoader;
import ubic.pubmedgate.NeuroNamesMouseAndRatLoader;

public class NewBrainRegionResolver {
    protected static Log log = LogFactory.getLog( NewBrainRegionResolver.class );

    /**
     * @param args
     */
    public static void main2( String[] args ) throws Exception {
        GateInterface gateInt = new GateInterface();
        List<ConnectionsDocument> docs = gateInt.getDocuments();
        ResolveBrianRegions regionGetter = new ResolveBrianRegions( docs, "UnionMerge" );
        log.info( regionGetter.getAllBrainRegionText().size() );
        CountingMap<String> uniqueAnnotationText = regionGetter.getAllBrainRegionTextCounted();
        for ( String region : uniqueAnnotationText.keySet() ) {
            String encoded = URLEncoder.encode( region, "UTF-8" );
            log.info( URLEncoder.encode( region, "UTF-8" ) );
            log.info( URLDecoder.decode( encoded, "UTF-8" ) );
            if ( !URLDecoder.decode( encoded, "UTF-8" ).equals( region ) ) {
                log.info( "Error" );
                break;
            }
        }
    }

    public static void main( String[] args ) {

        NeuroNamesLoader NN = new NeuroNamesLoader();
        NeuroNamesMouseAndRatLoader NNMR = new NeuroNamesMouseAndRatLoader();


        // System.exit(1);

        // Resolver NNResolver = new StemmerResolver( NN.getRegionsForLexicon() );
        // Resolver NNMRResolver = new StemmerResolver( NNMR.getRegionsForLexicon() );
        BIRNLexResolver birn = new BIRNLexResolver();
        // Resolver NNResolver = new SimpleExactMatcher( birn.getRegionsForLexicon() );
        // Resolver NNMRResolver = new SimpleExactMatcher( birn.getRegionsForLexicon() );

        // Resolver NNResolver = new BagOfWordsResolver( birn.getRegionsForLexicon() );
        // Resolver NNMRResolver = new BagOfWordsResolver( birn.getRegionsForLexicon() );
        Set<String> NNSet = NN.getRegionsForLexicon();
        NNSet.addAll( NNMR.getRegionsForLexicon() );

        // Resolver bagStem = new BaggedStemmer( NNSet );
        // bagStem = new BaggedStemmer( NNSet );
        // testResolver( NNCombined, uniqueAnnotationText );

        // Add BIRNLex
        //NNSet.addAll( birn.getRegionsForLexicon() );

        Resolver NNSimple = new SimpleExactMatcher( NNSet );
        Resolver NNResolver = new BagOfWordsResolver( NNSet );
        Resolver NNStem = new StemResolver( NNSet );
        log.info( NNSimple.resolve( "subparaventricular zone"));
        log.info( NNResolver.resolve( "subparaventricular zone"));
        log.info( NNStem.resolve( "subparaventricular zone"));
        System.exit(1);

        Resolver NNCombined = new CombinedResolver( NNResolver, NNStem, NNSimple );

        GateInterface gateInt = new GateInterface();
        List<ConnectionsDocument> docs = gateInt.getDocuments();

        ResolveBrianRegions regionGetter = new ResolveBrianRegions( docs, "UnionMerge" );
        log.info( regionGetter.getAllBrainRegionText().size() );
        CountingMap<String> uniqueAnnotationText = regionGetter.getAllBrainRegionTextCounted();

        testResolver( NNCombined, uniqueAnnotationText );

    }

    private static void testResolver( Resolver resolver, CountingMap<String> uniqueAnnotationText ) {
        int matched = 0;
        int fullHits = 0;
        for ( String text : uniqueAnnotationText.sortedKeyList() ) {

            String NNResolution = resolver.resolve( text );

            if ( NNResolution != null ) {
                matched++;
                fullHits += uniqueAnnotationText.get( text );
                System.out.print( text + " -> " + uniqueAnnotationText.get( text ) );
                System.out.println( " MATCHED" );
            } else {
                //System.out.println( " NOT MATCHED" );
            }

        }
        // log.info( "lexiconsize=" + birnResolve.getRegionsForLexicon().size() );
        log.info( resolver.getName() + "matched=" + matched + " of " + uniqueAnnotationText.keySet().size() );
        log.info( resolver.getName() + "full hits=" + fullHits + " of " + uniqueAnnotationText.summation() );
    }
}

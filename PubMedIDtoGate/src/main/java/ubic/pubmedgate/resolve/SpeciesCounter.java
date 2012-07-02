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

package ubic.pubmedgate.resolve;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.JenaUtil;
import ubic.BAMSandAllen.Util;
import ubic.BAMSandAllen.Vocabulary;
import ubic.basecode.dataStructure.CountingMap;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.organism.SpeciesLoader;
import ubic.pubmedgate.resolve.depreciated.ResolveBrianRegions;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class SpeciesCounter {
    protected static Log log = LogFactory.getLog( SpeciesCounter.class );

    GateInterface p2g;
    // shouldn't use this class it's old
    ResolveBrianRegions resolver;
    SpeciesLoader filterLoader;

    public SpeciesCounter( String annotationSet ) throws Exception {
        p2g = new GateInterface();
        filterLoader = new SpeciesLoader();
        resolver = new ResolveBrianRegions( p2g.getConnectionDocuments(), annotationSet );
    }

    public boolean isFiltered( String ID ) {
        return filterLoader.isFiltered( ID );
    }

    // how many species per abstract!
    public double getAverageSpeciesPerAbstract() {
        double result = 0;
        int moreThanOne = 0;
        int zero = 0;
        List<ConnectionsDocument> documents = p2g.getDocuments();
        for ( ConnectionsDocument doc : documents ) {
            int species = doc.getLinnaeusSpecies().size();
            if ( species > 1 ) moreThanOne++;
            if ( species == 0 ) zero++;
            result += species;
        }
        log.info( "Number of documents:" + documents.size() );
        log.info( "Documents with no species:" + zero );
        log.info( "Documents with more than one species:" + moreThanOne );
        return result / ( double ) documents.size();
    }

    /**
     * For a given species return the number of abstracts where the mention and the species occur. If frequency is set
     * to true it will take into account the number of times that mention occurs in the abstract.
     * 
     * @param mentions
     * @param frequency
     * @param model
     * @return
     */
    public CountingMap<String> getSpeciesCounts( Set<Resource> mentions, boolean frequency, ResolutionRDFModel model )
            throws Exception {
        Set<String> filtered = filterLoader.getFilteredIDs();
        Set<String> common = getCommonSpeciesLinStrings();

        CountingMap<String> speciesCounts = new CountingMap<String>();
        for ( Resource r : mentions ) {
            // get pmids
            Set<Resource> pmids = model.getPMIDs( r );
            for ( Resource pmid : pmids ) {
                // doesnt count how often that mention occurs in the abstract
                String url = pmid.getURI();
                String pmidString = url.replace( Vocabulary.getpubmedURIPrefix(), "" );
                ConnectionsDocument document = p2g.getByPMID( pmidString );
                // get species from the doc and add to countingmap
                Set<String> species = document.getLinnaeusSpecies();

                // removed filtered species
                species.removeAll( filtered );

                int incrementAmount = 0;
                if ( frequency ) {
                    // need a newer method for number of occurances in an abstract
                    incrementAmount = resolver.getFrequenceInAbstract( document, JenaUtil.getLabel( r ) );
                } else
                    incrementAmount = 1;

                // a separate category for common species
                boolean hasCommon = Util.intersectSize( common, species ) > 0;

                for ( int i = 0; i < incrementAmount; i++ ) {
                    if ( hasCommon ) {
                        speciesCounts.increment( "Common" );
                    }
                    speciesCounts.incrementAll( species );
                }

            }
        }
        return speciesCounts;
    }

    public static Set<String> getCommonSpeciesLinStrings() {
        Set<String> common = new HashSet<String>();
        for ( String s : getCommonSpeciesIDStrings() )
            common.add( "species:ncbi:" + s );
        return common;
    }

    public static Set<Resource> getCommonSpeciesResources( Model m ) {
        Set<Resource> common = new HashSet<Resource>();
        for ( String s : getCommonSpeciesIDStrings() )
            common.add( m.createResource( "http://bio2rdf.org/taxon:" + s ) );
        return common;
    }

    public static Set<String> getCommonSpeciesIDStrings() {
        Set<String> common = new HashSet<String>();
        common.add( "10116" ); // rat
        common.add( "10090" ); // mouse
        common.add( "9606" ); // human
        common.add( "9544" ); // rhesus
        common.add( "9541" ); // macaca fascicularis
        return common;
    }

    /**
     * given an corpus return a list of mentions also we have the species mentioned in that abstract create a list of
     * mentions for each species // or // what species is this mention linked to
     * 
     * @param mentions
     * @param model
     * @return
     */
    public Map<String, Set<Resource>> getSpeciesMentions( Set<Resource> mentions, ResolutionRDFModel model ) {
        Map<String, Set<Resource>> result = new HashMap<String, Set<Resource>>();

        for ( Resource r : mentions ) {
            // get pmids
            Set<Resource> pmids = model.getPMIDs( r );
            for ( Resource pmid : pmids ) {
                // doesnt count how often that mention occurs in the abstract
                String url = pmid.getURI();
                String pmidString = url.replace( Vocabulary.getpubmedURIPrefix(), "" );
                ConnectionsDocument document = p2g.getByPMID( pmidString );
                // get species from the doc and add to countingmap
                Set<String> species = document.getLinnaeusSpecies();
                for ( String spec : species ) {
                    Set<Resource> current = result.get( spec );
                    if ( current == null ) {
                        current = new HashSet<Resource>();
                        result.put( spec, current );
                    }
                    current.add( r );
                }
            }
        }
        return result;
    }

    public static void main( String args[] ) throws Exception {
        SpeciesCounter sc = new SpeciesCounter( "UnionMerge" );
        log.info( sc.getAverageSpeciesPerAbstract() );
    }
}

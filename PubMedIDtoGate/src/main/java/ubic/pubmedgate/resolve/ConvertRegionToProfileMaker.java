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

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.JenaUtil;
import ubic.BAMSandAllen.Vocabulary;
import ubic.BAMSandAllen.AllenDataLoaders.StructureExpressionInformationLoader;
import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.resolve.depreciated.ResolveBrianRegions;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Given a ontology and a set of normalizations, convert the abstracts that have those mentions into text profiles.
 * 
 * @author leon
 */

public class ConvertRegionToProfileMaker {
    protected static Log log = LogFactory.getLog( ResolveBrianRegions.class );
    ResolutionRDFModel model;
    Resource sourceType;
    GateInterface p2g;

    // up propigate?
    public ConvertRegionToProfileMaker( ResolutionRDFModel model ) {
        this( Vocabulary.BAMSName, model );
    }

    public ConvertRegionToProfileMaker( Resource sourceType, ResolutionRDFModel model ) {
        p2g = new GateInterface();
        this.model = model;
        this.sourceType = this.model.getResource( sourceType );
    }

    public void run() throws Exception {
        log.info( "Loading:" + JenaUtil.getLabel( sourceType ) );
        StringToStringSetMap result = new StringToStringSetMap();
        // get all concept to mention links
        Set<Resource> concepts = model.getConceptsBySource( sourceType );
        int i = 0;
        for ( Resource concept : concepts ) {
            String label = JenaUtil.getLabel( concept );
            log.info( label );
            // log.info( model.get )
            // if ( i++ > 10 ) System.exit(1);
            Set<Resource> terms = model.getTermsFromConcepts( concept );
            Set<Resource> mentions = model.getMentionsFromTerms( terms );
            // log.info( terms );
            // log.info( mentions );
            for ( Resource mention : mentions ) {
                Set<Resource> pmids = model.getPMIDs( mention );
                Set<String> words = convertPMIDsToBagsOfWords( pmids );
                // log.info( "words:" + words );
                // result.put( label, words ); --wrong
                for ( String word : words ) {
                    result.put( label, word );
                }

                // result.addAll( label, words );

                // result.out()

                // strip pmid from URL?
                // get bag of words for pmid
            }
        }
        DoubleMatrix<String, String> resultMatrix = convertMapToMatrix( result );
        // log.info( resultMatrix.toString() );
        // FIX!
        ObjectOutputStream out = new ObjectOutputStream( new FileOutputStream( JenaUtil.getLabel( sourceType )
                + ".matrix.cache" ) );
        out.writeObject( resultMatrix );
        out.close();

    }

    public Set<String> convertPMIDsToBagsOfWords( Set<Resource> pmids ) {
        Set<String> pmidStrings = getPMIDStrings( pmids );
        Set<String> result = new HashSet<String>();
        for ( String pmidString : pmidStrings ) {
            log.info( pmidString );
            ConnectionsDocument doc = p2g.getByPMID( pmidString );
            result.addAll( doc.getBagOfWords() );
        }
        return result;
    }

    public DoubleMatrix<String, String> convertMapToMatrix( StringToStringSetMap map ) {
        // keys are columns
        Set<String> keys = map.keySet();
        // values are rows
        Set<String> rows = map.getSeenValues();

        DoubleMatrix<String, String> resultMatrix = new DenseDoubleMatrix<String, String>( rows.size(), keys.size() );
        resultMatrix.setRowNames( new LinkedList<String>( rows ) );
        resultMatrix.setColumnNames( new LinkedList<String>( keys ) );

        // simple binary setup
        for ( String key : keys ) {
            for ( String value : map.get( key ) ) {
                resultMatrix.setByKeys( value, key, 1.0 );
            }
        }

        return resultMatrix;
    }

    /**
     * Convert pmid URL/resources to strings
     * 
     * @param pmids
     * @return
     */
    public Set<String> getPMIDStrings( Set<Resource> pmids ) {
        Set<String> result = new HashSet<String>();
        for ( Resource pmid : pmids ) {
            String uri = pmid.getURI();
            int start = uri.lastIndexOf( ':' ) + 1;
            result.add( uri.substring( start ) );
        }
        return result;
    }

    public static void main( String[] args ) throws Exception {
        ResolutionRDFModel resolutionModel = new ResolutionRDFModel( Config.config
                .getString( "resolve.Lexicon.resolution.RDF.allComp" ) );

        ConvertRegionToProfileMaker profile = new ConvertRegionToProfileMaker( Vocabulary.BAMSName, resolutionModel );
        profile.run();
        
        profile = new ConvertRegionToProfileMaker( Vocabulary.ABAName, resolutionModel );
        profile.run();

    }
}

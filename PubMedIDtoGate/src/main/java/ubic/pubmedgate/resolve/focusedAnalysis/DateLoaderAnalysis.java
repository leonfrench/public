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

package ubic.pubmedgate.resolve.focusedAnalysis;

//ubic.pubmedgate.resolve.focusedAnalysis.DateLoaderAnalysis

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.dataStructure.params.ParamKeeper;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.resolve.EvaluationRDFModel;
import ubic.pubmedgate.resolve.SpeciesCounter;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class DateLoaderAnalysis {
    protected static Log log = LogFactory.getLog( DateLoaderAnalysis.class );

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {

        Model modelLoad = ModelFactory.createDefaultModel();
        String fileProperty;
        // fileProperty = "resolve.Lexicon.resolution.RDF.allComp";
        // modelLoad.read( new FileInputStream( Config.config.getString( fileProperty ) ), null );
        fileProperty = "resolve.Lexicon.resolution.RDF";
        modelLoad.read( new FileInputStream( Config.config.getString( fileProperty ) ), null );

        boolean reason = true;
        EvaluationRDFModel model = new EvaluationRDFModel( modelLoad, reason );

        Set<Resource> commonSpecies = SpeciesCounter.getCommonSpeciesResources( modelLoad );

        writeYearStats( model, commonSpecies );

        // run again with everything
        fileProperty = "resolve.Lexicon.resolution.RDF.allComp";
        modelLoad.read( new FileInputStream( Config.config.getString( fileProperty ) ), null );
        model = new EvaluationRDFModel( modelLoad, reason );
        commonSpecies = SpeciesCounter.getCommonSpeciesResources( modelLoad );
        writeYearStats( model, commonSpecies );

    }

    public static void writeYearStats( EvaluationRDFModel model, Set<Resource> commonSpecies ) throws Exception {
        int iterations = 0;

        Map<Resource, Integer> pubDates = model.getPMIDtoYearMap();
        log.info( "Number of abstracts:" + pubDates.keySet().size() );
        ParamKeeper keeper = new ParamKeeper();
        Set<Integer> allYears = new HashSet<Integer>( pubDates.values() );
        for ( Integer year : allYears ) {
            Map<String, String> params = new HashMap<String, String>();
            params.put( "Year", year + "" );

            Set<Resource> mentions = new HashSet<Resource>();
            CountingMap<Resource> taxons = new CountingMap<Resource>();
            // CountingMap<String> speciesCounts = new
            int pmidCount = 0;
            for ( Resource pmid : pubDates.keySet() ) {
                // if its the right year
                if ( pubDates.get( pmid ).intValue() == year ) {
                    pmidCount++;
                    mentions.addAll( model.getMentionsInPMID( pmid ) );
                    Set<Resource> taxonInPMID = model.getSpeciesForPMID( pmid );
                    taxons.incrementAll( taxonInPMID );
                }
            }
            params.put( "Abstracts", pmidCount + "" );
            for ( Resource taxon : taxons.keySet() ) {
                if ( commonSpecies.contains( taxon ) ) {
                    int count = taxons.get( taxon );
                    params.put( taxon.toString(), count + "" );
                }
            }
            params.put( "TaxonCount", taxons.keySet().size() + "" );
            // write out how many taxons
            // get mention data on evaluations
            params.putAll( model.evaluateConceptMapping( model.getAllProperties(), mentions ) );

            keeper.addParamInstance( params );
            iterations++;
            log.info( iterations + " of " + allYears.size() );
            if ( iterations % 10 == 0 ) {

            }
        }

        keeper.writeExcel( Config.config.getString( "whitetext.resolve.results.folder" ) + "ByYear"
                + pubDates.keySet().size() + ".xls" );
        // Set<Resource> humanMentions = model.getMentionsForSpecies( "9606" );
        // Set<Resource> commonSpeciesMentions = new HashSet<Resource>();
        // Set<Resource> commonSpeciesPMIDs = new HashSet<Resource>();

        // iterate years
        // for each year
        // species distribution
        // evaluations for mentions
    }

}

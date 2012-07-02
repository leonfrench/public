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

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.JenaUtil;
import ubic.BAMSandAllen.Util;
import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.dataStructure.params.ParamKeeper;
import ubic.basecode.math.CorrelationStats;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.resolve.EvaluationRDFModel;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Outputs spreadsheets that compute the correlation between an abstracts yearly distribution and time
 * 
 * @author leon
 */
public class RegionsOverTime {
    protected static Log log = LogFactory.getLog( RegionsOverTime.class );

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        boolean allComp = true;
        log.info( "Compute all:" + allComp );
        computeAllCorrelations( allComp );
    }

    public static Set<String> getInteresingURIs() throws Exception {
        Set<String> URIs = new HashSet<String>();
        URIs.add( "http://www.purl.org/neuronames#279" );// olfactory bulb
        URIs.add( "http://www.purl.org/neuronames#237" );// amygdala
        URIs.add( "http://www.purl.org/neuronames#353" );// lateral geniculate nucleus
        URIs.add( "http://www.purl.org/neuronames#465" );// tectum
        URIs.add( "http://www.purl.org/neuronames#182" );// CA Fields HIPPOCAMPUS
        URIs.add( "http://www.purl.org/neuronames#H456" );// SUPERIOR COLLICULUS
        URIs.add( "http://www.purl.org/neuronames#H20" );// cerebral cortex 
        URIs.add( "http://www.purl.org/neuronames#698" );// medula
        URIs.add( "http://www.purl.org/neuronames#H467" );// inferior colliculus
        
        return URIs;
    }

    public static void computeAllCorrelations( boolean allComp ) throws Exception {

        Model modelLoad = ModelFactory.createDefaultModel();
        String fileProperty;

        if ( allComp ) {
            fileProperty = "resolve.Lexicon.resolution.RDF.allComp";
            modelLoad.read( new FileInputStream( Config.config.getString( fileProperty ) ), null );
        }

        fileProperty = "resolve.Lexicon.resolution.RDF";
        modelLoad.read( new FileInputStream( Config.config.getString( fileProperty ) ), null );

        ParamKeeper results = new ParamKeeper();
        ParamKeeper regionsOfInterestResults = new ParamKeeper();
        Set<String> regionsOfInterest = getInteresingURIs();

        boolean reason = true;
        EvaluationRDFModel model = new EvaluationRDFModel( modelLoad, reason );
        
        model.printLexiconCoverageStats();
        System.exit(1);

        Map<Resource, Integer> pmidToYearMap = model.getPMIDtoYearMap();

        CountingMap<Integer> pmidCountPerYear = new CountingMap<Integer>();
        for ( Integer year : pmidToYearMap.values() ) {
            pmidCountPerYear.increment( year );
        }

        // for ( Resource mention : model.getMentions() ) {
        int count = 0;
        for ( Resource concept : model.getConcepts() ) {
            Map<String, String> params = new HashMap<String, String>();
            params.put( "URI", concept.getURI() );
            log.info( concept.getURI() );
            params.put( "Label", "\"" + JenaUtil.getLabel( concept ) + "\"" );

            Set<Resource> mentions = model.getMentionsFromTerms( model.getTermsFromConcepts( concept ) );
            params.put( "Mentions", mentions.size() + "" );
            Set<Resource> pmids = model.getPMIDs( mentions );
            params.put( "PMIDCount", pmids.size() + "" );

            CountingMap<Integer> years = new CountingMap<Integer>();
            for ( Resource pmid : pmids ) {
                years.increment( pmidToYearMap.get( pmid ) );
            }

            int numberOfYears = years.size();

            params.put( "numberOfYears", numberOfYears + "" );

            // if it's one of the interesting regions, then dump it's data
            if ( regionsOfInterest.contains( concept.getURI() ) ) {
                Map<String, String> interestParams = new HashMap<String, String>();
                interestParams.putAll( params );
                for ( Integer i : years.keySet() ) {
                    interestParams.put( i + "", years.get( i ) + "" );
                }
                regionsOfInterestResults.addParamInstance( interestParams );
            }

            if ( numberOfYears > 2 ) {
                log.info( "Number of years published:" + numberOfYears );
                log.info( params.toString() );

                double[] yearArray = new double[numberOfYears];
                double[] frequencyArray = new double[numberOfYears];
                double[] bumpArray = new double[numberOfYears];
                int spot = 0;
                for ( Integer i : years.keySet() ) {
                    yearArray[spot] = i;
                    frequencyArray[spot] = ( double ) years.get( i ) / ( double ) pmidCountPerYear.get( i );
                    if ( i > 1983 && i < 2001 )
                        bumpArray[spot] = 1;
                    else
                        bumpArray[spot] = 0;
                    spot++;
                    
                }

                double correlation = Util.spearmanCorrel( yearArray, frequencyArray );
                log.info( "Spearman correlation:" + correlation );
                params.put( "Spearman correlation", correlation + "" );
                params.put( "Spearman p-value", CorrelationStats.spearmanPvalue( correlation, numberOfYears ) + "" );

                double bumpCorrelation = Util.spearmanCorrel( bumpArray, frequencyArray );
                params.put( "Bump Spearman correlation", bumpCorrelation + "" );
                params.put( "Bump Spearman p-value", CorrelationStats.spearmanPvalue( bumpCorrelation, numberOfYears )
                        + "" );

                results.addParamInstance( params );

                // if ( count++ > 10 ) break;
            }
        }

        results.writeExcel( Config.config.getString( "whitetext.resolve.results.folder" ) + "RegionsByYear."
                + modelLoad.size() + ".statements.xls" );
        regionsOfInterestResults.writeExcel( Config.config.getString( "whitetext.resolve.results.folder" )
                + "RegionsOfInterestByYear." + modelLoad.size() + ".statements.xls" );

        // for each mention
        // rank it's abstracts according to year

    }
}

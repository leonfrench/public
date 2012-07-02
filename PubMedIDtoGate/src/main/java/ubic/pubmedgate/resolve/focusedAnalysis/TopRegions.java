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
import ubic.basecode.dataStructure.params.ParamKeeper;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.resolve.EvaluationRDFModel;
import ubic.pubmedgate.resolve.SpeciesCounter;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class TopRegions {
    protected static Log log = LogFactory.getLog( TopRegions.class );

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        // iterate and dump out regions - use Excel to sort and present
        ParamKeeper keeper = new ParamKeeper();

        boolean allComp = true;
        // if training then check for accepted
        Model modelLoad;
        String fileProperty;
        boolean reason = true;
        EvaluationRDFModel allCompModel = null;

        if ( allComp ) {
            modelLoad = ModelFactory.createDefaultModel();
            fileProperty = "resolve.Lexicon.resolution.RDF.allComp";
            modelLoad.read( new FileInputStream( Config.config.getString( fileProperty ) ), null );
            allCompModel = new EvaluationRDFModel( modelLoad, reason );
        }

        fileProperty = "resolve.Lexicon.resolution.RDF";
        modelLoad = ModelFactory.createDefaultModel();
        modelLoad.read( new FileInputStream( Config.config.getString( fileProperty ) ), null );

        EvaluationRDFModel model = new EvaluationRDFModel( modelLoad, reason );

        Set<Resource> commonSpeciesMentions = new HashSet<Resource>();
        Set<Resource> commonAllSpeciesMentions = new HashSet<Resource>();
        Set<String> commonSpecies = SpeciesCounter.getCommonSpeciesLinStrings();

        for ( String common : commonSpecies ) {
            common = common.substring( common.lastIndexOf( ":" ) + 1 );
            Set<Resource> mentionsForSpecies = model.getMentionsForSpecies( common );
            commonSpeciesMentions.addAll( mentionsForSpecies );
            if ( allComp ) commonAllSpeciesMentions.addAll( allCompModel.getMentionsForSpecies( common ) );
        }

        int count = 0;
        for ( Resource concept : model.getConcepts() ) {
            Map<String, String> line = new HashMap<String, String>();
            line.put( "conceptURI", concept.getURI() );
            line.put( "conceptLabel", "\"" + JenaUtil.getLabel( concept ) + "\"" );

            Set<Resource> terms = model.getTermsFromConcepts( concept );
            line.put( "terms", terms.size() + "" );

            Set<Resource> mentions = model.getMentionsFromTerms( terms );
            line.put( "unique mentions", mentions.size() + "" );

            line.put( "mentionFrequency", model.sumMentionFrequencies( mentions ) + "" );

            Set<Resource> commonMentions = ( Set<Resource> ) Util.intersect( commonSpeciesMentions, mentions );
            line.put( "unique Common Mentions", commonMentions.size() + "" );
            line.put( "Common mentionFrequency", model.sumMentionFrequencies( commonMentions ) + "" );

            Set<Resource> rejectMentions = new HashSet<Resource>();
            Set<Resource> spec2genMentions = new HashSet<Resource>();
            for ( Resource mention : mentions ) {
                if ( model.rejected( mention, concept ) ) {
                    rejectMentions.add( mention );
                }
                if ( model.specToGen( mention, concept ) ) {
                    spec2genMentions.add( mention );
                }
            }
            line.put( "Rejected Mentions", rejectMentions.size() + "" );
            line.put( "Spec2Gen Mentions", spec2genMentions.size() + "" );
            String baseURI = concept.getURI();
            baseURI = baseURI.substring( 0, baseURI.lastIndexOf( '#' ) );
            line.put( "Base URI", baseURI );

            Set<String> termStrings = new HashSet<String>();
            for ( Resource term : terms ) {
                termStrings.add( JenaUtil.getLabel( term ) );
            }
            line.put( "terms", termStrings.toString() );
            line.put( "termCount", termStrings.size() + "" );

            if ( allComp ) {
                Set<Resource> allMentions = allCompModel.getMentionsFromTerms( terms );

                line.put( "unique allCompMentions", allMentions.size() + "" );
                line.put( "allCompMentions frequency", model.sumMentionFrequencies( allMentions ) + "" );

                Set<Resource> commonMentionsAll = ( Set<Resource> ) Util.intersect( commonAllSpeciesMentions,
                        allMentions );

                line.put( "unique allComp Common Mentions", commonMentionsAll.size() + "" );
                line.put( "frequency allComp Common Mentions", allCompModel.sumMentionFrequencies( commonMentionsAll )
                        + "" );
            }

            keeper.addParamInstance( line );
            if ( count++ % 100 == 0 ) {
                log.info( count );
//                if (count > 100) break;
            }

        }

        keeper.writeExcel( Config.config.getString( "whitetext.resolve.results.folder" ) + "TopRegions.xls" );

    }
}

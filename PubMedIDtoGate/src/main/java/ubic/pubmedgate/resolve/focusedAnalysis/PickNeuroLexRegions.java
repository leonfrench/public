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

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.JenaUtil;
import ubic.BAMSandAllen.Util;
import ubic.BAMSandAllen.Vocabulary;
import ubic.basecode.dataStructure.params.ParamKeeper;
import ubic.basecode.util.FileTools;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.resolve.EvaluationRDFModel;
import ubic.pubmedgate.resolve.SpeciesCounter;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class PickNeuroLexRegions {
    // runs on sever
    protected static Log log = LogFactory.getLog( PickNeuroLexRegions.class );

    public static void printMentionCountForPaper( Set<Resource> mentions, boolean training ) throws Exception {
        Model modelLoad = ModelFactory.createDefaultModel();
        String fileProperty;

        if ( training ) {
            fileProperty = "resolve.Lexicon.resolution.RDF";
            modelLoad.read( new FileInputStream( Config.config.getString( fileProperty ) ), null );
        } else {
            fileProperty = "resolve.Lexicon.resolution.RDF.allComp";
            modelLoad.read( new FileInputStream( Config.config.getString( fileProperty ) ), null );
        }
        log.info( modelLoad.size() );

        Set<Resource> newMentions = new HashSet<Resource>();
        for ( Resource mention : mentions ) {
            newMentions.add( modelLoad.createResource( mention.getURI() ) );
        }
        mentions = newMentions;

        boolean reason = true;
        EvaluationRDFModel model = new EvaluationRDFModel( modelLoad, reason );
        log.info( "mentions:" + mentions.size() );
        log.info( "Mention freq:" + model.sumMentionFrequencies( mentions ) );
        log.info( "Mention abstract freq:" + model.sumAbstractFrequencies( mentions ) );
    }

    public static void countSizes() throws Exception {
        Model modelLoad = ModelFactory.createDefaultModel();
        String fileProperty;

        fileProperty = "resolve.Lexicon.resolution.RDF.allComp";
        modelLoad.read( new FileInputStream( Config.config.getString( fileProperty ) ), null );
        boolean reason = true;
        EvaluationRDFModel model = new EvaluationRDFModel( modelLoad, reason );
        model.getStats();

    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        // has human species
        // has spec to gen match to NIFSTD
        // no rejections
        // sort by occurances
        // get PMID references

        // common.add( "species:ncbi:9606" ); // human

        Model modelLoad = ModelFactory.createDefaultModel();
        String fileProperty;
        fileProperty = "resolve.Lexicon.resolution.RDF.allComp";
        modelLoad.read( new FileInputStream( Config.config.getString( fileProperty ) ), null );
        log.info( modelLoad.size() );
        fileProperty = "resolve.Lexicon.resolution.RDF";
        modelLoad.read( new FileInputStream( Config.config.getString( fileProperty ) ), null );
        log.info( modelLoad.size() );

        boolean reason = true;
        EvaluationRDFModel model = new EvaluationRDFModel( modelLoad, reason );
        Set<Resource> humanMentions = model.getMentionsForSpecies( "9606" );
        Set<Resource> commonSpeciesMentions = new HashSet<Resource>();
        Set<String> commonSpecies = SpeciesCounter.getCommonSpeciesLinStrings();
        Set<Resource> commonSpeciesPMIDs = new HashSet<Resource>();

        for ( String common : commonSpecies ) {
            common = common.substring( common.lastIndexOf( ":" ) + 1 );
            Set<Resource> mentionsForSpecies = model.getMentionsForSpecies( common );
            commonSpeciesMentions.addAll( mentionsForSpecies );

            commonSpeciesPMIDs.addAll( model.getPMIDsForSpecies( common ) );

        }

        log.info( "Common mentions:" + commonSpeciesMentions.size() );
        commonSpeciesMentions.retainAll( model.getMatchedMentions() );

        log.info( "Common pmids:" + commonSpeciesPMIDs.size() );

        log.info( "Matched common mentions:" + commonSpeciesMentions.size() );

        log.info( "Human mentions:" + humanMentions.size() );
        Set<Resource> BIRNConcepts = model.getNIFSTDConcepts();
        Set<Resource> NNConcepts = model.getNNConcepts();
        humanMentions.retainAll( model.getMatchedMentions() );

        log.info( "Matched Human mentions:" + humanMentions.size() );

        Set<Resource> finalRegionMentions = new HashSet<Resource>();
        int occuranceThreshold = 0;
        int count = 0;
        int nnLinks = 0;
        int birnConcepts = 0;
        ParamKeeper keeper = new ParamKeeper();

        ParamKeeper finalKeeper = new ParamKeeper();

        for ( Resource commonMention : commonSpeciesMentions ) {
            Set<Resource> concepts = model.getMentionEvaluations( commonMention );
            // there's going to be two of these properties - get the larger? should have number of unseen occurances
            int occurances = commonMention.getProperty( Vocabulary.number_of_occurances ).getInt();
            // int abstractCount = commonMention.getProperty( Vocabulary.number_of_abstracts ).getInt();
            Set<Resource> pmids = model.getPMIDs( commonMention );
            Set<Resource> commonPmids = ( Set<Resource> ) Util.intersect( commonSpeciesPMIDs, pmids );
            // filter pmids for common species
            // if the pmid has link to one of common

            // StmtIterator statements = neuromention.listProperties( Vocabulary.in_PMID );
            // Resource taxon = model.createResource( "http://bio2rdf.org/taxon:" + species );
            // ResIterator resIt = model.listResourcesWithProperty( Vocabulary.mentions_species, taxon );

            int abstractCount = pmids.size();
            int commonAbstractCount = commonPmids.size();

            for ( Resource concept : concepts ) {
                if ( BIRNConcepts.contains( concept ) ) {
                    birnConcepts++;
                    // how do I know it's spec to gen? -need to create automatic evaluations!
                    if ( model.specToGen( commonMention, concept ) && commonAbstractCount > 1 ) {
                        Map<String, String> params = new HashMap<String, String>();
                        params.put( "mention URI", commonMention.getURI() );
                        String mentionLabel = JenaUtil.getLabel( commonMention );
                        mentionLabel = mentionLabel.substring( 0, 1 ).toUpperCase() + mentionLabel.substring( 1 );
                        params.put( "mention label", mentionLabel );
                        params.put( "parent concept URI", concept.getURI() );
                        params.put( "parent concept label", "\"" + JenaUtil.getLabel( concept ) + "\"" );
                        params.put( "abstract count", abstractCount + "" );
                        params.put( "common abstract count", commonAbstractCount + "" );

                        params.put( "PubMed link", "HYPERLINK(\"" + model.getNCBIPMIDLink( commonPmids )
                                + "\",\"PubMed Link\")" );

                        count++;
                        finalRegionMentions.add( commonMention );

                        log.info( abstractCount + " " + commonAbstractCount + " " + JenaUtil.getLabel( commonMention )
                                + " < " + JenaUtil.getLabel( concept ) );

                        // TODO get birnlex IDs? - are they exact?
                        if ( Util.intersectSize( NNConcepts, concepts ) > 0 ) {
                            nnLinks++;
                            params.put( "hasNNlink", "true" );
                        } else {
                            params.put( "hasNNlink", "false" );
                        }

                        keeper.addParamInstance( params );

                        Map<String, String> finalParams = new HashMap<String, String>();
                        finalParams.put( "Label", mentionLabel );
                        finalParams.put( "Is part of", JenaUtil.getLabel( concept ) );
                        finalParams.put( "Has role", "Brain Subdivisions based on automated term selection" );
                        finalParams.put( "Id", "" );
                        String pmidComaString = "\"" + model.getComaSepPMIDs( commonPmids, 140 ) + "\"";
                        finalParams.put( "PMID", pmidComaString );
                        finalKeeper.addParamInstance( finalParams );
                    }
                }
            }
        }
        log.info( "Number printed:" + count );
        log.info( "Number in set:" + finalRegionMentions.size() );
        log.info( "Has NN links:" + nnLinks );
        log.info( "Birn concepts:" + birnConcepts );

        keeper.writeExcel( Config.config.getString( "whitetext.resolve.results.folder" ) + "forNeuroLex.xls" );
        finalKeeper.writeExcel( Config.config.getString( "whitetext.resolve.results.folder" ) + "forNeuroLexFinal.xls" );

        // printMentionCountForPaper( finalRegionMentions, true );
        // printMentionCountForPaper( finalRegionMentions, false );
    }
}

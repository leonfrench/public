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

package ubic.pubmedgate.resolve.evaluation;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ubic.BAMSandAllen.JenaUtil;
import ubic.BAMSandAllen.Vocabulary;
import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.basecode.io.excel.CreateSpreadSheet;
import ubic.basecode.io.excel.ExcelUtil;
import ubic.pubmedgate.resolve.ResolutionRDFModel;
import ubic.pubmedgate.resolve.RDFResolvers.RDFResolver;
import ubic.pubmedgate.resolve.RDFResolvers.SimpleExactRDFMatcher;
import ubic.pubmedgate.resolve.mentionEditors.HemisphereStripMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.CytoPrefixMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.BracketRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.BroadmannPrefixAdderMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.DirectionRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.DirectionSplittingMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.NDotExpanderMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.NucleusOfTheRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.OfTheRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.RegionSuffixRemover;

import com.hp.hpl.jena.rdf.model.Resource;

public class MappingSpreadSheet extends CreateSpreadSheet {

    public MappingSpreadSheet( String filename ) throws Exception {
        super( filename, new MappingSchema() );
    }

    public void populate( ResolutionRDFModel model ) throws Exception {
        boolean removeExactMatches = true;
        boolean unMatched = false;
        boolean specToGen = false;
        populate( model, unMatched, removeExactMatches, specToGen );
    }

    public void populate( ResolutionRDFModel model, boolean unMatched, boolean removeExactMatches, boolean specToGen )
            throws Exception {

        List<Resource> mentions;
        if ( unMatched ) {
            mentions = new LinkedList<Resource>( model.getUnMatchedMentions() );
            Collections.shuffle( mentions );
        } else {
            mentions = new LinkedList<Resource>( model.getMentions() );
        }

        int exactMatchedConcepts = 0;
        int matchedConcepts = 0;

        int row = 0;
        int totalMentions = 0;
        int mentionsMatched = 0;
        int onlyExact = 0;

        for ( Resource mention : mentions ) {
            String label = JenaUtil.getLabel( mention );

            int freq = mention.getProperty( Vocabulary.number_of_occurances ).getInt();
            totalMentions += freq;
            // Set<Resource> PMIDs = model.getPMIDs( mention );
            String NCBILink = model.getNCBIPMIDLink( mention, 254 );

            // Map<String, Resource> targets = new HashMap<String, Resource>();

            // if we are processing unmatched mentions than skip the rest and just put out these three
            if ( unMatched ) {
                row++;
                ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Mention" ), label );
                ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Freq" ), freq );
                ExcelUtil.setFormula( spreadsheet, row, schema.getPosition( "Context" ), "HYPERLINK(\"" + NCBILink
                        + "\",\"PubMed Link\")" );
                continue;
            }

            Set<Resource> neuroTerms = model.getLinkedNeuroTerms( mention );

            // get terms that have a exact simple match
            Set<Resource> exactMatchTerms = new HashSet<Resource>();
            for ( Resource neuroTerm : neuroTerms ) {
                if ( model.containsExactConnection( mention, neuroTerm ) ) {
                    exactMatchTerms.add( neuroTerm );
                }
            }
            Set<Resource> neuroConcepts = model.getConceptsFromTerms( neuroTerms );
            matchedConcepts += neuroConcepts.size();

            if ( !neuroConcepts.isEmpty() ) mentionsMatched++;

            Set<Resource> neuroExactConcepts = model.getConceptsFromTerms( exactMatchTerms );
            exactMatchedConcepts += neuroExactConcepts.size();

            // are the concepts already matched?

            if ( neuroExactConcepts.equals( neuroConcepts ) && !neuroConcepts.isEmpty() ) onlyExact++;
            boolean wroteRow = false;

            // go through each term
            for ( Resource neuroTerm : neuroTerms ) {
                Set<String> targetConceptStrings = new HashSet<String>();
                Set<Resource> concepts = model.getConceptsFromTerms( neuroTerm );

                // remove all simple linked concepts
                if ( removeExactMatches ) concepts.removeAll( neuroExactConcepts );

                // remove all evaluated concepts - IMPORTANT
                concepts.removeAll( model.getMentionEvaluations( mention ) );

                if ( concepts.isEmpty() ) continue;
                log.info( label );
                // if it has terms left then print terms and concepts

                // get predicates
                Set<Resource> predicates = model.getConnectingPredicates( mention, neuroTerm );
                String predicatesLocalNames = model.getConnectingPredicatesShortNames( mention, neuroTerm ).toString();

                StringToStringSetMap labelToURI = new StringToStringSetMap();
                // go through each concept linked to the term
                for ( Resource neuroConcept : concepts ) {
                    // ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Mention" ), label );

                    // reminder - this is the label of the concept - not the term
                    String conceptLabel = JenaUtil.getLabel( neuroConcept ).toLowerCase();
                    targetConceptStrings.add( conceptLabel );
                    // store the concept URI's that got that label
                    labelToURI.put( conceptLabel, neuroConcept.getURI() );

                    // targets.add( neuroterm.toString() );
                }

                for ( String target : targetConceptStrings ) {
                    row++;
                    wroteRow = true;
                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Mention" ), label );
                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Freq" ), freq );
                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Term" ), JenaUtil.getLabel( neuroTerm ) );
                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Concept" ), target );

                    ExcelUtil.setFormula( spreadsheet, row, schema.getPosition( "Context" ), "HYPERLINK(\"" + NCBILink
                            + "\",\"PubMed Link\")" );
                    String URLString = labelToURI.get( target ).toString();
                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "URIs" ), URLString );
                    URLString = URLString.replaceAll( "http://hissa.nist.gov/jb/biordf-demo/", "" );
                    URLString = URLString.replaceAll( "http://www.purl.org/", "" );
                    URLString = URLString
                            .replaceAll( "http://purl.org/nbirn/birnlex/ontology/BIRNLex-Anatomy.owl#", "" );
                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "ShortNames" ), URLString );
                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Predicates" ), predicates.toString() );
                    ExcelUtil
                            .setValue( spreadsheet, row, schema.getPosition( "PredicatesShort" ), predicatesLocalNames );

                    if ( specToGen ) {
                        ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Specific to General" ), "X" );
                    }
                }

            }
            // if at least one wrote was written then make it a isolated block for that mention
            if ( wroteRow ) row++;

        }
        log.info( "Total mentions:" + totalMentions );
        log.info( "Unique mentions:" + mentions.size() );
        log.info( "Mentions matched at least once (mention to concept):" + mentionsMatched );
        log.info( "Total mention to concept links:" + matchedConcepts );
        log.info( "Total mention to concept links via exact match:" + exactMatchedConcepts );
        log.info( "Mentions matched using only exact match:" + onlyExact );
    }

    public static void createManualExactSheet( boolean specToGen ) throws Exception {
        String filename = "Exact.Auto.xls";
        if ( specToGen ) filename = "Exact.Auto.loss.xls";
        MappingSpreadSheet sheet = new MappingSpreadSheet( filename );
        ResolutionRDFModel resolutionModel = new ResolutionRDFModel();
        resolutionModel.loadManualMatches();

        if ( specToGen ) {
            resolutionModel.loadExactAutomaticEvaluations();
        }

        log.info( "Done loading evaluations" );
        resolutionModel.getStats();

        Set<Resource> terms = resolutionModel.getTerms();
        List<RDFResolver> resolvers = new LinkedList<RDFResolver>();
        resolvers.add( new SimpleExactRDFMatcher( terms ) );

        // split direction conjunctions
        resolutionModel.addMentionEditorToResolvers( resolvers, new DirectionSplittingMentionEditor() );
        resolutionModel.addMentionEditorToResolvers( resolvers, new HemisphereStripMentionEditor() );
        resolutionModel.addMentionEditorToResolvers( resolvers, new BracketRemoverMentionEditor() );
        resolutionModel.addMentionEditorToResolvers( resolvers, new NDotExpanderMentionEditor() );
        resolutionModel.addMentionEditorToResolvers( resolvers, new OfTheRemoverMentionEditor() );
        // removed
        // resolutionModel.addMentionEditorToResolvers( resolvers, new CytoPrefixMentionEditor() );
        // resolutionModel.addMentionEditorToResolvers( resolvers, new BroadmannPrefixAdderMentionEditor() );

        // / NEW!!
        resolutionModel.addMentionEditorToResolvers( resolvers, new RegionSuffixRemover() );

        resolutionModel.runResolvers( resolvers, resolutionModel.getMentions() );

        if ( specToGen ) {
            resolutionModel.addMentionEditorToResolvers( resolvers, new CytoPrefixMentionEditor() );
            resolutionModel.runResolvers( resolvers, resolutionModel.getUnMatchedMentions() );

            resolutionModel.addMentionEditorToResolvers( resolvers, new DirectionRemoverMentionEditor() );
            resolutionModel.runResolvers( resolvers, resolutionModel.getUnMatchedMentions() );

            resolutionModel.addMentionEditorToResolvers( resolvers, new NucleusOfTheRemoverMentionEditor() );
            resolutionModel.runResolvers( resolvers, resolutionModel.getUnMatchedMentions() );

            resolutionModel.addMentionEditorToResolvers( resolvers, new DirectionRemoverMentionEditor() );
            resolutionModel.runResolvers( resolvers, resolutionModel.getUnMatchedMentions() );
        }

        boolean unMatched = false;
        boolean removeExactMatches = false;

        sheet.populate( resolutionModel, unMatched, removeExactMatches, specToGen );
        sheet.save();
    }

    public static void main( String[] args ) throws Exception {
        // set to false, replace the exact.auto file, then set to true and replace the after loss file for automatic
        // evaluations
        boolean specToGen = true;
        createManualExactSheet( specToGen );
        // specToGen = true;
        // createManualExactSheet( specToGen );Normalization Evaluation.Exact.AllAssumedCorrect.newSetup.plusABA.xls
    }

}

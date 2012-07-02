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

import gate.Annotation;
import gate.Corpus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.JenaUtil;
import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.organism.SpeciesUtil;
import ubic.pubmedgate.resolve.EvaluationRDFModel;
import ubic.pubmedgate.resolve.MakeLexiconRDFModel;
import ubic.pubmedgate.resolve.RDFResolvers.BagOfStemsRDFMatcher;
import ubic.pubmedgate.resolve.RDFResolvers.RDFResolver;
import ubic.pubmedgate.resolve.mentionEditors.BracketRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.CytoPrefixMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.DirectionRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.DirectionSplittingMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.HemisphereStripMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.NucleusOfTheRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.OfTheRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.RegionSuffixRemover;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Example class to show how to access information from an abstract
 * 
 * @author leon
 */
public class PrintAndResolveBrainRegions {
    protected static Log log = LogFactory.getLog( PrintAndResolveBrainRegions.class );
    EvaluationRDFModel evaluationModel;
    RDFResolver resolver;
    Set<Resource> allTerms;
    Set<Resource> allConcepts;

    public PrintAndResolveBrainRegions() throws Exception {
        setupResolver();
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        PrintAndResolveBrainRegions resolver = new PrintAndResolveBrainRegions();
        GateInterface p2g = new GateInterface();
        Corpus corp = p2g.getUnseenCorp();
        List<ConnectionsDocument> docs = p2g.getDocuments( corp );
        StringToStringSetMap speciesStrings = SpeciesUtil.getSpeciesStrings( p2g, corp ).strings;

        for ( ConnectionsDocument doc : docs ) {
            String pmid = doc.getPMID();
            String title = doc.getAnnotationText( doc.getTitle() );

            log.info( "PMID:" + pmid + " Title:" + title );
            Set<String> species = doc.getLinnaeusSpecies();
            for ( String specie : species ) {
                log.info( "   Species:" + speciesStrings.get( specie ).toString() + " [" + specie + "]" );
            }

            List<Annotation> regionAnnotations = doc.getAnnotationsByType( "Mallet", "BrainRegion" );
            Set<String> regionStrings = new HashSet<String>();
            for ( Annotation regionAnnotation : regionAnnotations ) {
                regionStrings.add( doc.getAnnotationText( regionAnnotation ) );
            }

            for ( String regionString : regionStrings ) {
                log.info( "   Region string:" + regionString );

                Set<Resource> concepts = resolver.resolveToFilteredConcepts( regionString );
                for ( Resource concept : concepts ) {
                    log.info( "       Mapped concept:" + JenaUtil.getLabel( concept ) + " (" + concept.getURI() + ")" );
                }
            }

        }

    }

    public void setupResolver() throws Exception {
        MakeLexiconRDFModel lexiconModel = new MakeLexiconRDFModel();
        // this loads in all the lexicons, you may want to comment out some of these
        lexiconModel.addNN2007Nodes();
        lexiconModel.addNN2010Nodes();
        lexiconModel.addNIFSTDNodes();
        lexiconModel.addABANodes();
        lexiconModel.addBredeNodes();
        lexiconModel.addBAMSNodes();

        // add evaluations
        boolean reason = true;

        evaluationModel = new EvaluationRDFModel( lexiconModel.getModel(), reason );
        evaluationModel.loadManualMatches();
        boolean createMentions = true;
        evaluationModel.loadManualEvaluations( createMentions );
        evaluationModel.getStats();

        allTerms = evaluationModel.getTerms();
        allConcepts = evaluationModel.getConcepts(); // for speed

        resolver = new BagOfStemsRDFMatcher( allTerms );
        resolver.addMentionEditor( new DirectionSplittingMentionEditor() );
        resolver.addMentionEditor( new HemisphereStripMentionEditor() );
        resolver.addMentionEditor( new BracketRemoverMentionEditor() );
        resolver.addMentionEditor( new OfTheRemoverMentionEditor() );
        resolver.addMentionEditor( new CytoPrefixMentionEditor() );
        resolver.addMentionEditor( new RegionSuffixRemover() );
        resolver.addMentionEditor( new DirectionRemoverMentionEditor() );
        resolver.addMentionEditor( new NucleusOfTheRemoverMentionEditor() );
        resolver.addMentionEditor( new DirectionRemoverMentionEditor() );

    }

    public Set<Resource> resolveToFilteredConcepts( String mentionString ) {
        Set<Resource> result = new HashSet<Resource>();
        Set<Resource> neuroConcepts = evaluationModel
                .resolveToConcepts( mentionString, resolver, allTerms, allConcepts );
        for ( Resource neuroConcept : neuroConcepts ) {
            if ( !evaluationModel.rejected( mentionString, neuroConcept ) ) {
                result.add( neuroConcept );
            } else {
                log.info( "Rejected:" + mentionString + " -> " + JenaUtil.getLabel( neuroConcept ) );
            }
        }
        return result;
    }
}

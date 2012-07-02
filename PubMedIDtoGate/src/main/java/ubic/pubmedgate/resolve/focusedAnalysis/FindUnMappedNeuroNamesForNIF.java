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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.Resource;

import ubic.BAMSandAllen.JenaUtil;
import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.basecode.io.excel.CreateSpreadSheet;
import ubic.basecode.io.excel.ExcelUtil;
import ubic.basecode.io.excel.SpreadSheetSchema;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.resolve.MakeLexiconRDFModel;
import ubic.pubmedgate.resolve.ResolutionRDFModel;
import ubic.pubmedgate.resolve.RDFResolvers.BagOfStemsRDFMatcher;
import ubic.pubmedgate.resolve.RDFResolvers.RDFResolver;
import ubic.pubmedgate.resolve.mentionEditors.BracketRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.CytoPrefixMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.DirectionSplittingMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.HemisphereStripMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.OfTheRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.RegionSuffixRemover;

public class FindUnMappedNeuroNamesForNIF extends CreateSpreadSheet {
    protected static Log log = LogFactory.getLog( FindUnMappedNeuroNamesForNIF.class );

    public FindUnMappedNeuroNamesForNIF( String outFilename, SpreadSheetSchema schema ) throws Exception {
        super( outFilename, schema );
    }

    public void populate() throws Exception {
        MakeLexiconRDFModel lexiconModel = new MakeLexiconRDFModel();
        lexiconModel.addNIFSTDNodes();

        boolean reason = true;
        ResolutionRDFModel resolutionModel = new ResolutionRDFModel( lexiconModel.getModel(), reason );

        resolutionModel.loadManualMatches();
        resolutionModel.loadManualEvaluations();

        ResolutionRDFModel resolutionForNeuroNames = new ResolutionRDFModel();
        resolutionForNeuroNames.getStats();

        Set<Resource> allTerms = resolutionModel.getTerms(); // for speed
        Set<Resource> allConcepts = resolutionModel.getConcepts(); // for speed

        RDFResolver resolver;
        resolver = new BagOfStemsRDFMatcher( resolutionModel.getTerms() );
        resolver.addMentionEditor( new DirectionSplittingMentionEditor() );
        resolver.addMentionEditor( new HemisphereStripMentionEditor() );
        resolver.addMentionEditor( new BracketRemoverMentionEditor() );
        resolver.addMentionEditor( new OfTheRemoverMentionEditor() );
        resolver.addMentionEditor( new CytoPrefixMentionEditor() );
        resolver.addMentionEditor( new RegionSuffixRemover() );

        int rejectedMatches = 0;
        int resolved = 0;
        int row = 1;
        Set<Resource> allNNConcepts = resolutionForNeuroNames.getNNConcepts(); // for speed
        for ( Resource NNConcept : allNNConcepts ) {
            String NNConceptLabel = JenaUtil.getLabel( NNConcept );
            if ( row % 100 == 0 ) log.info( "Row:" + row );
            Set<Resource> NNTerms = resolutionForNeuroNames.getTermsFromConcepts( NNConcept );
            for ( Resource NNTerm : NNTerms ) {
                String NNTermString = JenaUtil.getLabel( NNTerm );
                Set<Resource> regionResolves = resolutionModel.resolveToTerms( NNTermString, resolver, allTerms );

                StringToStringSetMap URItoTerms = new StringToStringSetMap();
                Set<Resource> allResolvedConcepts = new HashSet<Resource>();

                for ( Resource resolvedTerm : regionResolves ) {
                    Set<Resource> concepts = resolutionModel.getConceptsFromTerms( resolvedTerm, allConcepts );
                    for ( Resource regionConcept : concepts ) {
                        if ( resolutionModel.rejected( resolvedTerm, regionConcept ) ) {
                            log.info( "Rejected match" );
                            rejectedMatches++;
                            continue;
                        }

                        allResolvedConcepts.add( regionConcept );
                        URItoTerms.put( regionConcept.getURI(), JenaUtil.getLabel( resolvedTerm ) );
                    }
                }
                // can match to more than one NIFSTD concept, present each on new row?
                if ( allResolvedConcepts.isEmpty() ) {
                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "NeuronameTerm" ), NNTermString );
                    ExcelUtil
                            .setValue( spreadsheet, row, schema.getPosition( "NeuronameConceptLabel" ), NNConceptLabel );
                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "NeuronameConceptURI" ), NNConcept
                            .getURI() );
                    row++;
                } else {
                    resolved++;
                    // for ( Resource resolvedTerm : regionResolves ) {
                    // Set<Resource> concepts = resolutionModel.getConceptsFromTerms( resolvedTerm, NIFConcepts );
                    for ( Resource regionConcept : allResolvedConcepts ) {
                        ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "NeuronameTerm" ), NNTermString );
                        ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "NeuronameConceptLabel" ),
                                NNConceptLabel );
                        ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "NeuronameConceptURI" ), NNConcept
                                .getURI() );

                        // ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "database" ), source );
                        // ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "con_from_abb" ), line[1] );
                        // ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "con_from" ), line[2] );
                        // ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "RegionName" ), regionName );

                        Set<String> matchedTerms = URItoTerms.get( regionConcept.getURI() );
                        String matchedTermsString = matchedTerms.toString();
                        matchedTermsString = matchedTermsString.substring( 1, matchedTermsString.length() - 1 );

                        String resolvedRegionLabel = JenaUtil.getLabel( regionConcept );

                        ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "MatchedTerm" ), matchedTermsString );
                        ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "NIFSTDConceptLabel" ),
                                resolvedRegionLabel );
                        ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "ExactMatch" ), matchedTermsString );
                        ExcelUtil
                                .setValue( spreadsheet, row, schema.getPosition( "NIFSTDURI" ), regionConcept.getURI() );

                        boolean exactMatch = false;
                        for ( String termString : matchedTerms ) {
                            if ( NNTermString.equalsIgnoreCase( termString ) ) {
                                exactMatch = true;
                                break;
                            }
                        }

                        ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "ExactMatch" ), exactMatch + "" );

                        row++;
                    }

                }

            }
        }
        log.info( "Rejected matches:" + rejectedMatches );
        log.info( "Resolved:" + resolved );

    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        // TODO Auto-generated method stub
        String filename = Config.config.getString( "whitetext.resolve.results.folder" )
                + "NeuronamesMappedToNIFSTD.xls";
        FindUnMappedNeuroNamesForNIF forNIF = new FindUnMappedNeuroNamesForNIF( filename,
                new FindUnMappedNeuroNamesForNIFSchema() );
        forNIF.populate();
        forNIF.save();
        log.info( filename );
    }

}

class FindUnMappedNeuroNamesForNIFSchema extends SpreadSheetSchema {

    public FindUnMappedNeuroNamesForNIFSchema() {
        super();
        positions.put( "NeuronameTerm", 0 );
        positions.put( "NeuronameConceptLabel", 1 );
        positions.put( "MatchedTerm", 2 );
        positions.put( "NIFSTDConceptLabel", 3 );
        positions.put( "ExactMatch", 4 );
        positions.put( "NeuronameConceptURI", 5 );
        positions.put( "NIFSTDURI", 6 );
    }
}

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

import java.io.FileReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.JenaUtil;
import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.basecode.io.excel.CreateSpreadSheet;
import ubic.basecode.io.excel.ExcelUtil;
import ubic.basecode.io.excel.SpreadSheetSchema;
import ubic.pubmedgate.resolve.MakeLexiconRDFModel;
import ubic.pubmedgate.resolve.ResolutionRDFModel;
import ubic.pubmedgate.resolve.RDFResolvers.RDFResolver;
import ubic.pubmedgate.resolve.RDFResolvers.SimpleMappingRDFMatcher;
import ubic.pubmedgate.resolve.mentionEditors.BracketRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.CytoPrefixMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.DirectionRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.DirectionSplittingMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.HemisphereStripMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.NucleusOfTheRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.OfTheRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.RegionSuffixRemover;
import au.com.bytecode.opencsv.CSVReader;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Loads in connectivty.txt and maps the full region names to NIF ID's
 * 
 * @author Leon
 */
public class MapNIFConnectionRegions extends CreateSpreadSheet {
    protected static Log log = LogFactory.getLog( MapNIFConnectionRegions.class );

    String inFilename;
    Set<String> regionNames;
    boolean useNN; // use neuronames instead of NIFSTD

    public MapNIFConnectionRegions( String outFilename, SpreadSheetSchema schema, String inFilename, boolean useNN )
            throws Exception {
        super( outFilename, schema );
        this.inFilename = inFilename;
        regionNames = new HashSet<String>();
        this.useNN = useNN;

    }

    // TODO: Use evaluation filter !
    public void populate() throws Exception {
        MakeLexiconRDFModel lexiconModel = new MakeLexiconRDFModel();
        if ( useNN ) {
            lexiconModel.addNN2007Nodes();
            lexiconModel.addNN2010Nodes();
        } else {
            lexiconModel.addNIFSTDNodes();
        }

        // add evaluations
        boolean reason = true;
        ResolutionRDFModel resolutionModel = new ResolutionRDFModel( lexiconModel.getModel(), reason );

        resolutionModel.loadManualMatches();
        resolutionModel.loadManualEvaluations();

        Set<Resource> NIFConcepts;
        if ( useNN ) {
            NIFConcepts = resolutionModel.getNNConcepts();
        } else {
            NIFConcepts = resolutionModel.getNIFSTDConcepts();
        }

        Set<Resource> allTerms = resolutionModel.getTerms(); // for speed

        RDFResolver resolver;
        // resolver = new BagOfStemsRDFMatcher( resolutionModel.getTerms() );
        resolver = new SimpleMappingRDFMatcher( resolutionModel.getTerms() );
        resolver.addMentionEditor( new DirectionSplittingMentionEditor() );
        resolver.addMentionEditor( new HemisphereStripMentionEditor() );
        resolver.addMentionEditor( new BracketRemoverMentionEditor() );
        resolver.addMentionEditor( new OfTheRemoverMentionEditor() );
        resolver.addMentionEditor( new CytoPrefixMentionEditor() );
        resolver.addMentionEditor( new RegionSuffixRemover() );
//        resolver.addMentionEditor( new DirectionRemoverMentionEditor() );
//        resolver.addMentionEditor( new NucleusOfTheRemoverMentionEditor() );
//        resolver.addMentionEditor( new DirectionRemoverMentionEditor() );

        int resolved = 0;
        // start a spreadsheet

        int row = 1;
        int count = 0;
        CSVReader reader = new CSVReader( new FileReader( inFilename ), '\t' );

        List<String[]> lines = reader.readAll();

        // first line is header
        lines.remove( 0 );
        StopWatch watch = new StopWatch();
        watch.start();
        int rejectedMatches = 0;
        int skipped = 0;
        for ( String[] line : lines ) {
            boolean badregioname = false;
            String source = line[0];
            String regionName = line[2];
            if ( regionName.equals( "(null)" ) || regionName.equals( "" ) ) {
                log.info( "Skipping on null or empty name" );
                badregioname = true;
            }
            if ( source.equals( "CoCoMac" ) ) {
                // log.info( regionName );
                if ( regionName.contains( ":" ) ) {
                    regionName = regionName.substring( regionName.indexOf( ':' ) + 2 );
                } else {
                    log.info( "missing colon" );
                    badregioname = true;
                }
                // log.info( regionName );
            }

            Set<Resource> regionResolves;
            if ( badregioname ) {
                regionResolves = new HashSet<Resource>();
                regionName = "";
            } else {
                regionResolves = resolutionModel.resolveToTerms( regionName, resolver, allTerms );
            }

            long t = lines.size() / ( 1 + count ) * watch.getTime() / 1000 / 60;
            log.info( regionName + " " + count++ + " of " + lines.size() + " Time:" + watch.getTime() + " Remaining:"
                    + t + " mins" );

            // if ( count > 50 ) break;

            StringToStringSetMap URItoTerms = new StringToStringSetMap();
            Set<Resource> allResolvedConcepts = new HashSet<Resource>();
            // deal with more than one way to resolve it, condense the synonyms to concept mappings
            for ( Resource resolvedTerm : regionResolves ) {
                Set<Resource> concepts = resolutionModel.getConceptsFromTerms( resolvedTerm, NIFConcepts );
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

            if ( allResolvedConcepts.isEmpty() ) {
                ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "database" ), source );
                ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "con_from_abb" ), line[1] );
                ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "con_from" ), line[2] );
                ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "RegionName" ), regionName );
                row++;
            } else {
                resolved++;
                // for ( Resource resolvedTerm : regionResolves ) {
                // Set<Resource> concepts = resolutionModel.getConceptsFromTerms( resolvedTerm, NIFConcepts );
                for ( Resource regionConcept : allResolvedConcepts ) {
                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "database" ), source );
                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "con_from_abb" ), line[1] );
                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "con_from" ), line[2] );
                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "RegionName" ), regionName );

                    Set<String> matchedTerms = URItoTerms.get( regionConcept.getURI() );
                    String matchedTermsString = matchedTerms.toString();
                    matchedTermsString = matchedTermsString.substring( 1, matchedTermsString.length() - 1 );
                    // comas are bad idea

                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "MatchedTerm" ), matchedTermsString );
                    String regionConceptLabel = JenaUtil.getLabel( regionConcept );
                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "NIFSTDLabel" ), regionConceptLabel );

                    boolean exactMatch = false;
                    for ( String termString : matchedTerms ) {
                        if ( regionName.equalsIgnoreCase( termString ) ) {
                            exactMatch = true;
                            break;
                        }
                    }

                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "ExactMatch" ), exactMatch + "" );
                    ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "NIFSTDURI" ), regionConcept.getURI() );
                    row++;
                }
            }
        }

        log.info( "Rejected matches:" + rejectedMatches );
    }

    public static void main( String[] args ) throws Exception {
        boolean useNN = true;
        String filename = "/grp/java/workspace/PubMedIDtoGate/spreadsheets/ConnectionRegionsFromNIF/connectivity.txt";
        MapNIFConnectionRegions mapper = new MapNIFConnectionRegions( filename + ".NeuroNames.full.xls",
                new NIFMappingSchema(), filename, useNN );
        mapper.populate();
        mapper.save();
    }
}

class NIFMappingSchema extends SpreadSheetSchema {

    public NIFMappingSchema() {
        super();
        positions.put( "database", 0 );
        positions.put( "con_from_abb", 1 );
        positions.put( "con_from", 2 );
        positions.put( "RegionName", 3 );
        positions.put( "MatchedTerm", 4 );
        positions.put( "NIFSTDLabel", 5 );
        positions.put( "ExactMatch", 6 );
        positions.put( "NIFSTDURI", 7 );
    }
}

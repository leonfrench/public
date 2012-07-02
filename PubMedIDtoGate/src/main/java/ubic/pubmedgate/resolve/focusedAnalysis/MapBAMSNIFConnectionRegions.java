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

import java.awt.Toolkit;
import java.io.FileReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.ABAMSDataMatrix;
import ubic.BAMSandAllen.JenaUtil;
import ubic.BAMSandAllen.SetupParameters;
import ubic.BAMSandAllen.StructureCatalogAnalyze;
import ubic.BAMSandAllen.Util;
import ubic.BAMSandAllen.AnalyzeBAMSandAllenGenes.Direction;
import ubic.BAMSandAllen.ClassSelectors.BrainRegionClassSelector;
import ubic.BAMSandAllen.adjacency.CorrelationAdjacency;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.excel.CreateSpreadSheet;
import ubic.basecode.io.excel.ExcelUtil;
import ubic.basecode.io.excel.SpreadSheetSchema;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.resolve.ResolutionRDFModel;
import ubic.pubmedgate.resolve.RDFResolvers.BagOfStemsRDFMatcher;
import ubic.pubmedgate.resolve.RDFResolvers.RDFResolver;
import ubic.pubmedgate.resolve.mentionEditors.BracketRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.CytoPrefixMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.DirectionSplittingMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.HemisphereStripMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.OfTheRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.RegionSuffixRemover;
import au.com.bytecode.opencsv.CSVReader;

import com.hp.hpl.jena.rdf.model.Resource;

public class MapBAMSNIFConnectionRegions extends CreateSpreadSheet {
    protected static Log log = LogFactory.getLog( MapBAMSNIFConnectionRegions.class );

    String filename;
    Set<String> BAMSNIFConNames;
    Set<String> additionalBAMSNames;

    public MapBAMSNIFConnectionRegions( String outFilename, SpreadSheetSchema schema, String filename ) throws Exception {
        super( outFilename, schema );
        this.filename = filename;
        BAMSNIFConNames = new HashSet<String>();
        CSVReader reader = new CSVReader( new FileReader( filename ), '\t' );
        List<String[]> lines = reader.readAll();
        for ( String[] line : lines ) {
            if ( line[0].equals( "BAMS" ) ) {
                log.info( line[2] );
                BAMSNIFConNames.add( line[2] );
            }
        }
        log.info( "Number of BAMS names:" + BAMSNIFConNames.size() );

        additionalBAMSNames = getUsedBAMSRegions();

        log.info( "intersection:" + Util.intersectSize( BAMSNIFConNames, getUsedBAMSRegions() ) );
    }

    public Set<String> getUsedBAMSRegions() throws Exception {
        Direction direction = Direction.ANYDIRECTION;
        StructureCatalogAnalyze forMatrix = new StructureCatalogAnalyze( new BrainRegionClassSelector() );
        forMatrix.readModel( SetupParameters.getDataFolder() + "Propigated.rdf" );
        DoubleMatrix<String, String> dataMatrix = forMatrix.makeConnectionMatrix( direction );
        ABAMSDataMatrix matrix = new ABAMSDataMatrix( dataMatrix, "Connectivity", new CorrelationAdjacency( dataMatrix ) );
        matrix = matrix.removeZeroColumns();
        matrix = matrix.removeZeroRows();
        log.info( matrix.getDimensionString() );
        return new HashSet<String>( matrix.getRowNames() );
    }

    public void populate() throws Exception {
        String filename;
        filename = Config.config.getString( "resolve.Lexicon.RDF" );
        // filename = "/home/lfrench/WhiteText/rdf/LexiconRDF.allComp (only Mallet predictions on unseen corp).rdf";
        ResolutionRDFModel resolutionModel = new ResolutionRDFModel( filename );

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
        // resolver.addMentionEditor( new DirectionRemoverMentionEditor() );
        // resolver.addMentionEditor( new NucleusOfTheRemoverMentionEditor() );
        // resolver.addMentionEditor( new DirectionRemoverMentionEditor() );

        int resolved = 0;
        Set<Resource> BAMSConcepts = resolutionModel.getBAMSConcepts();
        Set<String> allBAMSNames = ( Set<String> ) Util.union( BAMSNIFConNames, additionalBAMSNames );
        // start a spreadsheet

        int row = 1;
        int count = 0;
        for ( String BAMSName : allBAMSNames ) {
            StringTokenizer BAMSTokens = new StringTokenizer( BAMSName );
            int bamsTokenSize = BAMSTokens.countTokens();
            // get resolved concepts
            Set<Resource> regionResolves = resolutionModel
                    .resolveToConcepts( BAMSName, resolver, allTerms, allConcepts );
            // maybe remove BAMS before?
            // remove BAMS concepts
            regionResolves.removeAll( BAMSConcepts );

            // if the name is in NIF or not

            // focus on synonyms for now

            boolean inNIF = BAMSNIFConNames.contains( BAMSName );

            log.info( BAMSName + " " + count++ + " of " + allBAMSNames.size() );

            // if ( count > 20 ) return;

            if ( regionResolves.isEmpty() ) {
                ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "BAMSName" ), BAMSName );
                ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "InNIF" ), "" + inNIF );
                ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Resolves" ), "" + false );

                row++;
            } else {
                resolved++;
                for ( Resource resolve : regionResolves ) {
                    String conceptString = JenaUtil.getLabel( resolve );
                    if ( conceptString == null ) {
                        log.info( "Null label:" + resolve.getURI() );
                        continue;
                    }

                    // get all terms from concept
                    Set<Resource> terms = resolutionModel.getTermsFromConcepts( resolve );
                    for ( Resource term : terms ) {
                        String termString = JenaUtil.getLabel( term );
                        if ( termString.equalsIgnoreCase( BAMSName ) ) continue;
                        StringTokenizer conceptTokens = new StringTokenizer( conceptString );
                        int conceptTokenSize = conceptTokens.countTokens();

                        // if it's way longer than the matched term
                        if ( bamsTokenSize > 3 && bamsTokenSize > ( 1.6 * conceptTokenSize ) ) {
                            log.info( "Skipping:" + conceptString );
                            continue;
                        }

                        ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "BAMSName" ), BAMSName );
                        ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "InNIF" ), "" + inNIF );
                        ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Resolves" ), "" + true );
                        ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Concept" ), conceptString );
                        ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "ConceptURI" ), resolve.getURI() );
                        ExcelUtil.setValue( spreadsheet, row, schema.getPosition( "Term" ), termString );

                        row++;
                    }
                }
            }

        }
        log.info( "Resolved:" + resolved + " of " + allBAMSNames.size() );

    }

    public static void main( String[] args ) throws Exception {
        Toolkit.getDefaultToolkit().beep();
        String filename = "/grp/java/workspace/PubMedIDtoGate/spreadsheets/ConnectionRegionsFromNIF/connectivity.txt";
        MapBAMSNIFConnectionRegions mapper = new MapBAMSNIFConnectionRegions( filename + ".xls", new SynonymSchema(), filename );
        mapper.populate();
        mapper.save();
        Toolkit.getDefaultToolkit().beep();

    }

}

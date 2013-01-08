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

package ubic.pubmedgate.interactions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.SetupParameters;
import ubic.BAMSandAllen.StructureCatalogAnalyze;
import ubic.BAMSandAllen.Util;
import ubic.BAMSandAllen.AnalyzeBAMSandAllenGenes.Direction;
import ubic.BAMSandAllen.BAMSDataLoaders.BAMSDataLoader;
import ubic.BAMSandAllen.ClassSelectors.BrainRegionClassSelector;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.params.ParamKeeper;
import ubic.basecode.math.ROC;
import ubic.basecode.util.FileTools;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.interactions.evaluation.NormalizedConnection;
import ubic.pubmedgate.resolve.EvaluationRDFModel;
import ubic.pubmedgate.resolve.MakeLexiconRDFModel;
import ubic.pubmedgate.resolve.ResolutionRDFModel;
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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class NormalizePairs {
    protected static Log log = LogFactory.getLog( NormalizePairs.class );
    public static final String RAT = "species:ncbi:10116";
    ResolutionRDFModel resolveModel;
    RDFResolver resolver;

    public NormalizePairs() throws Exception {
        // setup resolver
        MakeLexiconRDFModel lexiconModel = new MakeLexiconRDFModel();
        lexiconModel.addBAMSNodes();
        lexiconModel.extendBAMSNodes();
        // lexiconModel.addNN2010Nodes();

        // add evaluations
        boolean reason = true;
        resolveModel = new ResolutionRDFModel( lexiconModel.getModel(), reason );
        resolveModel.getStats();

        resolver = new BagOfStemsRDFMatcher( resolveModel.getTerms() );
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

    /**
     * Given a list of pairs and an XML reader to resolve them, create a connection matrix.
     * 
     * @param reader
     * @param pairs
     */
    public NormalizeResult normalizePairsToMatrix( AirolaXMLReader reader, List<String> pairs, SLOutputReader SLReader,
            boolean writeOut, String name ) throws Exception {
        // dump out pairs to HTML
        ShowSLErrors.writeExamples( reader, SLReader, pairs,
                Config.config.getString( "whitetext.iteractions.results.folder" ) + name + ".html" );

        // sort pairs by score (for AUC)
        pairs = SLReader.sortPairUsingScore( new LinkedList<String>( pairs ) );
        Collections.reverse( pairs );

        Map<String, String> results = new HashMap<String, String>();
        results.put( "Input pair list size", pairs.size() + "" );
        results.put( "annotationSet", reader.getAnnotatorNameSet() );

        Direction direction = Direction.ANYDIRECTION;
        boolean propigated = true;
        DoubleMatrix<String, String> dataMatrix = getBAMSConnectionMatrix( propigated, direction );

        BAMSDataLoader bamsLoader = new BAMSDataLoader();

        List<String> connectionRegionNames = dataMatrix.getRowNames();
        DoubleMatrix<String, String> predictedMatrix = new DenseDoubleMatrix<String, String>(
                connectionRegionNames.size(), connectionRegionNames.size() );
        predictedMatrix.setRowNames( connectionRegionNames );
        predictedMatrix.setColumnNames( connectionRegionNames );

        Set<Resource> allTerms = resolveModel.getTerms(); // for speed
        Set<Resource> allConcepts = resolveModel.getConcepts(); // for speed

        int totalResolves = 0;
        int inMatrix = 0;
        int oneToManyMappings = 0;
        Set<String> uniqueConnections = new HashSet<String>();
        Set<String> uniqueRegions = new HashSet<String>();
        List<String> resovledPairs = new LinkedList<String>();

        Set<NormalizedConnection> normalizedPairs = new HashSet<NormalizedConnection>();
        int selfConnections = 0;
        int notInRowNames = 0;
        int pairsNotResolved = 0;
        int bothMatched = 0;
        int count = 0;
        List<Double> positiveRanks = new LinkedList<Double>();
        int totalDepth = 0;
        int pairsWithOneMatch = 0;

        for ( String pair : pairs ) {
            count++;
            boolean atLeast1Match = false;
            boolean atLeast1Normalization = false;

            // for testing
            // if ( count > 300 ) {
            // break;
            // }

            StopWatch s = new StopWatch();
            s.start();
            String regionA = reader.getPartnerAText( pair );
            String regionB = reader.getPartnerBText( pair );
            Set<String> regionAresolves = resolveModel.resolve( regionA, resolver, allTerms, allConcepts );

            log.info( "Testing:" + regionA + " -> " + regionB + " " + count + " of " + pairs.size() );

            // for speed only go forward if A resolved
            if ( !regionAresolves.isEmpty() ) {
                Set<String> regionBresolves = resolveModel.resolve( regionB, resolver, allTerms, allConcepts );

                if ( !regionBresolves.isEmpty() ) {
                    log.info( "Resolved:" );
                    log.info( "   " + regionA + " -> " + regionAresolves.toString() );
                    log.info( "   " + regionB + " -> " + regionBresolves.toString() );
                    totalResolves++;
                    atLeast1Normalization = true;
                    if ( regionAresolves.size() > 1 ) oneToManyMappings++;
                    if ( regionBresolves.size() > 1 ) oneToManyMappings++;

                    if ( regionAresolves.equals( regionBresolves ) ) {
                        selfConnections++;
                        uniqueRegions.addAll( regionAresolves );
                        resovledPairs.add( pair );
                    } else {
                        for ( String resolvedA : regionAresolves ) {
                            for ( String resolvedB : regionBresolves ) {
                                resovledPairs.add( pair );
                                uniqueRegions.add( resolvedA );
                                uniqueRegions.add( resolvedB );

                                totalDepth += bamsLoader.getParents( resolvedA ).size();
                                totalDepth += bamsLoader.getParents( resolvedB ).size();

                                if ( dataMatrix.getRowNames().contains( resolvedA )
                                        && dataMatrix.getRowNames().contains( resolvedB ) ) {
                                    if ( resolvedA.equals( resolvedB ) ) { // also done at the set level
                                        selfConnections++;
                                    } else {
                                        uniqueConnections.add( resolvedA + resolvedB );
                                        uniqueConnections.add( resolvedB + resolvedA );

                                        // a pair can match to more than one connection!! FIX - list
                                        NormalizedConnection c = new NormalizedConnection();
                                        c.regionA = resolvedA;
                                        c.regionB = resolvedB;
                                        c.pairID = pair;
                                        normalizedPairs.add( c );

                                        // put in connection matrix
                                        double currentValue = predictedMatrix.getByKeys( resolvedA, resolvedB );
                                        currentValue += 1;
                                        predictedMatrix.setByKeys( resolvedA, resolvedB, currentValue );
                                        predictedMatrix.setByKeys( resolvedB, resolvedA, currentValue );
                                        if ( dataMatrix.getByKeys( resolvedA, resolvedB ) == 1d ) {
                                            atLeast1Match = true;
                                            positiveRanks.add( ( double ) ( resovledPairs.size() ) );
                                            inMatrix++;
                                        }
                                    }
                                } else {
                                    notInRowNames++;
                                    log.info( "Not in matrix but resolved" );
                                }
                            }
                        }
                    }
                }
            } // end if on region A resolve
            if ( atLeast1Normalization ) {
                bothMatched++;
            } else {
                pairsNotResolved++;
            }
            if ( atLeast1Match ) {
                pairsWithOneMatch++;
            }
        }

        results.put( "PairsWithOneNormalize", bothMatched + "" );
        results.put( "PairsWithOneNormalize2", totalResolves + "" );
        results.put( "PairsWithOneMatch", pairsWithOneMatch + "" );
        results.put( "Unresolved pairs", pairsNotResolved + "" );
        results.put( "RP connected", "" + inMatrix );
        results.put( "RP Self connections", "" + selfConnections );
        results.put( "Not in BAMS", "" + notInRowNames );
        results.put( "RP Unique normalized pairs (not counting self connects)", "" + ( uniqueConnections.size() / 2 ) );
        results.put( "RP AUC", ROC.aroc( resovledPairs.size(), positiveRanks ) + "" );
        results.put( "RP Resolved pairings", resovledPairs.size() + "" );
        results.put( "RP Average pair depth", ( totalDepth / ( double ) resovledPairs.size() ) + "" );
        results.put( "Unique regions", uniqueRegions.size() + "" );
        results.put( "One to many mapping rate", "" + ( ( double ) oneToManyMappings / ( 2 * totalResolves ) ) );
        results.put( "Name", name );

        log.info( "Pairs:" + pairs.size() );
        log.info( "Total resolves:" + totalResolves + " of " + pairs.size() );
        log.info( "connected in BAMS Matrix:" + inMatrix );
        log.info( "Self connections:" + selfConnections );
        log.info( "Not in BAMS ROWS:" + notInRowNames );
        log.info( "Unresolved pairs:" + pairsNotResolved );

        log.info( "Unique normalized pairs (not counting self connects):" + ( uniqueConnections.size() / 2 ) );

        if ( writeOut ) FileTools.stringsToFile( resovledPairs, reader.getNormalizedPairsFilename() );

        // write out matrix, where??
        String matrixFileName = ( Config.config.getString( "whitetext.iteractions.results.folder" ) + name + ".matrix" );
        Util.writeRTable( matrixFileName + ".txt", predictedMatrix );
        Util.writeImage( matrixFileName + ".png", predictedMatrix );

        NormalizeResult normResult = new NormalizeResult();
        normResult.normalizedPairs = normalizedPairs;
        normResult.statisticMap = results;
        normResult.name = name;

        return normResult;
    }

    public static DoubleMatrix<String, String> getBAMSConnectionMatrix( boolean propigated, Direction direction )
            throws IOException {
        StructureCatalogAnalyze forMatrix = new StructureCatalogAnalyze( new BrainRegionClassSelector() );
        String filename = "Propigated.rdf";
        if ( !propigated ) filename = "Non" + filename;

        forMatrix.readModel( SetupParameters.getDataFolder() + filename );
        DoubleMatrix<String, String> dataMatrix = forMatrix.makeConnectionMatrix( direction );
        return dataMatrix;
    }

    public static NormalizeResult analyseTest( boolean usePredictions, boolean eraTest, boolean runAll, String species )
            throws Exception {
        String testSet = "Annotated";
        String annotationSet = "Suzanne";
        NormalizeResult result = null;

        String baseFolder = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Saved Results/SL/CV/WhiteTextNegFixFull/predict/WhiteTextNegFixFull";
        String filename = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Corpora/Original-Modified/WhiteTextNegFixFull.xml";

        GateInterface p2g = new GateInterface();
        p2g.setUnSeenCorpNull();

        AirolaXMLReader XMLReader = new AirolaXMLReader( filename, p2g, annotationSet );
        SLOutputReader SLReader = new SLOutputReader( new File( baseFolder ) );

        boolean doAll = false;
        result = runNormalization( testSet, XMLReader, SLReader, doAll, usePredictions, eraTest, species );

        if ( runAll ) {
            result = runNormalization( testSet, XMLReader, SLReader, doAll, usePredictions, eraTest, species );
        }
        return result;
    }

    // public static List<String> getspeciesPairs( AirolaXMLReader XMLReader, Collection<String> allPairs ) {
    // return getSpeciesPairs( XMLReader, allPairs, "species:ncbi:10116" );
    // }

    public static List<String> getSpeciesPairs( AirolaXMLReader XMLReader, Collection<String> allPairs,
            String speciesNCBIID ) {
        StopWatch s = new StopWatch();
        s.start();
        List<String> speciesSpecificPairs = new LinkedList<String>();
        int count = 0;
        for ( String pair : allPairs ) {
            if ( count++ % 1000 == 0 ) log.info( "Getting species pairs:" + count );
            ConnectionsDocument doc = XMLReader.getDocumentFromPairID( pair );
            Set<String> species = doc.getLinnaeusSpecies();
            if ( species.contains( speciesNCBIID ) ) { // rats
                speciesSpecificPairs.add( pair );
            }
        }
        log.info( "Getting species pairs took:" + s.toString() );
        return speciesSpecificPairs;
    }

    public static NormalizeResult runUnseen( boolean eraTest, boolean runAll, boolean notInBAMS, String testSet,
            String species ) throws Exception {

        String trainingSet = "WhiteTextNegFixFull";
        // String testSet = "WhiteTextUnseen";
        String annotationSet = "Mallet";

        String baseFolder = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Saved Results/SL/CC/NegFixFullOn" + testSet + "/";
        String filename = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Corpora/Original-Modified/" + testSet + ".orig.xml";

        GateInterface p2g = new GateInterface();

        p2g.setNamedCorpNull( "PubMedUnseenJNChem" );
        p2g.setNamedCorpNull( "PubMedUnseenJCN" );
        p2g.setNamedCorpNull( "PubMedUnseenMScan1" );

        AirolaXMLReader XMLReader = new AirolaXMLReader( filename, p2g, annotationSet );
        SLOutputReader SLReader = new SLOutputReader( trainingSet, testSet, baseFolder );

        boolean predictions = true;
        boolean doAll = false;
        NormalizeResult result = runNormalization( testSet, XMLReader, SLReader, doAll, predictions, eraTest,
                notInBAMS, species );

        if ( runAll ) {
            // Era test turned off!
            eraTest = false;
            runNormalization( testSet, XMLReader, SLReader, runAll, predictions, eraTest, species );
        }

        return result;
    }

    private static NormalizeResult runNormalization( String testSet, AirolaXMLReader XMLReader,
            SLOutputReader SLReader, boolean doAll, boolean usePredictions, String speciesID ) throws Exception {
        boolean year1987Split = false;
        return runNormalization( testSet, XMLReader, SLReader, doAll, usePredictions, year1987Split, speciesID );
    }

    private static NormalizeResult runNormalization( String testSet, AirolaXMLReader XMLReader,
            SLOutputReader SLReader, boolean doAll, boolean usePredictions, boolean year1987Split, String speciesID )
            throws Exception {
        boolean notInBAMS = false;
        return runNormalization( testSet, XMLReader, SLReader, doAll, usePredictions, year1987Split, notInBAMS,
                speciesID );
    }

    private static NormalizeResult runNormalization( String testSet, AirolaXMLReader XMLReader,
            SLOutputReader SLReader, boolean doAll, boolean usePredictions, boolean year1987Split, boolean notInBAMS,
            String speciesID ) throws Exception {
        Map<String, String> results;
        boolean writeOut = true;
        if ( doAll ) testSet += ".all";
        if ( year1987Split ) testSet += ".yearSplit";
        if ( usePredictions ) testSet += ".predictions";
        if ( doAll && year1987Split ) throw new Exception( "Year split and do all options are incompatable" );

        String excelOutFilename = Config.config.getString( "whitetext.iteractions.results.folder" ) + testSet
                + ".xml.normalized.xls";

        NormalizePairs normPairs = new NormalizePairs();

        List<String> positives;
        List<String> negatives;

        if ( usePredictions ) {
            positives = SLReader.getPositivePredictions();
            negatives = SLReader.getNegativePredictions();
        } else {
            positives = SLReader.getPositives();
            negatives = SLReader.getNegatives();
        }
        ParamKeeper keeper = new ParamKeeper();
        String name;
        NormalizeResult normResult = null;

        if ( notInBAMS ) {
            List<String> speciesPairs = getSpeciesPairs( XMLReader, SLReader.getAll(), speciesID );
            // negatives.retainAll( speciesPairs );
            log.info( "Retaining species pairs:" + speciesPairs.size() );
            positives.retainAll( speciesPairs );
            // normalize
            name = "NotInBAMS.rat.positives";
            normResult = normPairs.normalizePairsToMatrix( XMLReader, positives, SLReader, writeOut, name );
            keeper.addParamInstance( normResult.statisticMap );

        } else if ( year1987Split ) {
            int yearSplit1 = 1986;
            int yearSplit2 = 2001;
            log.info( "Doing year based split" );
            List<String> speciesPairs = getSpeciesPairs( XMLReader, SLReader.getAll(), speciesID );
            // negatives.retainAll( speciesPairs );
            log.info( "Retaining species pairs" );
            positives.retainAll( speciesPairs );
            log.info( "Done retaining rat pairs" );

            // split rat positives into years
            Model modelLoad = ModelFactory.createDefaultModel();
            String fileProperty;
            fileProperty = "resolve.Lexicon.resolution.RDF.allComp";
            modelLoad.read( new FileInputStream( Config.config.getString( fileProperty ) ), null );
            fileProperty = "resolve.Lexicon.resolution.RDF";
            modelLoad.read( new FileInputStream( Config.config.getString( fileProperty ) ), null );

            boolean reason = true;
            EvaluationRDFModel model = new EvaluationRDFModel( modelLoad, reason );

            Map<String, Integer> pmidToYearMap = model.getPMIDStringtoYearMap();

            // filter pairs based on date
            List<String> positivesAfter2002 = new LinkedList<String>();
            List<String> positivesAfter1987 = new LinkedList<String>();
            List<String> positivesBefore1986 = new LinkedList<String>();
            Map<String, String> pairToPMID = XMLReader.getPairIDToPMID();
            for ( String pair : positives ) {
                String pmid = pairToPMID.get( pair );
                log.info( "PMID:" + pmid );
                int year = pmidToYearMap.get( pmid );
                log.info( pmid + "->" + year );
                if ( year <= yearSplit1 ) {
                    positivesBefore1986.add( pair );
                } else if ( year <= yearSplit2 ) {
                    positivesAfter1987.add( pair );
                } else {
                    positivesAfter2002.add( pair );
                }
            }

            log.info( "All positives:" + positives.size() );
            log.info( "All positivesBefore1986:" + positivesBefore1986.size() );
            log.info( "All positivesAfter1987:" + positivesAfter1987.size() );

            name = "Positives.rat.before1986" + testSet;
            results = normPairs.normalizePairsToMatrix( XMLReader, positivesBefore1986, SLReader, writeOut, name ).statisticMap;
            keeper.addParamInstance( results );

            name = "Positives.rat.1987to2002" + testSet;
            results = normPairs.normalizePairsToMatrix( XMLReader, positivesAfter1987, SLReader, writeOut, name ).statisticMap;
            keeper.addParamInstance( results );

            name = "Positives.rat.after2002" + testSet;
            results = normPairs.normalizePairsToMatrix( XMLReader, positivesAfter2002, SLReader, writeOut, name ).statisticMap;
            keeper.addParamInstance( results );

        } else if ( !doAll ) {
            name = "Positives." + testSet;
            results = normPairs.normalizePairsToMatrix( XMLReader, positives, SLReader, writeOut, name ).statisticMap;
            keeper.addParamInstance( results );

            name = "Negatives." + testSet;
            results = normPairs.normalizePairsToMatrix( XMLReader, negatives, SLReader, writeOut, name ).statisticMap;
            keeper.addParamInstance( results );

            // filter rats
            List<String> speciesPairs = getSpeciesPairs( XMLReader, SLReader.getAll(), speciesID );
            negatives.retainAll( speciesPairs );
            positives.retainAll( speciesPairs );

            name = "Positives.rat." + testSet;
            normResult = normPairs.normalizePairsToMatrix( XMLReader, positives, SLReader, writeOut, name );
            results = normResult.statisticMap;
            keeper.addParamInstance( results );
            // return the rat positive results

            name = "Negatives.rat." + testSet;
            results = normPairs.normalizePairsToMatrix( XMLReader, negatives, SLReader, writeOut, name ).statisticMap;
            keeper.addParamInstance( results );
        } else {
            name = "all." + testSet;
            results = normPairs.normalizePairsToMatrix( XMLReader, SLReader.getAll(), SLReader, writeOut, name ).statisticMap;
            keeper.addParamInstance( results );

            List<String> speciesPairs = getSpeciesPairs( XMLReader, SLReader.getAll(), speciesID );
            name = "all.rat" + testSet;

            results = normPairs.normalizePairsToMatrix( XMLReader, speciesPairs, SLReader, writeOut, name ).statisticMap;

            keeper.addParamInstance( results );
        }

        keeper.writeExcel( excelOutFilename );
        log.info( "Wrote to:" + excelOutFilename );

        // only returned for not in bams setting
        return normResult;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        boolean usePredictions = false; // use SL predictions or annotated truth
        boolean eraTest = false; // test for 1987 etc eras
        boolean runAll = true; // also run on both positive and negatives combined
        boolean notInBAMS = false;
        // analyseTest( usePredictions, eraTest, runAll, NormalizePairs.RAT );
        // String testSet = "WhiteTextUnseen";
        String testSet = "WhiteTextUnseenMScan2";

        runUnseen( eraTest, runAll, notInBAMS, testSet, RAT );
    }
}

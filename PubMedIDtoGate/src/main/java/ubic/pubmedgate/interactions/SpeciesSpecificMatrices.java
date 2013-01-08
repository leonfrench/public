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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.Util;
import ubic.BAMSandAllen.AnalyzeBAMSandAllenGenes.Direction;
import ubic.BAMSandAllen.BAMSDataLoaders.BAMSDataLoader;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.interactions.evaluation.AllUnseenEvaluationsCombined;
import ubic.pubmedgate.interactions.evaluation.NormalizedConnection;

public class SpeciesSpecificMatrices {
    protected static Log log = LogFactory.getLog( SpeciesSpecificMatrices.class );

    String trainingSet;
    String annotationSet;

    String baseFolder;
    String filename;

    static GateInterface p2g;

    AirolaXMLReader XMLReader;
    SLOutputReader SLReader;
    NormalizePairs BAMSNormalizer;

    Map<String, Set<NormalizedConnection>> speciesIDtoPairs;

    // just one SL+XML reader, combine matrices later
    // start with just annotated

    public SpeciesSpecificMatrices() throws Exception {
        // "WhiteTextNegFixTrain", "Suzanne" );
        // gen.run( gen.getTrainingCorp() );

    }

    public SpeciesSpecificMatrices( String testSet, Set<String> speciesIDs ) throws Exception {
        speciesIDtoPairs = new HashMap<String, Set<NormalizedConnection>>();
        for ( String inputSpeciesID : speciesIDs ) {
            speciesIDtoPairs.put( inputSpeciesID, new java.util.HashSet<NormalizedConnection>() );
        }

        BAMSNormalizer = new NormalizePairs();

        String trainingSet, annotationSet;
        List<String> positivePairs;
        // hand annotated set
        if ( testSet.equals( "WhiteTextNegFixFull" ) ) {
            trainingSet = "WhiteTextNegFixFull";
            annotationSet = "Suzanne";

            String baseFolder = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                    + "Saved Results/SL/CV/WhiteTextNegFixFull/predict/WhiteTextNegFixFull";
            String filename = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                    + "Corpora/Original-Modified/WhiteTextNegFixFull.xml";

            XMLReader = new AirolaXMLReader( filename, p2g, annotationSet );
            SLReader = new SLOutputReader( new File( baseFolder ) );

            positivePairs = SLReader.getPositives();

        } else {
            trainingSet = "WhiteTextNegFixFull";
            annotationSet = "Mallet";

            String baseFolder = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                    + "Saved Results/SL/CC/NegFixFullOn" + testSet + "/";
            String filename = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                    + "Corpora/Original-Modified/" + testSet + ".orig.xml";

            XMLReader = new AirolaXMLReader( filename, p2g, annotationSet );
            SLReader = new SLOutputReader( trainingSet, testSet, baseFolder );

            positivePairs = SLReader.getPositivePredictions();
        }

        boolean writeOut = false;
        String name = testSet + ".species.positives";

        // normalize all first, then split up
        NormalizeResult allResult = BAMSNormalizer.normalizePairsToMatrix( XMLReader, positivePairs, SLReader,
                writeOut, name );

        Map<String, String> pairIDtoPMID = XMLReader.getPairIDToPMID();

        Set<NormalizedConnection> normalizedPairs = allResult.normalizedPairs;
        // remove rejected pairs
        log.info( "Normalized Pairs:" + normalizedPairs.size() );
        AllUnseenEvaluationsCombined allCombined = new AllUnseenEvaluationsCombined();
        normalizedPairs.removeAll( allCombined.getAllRejectedPairs() );
        log.info( "Normalized Pairs after filtering:" + normalizedPairs.size() );

        // create a map from species to normalized pairs
        for ( NormalizedConnection connection : normalizedPairs ) {
            String pairID = connection.pairID;
            String PMID = pairIDtoPMID.get( pairID );
            ConnectionsDocument doc = p2g.getByPMID( PMID );
            Set<String> species = doc.getLinnaeusSpecies();

            // if it has one of the species we are interested in, store it
            for ( String capturedSpeciesID : speciesIDtoPairs.keySet() ) {
                if ( species.contains( capturedSpeciesID ) ) {
                    speciesIDtoPairs.get( capturedSpeciesID ).add( connection );
                }
            }
        }

        // write out matrices for each
        for ( String capturedSpeciesID : speciesIDtoPairs.keySet() ) {
            Set<NormalizedConnection> speciesSpecific = speciesIDtoPairs.get( capturedSpeciesID );

            log.info( capturedSpeciesID + " : " + speciesSpecific.size() );

            DoubleMatrix<String, String> matrix = convertPairsToBAMSMatrix( speciesSpecific );
            String matrixFileName = ( Config.config.getString( "whitetext.iteractions.results.folder" ) + name + "."
                    + capturedSpeciesID + ".matrix" );
            Util.writeRTable( matrixFileName + ".txt", matrix );
        }

        log.info( "Done" );

        // store evaluation results? just add species name

    }

    public DoubleMatrix<String, String> convertPairsToBAMSMatrix( Set<NormalizedConnection> connections )
            throws Exception {
        // only used to get all region names
        Direction direction = Direction.ANYDIRECTION;
        boolean propigated = true;
        DoubleMatrix<String, String> dataMatrix = NormalizePairs.getBAMSConnectionMatrix( propigated, direction );

        List<String> connectionRegionNames = dataMatrix.getRowNames();
        DoubleMatrix<String, String> predictedMatrix = new DenseDoubleMatrix<String, String>(
                connectionRegionNames.size(), connectionRegionNames.size() );
        predictedMatrix.setRowNames( connectionRegionNames );
        predictedMatrix.setColumnNames( connectionRegionNames );

        for ( NormalizedConnection connection : connections ) {
            String resolvedA = connection.regionA;
            String resolvedB = connection.regionB;

            double currentValue = predictedMatrix.getByKeys( resolvedA, resolvedB );
            currentValue += 1;
            predictedMatrix.setByKeys( resolvedA, resolvedB, currentValue );
            predictedMatrix.setByKeys( resolvedB, resolvedA, currentValue );
        }

        return predictedMatrix;
    }

    //

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        // run on hand annotated set

        // String testSet = "WhiteTextNegFixFull"; // annotated set
        String testSet = "WhiteTextUnseen";
        // String testSet = "WhiteTextUnseenMScan";
        // String testSet = "WhiteTextUnseenMScan2"; // corespondes to unseen corpus
        // String testSet = "Random";

        // DONE - MSCAN1 and MSCAN2

        p2g = new GateInterface();
        p2g.setNamedCorpNull( "PubMedUnseenJNChem" );
        // p2g.setNamedCorpNull( "PubMedUnseenJCN" );
        p2g.setNamedCorpNull( "PubMedUnseen" ); // unseen is mscan2?
        p2g.setNamedCorpNull( "PubMedUnseenMScan1" );

        Set<String> speciesIDs = new HashSet<String>();
        speciesIDs.add( "species:ncbi:10116" ); // rat
        speciesIDs.add( "species:ncbi:9685" ); // cat
        speciesIDs.add( "species:ncbi:9544" ); // rhesus
        speciesIDs.add( "species:ncbi:9986" ); // rabbit
        speciesIDs.add( "species:ncbi:9606" ); // human
        speciesIDs.add( "species:ncbi:9541" ); // macaca fascilularis
        speciesIDs.add( "species:ncbi:10090" ); // mouse
        speciesIDs.add( "species:ncbi:9031" ); // chicken

        SpeciesSpecificMatrices species = new SpeciesSpecificMatrices( testSet, speciesIDs );

    }

}

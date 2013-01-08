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

package ubic.pubmedgate.interactions.focusedAnalysis;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.ABAMSDataMatrix;
import ubic.BAMSandAllen.RegressionVector;
import ubic.BAMSandAllen.Util;
import ubic.BAMSandAllen.AnalyzeBAMSandAllenGenes.Direction;
import ubic.BAMSandAllen.BAMSDataLoaders.BAMSDataLoader;
import ubic.BAMSandAllen.BAMSDataLoaders.BAMSFromNIFDataLoader;
import ubic.BAMSandAllen.MatrixPairs.MatrixPair;
import ubic.BAMSandAllen.MatrixPairs.SimpleMatrixPair;
import ubic.BAMSandAllen.adjacency.CorrelationAdjacency;
import ubic.BAMSandAllen.adjacency.IdentityAdjacency;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.params.ParamKeeper;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.interactions.NormalizePairs;


/**
 * Propigates, filters and analyizes literature based connectivity matrices.
 * 
 * @author leon
 */
public class FilterConnectionMatrix {
    String filename;
    BAMSDataLoader bamsLoader;
    DoubleMatrix<String, String> connectionMatrix;
    protected static Log log = LogFactory.getLog( FilterConnectionMatrix.class );
    ABAMSDataMatrix BAMSNIFConnections;
    Direction direction;

    public FilterConnectionMatrix( String filename ) throws Exception {
        direction = Direction.ANYDIRECTION;
        DoubleMatrixReader matrixReader = new DoubleMatrixReader();
        // matrixReader.setTopLeft( false );
        this.filename = filename;
        connectionMatrix = matrixReader.read( filename );
        log.info( "Loaded matrix: " + connectionMatrix.rows() + " X " + connectionMatrix.columns() );
        LinkedList<String> newRowNames = new LinkedList<String>();
        for ( String row : connectionMatrix.getRowNames() ) {
            newRowNames.addLast( row.substring( 1, row.length() - 1 ) );
        }
        connectionMatrix.setRowNames( newRowNames );
        connectionMatrix.setColumnNames( newRowNames );
        bamsLoader = new BAMSDataLoader();

        // load BAMS matrix from NIF (not up-propigated)
        BAMSFromNIFDataLoader connectionLoader = new BAMSFromNIFDataLoader();
        boolean skipFibers = true;
        DoubleMatrix<String, String> BAMSNIFConnectionMatrix = connectionLoader.getBAMSMatrix( direction, false, false,
                skipFibers );
        BAMSNIFConnections = new ABAMSDataMatrix( BAMSNIFConnectionMatrix, "NIFConnectivity", new CorrelationAdjacency(
                BAMSNIFConnectionMatrix ) );
        BAMSNIFConnections = BAMSNIFConnections.removeZeroColumns();
        BAMSNIFConnections = BAMSNIFConnections.removeZeroRows();
        log.info( "NIF Connections:" + BAMSNIFConnections.getDimensionString() );

    }

    public Map<String, String> compareToReal( boolean propigated, double threshold ) throws Exception {
        Map<String, String> results = new HashMap<String, String>();

        DoubleMatrix<String, String> BAMSconnectionMatrix = NormalizePairs.getBAMSConnectionMatrix( propigated, direction );

        int totalDepth = 0;
        int BAMStotalDepth = 0;

        int matchedConnections = 0;
        int weightedMatchedConnections = 0;
        int BAMSConnectionCount = 0;
        List<Integer> positiveRanks = new LinkedList<Integer>();

        int connectionCount = 0;
        for ( String row : connectionMatrix.getRowNames() ) {
            for ( String col : connectionMatrix.getRowNames() ) {
                // just do the triangle
                if ( connectionMatrix.getRowIndexByName( row ) < connectionMatrix.getRowIndexByName( col ) ) continue;

                double connectionMatrixValue = connectionMatrix.getByKeys( row, col );
                double BAMSValue = BAMSconnectionMatrix.getByKeys( row, col );

                boolean BAMSConnection = BAMSValue == 1d;
                boolean literatureConnection = connectionMatrixValue >= threshold;

                boolean NIFBAMSConnection = false;
                try {
                    NIFBAMSConnection = BAMSNIFConnections.getByKeys( row, col ) > 0;
                } catch ( Exception e ) {

                }

                if ( literatureConnection ) {
                    int rowDepth = bamsLoader.getParents( row ).size();
                    int colDepth = bamsLoader.getParents( col ).size();
                    connectionCount++;
                    totalDepth += rowDepth + colDepth;
                }

                if ( BAMSConnection ) {
                    BAMSConnectionCount++;
                    // get parents call is very slow!
                    // BAMStotalDepth += rowDepth + colDepth;
                }

                if ( !BAMSConnection && literatureConnection && NIFBAMSConnection ) {
                    log.info( "Connection in new BAMS but not old BAMS" );
                    log.info( row + "->" + col );
                }

                if ( BAMSConnection && literatureConnection ) {
                    positiveRanks.add( connectionCount );

                    if ( row.equals( col ) ) {
                        log.info( "Self connect" );
                    }

                    if ( connectionMatrixValue > 10d ) { // print some out
                        log.info( connectionMatrixValue + " connections:" + row + " -> " + col );
                        log.info( "Depth:" + bamsLoader.getParents( row ).size() + " -> "
                                + bamsLoader.getParents( col ).size() );
                    }

                    // log.info( row + " " + col );
                    matchedConnections++;
                    weightedMatchedConnections += connectionMatrix.getByKeys( row, col );
                }
            }
        }

        if ( !BAMSconnectionMatrix.getRowNames().equals( connectionMatrix.getRowNames() ) )
            throw new RuntimeException( "Error mismatched matrices" );
        if ( !BAMSconnectionMatrix.getColNames().equals( connectionMatrix.getColNames() ) )
            throw new RuntimeException( "Error mismatched matrices" );

        ABAMSDataMatrix BAMSConnection = new ABAMSDataMatrix( BAMSconnectionMatrix, "BAMSconnectionMatrix",
                new IdentityAdjacency( BAMSconnectionMatrix ) );
        ABAMSDataMatrix literatureConnections = new ABAMSDataMatrix( connectionMatrix, "LiteratureConnectionMatrix",
                new IdentityAdjacency( connectionMatrix ) );
        MatrixPair pair = new SimpleMatrixPair( BAMSConnection, literatureConnections );

        ABAMSDataMatrix LiteratureConnectionsThreshold = new ABAMSDataMatrix( threshold( threshold ),
                "LiteratureConnectionMatrixThresh", new IdentityAdjacency( connectionMatrix ) );
        MatrixPair pairThresh = new SimpleMatrixPair( BAMSConnection, LiteratureConnectionsThreshold );

        pair.printDimensions();

        boolean fast = true;
        results.put( "Correlation of matrix values", "" + pair.getCorrelation( fast ) );
        results.put( "Correlation of thresholded matrix values", "" + pairThresh.getCorrelation( fast ) );

        literatureConnections = literatureConnections.removeZeroColumns();
        literatureConnections = literatureConnections.removeZeroRows();
        results.put( "Nonzero matrix rows", "" + literatureConnections.rows() );
        results.put( "Nonzero matrix cols", "" + literatureConnections.columns() );

        BAMSConnection = BAMSConnection.retainColumns( literatureConnections.getColNames() );
        BAMSConnection = BAMSConnection.retainRows( literatureConnections.getRowNames() );

        results.put( "Full zSum", "" + Util.zSum( connectionMatrix ) );
        results.put( "Full zSum on matching BAMS matrix", "" + Util.zSum( BAMSConnection ) );

        results.put( "Threshold", "" + threshold );
        results.put( "Connections", "" + connectionCount );
        results.put( "BAMS Connections", "" + BAMSConnectionCount );
        results.put( "Matched connections", "" + matchedConnections );
        results.put( "Precision", "" + ( matchedConnections / ( double ) connectionCount ) );
        results.put( "Recall", "" + ( matchedConnections / ( double ) BAMSConnectionCount ) );

        results.put( "Weighted Matched connections", "" + weightedMatchedConnections );
        results.put( "TotalDepth", "" + connectionCount );
        // results.put( "BAMSTotalDepth", "" + BAMStotalDepth );
        results.put( "Average pair depth", "" + ( totalDepth / ( double ) connectionCount ) );
        results.put( "Propigated BAMS", "" + propigated );
        // results.put( "AUC", ROC.aroc( connectionCount, positiveRanks ) + "" ); // AUC doesnt make sence here, it's
        // not sorted
        results.put( "Filename", filename );
        results.put( "BaseFilename", getBaseFilename() );

        return results;
    }

    public String getBaseFilename() {
        return new File( filename ).getName();
    }

    public DoubleMatrix<String, String> threshold( double threshold ) {
        List<String> regions = connectionMatrix.getRowNames();
        DoubleMatrix<String, String> newConnectionMatrix = new DenseDoubleMatrix<String, String>( regions.size(),
                regions.size() );
        newConnectionMatrix.setRowNames( regions );
        newConnectionMatrix.setColumnNames( regions );

        for ( String row : regions ) {
            for ( String col : regions ) {
                double connectionMatrixValue = connectionMatrix.getByKeys( row, col );
                if ( connectionMatrixValue >= threshold ) {
                    newConnectionMatrix.setByKeys( row, col, 1d );
                } else {
                    newConnectionMatrix.setByKeys( row, col, 0d );
                }
            }
        }
        return newConnectionMatrix;
    }

    public void propigate() {
        // how to deal with weights? add them?

        // for each region count how many it has, plus how many its children has, put in new matrix
        List<String> regions = connectionMatrix.getRowNames();

        DoubleMatrix<String, String> newConnectionMatrix = new DenseDoubleMatrix<String, String>( regions.size(),
                regions.size() );
        newConnectionMatrix.setRowNames( regions );
        newConnectionMatrix.setColumnNames( regions );

        for ( String row : regions ) {
            for ( String col : regions ) {
                if ( col.equals( row ) ) continue;
                double connections = 0;

                boolean indirect = true;
                Set<String> rowChildren = bamsLoader.getChildren( row, indirect );
                if ( rowChildren == null ) {
                    log.info( "Row null:" + row );
                    rowChildren = new HashSet<String>();
                }
                Set<String> colChildren = bamsLoader.getChildren( col, indirect );
                if ( colChildren == null ) colChildren = new HashSet<String>();

                rowChildren.add( row );
                colChildren.add( col );

                // count any connections between the two trees
                for ( String rowChild : rowChildren ) {
                    for ( String colChild : colChildren ) {
                        connections += connectionMatrix.getByKeys( rowChild, colChild );
                    }
                }
                newConnectionMatrix.setByKeys( row, col, connections );
                newConnectionMatrix.setByKeys( col, row, connections );
            }
        }
        connectionMatrix = newConnectionMatrix;
        filename = filename + ".propigated";
    }

    public void writeMatrix() throws Exception {
        log.info( "Writing to:" + filename );
        Util.writeRTable( filename, connectionMatrix );
    }

    public void writeMatrixImage() throws Exception {
        ABAMSDataMatrix literatureConnections = new ABAMSDataMatrix( connectionMatrix, "LiteratureConnectionMatrix",
                new IdentityAdjacency( connectionMatrix ) );

        // remove zero rows and cols
        literatureConnections = literatureConnections.removeZeroColumns();
        literatureConnections = literatureConnections.removeZeroRows();

        // order rows and cols
        List<String> sortedRegions = new LinkedList<String>( literatureConnections.getColNames() );
        Collections.sort( sortedRegions );
        literatureConnections = literatureConnections.orderRows( sortedRegions );
        literatureConnections = literatureConnections.orderCols( sortedRegions );

        Util.writeImage( filename + ".zeroes.removed.png", literatureConnections );
        log.info( "Write image to:" + filename + ".zeroes.removed.png" );

    }

    /**
     * @param args
     */

    public static void makepropigates( String folder ) throws Exception {
        File folderF = new File( folder );
        for ( String f : folderF.list() ) {
            if ( f.endsWith( ".matrix.txt" ) ) {
                String filename = folder + "/" + f.toString();
                log.info( folder );
                FilterConnectionMatrix test = new FilterConnectionMatrix( filename );
                test.propigate();
                test.writeMatrix();
            }
        }
    }

    public DoubleMatrix<String, String> getConnectionMatrix() {
        return connectionMatrix;
    }

    public static void quickTestTwo( FilterConnectionMatrix firstMatrix, FilterConnectionMatrix secondMatrix,
            boolean threshold ) throws Exception {
        DoubleMatrix<String, String> firstZeroOne;
        DoubleMatrix<String, String> secondZeroOne;
        if ( threshold ) {
            firstZeroOne = firstMatrix.threshold( 1 );
            secondZeroOne = secondMatrix.threshold( 1 );
        } else {
            firstZeroOne = firstMatrix.getConnectionMatrix();
            secondZeroOne = secondMatrix.getConnectionMatrix();
        }

        ABAMSDataMatrix LiteratureConnections1 = new ABAMSDataMatrix( firstZeroOne, firstMatrix.getBaseFilename(),
                new IdentityAdjacency( firstZeroOne ) );
        ABAMSDataMatrix LiteratureConnections2 = new ABAMSDataMatrix( secondZeroOne, secondMatrix.getBaseFilename(),
                new IdentityAdjacency( secondZeroOne ) );
        MatrixPair pair = new SimpleMatrixPair( LiteratureConnections1, LiteratureConnections2 );

        log.info( "Pairs:" + firstMatrix.getBaseFilename() + " and " + secondMatrix.getBaseFilename() );
        log.info( "  Correlation:" + pair.getCorrelation( true ) );
        log.info( "  threshold:" + threshold );

        ABAMSDataMatrix matrixA = pair.getMatrixA();
        ABAMSDataMatrix matrixB = pair.getMatrixB();
        Set<String> regions = ( Set<String> ) Util.union( matrixA.getColNames(), matrixB.getColNames() );

        if ( threshold ) {
            int intersection = 0;
            for ( String row : matrixA.getRowNames() ) {
                for ( String col : regions ) {
                    if ( row.equals( col ) ) continue;
                    double connectionMatrixValueA = matrixA.getByKeys( row, col );
                    double connectionMatrixValueB = matrixB.getByKeys( row, col );
                    if ( connectionMatrixValueA > 0 && connectionMatrixValueB > 0 ) {
                        intersection++;
                    }
                }
            }
            int matrixACons = ( int ) Util.zSum( matrixA ) / 2;
            int matrixBCons = ( int ) Util.zSum( matrixB ) / 2;
            log.info( "  " + "Common connections:" + intersection / 2 );
            log.info( "  " + "Connections " + matrixA.getName() + ":" + matrixACons + " " + ( double ) 50
                    * intersection / matrixACons );
            log.info( "  " + "Connections " + matrixB.getName() + ":" + matrixBCons + " " + ( double ) 50
                    * intersection / matrixBCons );
        }

    }

    public void degreeTest( double threshold ) throws Exception {
        boolean propigated = true;
        Direction direction = Direction.ANYDIRECTION;
        DoubleMatrix<String, String> BAMSconnectionMatrix = NormalizePairs.getBAMSConnectionMatrix( propigated, direction );

        DoubleMatrix<String, String> connectionMatrixForUse = threshold( threshold );

        ABAMSDataMatrix BAMSConnection = new ABAMSDataMatrix( BAMSconnectionMatrix, "BAMSconnectionMatrix",
                new CorrelationAdjacency( BAMSconnectionMatrix ) );
        BAMSConnection = BAMSConnection.removeZeroColumns();

        ABAMSDataMatrix literatureConnections = new ABAMSDataMatrix( connectionMatrixForUse,
                "LiteratureConnectionMatrix", new CorrelationAdjacency( connectionMatrixForUse ) );
        literatureConnections = literatureConnections.removeZeroColumns();
        
        log.info( "Literature non-zero regions:" + literatureConnections.columns() );

        MatrixPair pair = new SimpleMatrixPair( BAMSConnection, literatureConnections );
        pair.sameSpace();
        pair.printDimensions();
        pair.switchMatrices();
        pair.sameSpace();
        pair.slimMatrices();
        pair.printDimensions();

        if ( !BAMSconnectionMatrix.getColNames().equals( connectionMatrixForUse.getColNames() ) )
            throw new RuntimeException( "Error mismatched matrices" );

        boolean spearman = true;
        log.info( "Threshold:" + threshold );
        log.info( "Degree, spearman:" + pair.getFlattenedCorrelation( spearman ) );
        log.info( "Degree, pearson:" + pair.getFlattenedCorrelation( !spearman ) );
        log.info( "Mantel (shared connections):" + pair.getCorrelation( true ) );

        boolean removeNan = true;

        DoubleMatrix<String, String> matrixASums = Util.columnSums( pair.getMatrixA(), removeNan );
        DoubleMatrix<String, String> matrixBSums = Util.columnSums( pair.getMatrixB(), removeNan );

        DoubleMatrix<String, String> matrixForR = new DenseDoubleMatrix<String, String>( 2, matrixASums.columns() );

        // RegressionVector vector = new RegressionVector( 2, matrixASums.columns(), false );
        matrixForR.setColumnNames( pair.getMatrixB().getColNames() );
        matrixForR.addRowName( pair.getMatrixA().getName() );
        matrixForR.addRowName( pair.getMatrixB().getName() );
        for ( int i = 0; i < matrixForR.columns(); i++ ) {
            matrixForR.set( 0, i, matrixASums.get( 0, i ) );
            matrixForR.set( 1, i, matrixBSums.get( 0, i ) );
        }
        log.info( "Writing table" );
        Util.writeRTable( "/grp/java/workspace/PubMedIDtoGate/spreadsheets/interaction.results/degreeSequence.rat.txt",
                matrixForR );

    }

    public static void testDegree() throws Exception {
        // tests all columns
        String base = "/grp/java/workspace/PubMedIDtoGate/spreadsheets/interaction.results/matrices/";
        String filename = base;
//         filename += "Positives.WhiteTextUnseen.matrix.txt.propigated";
        filename += "all.ratWhiteTextUnseen.all.matrix.txt.propigated";
//        filename += "Positives.rat.WhiteTextUnseen.matrix.txt.propigated";

        FilterConnectionMatrix matrix = new FilterConnectionMatrix( filename );
        double threshold = 1;
        matrix.degreeTest( threshold );
    }

    private static void quickTest( String first, String second ) throws Exception {
        String base = Config.config.getString( "whitetext.iteractions.matricesFolder" );
        FilterConnectionMatrix a = new FilterConnectionMatrix( base + first );
        FilterConnectionMatrix b = new FilterConnectionMatrix( base + second );
        quickTestTwo( a, b, true );
        quickTestTwo( a, b, false );
    }

    public static void main( String[] args ) throws Exception {
        makepropigates( "/home/lfrench/WhiteText/spreadsheets/interaction.results/species specific" );
        System.exit(1);
        

        String filename;
        ParamKeeper keeper = new ParamKeeper();
        String folder = Config.config.getString( "whitetext.iteractions.matricesFolder" );

        // makepropigates( folder );
        testDegree();
        System.exit( 1 );

        // TODO add in degree correlation statistics

        // quickTest( "Positives.WhiteTextUnseen.matrix.txt", "Negatives.WhiteTextUnseen.matrix.txt" );

        quickTest( "Positives.WhiteTextUnseen.matrix.txt", "Positives.WhiteTextUnseenMScan.predictions.matrix.txt" );
        quickTest( "Negatives.WhiteTextUnseenMScan.predictions.matrix.txt", "Negatives.WhiteTextUnseen.matrix.txt" );

        // quickTest( "Positives.WhiteTextUnseen.matrix.txt.propigated",
        // "Negatives.WhiteTextUnseen.matrix.txt.propigated" );
        // quickTest( "Positives.rat.WhiteTextUnseen.matrix.txt", "Negatives.rat.WhiteTextUnseen.matrix.txt" );
        // quickTest( "Positives.rat.Annotated.matrix.txt", "Negatives.rat.Annotated.matrix.txt" );
        // quickTest( "Positives.Annotated.matrix.txt", "Negatives.Annotated.matrix.txt" );
        System.exit( 1 );

        // String endfix = "matrix.txt";
        // String endfix = "all.ratWhiteTextUnseen.all.matrix.txt";
        // String endfix = "Positives.rat.WhiteTextUnseen.matrix.txt";
        String endfix = "WhiteTextUnseenMScan.predictions.matrix.txt";

        File folderF = new File( folder );
        for ( String f : folderF.list() ) {
            if ( f.endsWith( endfix ) ) {
                filename = folder + f.toString();
                log.info( filename );
                FilterConnectionMatrix test = new FilterConnectionMatrix( filename );
                for ( double threshold = 1; threshold < 7d; threshold++ ) {
                    boolean propigated = true;
                    Map<String, String> results = test.compareToReal( propigated, threshold );
                    keeper.addParamInstance( results );

                    propigated = false;
                    results = test.compareToReal( propigated, threshold );
                    keeper.addParamInstance( results );

                    if ( threshold == 1 ) test.writeMatrixImage();

                    if ( results.get( "Connections" ).equals( "0" ) ) break;

                }
            }
        }
        String outFilename = Config.config.getString( "whitetext.iteractions.results.folder" ) + endfix + ".compared."
                + System.currentTimeMillis() + ".xls";
        keeper.writeExcel( outFilename );
        log.info( "Wrote to:" + outFilename );
    }

}

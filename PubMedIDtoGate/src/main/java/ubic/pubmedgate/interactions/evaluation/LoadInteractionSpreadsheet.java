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

package ubic.pubmedgate.interactions.evaluation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFSheet;

import ubic.BAMSandAllen.Util;
import ubic.BAMSandAllen.BAMSDataLoaders.BAMSXMLConnectivityLoader;
import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.excel.ExcelUtil;
import ubic.basecode.io.excel.SpreadSheetSchema;

public class LoadInteractionSpreadsheet {
    protected static Log log = LogFactory.getLog( LoadInteractionSpreadsheet.class );
    protected String filename;
    protected Set<Integer> rows;
    protected String name;
    protected HSSFSheet sheet;
    protected SpreadSheetSchema schema;

    public LoadInteractionSpreadsheet( String filename, Set<Integer> rows, String name, SpreadSheetSchema schema )
            throws Exception {
        super();
        this.filename = filename;
        this.rows = rows;
        this.name = name;
        this.schema = schema;
        sheet = ExcelUtil.getSheetFromFile( filename, "Sheet1" );
        if ( sheet == null ) {
            sheet = ExcelUtil.getSheetFromFile( filename, "Sheet0" );
        }
        if ( sheet == null ) {
            sheet = ExcelUtil.getSheetFromFile( filename, "Sheet0 - Table 1" );
        }
        if ( sheet == null ) {
            sheet = ExcelUtil.getSheetFromFile( filename, "Sheet 1 - 0" );
        }

    }

    /**
     * @param args
     */

    public Set<String> getAcceptedPairs() throws Exception {
        Set<String> rejected = getNonBlankPairs( "Reject" );
        Set<String> result = ( Set<String> ) Util.subtract( getAllPairs(), rejected );
        return result;
    }

    public Set<String> getNonAmbigAcceptedPairs() throws Exception {
        Set<String> result = new HashSet<String>();
        Set<String> ambig = getNonBlankPairs( "Ambiguous" );
        Set<String> rejected = getNonBlankPairs( "Reject" );

        result = ( Set<String> ) Util.subtract( getAllPairs(), rejected, ambig );
        return result;
    }

    public static String convertToPercent( double numerator, double divisor ) {
        return String.format( "%.1f", ( 100d * ( numerator ) / divisor ) ) + "%";
    }

    public Set<String> compareToOther( LoadInteractionSpreadsheet other, String position ) throws Exception {
        // find intersecting rows
        Set<String> bothAnnotated = ( Set<String> ) Util.intersect( getAllPairs(), other.getAllPairs() );
        if ( bothAnnotated.size() == 0 ) return new HashSet<String>();

        System.out.println( "Comparison between " + name + " and " + other.name );
        System.out.println( " Done by both:" + bothAnnotated.size() );

        Set<String> aNonBlankPairs = getNonBlankPairs( position );
        aNonBlankPairs.retainAll( bothAnnotated );
        Set<String> bNonBlankPairs = other.getNonBlankPairs( position );
        bNonBlankPairs.retainAll( bothAnnotated );

        Set<String> bothNonBlank = ( Set<String> ) Util.intersect( aNonBlankPairs, bNonBlankPairs );
        bothNonBlank.retainAll( bothAnnotated );

        System.out.println( " Both non blank for " + position + ":" + bothNonBlank.size() );
        int aMarkedNotB = Util.subtract( aNonBlankPairs, bNonBlankPairs ).size();
        System.out.println( " " + aMarkedNotB + " marked by " + name + " ("
                + convertToPercent( aMarkedNotB, aNonBlankPairs.size() ) + ") but not by " + other.name );
        int bMarkedNotA = Util.subtract( bNonBlankPairs, aNonBlankPairs ).size();
        System.out.println( " " + bMarkedNotA + " marked by " + other.name + " ("
                + convertToPercent( bMarkedNotA, bNonBlankPairs.size() ) + ") but not by " + name );

        Set<String> bothBlank = ( Set<String> ) Util.intersect( getBlankPairs( position ),
                other.getBlankPairs( position ) );
        bothBlank.retainAll( bothAnnotated );
        System.out.println( " Both blank for " + position + ":" + bothBlank.size() );
        System.out.println( " Agreement:"
                + convertToPercent( bothBlank.size() + bothNonBlank.size(), bothAnnotated.size() ) );
        System.out.println();

        Set<String> disagreements = new HashSet<String>( bothAnnotated );
        disagreements.removeAll( bothNonBlank );
        disagreements.removeAll( bothBlank );
        return disagreements;
    }

    public Map<Integer, String> getRowToPairMap() {
        Map<Integer, String> result = new HashMap<Integer, String>();
        for ( int row : rows ) {
            String pairID = ExcelUtil.getValue( sheet, row, schema.getPosition( "PairID" ) );
            result.put( row, pairID );
        }
        return result;
    }

    public Set<String> compareToNewBAMS() throws Exception {
        Set<String> resultPairs = new HashSet<String>();
        BAMSXMLConnectivityLoader loader = new BAMSXMLConnectivityLoader();
        DoubleMatrix<String, String> outgoingMatrix = loader.getOutgoingMatrix();
        DoubleMatrix<String, String> notPresentMatrix = loader.getNotPresentMatrix();

        for ( int row : rows ) {
            String pairID = ExcelUtil.getValue( sheet, row, schema.getPosition( "PairID" ) );
            String regionAName = ExcelUtil.getValue( sheet, row, schema.getPosition( "RegionAResolve" ) );
            String regionBName = ExcelUtil.getValue( sheet, row, schema.getPosition( "RegionBResolve" ) );

            double outgoing = 0, incoming = 0, notPresentOut = 0, notPresentIn = 0;
            try {
                outgoing = outgoingMatrix.getByKeys( regionAName, regionBName );
                incoming = outgoingMatrix.getByKeys( regionBName, regionAName );
                notPresentOut = notPresentMatrix.getByKeys( regionAName, regionBName );
                notPresentIn = notPresentMatrix.getByKeys( regionBName, regionAName );
            } catch ( Exception e ) {
                // log.warn( e.getMessage() );
            }
            if ( outgoing != 0d || incoming != 0d ) {
                log.info( "connection in new for " + regionAName + "->" + regionBName );
            }
            if ( notPresentOut != 0d || notPresentIn != 0d ) {
                log.info( "NO connection in new for " + regionAName + "->" + regionBName + " " + pairID );
                resultPairs.add( pairID );
            }
        }
        log.info( "Number of pairs that are in Not in BAMS and are listed as not present:" + resultPairs.size() );
        return resultPairs;
    }

    public Set<String> getAllPairs() {
        Set<String> result = new HashSet<String>();
        for ( int row : rows ) {
            String pairID = ExcelUtil.getValue( sheet, row, schema.getPosition( "PairID" ) );
            result.add( pairID );
        }
        return result;
    }

    public int getPairIDRowCount( String queryPairID ) {
        int result = 0;
        for ( int row : rows ) {
            String pairID = ExcelUtil.getValue( sheet, row, schema.getPosition( "PairID" ) );
            if ( pairID.equals( queryPairID ) ) result++;
        }
        return result;
    }

    public int getPairIDAcceptCount( String queryPairID ) {
        int result = 0;
        for ( int row : rows ) {
            String pairID = ExcelUtil.getValue( sheet, row, schema.getPosition( "PairID" ) );
            String value = ExcelUtil.getValue( sheet, row, schema.getPosition( "Reject" ) );
            if ( pairID.equals( queryPairID ) ) {
                if ( value == null || value.equals( "" ) ) {
                    result++;
                }
            }
        }
        return result;
    }

    public void printLine( String column ) throws Exception {
        int allPairs = getAllPairs().size();
        System.out.println( " " + column + ":" + countNonBlank( column ) + "("
                + convertToPercent( countNonBlank( column ), allPairs ) + ")" );
    }

    public void printDirection( String column ) throws Exception {
        // normalize by number of accepted connections
        int allPairs = getAcceptedPairs().size();
        System.out.println( " " + column + ":" + countNonBlank( column ) + "("
                + convertToPercent( countNonBlank( column ), allPairs ) + ") of " + allPairs );
    }

    public void printBiDirectional() throws Exception {
        int allPairs = getAcceptedPairs().size();
        int bi = Util.intersectSize( getNonBlankPairs( "Efferent/Outgoing" ), getNonBlankPairs( "Afferent/Incoming" ) );
        int acceptAndOneDir = Util.intersectSize( getAcceptedPairs(),
                Util.union( getNonBlankPairs( "Efferent/Outgoing" ), getNonBlankPairs( "Afferent/Incoming" ) ) );
        System.out.println( " Both afferent and efferent:" + bi + " " + convertToPercent( bi, allPairs ) );
        System.out.println( " Accepted and one direction:" + acceptAndOneDir );
    }

    public void printStats() throws Exception {
        int size = rows.size();
        System.out.println( "Name:" + name );
        System.out.println( "Row count:" + size );
        System.out.println( "Pair count:" + getAllPairs().size() );
        printLine( "Reject" );
        // printLine( "Efferent/Outgoing" );
        printDirection( "Efferent/Outgoing" );
        // printLine( "Afferent/Incoming" );
        printDirection( "Afferent/Incoming" );
        printBiDirectional();
        printLine( "Named entity error" );
        printLine( "Abbreviation error" );
        printLine( "Ambiguous" );
        printLine( "Comment" );
        System.out.println( " getNonAmbigAcceptedPairs:" + getNonAmbigAcceptedPairs().size() + "("
                + convertToPercent( getNonAmbigAcceptedPairs().size(), size ) + ")" );
    }

    public void printHeader() {
        for ( int i = 0; i < 10; i++ ) {
            log.info( i + "->" + ExcelUtil.getValue( sheet, 0, i ) );
        }
    }

    public Set<String> getNonBlankPairs( String colName ) throws Exception {
        int pos = schema.getPosition( colName );
        Set<String> result = new HashSet<String>();
        for ( int row : rows ) {
            String value = ExcelUtil.getValue( sheet, row, pos );
            if ( value == null || value.equals( "" ) ) {
            } else {
                String pairID = ExcelUtil.getValue( sheet, row, schema.getPosition( "PairID" ) );
                // log.info( pairID );
                result.add( pairID );
            }
        }
        return result;
    }

    public CountingMap<String> getColumnBreakDown( String colName ) {
        int pos = schema.getPosition( colName );
        CountingMap<String> result = new CountingMap<String>();
        for ( int row : rows ) {
            String value = ExcelUtil.getValue( sheet, row, pos );
            result.increment( value );
        }
        return result;
    }

    public void printColumnBreakDown( String colName ) {
        CountingMap<String> result = getColumnBreakDown( colName );
        int size = getAllPairs().size();
        System.out.println( "Column breakdown of " + colName + " for " + name );
        for ( String key : result.sortedKeyList() ) {
            System.out.println( "   " + key + ":" + result.get( key ) + " ("
                    + convertToPercent( result.get( key ), size ) + ")" );
        }
    }

    public Set<String> getBlankPairs( String colName ) throws Exception {
        Set<String> nonBlank = getNonBlankPairs( colName );
        Set<String> all = getAllPairs();
        all.removeAll( nonBlank );
        return all;
    }

    public int countNonBlank( String colName ) throws Exception {
        return getNonBlankPairs( colName ).size();
    }

}

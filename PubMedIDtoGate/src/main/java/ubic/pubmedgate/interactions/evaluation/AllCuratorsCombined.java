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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.Util;
import ubic.BAMSandAllen.BAMSDataLoaders.BAMSXMLConnectivityLoader;
import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.excel.SpreadSheetSchema;
import ubic.pubmedgate.Config;

public class AllCuratorsCombined {
    protected static Log log = LogFactory.getLog( AllCuratorsCombined.class );
    LoadInteractionSpreadsheet Cel;
    LoadInteractionSpreadsheet YiQi;
    LoadInteractionSpreadsheet Cathy;

    public AllCuratorsCombined() throws Exception {

    }

    public void loadNotInBAMSSet() throws Exception {
        Set<Integer> CelRows = new HashSet<Integer>();
        Set<Integer> YiRows = new HashSet<Integer>();
        Set<Integer> CatRows = new HashSet<Integer>();
        for ( int i = 1; i < 837; i++ ) {
            CatRows.add( i );
            CelRows.add( i );
            YiRows.add( i );
        }

        log.info( "Celia rows:" + CelRows.size() );
        log.info( "Yiqi rows:" + YiRows.size() );
        log.info( "Cathy rows:" + CatRows.size() );

        String baseFolder = Config.config.getString( "whitetext.iteractions.evaluations.folder" ) + "Not in BAMS/";
        String CeliaFile = baseFolder + "csiu_NotInBAMSSorted.xls";
        String YiQiFile = baseFolder + "NotInBAMSSorted_yiqi.xls";
        String CathyFile = baseFolder + "NotInBAMSSortedCathy.xls";
        SpreadSheetSchema schema = new NotInBAMSSchema();

        Cel = new LoadInteractionSpreadsheet( CeliaFile, CelRows, "Celia", schema );

        YiQi = new LoadInteractionSpreadsheet( YiQiFile, YiRows, "YiQi", schema );

        Cathy = new LoadInteractionSpreadsheet( CathyFile, CatRows, "Cathy", schema );
    }

    public void loadFirstSet() throws Exception {
        Set<Integer> CelRows = new HashSet<Integer>();
        Set<Integer> YiRows = new HashSet<Integer>();
        Set<Integer> CatRows = new HashSet<Integer>();
        for ( int i = 0; i < 2001; i++ ) {
            if ( i > 0 && i < 1401 ) {
                CatRows.add( i );
            }
            if ( ( i > 200 && i < 801 ) || ( i > 1400 && i < 2001 ) ) {
                CelRows.add( i );
            }
            if ( ( i > 800 && i < 2001 ) ) {
                YiRows.add( i );
            }
        }

        log.info( "Celia rows:" + CelRows.size() );
        log.info( "Yiqi rows:" + YiRows.size() );
        log.info( "Cathy rows:" + CatRows.size() );

        String baseFolder = Config.config.getString( "whitetext.iteractions.evaluations.folder" );
        String CeliaFile = baseFolder + "celia_202-800_AND_1401-2001_version2.xls";
        String YiQiFile = baseFolder + "Yiqi brain interactions.xls";
        String CathyFile = baseFolder + "Cathy-2-1400.xls";

        SpreadSheetSchema schema = new InteractionSchema();

        Cel = new LoadInteractionSpreadsheet( CeliaFile, CelRows, "Celia", schema );

        YiQi = new LoadInteractionSpreadsheet( YiQiFile, YiRows, "YiQi", schema );

        Cathy = new LoadInteractionSpreadsheet( CathyFile, CatRows, "Cathy", schema );
    }

    public void printAllStats() throws Exception {
        Cel.printStats();
        YiQi.printStats();
        Cathy.printStats();
    }

    public Map<Integer, String> getRowToPairMap() {
        Map<Integer, String> result = new HashMap<Integer, String>();
        result.putAll( Cel.getRowToPairMap() );
        result.putAll( YiQi.getRowToPairMap() );
        result.putAll( Cathy.getRowToPairMap() );
        return result;
    }

    public Collection<String> getAllPairs() {
        return getRowToPairMap().values();
    }

    public void printDisagreements( String column ) throws Exception {
        // for all Pairs, find disagreements
        Cathy.compareToOther( Cel, column );
        Cel.compareToOther( YiQi, column );
        Cathy.compareToOther( YiQi, column );
        System.out.println();
    }

    public void showDisagreements( String column ) throws Exception {
        Map<Integer, String> rowToPairMap = getRowToPairMap();
        Set<String> ambig = new HashSet<String>();
        ambig.addAll( Cathy.getNonBlankPairs( "Ambiguous" ) );
        ambig.addAll( Cel.getNonBlankPairs( "Ambiguous" ) );
        ambig.addAll( YiQi.getNonBlankPairs( "Ambiguous" ) );

        Set<String> disagreements = new HashSet<String>();
        disagreements.addAll( Cathy.compareToOther( Cel, column ) );
        disagreements.addAll( Cel.compareToOther( YiQi, column ) );
        disagreements.addAll( Cathy.compareToOther( YiQi, column ) );

        for ( int row = 0; row < 2002; row++ ) {
            String pairID = rowToPairMap.get( row );
            String disagree = "Agree";
            String ambiguous = "Not ambiguous";

            if ( pairID != null && disagreements.contains( pairID ) ) {
                disagree = "Disagree";
            }
            if ( pairID != null && ambig.contains( pairID ) ) {
                ambiguous = "Ambiguous";
            }

            System.out.println( row + "," + disagree + "," + ambiguous + "," + pairID );
        }
        log.info( "Ambiguous:" + ambig.size() );
        log.info( "Disagreements:" + disagreements.size() );
    }

    public Set<String> getAcceptedTwice() throws Exception {
        CountingMap<String> counts = new CountingMap<String>();
        counts.incrementAll( Cel.getAcceptedPairs() );
        counts.incrementAll( YiQi.getAcceptedPairs() );
        counts.incrementAll( Cathy.getAcceptedPairs() );
        Set<String> result = new HashSet<String>();
        for ( String key : counts.keySet() ) {
            if ( counts.get( key ) > 1 ) result.add( key );
        }
        return result;

    }

    public Set<String> getAllAcceptedPairs() throws Exception {
        Set<String> result = new HashSet<String>();
        result.addAll( Cathy.getAcceptedPairs() );
        result.addAll( Cel.getAcceptedPairs() );
        result.addAll( YiQi.getAcceptedPairs() );
        return result;
    }

    public void compareToNewBAMS() throws Exception {
        Set<String> BAMSDisagree = Cathy.compareToNewBAMS();
        log.info( "Intersect of accepted:" + Util.intersectSize( BAMSDisagree, getAllAcceptedPairs() ) );

    }

    public void resolveColumns() {
        Cathy.printColumnBreakDown( "Resolve Evaluation A" );
        Cel.printColumnBreakDown( "Resolve Evaluation A" );
        YiQi.printColumnBreakDown( "Resolve Evaluation A" );
        System.out.println();
        Cathy.printColumnBreakDown( "Resolve Evaluation B" );
        Cel.printColumnBreakDown( "Resolve Evaluation B" );
        YiQi.printColumnBreakDown( "Resolve Evaluation B" );
    }

    public static void final2000() throws Exception {
        // final evaluations on the 2000

        LoadInteractionSpreadsheet allSpreadSheet = getFinal2000Results();
        allSpreadSheet.printStats();

    }

    public static LoadInteractionSpreadsheet getFinal2000Results() throws Exception {
        SpreadSheetSchema schema = new InteractionSchema();

        String allFile = Config.config.getString( "whitetext.iteractions.evaluations.folder" )
                + "Final 2000 evaluation Cathy Base.xls";

        Set<Integer> allRows = new HashSet<Integer>();

        for ( int i = 1; i < 2001; i++ ) {
            allRows.add( i );
        }

        LoadInteractionSpreadsheet allSpreadSheet = new LoadInteractionSpreadsheet( allFile, allRows, "Final2000",
                schema );
        return allSpreadSheet;
    }

    public static LoadInteractionSpreadsheet getFirst6000Results() throws Exception {
        return get6000Results( "/evaluations for Recall/full 6000 set/All Combined.final.forLoading.xls" );
    }

    public static LoadInteractionSpreadsheet getSecond6000Results() throws Exception {
        return get6000Results( "/evaluations for Recall/second 6000 set/All Combined.final.forLoading.xls" );
    }

    public static LoadInteractionSpreadsheet get6000Results( String path ) throws Exception {
        SpreadSheetSchema schema = new ForRecallEvaluationSchema();

        String allFile = Config.config.getString( "whitetext.iteractions.evaluations.folder" ) + path;

        Set<Integer> allRows = new HashSet<Integer>();

        for ( int i = 1; i < 6002; i++ ) {
            allRows.add( i );
        }

        LoadInteractionSpreadsheet allSpreadSheet = new LoadInteractionSpreadsheet( allFile, allRows, "First6000",
                schema );
        return allSpreadSheet;
    }

    public static LoadInteractionSpreadsheet getNotInBAMSResults() throws Exception {
        SpreadSheetSchema schema = new NotInBAMSSchema();

        String allFile = Config.config.getString( "whitetext.iteractions.evaluations.folder" )
                + "Not in BAMS/disagreement evaluations/NotInBAMSSortedCombined.Reviewed.for.paper.sorted.forLoading.xls";

        Set<Integer> allRows = new HashSet<Integer>();

        for ( int i = 1; i < 900; i++ ) {
            allRows.add( i );
        }

        LoadInteractionSpreadsheet allSpreadSheet = new LoadInteractionSpreadsheet( allFile, allRows, "NotInBAMS",
                schema );
        return allSpreadSheet;
    }


    public static void notInBAMS() throws Exception {
        AllCuratorsCombined curators = new AllCuratorsCombined();
        curators.loadNotInBAMSSet();
        curators.printAllStats();
        curators.resolveColumns();

        System.out.println( "Warning: disagreements are operating on pairID level" );
        // curators.printDisagreements( "Afferent/Incoming" );
        curators.printDisagreements( "Reject" );
        // curators.printDisagreements( "Ambiguous" );
        // curators.printDisagreements( "Named entity error" );

        curators.compareToNewBAMS();
        System.exit( 1 );

        curators.showDisagreements( "Reject" );

        log.info( "All accepted pairs:" + curators.getAllAcceptedPairs().size() + " of "
                + curators.getAllPairs().size() );
        log.info( "getAcceptedTwice:" + curators.getAcceptedTwice().size() );
    }

    public static void firstSet() throws Exception {
        AllCuratorsCombined curators = new AllCuratorsCombined();
        curators.loadFirstSet();
        curators.printAllStats();
        curators.resolveColumns();

        System.out.println( "Warning: disagreements are operating on pairID level" );
        // curators.printDisagreements( "Afferent/Incoming" );
        curators.printDisagreements( "Reject" );
        // curators.printDisagreements( "Ambiguous" );
        // curators.printDisagreements( "Named entity error" );

        System.exit( 1 );

        curators.showDisagreements( "Reject" );

        log.info( "All accepted pairs:" + curators.getAllAcceptedPairs().size() + " of "
                + curators.getAllPairs().size() );
        log.info( "getAcceptedTwice:" + curators.getAcceptedTwice().size() );
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        // firstSet();
        LoadInteractionSpreadsheet x;
        x = getNotInBAMSResults();
        log.info( x.getAcceptedPairs().size() );
        log.info( x.getAllPairs().size() );
        log.info( x.getAcceptedPairs().iterator().next().toString() );
        // final2000();
        // notInBAMS();
    }
}

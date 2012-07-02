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

import java.util.HashSet;
import java.util.Set;

import ubic.basecode.io.excel.ExcelUtil;
import ubic.basecode.io.excel.SpreadSheetSchema;
import ubic.pubmedgate.interactions.evaluation.LoadInteractionSpreadsheet;

public class LoadInteractionRecallSpreadsheet extends LoadInteractionSpreadsheet {
    public LoadInteractionRecallSpreadsheet( String filename, Set<Integer> rows, String name, SpreadSheetSchema schema )
            throws Exception {
        super( filename, rows, name, schema );
        removePrevouslyAnnotated();
    }

    public void removePrevouslyAnnotated() {
        Set<Integer> result = new HashSet<Integer>();
        for ( int row : rows ) {
            String value = ExcelUtil.getValue( sheet, row, schema.getPosition( "Previous Annotation" ) );
            if ( value == null || value.equals( "" ) || value.equals( "Not done" ) ) {
                result.add( row );
            }
        }
        rows = result;
    }

    public void retainPairs( Set<String> pairsToRetain ) {
        Set<Integer> rowsToRetain = new HashSet<Integer>();
        for ( int row : rows ) {
            String pairID = ExcelUtil.getValue( sheet, row, schema.getPosition( "PairID" ) );
            if ( pairsToRetain.contains( pairID ) ) rowsToRetain.add( row );
        }
        rows.retainAll( rowsToRetain );
    }

    public void printStats() throws Exception {
        int size = rows.size();
        System.out.println( "Name:" + name );
        System.out.println( " Pair count:" + getAllPairs().size() );
        // System.out.println( " Pair count:" + getAllPairs().size() );
        printLine( "Accept" );
        printLine( "Named entity error" );
        printLine( "Abbreviation error" );
        printLine( "Ambiguous" );
        printLine( "Comment" );
        System.out.println();
    }
}

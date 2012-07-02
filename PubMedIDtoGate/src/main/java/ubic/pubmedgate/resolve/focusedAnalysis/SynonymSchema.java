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

import java.util.Arrays;

import ubic.basecode.io.excel.SpreadSheetSchema;
import ubic.pubmedgate.resolve.evaluation.MappingSchema;

public class SynonymSchema extends SpreadSheetSchema {

    public SynonymSchema() {
        super();
        positions.put( "BAMSName", 0 );
        positions.put( "Term", 1 );
        positions.put( "Concept", 2 );
        positions.put( "Accept", 3 );
        positions.put( "ConceptURI", 4 );
        positions.put( "Resolves", 5 );
        positions.put( "InNIF", 6 );
    }

    public static void main( String[] args ) {
        SynonymSchema test = new SynonymSchema();
        System.out.println( "Header:" + Arrays.asList( test.getHeaderRow() ).toString() );
    }

}

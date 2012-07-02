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

package ubic.pubmedgate.resolve.evaluation;

import java.util.Arrays;

import ubic.basecode.io.excel.SpreadSheetSchema;

public class MappingSchema extends SpreadSheetSchema {

    public MappingSchema() {
        super();
        positions.put( "Mention", 0 );
        positions.put( "Term", 1 );
        positions.put( "Concept", 2 );
        positions.put( "Context", 3 );
        positions.put( "Reject", 4 );
        positions.put( "Specific to General", 5 );
        positions.put( "Comment", 6 );
        positions.put( "Freq", 7 );
        // positions.put( "Set", 3 );
        positions.put( "ShortNames", 8 );
        positions.put( "URIs", 9 );
        positions.put( "PredicatesShort", 10 );
        positions.put( "Predicates", 11 );
    }

    public static void main( String[] args ) {
        MappingSchema test = new MappingSchema();
        System.out.println( "Header:" + Arrays.asList( test.getHeaderRow() ).toString() );
    }

}

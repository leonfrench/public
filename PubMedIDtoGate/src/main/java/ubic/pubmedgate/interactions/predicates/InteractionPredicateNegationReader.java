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

package ubic.pubmedgate.interactions.predicates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFSheet;

import ubic.basecode.io.excel.ExcelUtil;
import ubic.pubmedgate.Config;

public class InteractionPredicateNegationReader {
    protected static Log log = LogFactory.getLog( InteractionPredicateNegationReader.class );

    Map<String, Boolean> decisions;

    public InteractionPredicateNegationReader() throws Exception {

        decisions = new HashMap<String, Boolean>();
        String filename = Config.config.getString( "whitetext.iteractions.predicateNegation" );
        HSSFSheet sheet = ExcelUtil.getSheetFromFile( filename, "Sheet1" );

        boolean header = true;
        boolean clean = false;
        List<String> predicates = ExcelUtil.grabColumnValuesList( sheet, 0, header, clean );
        List<String> marks = ExcelUtil.grabColumnValuesList( sheet, 2, header, clean );

        for ( int i = 0; i < predicates.size(); i++ ) {
            decisions.put( predicates.get( i ), marks.get( i ).equals( "Yes" ) );
        }
        log.info( predicates.size() );
        log.info( marks.size() );
    }

    public boolean filter( String predicate ) {
        Boolean filter = decisions.get( predicate );
        if ( filter == null ) filter = false;
        return filter;
    }

    public static void main( String[] args ) throws Exception {
        // TODO Auto-generated method stub
        InteractionPredicateNegationReader x = new InteractionPredicateNegationReader();

    }

}

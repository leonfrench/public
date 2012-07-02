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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.pubmedgate.resolve.ResolutionRDFModel;

public class MappingSpreadSheetTests {
    protected static Log log = LogFactory.getLog( MappingSpreadSheetTests.class );

    public static void loadEverythingAndMakeSpreadSheet( boolean removeExactMatches ) throws Exception {

        ResolutionRDFModel resolutionModel = new ResolutionRDFModel();
        resolutionModel.getStats();

        resolutionModel.loadManualMatches();

        // loading in evaluations!
        resolutionModel.loadManualEvaluations(); // first, second and third round evaluations
        resolutionModel.loadAutomaticEvaluations();

        resolutionModel.getStats();

        resolutionModel.createMatches3();

        String filename = "loadEverythingAndMakeSpreadSheet.xls";
        MappingSpreadSheet sheet = new MappingSpreadSheet( filename );
        boolean unMatched = false;
        boolean specToGen = false;
        sheet.populate( resolutionModel, unMatched, removeExactMatches, specToGen );
        sheet.save();

        log.info( "Wrote to:" + filename );

        // resolutionModel.writeOut( Config.config.getString( "resolve.Lexicon.finalModel.RDF" ) );
    }

    public static void main( String[] args ) throws Exception {
        boolean removeExactMatches = false;
        loadEverythingAndMakeSpreadSheet( removeExactMatches );
    }

}

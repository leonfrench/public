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

import ubic.basecode.io.excel.CreateSpreadSheet;
import ubic.basecode.io.excel.SpreadSheetSchema;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.resolve.ResolutionRDFModel;

public class CreateUnmatchedMentionsSpreadSheet extends CreateSpreadSheet {
    public CreateUnmatchedMentionsSpreadSheet( String filename ) throws Exception {
        super( filename, new UnMatchedSchema() );
    }

    public static void main( String args[] ) throws Exception {
        String filename = Config.config.getString( "resolve.Lexicon.finalModel.RDF" );
        filename = "/grp/java/workspace/PubMedIDtoGate/rdf/ResolutionRDF.allComp.allResolvers.malletOnly.rdf";
        ResolutionRDFModel model = new ResolutionRDFModel( filename );
        model.getStats();

        MappingSpreadSheet spreadSheet = new MappingSpreadSheet( "/grp/java/workspace/PubMedIDtoGate/UnmatchedMalletAllComp.xls" );
        boolean unMatched = true;

        boolean removeExactMatches = false;
        boolean specToGen = false;

        spreadSheet.populate( model, unMatched, removeExactMatches, specToGen );
        spreadSheet.save();
    }

}

class UnMatchedSchema extends SpreadSheetSchema {
    public UnMatchedSchema() {
        super();
        positions.put( "Mention", 0 );
        positions.put( "Context", 1 );
        positions.put( "Freq", 2 );
    }
}

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

package ubic.pubmedgate.organism;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.basecode.dataStructure.params.ParamKeeper;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;

/**
 * This class dumps out(to spreadsheet) speciesID's and strings that matched so they can be filtered (manually). This is
 * loaded by SpeciesLoader.java. A better way would be to use the NCBI taxonomy API to find out if things are mammals
 * and what the common names are.
 * 
 * @author leon
 */
public class CreateSpreadSheetForSpeciesLoader {
    protected static Log log = LogFactory.getLog( CreateSpreadSheetForSpeciesLoader.class );

    public CreateSpreadSheetForSpeciesLoader() throws Exception {

    }

    public void findNewSpeciesIDs( List<ConnectionsDocument> docs ) throws Exception {
        // scan all documents for species that are not in the loader

        // load the ones we know about
        SpeciesLoader loader = new SpeciesLoader();
        Set<String> knownIDs = loader.getAllIDs();

        SpeciesUtil.SpeciesCountResult speciesMap = SpeciesUtil.getSpeciesStrings( docs );
        Set<String> ids = new HashSet<String>( speciesMap.strings.keySet() );
        ids.removeAll( knownIDs );

        Map<String, String> params;
        ParamKeeper keeper = new ParamKeeper();

        for ( String id : ids ) {
            String names = speciesMap.strings.get( id ).toString();
            params = new HashMap<String, String>();
            params.put( "Species", id );
            params.put( "SpeciesText", names );
            params.put( "Count", speciesMap.counts.get( id ) + "" );
            keeper.addParamInstance( params );
        }

        String filename = Config.config.getString( "whitetext.resolve.manual.species.filter" ) + ".missing.xls";
        keeper.writeExcel( filename );
    }

    public static void main( String[] args ) throws Exception {
        CreateSpreadSheetForSpeciesLoader sheet = new CreateSpreadSheetForSpeciesLoader();
        GateInterface p2g = new GateInterface();

        List<ConnectionsDocument> docs = new LinkedList<ConnectionsDocument>();
        docs.addAll( GateInterface.getDocuments( p2g.getUnseenCorp() ) );
        docs.addAll( GateInterface.getDocuments( p2g.getCorp() ) );
        docs.addAll( GateInterface.getDocuments( p2g.getCorpusByName( "PubMedUnseenJCN" ) ) );
        docs.addAll( GateInterface.getDocuments( p2g.getCorpusByName( "PubMedUnseenMScan1" ) ) );
        log.info( "Total docs to use:" + docs.size() );
        sheet.findNewSpeciesIDs( docs );
    }
}

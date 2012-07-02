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
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFSheet;

import ubic.BAMSandAllen.Vocabulary;
import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.basecode.io.excel.ExcelUtil;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

public class SpeciesLoader {

    // can turn species ID into species names
    // knows if a species should be filtered

    protected static Log log = LogFactory.getLog( SpeciesLoader.class );

    Set<String> filterOutIDs;
    Map<String, String> IDtoNames, IDtoSingleName;
    StringToStringSetMap commentToIDs;

    public Set<String> getFilteredIDs() {
        return filterOutIDs;
    }

    public SpeciesLoader() throws Exception {
        String excelFileName = Config.config.getString( "whitetext.resolve.manual.species.filter" );

        filterOutIDs = new HashSet<String>();
        IDtoNames = new HashMap<String, String>();
        IDtoSingleName = new HashMap<String, String>();
        commentToIDs = new StringToStringSetMap();

        HSSFSheet sheet = ExcelUtil.getSheetFromFile( excelFileName, "Sheet1" );

        int row = 1;
        int namePos = 0;
        int IDPos = 1;
        int filterPos = 2;

        while ( true ) {
            String names = ExcelUtil.getValue( sheet, row, namePos );
            if ( names == null ) break;
            String ID = ExcelUtil.getValue( sheet, row, IDPos );
            String filter = ExcelUtil.getValue( sheet, row, filterPos );
            if ( filter != null && filter.equals( "Yes" ) ) filterOutIDs.add( ID );
            // log.info( name + ID + filter );
            IDtoNames.put( ID, names );

            // find the shortest species string
            IDtoSingleName.put( ID, getSingleName( names ) );
            // log.info( "shortname:" + getSingleName( names ) );

            row++;
        }
        log.info( "Filtered out species IDs:" + filterOutIDs.size() );
        log.info( "IDs total:" + IDtoNames.size() );

        excelFileName = Config.config.getString( "whitetext.resolve.annotated.species" );
        sheet = ExcelUtil.getSheetFromFile( excelFileName, "Sheet1" );
        row = 1;
        while ( true ) {
            String name = ExcelUtil.getValue( sheet, row, namePos );
            if ( name == null ) break;
            String ID = ExcelUtil.getValue( sheet, row, IDPos );
            if ( ID != null ) {
                commentToIDs.put( name, ID );
            }
            row++;
        }

        log.info( "Annotated Species IDs:" + row );
        log.info( "               mapped:" + commentToIDs.size() );

    }

    public String getSingleName( String names ) {
        StringTokenizer toker = new StringTokenizer( names, "[]," );
        String shortest = toker.nextToken().toLowerCase().trim();
        while ( toker.hasMoreElements() ) {
            String current = toker.nextToken().toLowerCase().trim();
            // skip po as it's linked to rat.
            if ( current.equals( "po" ) ) continue;
            // skip 'man' so human is used
            if ( current.equals( "man" ) ) continue;
            if ( current.length() < shortest.length() ) shortest = current;
        }
        return shortest;
    }

    public void addToModel( ConnectionsDocument doc, Model model ) {
        Set<String> speciesSet = doc.getLinnaeusSpecies();
        speciesSet.removeAll( filterOutIDs );
        // filter
        String pmid = doc.getPMID();
        Resource docResource = model.createResource( Vocabulary.getpubmedURIPrefix() + pmid );
        for ( String species : speciesSet ) {
            // we only want the taxon ID at the end
            String fullSpeciesID = species;
            species = species.substring( species.lastIndexOf( ":" ) + 1 );
            Resource taxon = model.createResource( "http://bio2rdf.org/taxon:" + species );
            // for the MScanner sets we don't have species names so the below line returns null
            String singleNameFromID = getSingleNameFromID( fullSpeciesID );
            if ( singleNameFromID == null ) {
                log.info( species + " has null single name label" );
            } else {
                taxon.addLiteral( RDFS.label, singleNameFromID );
            }
            docResource.addProperty( Vocabulary.mentions_species, taxon );
//            log.info( docResource.getURI() + "->" + taxon.getURI() );
        }
    }

    public String getTaggedNamesFromID( String ID ) {
        return IDtoNames.get( ID );
    }

    public String getSingleNameFromID( String ID ) {
        return IDtoSingleName.get( ID );
    }

    public Set<String> getAllIDs() {
        return new HashSet<String>( IDtoNames.keySet() );
    }

    public boolean isFiltered( String ID ) {
        return filterOutIDs.contains( ID );
    }

    public Set<String> getIDfromAnnotatedSpecies( String species ) {
        return commentToIDs.get( species );
    }

    // requires mapping of annotated species to IDs

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        SpeciesLoader loader = new SpeciesLoader();
        log.info( loader.getTaggedNamesFromID( "species:ncbi:10116" ).toString() );

    }

}

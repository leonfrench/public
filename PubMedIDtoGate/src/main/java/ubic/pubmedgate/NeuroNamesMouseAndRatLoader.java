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

package ubic.pubmedgate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFSheet;

import ubic.BAMSandAllen.LexiconSource;
import ubic.BAMSandAllen.Vocabulary;
import ubic.basecode.io.excel.ExcelUtil;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class NeuroNamesMouseAndRatLoader implements LexiconSource {
    boolean toLower = false;
    protected static Log log = LogFactory.getLog( NeuroNamesMouseAndRatLoader.class );

    int BLANK_DONG_ROW = 355;

    HSSFSheet paxfrank;
    HSSFSheet hof;
    HSSFSheet swanson;
    HSSFSheet dong;
    // column A
    int TERM_COLUMN = 0;
    // column B
    int ABBREV_COLUMN = 1;
    int NNNAME_COLUMN = 2;
    int DIR_COLUMN = 3;
    int ID_COLUMN = 4;

    public NeuroNamesMouseAndRatLoader() {
        // get all the sheets
        String filename = Config.config.getString( "whitetext.neuronames2007.location" );
        try {
            paxfrank = ExcelUtil.getSheetFromFile( filename, "Paxinos & Franklin" );
            hof = ExcelUtil.getSheetFromFile( filename, "Hof et al." );
            swanson = ExcelUtil.getSheetFromFile( filename, "Swanson" );
            dong = ExcelUtil.getSheetFromFile( filename, "Dong" );
            log.info( ExcelUtil.getValue( dong, BLANK_DONG_ROW, 0 ) );
            log.info( ExcelUtil.grabColumnValues( dong, ID_COLUMN, true, true ).size() );
            log.info( ExcelUtil.grabColumnValues( dong, TERM_COLUMN, true, true ).size() );
            ExcelUtil.setValue( dong, BLANK_DONG_ROW, ID_COLUMN, "X" );
            log.info( ExcelUtil.getValue( dong, BLANK_DONG_ROW, ID_COLUMN ) );
            log.info( ExcelUtil.getValue( dong, BLANK_DONG_ROW, TERM_COLUMN ) );

            log.info( "Dong Term size:" + ExcelUtil.grabColumnValuesList( dong, ID_COLUMN, true, toLower ).size() );
            log.info( "Dong ID size:" + ExcelUtil.grabColumnValues( dong, TERM_COLUMN, true, true ).size() );
            ExcelUtil.setValue( dong, BLANK_DONG_ROW, DIR_COLUMN, "X" );
            ExcelUtil.setValue( dong, BLANK_DONG_ROW, NNNAME_COLUMN, "X" );
        } catch ( Exception e ) {
            e.printStackTrace();
        }

    }

    // this includes Latin and abbreviations
    public Set<String> getRegionsForLexicon() {
        Set<String> result = new HashSet<String>();
        result.addAll( ExcelUtil.grabColumnValues( paxfrank, TERM_COLUMN, true, true ) );
        result.addAll( ExcelUtil.grabColumnValues( hof, TERM_COLUMN, true, true ) );
        result.addAll( ExcelUtil.grabColumnValues( swanson, TERM_COLUMN, true, true ) );
        result.addAll( ExcelUtil.grabColumnValues( dong, TERM_COLUMN, true, true ) );
        log.info( result.size() );
        return result;
    }

    public void addToModel( Model model ) {
        addSheetToModel( model, paxfrank, Vocabulary.has_Paxinos_Franklin_term );
        addSheetToModel( model, hof, Vocabulary.has_Hof_term );
        addSheetToModel( model, swanson, Vocabulary.has_Swanson_term );
        log.info( "Dong" );
        addSheetToModel( model, dong, Vocabulary.has_Dong_term );
    }

    private void addSheetToModel( Model model, HSSFSheet sheet, Property prop ) {
        List<String> names = ExcelUtil.grabColumnValuesList( sheet, TERM_COLUMN, true, toLower );
        List<String> NNNames = ExcelUtil.grabColumnValuesList( sheet, NNNAME_COLUMN, true, toLower );

        List<String> dirs = ExcelUtil.grabColumnValuesList( sheet, DIR_COLUMN, true, toLower );
        List<String> IDs = ExcelUtil.grabColumnValuesList( sheet, ID_COLUMN, true, toLower );
        int badMappings = 0;
        log.info( "Bad mappings:" + badMappings + " of " + IDs.size() + ", " + names.size() );
        for ( int i = 0; i < IDs.size(); i++ ) {
            // one row in the Dong sheet has a blank entry
            if ( sheet.equals( dong ) && i == BLANK_DONG_ROW - 1 ) continue;

            int idNum = ( int ) Double.parseDouble( IDs.get( i ) );
            String id = dirs.get( i ) + idNum;
            String name = names.get( i );
            String NNName = NNNames.get( i );
            Resource r = model.createResource( Vocabulary.getNNURI() + id );
            if ( !model.contains( r, RDF.type, Vocabulary.neuroname ) ) {
                badMappings++;
                // log.info( "Bad mapping: " + name + " " + id );
                // make a neuroname entry for it
                r.addLiteral( RDFS.label, NNName );
                r.addProperty( RDF.type, Vocabulary.neuroname );
            }
            r.addProperty( prop, Vocabulary.makeNeurotermNode( name, model ) );
        }
        log.info( "Bad mappings:" + badMappings + " of " + IDs.size() + ", " + names.size() );
    }

    public Set<String> getAbbreviationsForLexicon() {
        Set<String> result = new HashSet<String>();
        result.addAll( ExcelUtil.grabColumnValues( paxfrank, ABBREV_COLUMN, true, true ) );
        result.addAll( ExcelUtil.grabColumnValues( hof, ABBREV_COLUMN, true, true ) );
        result.addAll( ExcelUtil.grabColumnValues( swanson, ABBREV_COLUMN, true, true ) );
        result.addAll( ExcelUtil.grabColumnValues( dong, ABBREV_COLUMN, true, true ) );

        return result;
    }

    public static void main( String argsp[] ) throws Exception {
        NeuroNamesMouseAndRatLoader loader = new NeuroNamesMouseAndRatLoader();
        System.out.println( "Loaded terms = " + loader.getRegionsForLexicon().size() );
    }

}

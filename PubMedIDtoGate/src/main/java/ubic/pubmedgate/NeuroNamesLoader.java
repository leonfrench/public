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
import ubic.basecode.io.excel.SpreadSheetFilter;
import ubic.pubmedgate.resolve.ResolutionRDFModel;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/*
 * Loads in NN2010.xls file, right now it just grabs all the names (for now)
 */
public class NeuroNamesLoader implements LexiconSource {
    protected static Log log = LogFactory.getLog( NeuroNamesLoader.class );
    boolean toLower = false;

    HSSFSheet Synonyms;
    HSSFSheet AncillaryStructures;
    HSSFSheet ClassicalStructures;

    // for All sheets
    int ID_COLUMN = 0;
    int TERM_COLUMN = 1;

    // for "Classical" sheet only
    int LATIN_COLUMN = 2;
    int ABBREV_COLUMN = 3;

    // for Synonyms sheet only
    int SYN_LANG_COLUMN = 2;
    int SYN_MAPPED_ID = 4;

    // for ancillary structures sheet only
    int ANC_ABBREV = 2;

    // int ALOOKUP_COLUMN = 5; //not used in 2010 version

    public NeuroNamesLoader() {
        // get all the sheets
        String filename = Config.config.getString( "whitetext.neuronames2010.location" );
        try {
            Synonyms = ExcelUtil.getSheetFromFile( filename, "Synonyms" );
            AncillaryStructures = ExcelUtil.getSheetFromFile( filename, "Ancillary Structures" );
            ClassicalStructures = ExcelUtil.getSheetFromFile( filename, "Classical Nervous System" );
        } catch ( Exception e ) {
            e.printStackTrace();
        }

    }

    // sources of names
    // Synonyms -> languages - English, Latin, Abbreviation
    // AncillaryStructures
    // HierarchyStructures - english latin abbreviation

    // this includes Latin and excludes abbreviations
    /*
     * get all the terms, lowercased and trimmed
     */
    public Set<String> getRegionsForLexicon() {
        Set<String> result = new HashSet<String>();
        boolean header = true;
        boolean clean = true;
        result.addAll( ExcelUtil.grabColumnValues( ClassicalStructures, TERM_COLUMN, header, clean ) );
        log.info( "Added main terms / concepts:" + result.size() );
        result.addAll( ExcelUtil.grabColumnValues( AncillaryStructures, TERM_COLUMN, header, clean ) );
        log.info( "Added Ancillary:" + result.size() );
        result.addAll( ExcelUtil.grabColumnValues( Synonyms, TERM_COLUMN, header, clean, getAbbrevFilter() ) );
        log.info( "Added Synonyms:" + result.size() );
        result.addAll( ExcelUtil.grabColumnValues( ClassicalStructures, LATIN_COLUMN, header, clean ) );
        log.info( "Added latin:" + result.size() );
        return result;
    }

    public void addClassicalStructuresToModel( Model model ) {
        List<String> IDs = ExcelUtil.grabColumnValuesList( ClassicalStructures, ID_COLUMN, true, toLower );
        List<String> names = ExcelUtil.grabColumnValuesList( ClassicalStructures, TERM_COLUMN, true, toLower );
        List<String> latins = ExcelUtil.grabColumnValuesList( ClassicalStructures, LATIN_COLUMN, true, toLower );
        if ( IDs.size() != names.size() ) throw new RuntimeException( "ID and name lengths do not match" );
        for ( int i = 0; i < IDs.size(); i++ ) {
            String name = names.get( i );
            String latin = latins.get( i );
            Resource r = model.createResource( getNNURIfromID( IDs.get( i ) ) );
            r.addLiteral( RDFS.label, name );
            r.addProperty( RDF.type, Vocabulary.neuroname );
            r.addProperty( Vocabulary.has_label_term, Vocabulary.makeNeurotermNode( name, model ) );
            r.addProperty( Vocabulary.has_NN_latin_term, Vocabulary.makeNeurotermNode( latin, model ) );
        }
    }

    public String getNNURIfromID( String strID ) {
        int id = ( int ) Double.parseDouble( strID );
        return Vocabulary.getNNURI() + id;
    }

    public void addAncillaryStructuresToModel( Model model ) {
        List<String> IDs = ExcelUtil.grabColumnValuesList( AncillaryStructures, ID_COLUMN, true, toLower );
        List<String> names = ExcelUtil.grabColumnValuesList( AncillaryStructures, TERM_COLUMN, true, toLower );
        if ( IDs.size() != names.size() ) throw new RuntimeException( "ID and name lengths do not match" );
        for ( int i = 0; i < IDs.size(); i++ ) {
            int id = ( int ) Double.parseDouble( IDs.get( i ) );
            String name = names.get( i );
            Resource r = model.createResource( Vocabulary.getNNURI() + id );
            r.addProperty( RDF.type, Vocabulary.neuroname );
            r.addLiteral( RDFS.label, name );
            r.addProperty( Vocabulary.has_label_term, Vocabulary.makeNeurotermNode( name, model ) );
        }
    }

    public void addSynonymsToModel( Model model ) throws Exception {
        boolean header = true;

        List<String> names = ExcelUtil.grabColumnValuesList( Synonyms, TERM_COLUMN, true, toLower, getAbbrevFilter() );
        List<String> classicalIDs = ExcelUtil.grabColumnValuesList( Synonyms, SYN_MAPPED_ID, header, toLower,
                getAbbrevFilter() );

        log.info( "Synonyms:" + names.size() );
        log.info( "ID's:" + classicalIDs.size() );
        int nullCount = 0;
        for ( int i = 0; i < names.size(); i++ ) {
            String name = names.get( i );
            String classicalID = classicalIDs.get( i );
            if ( classicalID.equals( "" ) ) {
                log.info( "No lookup available" );
                classicalID = null;
            }

            Resource r = model.createResource( getNNURIfromID( classicalID ) );
            if ( r == null ) {
                log.info( "No Neuronames Lookup entry: " + classicalID + " name: " + name );
                nullCount++;
            } else {
                r.addProperty( Vocabulary.has_synonym_term, Vocabulary.makeNeurotermNode( name, model ) );
            }
        }
        log.info( "Done, synonyms not mapped: " + nullCount );
    }

    public Set<String> getAbbreviationsForLexicon() {
        Set<String> result = new HashSet<String>();
        result.addAll( ExcelUtil.grabColumnValues( Synonyms, TERM_COLUMN, true, true, getNotAbbrevFilter() ) );
        result.addAll( ExcelUtil.grabColumnValues( ClassicalStructures, ABBREV_COLUMN, true, true ) );
        return result;
    }

    public SpreadSheetFilter getAbbrevFilter() {
        return new SpreadSheetFilter() {
            public boolean accept( HSSFSheet sheet, int row ) {
                // reject abbreviations
                return !ExcelUtil.getValue( sheet, row, SYN_LANG_COLUMN ).equals( "acronym" );
            }
        };
    }

    public SpreadSheetFilter getNotAbbrevFilter() {
        return new SpreadSheetFilter() {
            public boolean accept( HSSFSheet sheet, int row ) {
                // accept abbreviations
                return ExcelUtil.getValue( sheet, row, SYN_LANG_COLUMN ).equals( "acronym" );
            }
        };
    }

    public static void main( String argsp[] ) throws Exception {
        NeuroNamesLoader loader = new NeuroNamesLoader();
        System.out.println( "Loaded terms = " + loader.getRegionsForLexicon().size() );
        System.out.println( loader.getRegionsForLexicon() );
    }

}

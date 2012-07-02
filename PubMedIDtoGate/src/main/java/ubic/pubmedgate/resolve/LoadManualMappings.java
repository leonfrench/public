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

package ubic.pubmedgate.resolve;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFSheet;

import ubic.BAMSandAllen.JenaUtil;
import ubic.BAMSandAllen.Vocabulary;
import ubic.basecode.io.excel.ExcelUtil;
import ubic.pubmedgate.Config;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

public class LoadManualMappings {
    protected static Log log = LogFactory.getLog( LoadManualMappings.class );

    HSSFSheet resolutions;
    int FIRST_URL_COL = 2;

    public LoadManualMappings() {
        try {
            resolutions = ExcelUtil.getSheetFromFile( Config.config.getString( "whitetext.manual.resolutions" ),
                    "Sheet1" );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    public void addToModel( Model model ) {
        // check mapping one and on..
        // skip header
        int row = 0;
        outer: while ( true ) {
            // go through all the rows
            row++;
            // no rows left
            String term = ExcelUtil.getValue( resolutions, row, 0 );
            if ( term == null ) break outer;
            // up to four mapping coulmns
            for ( int col = FIRST_URL_COL; col < FIRST_URL_COL + 4; col++ ) {
                String URL = ExcelUtil.getValue( resolutions, row, col );
                if ( URL == null ) continue outer;
                // remove the angle brakets
                URL = URL.replace( "<", "" );
                URL = URL.replace( ">", "" );
                // log.info( row + " " + col + " " + URL );
                // term becomes a neuromention resource
                Resource neuroterm = Vocabulary.makeNeurotermNode( term, model );
                Resource neuroConcept = model.createResource( URL );
                if ( !model.contains( neuroConcept, RDFS.label, ( RDFNode ) null ) ) {
                    log.info( "Missing " + URL );
                } else {
                    String URLLabel = JenaUtil.getLabel( neuroConcept );
                    log.info( term + " -> " + URLLabel );
                }
                if ( !model.contains( neuroterm, RDFS.label, ( RDFNode ) null ) ) {
                    log.info( "Missing term " + neuroterm.toString() );
                }

                neuroConcept.addProperty( Vocabulary.has_manual_link, neuroterm );
            }
        }

    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        LoadManualMappings test = new LoadManualMappings();
        Model model = ModelFactory.createDefaultModel();
        test.addToModel( model );
        log.info( model.size() );
    }

}

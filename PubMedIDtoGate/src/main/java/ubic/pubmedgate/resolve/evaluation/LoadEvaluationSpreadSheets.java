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

import java.io.FileWriter;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFSheet;

import ubic.BAMSandAllen.Vocabulary;
import ubic.basecode.io.excel.ExcelUtil;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.resolve.ResolutionRDFModel;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

public class LoadEvaluationSpreadSheets {
    protected static Log log = LogFactory.getLog( LoadEvaluationSpreadSheets.class );
    Model model;
    
    boolean createRDFMentions = false;

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        Model m = ModelFactory.createDefaultModel();
        LoadEvaluationSpreadSheets x = new LoadEvaluationSpreadSheets( m );
        x.loadManual();
        x.loadAutomatic();
    }

    public LoadEvaluationSpreadSheets( Model model ) {
        this.model = model;
    }

    public void loadManual() throws Exception {
        String file = Config.config.getString( "whitetext.resolve.manual.evaluation" );
        loadEvaluationSpreadSheet( file );
        file = Config.config.getString( "whitetext.resolve.manual.evaluation.second" );
        loadEvaluationSpreadSheet( file );
        file = Config.config.getString( "whitetext.resolve.manual.evaluation.third" );
        loadEvaluationSpreadSheet( file );

    }

    public void loadAutomatic() throws Exception {
        String file = Config.config.getString( "whitetext.resolve.automatic.evaluation.loss" );
        loadEvaluationSpreadSheet( file );
        loadAutomaticExact();
    }

    public void loadAutomaticExact() throws Exception {
        String file;
        file = Config.config.getString( "whitetext.resolve.automatic.evaluation.exact" );
        loadEvaluationSpreadSheet( file );
    }

    public void loadEvaluationSpreadSheet( String file ) throws Exception {

        HSSFSheet sheet = ExcelUtil.getSheetFromFile( file, "Sheet0" );
        MappingSchema schema = new MappingSchema();

        int row = 1;
        int nullCount = 0;
        int mentionPos = schema.getPosition( "Mention" );
        int conceptPos = schema.getPosition( "Concept" );
        int rejectPos = schema.getPosition( "Reject" );
        int specToGenPos = schema.getPosition( "Specific to General" );
        int URIsPos = schema.getPosition( "URIs" );

        while ( nullCount < 2 ) {
            String mention = ExcelUtil.getValue( sheet, row, mentionPos );
            String concept = ExcelUtil.getValue( sheet, row, conceptPos );
            String rejectString = ExcelUtil.getValue( sheet, row, rejectPos );
            String specToGenString = ExcelUtil.getValue( sheet, row, specToGenPos );
            String URIs = ExcelUtil.getValue( sheet, row, URIsPos );

            boolean reject = ( rejectString != null && rejectString.equals( "X" ) );
            boolean specToGen = ( specToGenString != null && specToGenString.equals( "X" ) );

            if ( mention == null ) {
                nullCount++;
            } else {
                nullCount = 0;
                // convert mention to neuromention
                Resource mentionNode = Vocabulary.getMentionNode( mention, model );
                boolean containsMentionNode = model.contains( mentionNode, RDFS.label, ( RDFNode ) null ) ;
                if ( !containsMentionNode && !isCreateRDFMentions() ) {
                    log.info( "Missing mention " + mention );
                    // don't load in the evaluation
                } else {
                    if (!containsMentionNode) {
                        log.info( "Missing mention made:" + mention );
                        Vocabulary.makeMentionNode( mention, model );
                    }

                    // convert list of concepts to list of resources
                    // URIs
                    StringTokenizer toks = new StringTokenizer( URIs, "[], " );
                    while ( toks.hasMoreTokens() ) {
                        String conceptURI = toks.nextToken();
                        Resource conceptNode = model.createResource( conceptURI );
                        if ( reject ) {
                            mentionNode.addProperty( Vocabulary.evaluation_reject, conceptNode );
                        } else if ( specToGen ) {
                            mentionNode.addProperty( Vocabulary.evaluation_specific_to_general, conceptNode );
                        } else {
                            // accept
                            mentionNode.addProperty( Vocabulary.evaluation_accept, conceptNode );
                        }
                    }
                }
                // log.info( mention + "->" + concept );
                // log.info( reject + " + " + specToGen );
            }
            row++;
        }
        log.info( "loaded " + row + " rows of evaluations" );
    }

    public boolean isCreateRDFMentions() {
        return createRDFMentions;
    }

    public void setCreateRDFMentions( boolean createRDFMentions ) {
        this.createRDFMentions = createRDFMentions;
    }
}

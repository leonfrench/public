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

import java.io.FileInputStream;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import ubic.pubmedgate.Config;

public class CreateSupplementRDF {
    protected static Log log = LogFactory.getLog( CreateSupplementRDF.class );

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        // load in resolutions
        boolean addSpecies = true;
        boolean reason = true;
        boolean useUnseenCorp = true;

         String file = "resolve.Lexicon.resolution.RDF.allComp";

        //String file = "resolve.Lexicon.RDF.allComp";
        file = Config.config.getString( file );
        Model modelLoad = ModelFactory.createDefaultModel();
        FileInputStream fis = new FileInputStream( file );
        modelLoad.read( fis, null );
        fis.close();

        if ( addSpecies ) {
            log.info( "Adding species info" );
            String storeLoc = Config.config.getString( "whitetext.datastore.location" );
            MakeLexiconRDFModel lexiModel = new MakeLexiconRDFModel();
            lexiModel.setModel( modelLoad );
            lexiModel.addSpeciesToModel( storeLoc, useUnseenCorp );
        }

        EvaluationRDFModel evaluationModel = new EvaluationRDFModel( modelLoad, reason );

        evaluationModel.loadManualMatches();
        evaluationModel.loadManualEvaluations();
        evaluationModel.loadAutomaticEvaluations();
        //
        //
        log.info( "Writing out" );
        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        evaluationModel.writeOut( Config.config.getString( "resolve.supplement.RDF" ) );
        log.info( stopwatch.toString() );
        
        evaluationModel.getStats();

    }
}

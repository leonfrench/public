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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.pubmedgate.Config;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class RunSpeciesStats {
    protected static Log log = LogFactory.getLog( RunSpeciesStats.class );

    public static void main( String[] args ) throws Exception {

        boolean loadManualMatches = true;
        boolean reason = true;
        
        // which set of mentions to use?
        // resolve.Lexicon.resolution.RDF.mallet
        // resolve.Lexicon.resolution.RDF
        //String file = "resolve.Lexicon.resolution.RDF";
        String file = "resolve.Lexicon.resolution.RDF.allComp";
        if ( !loadManualMatches ) file += ".noManualMatches";
        file = Config.config.getString( file );
//        file = "/home/lfrench/WhiteText/rdf/ResolutionRDF.allComp.allResolvers.malletOnly.rdf";
        log.info( file );
        //String annotationSet = "Mallet";
        String annotationSet = "UnionMerge";

        Model modelLoad = ModelFactory.createDefaultModel();
        modelLoad.read( new FileInputStream( file ), null );

        EvaluationRDFModel evaluationModel;
        evaluationModel = new EvaluationRDFModel( modelLoad, reason );

        if ( loadManualMatches ) evaluationModel.loadManualMatches();
        evaluationModel.loadManualEvaluations();
        evaluationModel.loadAutomaticEvaluations();
        
        evaluationModel.getStats();

        log.info( "Here" );
        evaluationModel.printCorpusCoverage();
        
        //find out which were rejected for exactmatcher
        //evaluationModel.findRejectedExact();
        
        
        //evaluationModel.speciesStats(annotationSet);
    }
}

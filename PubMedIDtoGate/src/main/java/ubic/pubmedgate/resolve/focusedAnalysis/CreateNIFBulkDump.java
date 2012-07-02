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

package ubic.pubmedgate.resolve.focusedAnalysis;

import java.io.File;
import java.io.FileInputStream;
import java.util.Set;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.JenaUtil;
import ubic.BAMSandAllen.Util;
import ubic.basecode.util.FileTools;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.resolve.EvaluationRDFModel;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class CreateNIFBulkDump {
    protected static Log log = LogFactory.getLog( CreateNIFBulkDump.class );

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {

        // training or unsupervised
        boolean allComp = true;
        // if training then check for accepted
        Model modelLoad = ModelFactory.createDefaultModel();
        String fileProperty;

        if ( allComp ) {
            fileProperty = "resolve.Lexicon.resolution.RDF.allComp";
            modelLoad.read( new FileInputStream( Config.config.getString( fileProperty ) ), null );
        }

        fileProperty = "resolve.Lexicon.resolution.RDF";
        modelLoad.read( new FileInputStream( Config.config.getString( fileProperty ) ), null );

        boolean reason = true;
        EvaluationRDFModel model = new EvaluationRDFModel( modelLoad, reason );

        int count = 0;
        int rejected = 0;
        String result = "pmid,mention,concept";
        log.info( "total number of pmids:" + model.getPMIDs().size() );
        Set<Resource> NIFSTDConcepts = model.getNIFSTDConcepts();
        Set<Resource> allTerms = model.getTerms();
        int passedPMIDs = 0;
        StopWatch s = new StopWatch();
        s.start();
        Set<Resource> PMIDs = model.getPMIDs();
        for ( Resource pmid : PMIDs ) {

            log.info( "Time per PMID:" + ( s.getTime() / ++passedPMIDs ) + " processed:" + passedPMIDs + " of "
                    + PMIDs.size() );

            String pmidID = pmid.getURI().substring( pmid.getURI().lastIndexOf( ':' ) + 1 );
            log.info( pmidID );
            // PMID -> Mention -> Concept
            Set<Resource> mentions = model.getMentionsInPMID( pmid );
            for ( Resource mention : mentions ) {
                Set<Resource> terms = model.getLinkedResources( mention );
                // it's all resources before it's filtered
                terms.retainAll( allTerms );

                String mentionString = JenaUtil.getLabel( mention );
                for ( Resource term : terms ) {
                    Set<Resource> concepts = model.getConceptsFromTerms( term );
                    String termString = JenaUtil.getLabel( term );

                    concepts.retainAll( NIFSTDConcepts );

                    for ( Resource concept : concepts ) {
                        if ( !model.rejected( mention, concept ) ) {
                            String conceptLabel = JenaUtil.getLabel( concept );
                            String conceptURI = "http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-GrossAnatomy.owl#"
                                    + concept.getLocalName();
                            log.info( "  " + mentionString + "->" + termString + "->" + conceptLabel + "  "
                                    + conceptURI );
                            // write mention, concept and pubmed abstract
                            result += "\r\n" + pmidID + ",\"" + mentionString + "\"," + conceptURI;
                            count++;
                        } else {
                            rejected++;
                        }
                    }
                }
            }
        }
        log.info( "Number printed:" + count );
        log.info( "Number rejected:" + rejected );
        FileTools.stringToFile( result, new File( Config.config.getString( "whitetext.resolve.results.folder" )
                + "forNIFAll.txt" ) );

    }
}

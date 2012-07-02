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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.JenaUtil;
import ubic.BAMSandAllen.LexiconSource;
import ubic.BAMSandAllen.Vocabulary;
import ubic.basecode.ontology.OntologyLoader;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class NIFSTDRegionExtractor implements LexiconSource {
    protected static Log log = LogFactory.getLog( NIFSTDRegionExtractor.class );

    String mainURL = "http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-GrossAnatomy.owl";
    OntModel model;
    Property synonymProp;

    public NIFSTDRegionExtractor() {
        model = OntologyLoader.loadMemoryModel( mainURL );
        
        synonymProp = model
                .createProperty( "http://ontology.neuinfo.org/NIF/Backend/OBO_annotation_properties.owl#synonym" );

        String baseAnatomyURL = "http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-GrossAnatomy.owl";
        // Regional part of eye [birnlex_1145]

    }

    public void addToModel( Model model ) {

        Set<OntClass> regions = getAllRegionClasses();

        int synonyms = 0;
        for ( OntClass region : regions ) {
            String regionLabel = JenaUtil.getLabel( region );

            Resource r = model.createResource( region.getURI() );
            r.addLiteral( RDFS.label, regionLabel );
            // log.info( regionLabel );

            r.addProperty( RDF.type, Vocabulary.BIRNLexname );
            r.addProperty( Vocabulary.has_label_term, Vocabulary.makeNeurotermNode( regionLabel, model ) );

            StmtIterator it = region.listProperties( synonymProp );

            while ( it.hasNext() ) {
                Statement s = it.nextStatement();
                r.addProperty( Vocabulary.has_synonym_term, Vocabulary.makeNeurotermNode( s.getLiteral().getString(),
                        model ) );
                // log.info( "Synonym: " + s.getLiteral().getString() );
                synonyms++;
            }

        }

        log.info( "NIFSTD Total classes:" + regions.size() );
        log.info( "   Synonyms:" + synonyms );
        log.info( "   Total terms (check with Venn):" + ( regions.size() + synonyms ) );

    }

    public Set<OntClass> getSubClasses( String shortName ) {
        OntClass theClass = model.getOntClass( mainURL + shortName );

        ExtendedIterator x = theClass.listSubClasses( false );
        Set<OntClass> result = ( Set<OntClass> ) x.toSet();
        log.info( "Retreived " + result.size() + " subclasses for " + JenaUtil.getLabel( theClass ) );
        return result;

    }
    
    

    private Set<OntClass> getAllRegionClasses() {
        Set<OntClass> regions = new HashSet<OntClass>();
        // // Regional part of brain [birnlex_1167]
        // // Regional part of spine birnlex_2667
        // // Ganglion part of peripheral nervous system #birnlex_2548
        // // Regional part of ear [birnlex_1181]
        // Organ birnlex_4
        String eye = "#birnlex_1145";
        String brain = "#birnlex_1167";
        String spine = "#birnlex_1496";
        String peripheralGanglion = "#birnlex_2548";
        String ear = "#birnlex_1181";
        String organ = "#birnlex_4";

        regions.addAll( getSubClasses( eye ) );
        regions.addAll( getSubClasses( brain ) );
        regions.addAll( getSubClasses( spine ) );
        regions.addAll( getSubClasses( peripheralGanglion ) );
        regions.addAll( getSubClasses( ear ) );
        regions.addAll( getSubClasses( organ ) );
        return regions;
    }

    public Set<String> getRegionsForLexicon() {
        Set<OntClass> regions = getAllRegionClasses();

        Set<String> result = new HashSet<String>();

        for ( OntClass region : regions ) {
            result.add( JenaUtil.getLabel( region ) );

            StmtIterator it = region.listProperties( synonymProp );

            while ( it.hasNext() ) {
                Statement s = it.nextStatement();
                result.add( s.getLiteral().getString() );
            }
        }
        return result;
    }

    public static void main( String[] args ) throws Exception {
        NIFSTDRegionExtractor test = new NIFSTDRegionExtractor();
        Model m = ModelFactory.createDefaultModel();
        test.addToModel( m );
        log.info( "Model size:" + m.size() );
    }
}

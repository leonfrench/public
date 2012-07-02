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

package ubic.pubmedgate.resolve.depreciated;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.LexiconSource;
import ubic.BAMSandAllen.Vocabulary;
import ubic.BAMSandAllen.BAMSDataLoaders.BAMSDataLoader;
import ubic.basecode.ontology.OntologyLoader;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.BirnLexOntologyService;

import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Old NIFSTD/BIRNLex loader, has been replaced by NIFSTDRegionExtractor
 * 
 * @author leon
 *
 */
@Deprecated
public class BIRNLexResolver implements LexiconSource {
    protected static Log log = LogFactory.getLog( BIRNLexResolver.class );

    Model birnLex;
    Set<String> allTerms;

    public BIRNLexResolver() {
        log.info( "Loading:" + getMainURL() );
        if ( getMainURL() == null ) {
            log.info( "Error null BIRNLex URL" );
            System.exit( 1 );
        }
        birnLex = OntologyLoader.loadMemoryModel( getMainURL() );
        allTerms = new HashSet<String>();
        // loadLeixcon();
    }

    // make a map of synonyms and labels to URI, in birnlex... all things?
    // remove brakets?

    private static final String BIRNLEX_ONTOLOGY_URL = "url.birnlexOntology";

    public String getMainURL() {
        // return ConfigUtils.getString( BIRNLEX_ONTOLOGY_URL );
        return "http://ontology.neuinfo.org/BIRNLex/birnlex.owl";
    }

    public Set<String> getRegionsForLexicon() {
        return allTerms;
    }

    public void addToModel( Model model ) {
        BirnLexOntologyService service = new BirnLexOntologyService();
        service.startInitializationThread( true );
        while ( !( service.isOntologyLoaded() ) ) {
            try {
                Thread.sleep( 500 );
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }

        log.info( "Size:" + service.getAllURIs().size() );

        String baseAnatomyURL = "http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-GrossAnatomy.owl";
        // Regional part of eye [birnlex_1145]
        OntologyTerm eye = service.getTerm( baseAnatomyURL + "#birnlex_1145" );
        log.info( eye.getLabel() );

        // Regional part of brain [birnlex_1167]
        OntologyTerm brain = service.getTerm( baseAnatomyURL + "#birnlex_1167" );
        log.info( brain.getLabel() );

        // Regional part of spine birnlex_1496
        OntologyTerm spine = service.getTerm( baseAnatomyURL + "#birnlex_1496" );
        log.info( "Spine:" + spine.getLabel() );

        // Ganglion part of peripheral nervous system
        OntologyTerm peripheralGanglion = service.getTerm( baseAnatomyURL + "#birnlex_2548" );
        log.info( "PeripheralGanglion:" + peripheralGanglion.getLabel() );

        // Regional part of ear [birnlex_1181]
        OntologyTerm ear = service.getTerm( baseAnatomyURL + "#birnlex_1181" );
        log.info( "EAR:" + ear.getLabel() );

        // ORGAN!
        OntologyTerm organ = service.getTerm( "http://ontology.neuinfo.org/NIF/Backend/BIRNLex-OBO-UBO.owl#birnlex_4" );
        log.info( "Organ:" + organ.getLabel() );

        Property synonymProp = birnLex
                .getProperty( "http://ontology.neuinfo.org/NIF/Backend/OBO_annotation_properties.owl#synonym" );
        int fails = 0;
        int conceptCount = 0;
        int synonyms = 0;
        int NNCount = 0;
        for ( String uri : service.getAllURIs() ) {
            log.info( uri );
            OntologyTerm term = service.getTerm( uri );
            log.info( term );
            try {
                Collection<OntologyTerm> parents = term.getParents( false );
                if ( parents.contains( eye ) || parents.contains( ear ) || parents.contains( organ )
                        || parents.contains( brain ) || parents.contains( peripheralGanglion )
                        || parents.contains( spine ) ) {
                    // save syns and label
                    Resource r = model.createResource( term.getUri() );
                    r.addLiteral( RDFS.label, term.getLabel() );
                    log.info( term.getLabel() );

                    conceptCount++;
                    r.addProperty( RDF.type, Vocabulary.BIRNLexname );
                    r.addProperty( Vocabulary.has_label_term, Vocabulary.makeNeurotermNode( term.getLabel(), model ) );

                    Resource rBIRN = birnLex.getResource( uri );
                    Property BIRNLinkProp = birnLex
                            .createProperty( "http://ontology.neuinfo.org/NIF/Backend/BIRNLex_annotation_properties.owl#neuronamesID" );

                    Statement nnLink = rBIRN.getProperty( BIRNLinkProp );

                    if ( nnLink != null ) {
                        // Problem, bad ID mappings on BIRNLex - it's not using the NN2010 ID's
                        String NNURI = Vocabulary.getNNURI() + nnLink.getObject().toString();
                        r.addProperty( Vocabulary.has_NN_link, model.createResource( NNURI ) );
                        NNCount++;
                    }

                    StmtIterator it = rBIRN.listProperties( synonymProp );

                    while ( it.hasNext() ) {
                        Statement s = it.nextStatement();
                        r.addProperty( Vocabulary.has_synonym_term, Vocabulary.makeNeurotermNode( s.getObject()
                                .toString(), model ) );
                        log.info( "Synonym: " + s.getObject().toString() );
                        synonyms++;
                    }

                }

            } catch ( ConversionException e ) {
                e.printStackTrace();
                fails++;
            }
        }
        log.info( "Failed to get parents for:" + fails + " terms" );
        log.info( "Extracted Neuronames Links for:" + NNCount + " terms" );
        log.info( "Concepts:" + conceptCount );
        log.info( "Synonyms:" + synonyms );
        log.info( getMainURL() );

    }

    public Set<String> getSynonyms( String URI ) {
        Set<String> result = new HashSet<String>();
        Property p = birnLex
                .getProperty( "http://purl.org/nbirn/birnlex/ontology/annotation/OBO_annotation_properties.owl#synonym" );
        Resource r = birnLex.getResource( URI );
        StmtIterator it = r.listProperties( p );
        while ( it.hasNext() ) {
            Statement s = it.nextStatement();
            // log.info( s.toString() );
            result.add( s.getObject().toString() );
        }
        return result;
    }

    @Deprecated
    public void loadLeixcon() {
        BirnLexOntologyService service = new BirnLexOntologyService();
        service.startInitializationThread( true );
        while ( !( service.isOntologyLoaded() ) ) {
            try {
                Thread.sleep( 500 );
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }

        // Regional part of eye [birnlex_1145]
        OntologyTerm eye = service.getTerm( "http://purl.org/nbirn/birnlex/ontology/BIRNLex-Anatomy.owl#birnlex_1145" );
        log.info( eye.getLabel() );
        // Regional part of brain [birnlex_1167]
        OntologyTerm brain = service
                .getTerm( "http://purl.org/nbirn/birnlex/ontology/BIRNLex-Anatomy.owl#birnlex_1167" );
        log.info( brain.getLabel() );
        // Regional part of ear [birnlex_1181]
        OntologyTerm ear = service.getTerm( "http://purl.org/nbirn/birnlex/ontology/BIRNLex-Anatomy.owl#birnlex_1181" );
        log.info( ear.getLabel() );

        OntologyTerm spine = service.getTerm( "http://purl.org/nbirn/birnlex/ontology/BIRNLex-OBO-UBO.owl#birnlex_4" );
        log.info( spine.getLabel() );
        int fails = 0;
        for ( String uri : service.getAllURIs() ) {
            OntologyTerm term = service.getTerm( uri );
            try {
                Collection<OntologyTerm> parents = term.getParents( false );
                if ( parents.contains( eye ) || parents.contains( ear ) || parents.contains( spine )
                        || parents.contains( brain ) ) {
                    // save syns and label
                    allTerms.addAll( getSynonyms( uri ) );
                    allTerms.add( term.getLabel() );
                }

            } catch ( ConversionException e ) {
                fails++;
            }
        }
        log.info( "Failed to get parents for:" + fails + " terms" );
        // log.info( allTerms );

    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // TODO Auto-generated method stub
        BAMSDataLoader d = new BAMSDataLoader();

        BIRNLexResolver birn = new BIRNLexResolver();
        Model m = ModelFactory.createDefaultModel();
        log.info( "Model size:" + m.size() );
        // System.exit( 1 );
        birn.addToModel( m );
        log.info( "Model size:" + m.size() );

        // log.info( birn.getRegionsForLexicon().contains( "Superior colliculus" ) );
        // Resolver resolver = new SimpleExactMatcher( birn.getRegionsForLexicon() );
        // log.info( resolver.resolve( "Superior colliculus" ) );
        // log.info( resolver.resolve( "superior colliculus" ) );
        // log.info( resolver.resolve( "spinal cord" ) );

    }
}

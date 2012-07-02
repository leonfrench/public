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

package ubic.pubmedgate.resolve.RDFResolvers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.JenaUtil;
import ubic.BAMSandAllen.Vocabulary;
import ubic.pubmedgate.resolve.ResolutionRDFModel;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class SimpleExactRDFMatcher extends RDFResolverImpl implements RDFResolver {
    // have a bunch of vocabs - BAMS, Allen, Neuronames
    protected static Log log = LogFactory.getLog( RDFResolver.class );
    Property property = Vocabulary.string_match_ignorecase;

    // each cleaned string points to one or more neuroterm resources
    Map<String, Set<Resource>> names;

    public SimpleExactRDFMatcher( Set<Resource> terms ) {
        super();
        names = new HashMap<String, Set<Resource>>();
        for ( Resource term : terms ) {
            String label = JenaUtil.getLabel( term );
            label = processTerm( label );

            Set<Resource> resources = names.get( label );
            if ( resources == null ) {
                resources = new HashSet<Resource>();
                names.put( label, resources );
            }
            resources.add( term );
        }
    }

    public String processTerm( String s ) {
        return s.trim().toLowerCase();
    }

    public String getName() {
        return "Simple Exact Matcher, case insensitive";
    }

    public Property getProperty() {
        return property;
    }

    public void setProperty( Property property ) {
        this.property = property;
    }

    public Set<String> getNames() {
        return names.keySet();
    }

    // given a mention[s] resolve it to neuroterm RDF nodes and make a link in a model that is returned
    public Model resolve( Resource mention ) {
        Model model = ModelFactory.createDefaultModel();
        // each mention may expand to more than one string (e.g. superior and inferior raphe nuclei )
        Set<String> mentionTexts = processMentionString( JenaUtil.getLabel( mention ) );
        // get the matches
        for ( String mentionText : mentionTexts ) {
            Set<Resource> neuroterms = names.get( mentionText );
            if ( neuroterms != null ) {
                // log.info( "Match:" + mentionText );
                for ( Resource neuroterm : neuroterms ) {
                    // make the links
                    Statement triple = model.createStatement( mention, getProperty(), neuroterm );
                    model = model.add( triple );
                }
            }
        }
        return model;
    }

    public static void main( String[] args ) throws Exception {
        ResolutionRDFModel fullModel = new ResolutionRDFModel();
        // fullModel.loadManualMatches();

        Model blankModel = ModelFactory.createDefaultModel();
        Vocabulary.makeMentionNode( "orbital part of inferior frontal gyrus", blankModel );

        log.info( "Blank Model:" );
        ResolutionRDFModel blankRDFModel = new ResolutionRDFModel( blankModel, true );

        blankRDFModel.runOnUnMatched( new SimpleExactRDFMatcher( fullModel.getTerms() ) );
        blankRDFModel.getStats();
        blankRDFModel.writeOut( "test.RDF" );
    }
}

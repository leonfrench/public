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
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.JenaUtil;
import ubic.BAMSandAllen.Vocabulary;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class BagOfWordsRDFMatcher extends RDFResolverImpl implements RDFResolver {
    protected static Log log = LogFactory.getLog( BagOfWordsRDFMatcher.class );

    // a bag of words maps to one or more neuroterms(Resource)
    Map<Set<String>, Set<Resource>> names;

    // delims from Ngrampipefactory
    public static String delims = "~`!@#$%^&*()_-+={[}]|\\:;\"',<.>?/ \t\n\r";

    public String getName() {
        return "Bag of Words exact Matcher";
    }

    public BagOfWordsRDFMatcher( Set<Resource> terms ) {
        super();
        names = new HashMap<Set<String>, Set<Resource>>();
        for ( Resource term : terms ) {
            Set<String> bagOfWords = makeBagOfWords( JenaUtil.getLabel( term ) );

            Set<Resource> resources = names.get( bagOfWords );
            if ( resources == null ) {
                resources = new HashSet<Resource>();
                names.put( bagOfWords, resources );
            }
            resources.add( term );
        }
    }

    
    public Set<String> makeBagOfWords( String phrase ) {
        phrase = phrase.toLowerCase().trim();
        StringTokenizer tokens = new StringTokenizer( phrase, delims, false );
        Set<String> bagOfWords = new HashSet<String>();
        while ( tokens.hasMoreTokens() ) {
            bagOfWords.add( tokens.nextToken() );
        }
        bagOfWords.remove( "the" );
        bagOfWords.remove( "of" );
        return bagOfWords;
    }

    public Property getProperty() {
        return Vocabulary.word_bag_match_ignorecase;
    }

    // given a mention[s] resolve it to neuroterm RDF nodes and make a link in a model that is returned
    public Model resolve( Resource mention ) {
        Model model = ModelFactory.createDefaultModel();
        String mentionText = JenaUtil.getLabel( mention );
        // here the single string expands to more than one (ugly)
        Set<String> editedMentions = processMentionString( mentionText );
        for ( String editedMention : editedMentions ) {
            Set<String> mentionBag = makeBagOfWords( editedMention );
            // get the matches
            Set<Resource> neuroterms = names.get( mentionBag );
            if ( neuroterms != null ) {
                for ( Resource neuroterm : neuroterms ) {
                    // make the links
                    Statement triple = model.createStatement( mention, getProperty(), neuroterm );
                    model = model.add( triple );
                }
            }
        }
        return model;
    }
}

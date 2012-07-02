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

package ubic.pubmedgate.resolve.RDFResolvers.deprecated;

import java.util.HashSet;
import java.util.Set;

import ubic.BAMSandAllen.JenaUtil;
import ubic.BAMSandAllen.Vocabulary;
import ubic.pubmedgate.resolve.ResolutionRDFModel;
import ubic.pubmedgate.resolve.RDFResolvers.SimpleExactRDFMatcher;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class AdjectiveStripRDFResolver extends SimpleExactRDFMatcher {
    Set<String> adjectives;

    public AdjectiveStripRDFResolver( Set<Resource> terms ) throws Exception {
        super( terms );
        adjectives = new HashSet<String>();
        adjectives.add( "left" );
        adjectives.add( "right" );
        adjectives.add( "ipsilateral" );
        adjectives.add( "contralateral" );
    }

    // split the mention using the directions
    /* works on unmatched mentions, doesnt return a string always */

    public Set<String> processMention( String s ) {
        Set<String> result = new HashSet<String>();
        s = s.toLowerCase().trim();
        for ( String adj : adjectives ) {
            if ( s.startsWith( adj + " " ) && !s.equals( adj ) ) {
                String news = s.substring( adj.length() + 1 );
                // log.info( s + "->" + news );
                result.add( news );
            }
        }
        return result;
    }

    public String getName() {
        return "Lateral stripper exact Matcher, case insensitive";
    }

    public Property getProperty() {
        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        ResolutionRDFModel resolutionModel = new ResolutionRDFModel();

        AdjectiveStripRDFResolver matcher = new AdjectiveStripRDFResolver( resolutionModel.getTerms() );

        // how many mentions match?
        for ( Resource r : resolutionModel.getMentions() ) {
            matcher.processMention( JenaUtil.getLabel( r ) );
        }
    }
}

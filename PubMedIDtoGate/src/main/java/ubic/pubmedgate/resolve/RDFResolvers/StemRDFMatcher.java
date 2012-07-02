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

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import kea.LovinsStemmer;
import kea.Stemmer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.Vocabulary;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class StemRDFMatcher extends SimpleExactRDFMatcher implements RDFResolver {
    protected static Log log = LogFactory.getLog( StemRDFMatcher.class );

    static Stemmer stemmer;
    static {
        stemmer = new LovinsStemmer();
    }

    public Set<String> processMentionString( String mention ) {
        Set<String> mentions = super.processMentionString( mention );
        Set<String> result = new HashSet<String>();
        for ( String toStem : mentions ) {
            result.add( stemmer.stemString( toStem ) );
        }
        return result;
    }

    public String processTerm( String s ) {
        // trim and lowercase from superclass
        s = super.processTerm( s );

        // return the stem
        return stemmer.stemString( s );
    }

    public StemRDFMatcher( Set<Resource> terms ) {
        super( terms );
    }

    public String getName() {
        return "Stem RDF Matcher";
    }

    public Property getProperty() {
        return Vocabulary.stem_match;
    }

    public static void main( String args[] ) {
        System.out.println( stemmer.stemString( "ventral striatopallidal parts of the basal ganglia" ) );
        System.out.println( stemmer.stemString( "motor-related parts of the basal ganglia" ) );
        System.out.println( stemmer.stemString( "ventral striatopallidal" ) );
    }
}

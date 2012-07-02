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

public class ThreeLetterRDFMatcher extends SimpleExactRDFMatcher implements RDFResolver {
    protected static Log log = LogFactory.getLog( ThreeLetterRDFMatcher.class );

    public Set<String> processMentionString( String mention ) {
        Set<String> mentions = super.processMentionString( mention );
        Set<String> result = new HashSet<String>();
        for ( String toStem : mentions ) {
            result.add( processThreeLetters( toStem ) );
        }
        return result;
    }

    public static String processThreeLetters( String input ) {
        String result = "";
        input = input.toLowerCase().trim();
        StringTokenizer tokens = new StringTokenizer( input, BagOfWordsRDFMatcher.delims, false );
        while ( tokens.hasMoreTokens() ) {
            String token = tokens.nextToken();
            if ( token.length() < 3 )
                result += token + " ";
            else
                result += token.substring( 0, 3 ) + " ";

        }
        return result.trim();
    }

    public String processTerm( String s ) {
        // trim and lowercase from superclass
        s = super.processTerm( s );

        // return the stem
        return processThreeLetters( s );
    }

    public ThreeLetterRDFMatcher( Set<Resource> terms ) {
        super( terms );
    }

    public String getName() {
        return "3Letter RDF Matcher";
    }

    public Property getProperty() {
        return Vocabulary.three_match;
    }

    public static void main( String args[] ) {
        System.out.println( processThreeLetters( "ventral striatopallidal parts of the basal ganglia" ) );
        System.out.println( processThreeLetters( "motor-related parts of the basal ganglia" ) );
    }

}

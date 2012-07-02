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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ubic.BAMSandAllen.Vocabulary;
import ubic.pubmedgate.resolve.RDFResolvers.SimpleExactRDFMatcher;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class BracketRemoverRDFMatcher extends SimpleExactRDFMatcher {
    public BracketRemoverRDFMatcher( Set<Resource> terms ) throws Exception {
        super( terms );
    }

    public Set<String> processMention( String s ) {
        String olds = s;
        Set<String> result = new HashSet<String>();
        String regex = "\\s?\\((.*?)\\)\\s?";
        Pattern p = Pattern.compile( regex, Pattern.DOTALL );
        Matcher m = p.matcher( s );

        if ( m.find() ) {
            s = m.replaceAll( " " );
            log.info( olds + " -> " + s );
            result.add( s );
        }
        return result;
    }

    public String getName() {
        return "Braket remover";
    }

    public Property getProperty() {
        return Vocabulary.string_match_ignorecase;
    }

    public static void main( String[] args ) throws Exception {
        // TODO Auto-generated method stub
        BracketRemoverRDFMatcher x = new BracketRemoverRDFMatcher( new HashSet<Resource>() );
        log.info( x.processMention( "dorsal (x) cortex" ) );

    }

}

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

import ubic.BAMSandAllen.JenaUtil;
import ubic.BAMSandAllen.Vocabulary;
import ubic.pubmedgate.resolve.depreciated.SimpleExactMatcher;
import ubic.pubmedgate.resolve.mentionEditors.MentionEditor;

/*
 * Simple matching mapper algorithm from:
 *               Creating Mappings For Ontologies in Biomedicine:
 Simple Methods Work
 Amir Ghazvinian, Natalya F. Noy, PhD, Mark A. Musen, MD, PhD
 Stanford Center for Biomedical Informatics Research, Stanford University, Stanford, CA
 
 "Our string-comparison function first
 removes all delimiters from both strings (e.g., spaces,
 underscores, parentheses, etc.). It then uses an
 approximate matching technique to compare the
 strings, allowing for a mismatch of at most one
 character in strings with length greater than four and
 no mismatches for shorter strings.
 "
 
 */

public class SimpleMappingRDFMatcher extends SimpleExactRDFMatcher implements RDFResolver {
    protected static Log log = LogFactory.getLog( ThreeLetterRDFMatcher.class );

    private static final char[] DELIM = { '_', '-', ' ', '(', ')', '.', '/' };

    public Set<String> processMentionString( String mention ) {
        Set<String> mentions = super.processMentionString( mention );
        Set<String> result = new HashSet<String>();
        for ( String toStem : mentions ) {
            result.add( processTerm( toStem ) );
        }
        return result;
    }

    public SimpleMappingRDFMatcher( Set<Resource> terms ) {
        super( terms );
    }

    public String getName() {
        return "Simple Mapping RDF Matcher";
    }

    public Property getProperty() {
        return Vocabulary.simple_mapping_match;
    }

    public String processTerm( String str ) {
        str = super.processTerm( str );
        String result = str.toLowerCase();
        for ( int i = 0; i < DELIM.length; ++i ) {
            result = result.replaceAll( "[" + DELIM[i] + "]", "" );
        }
        return result;
    }

    public Model resolve( Resource mention ) {
        Model model = ModelFactory.createDefaultModel();
        // each mention may expand to more than one string (e.g. superior and inferior raphe nuclei )
        Set<String> mentionTexts = processMentionString( JenaUtil.getLabel( mention ) );
        // get the matches
        for ( String mentionText : mentionTexts ) {

            // if it contains digit or roman numeral?
            // roman regex - (IX|IV|V?I{0,3}) -prefix space or start, endfix space or end
            // digits [/d]+
            // ad hoc code here to skip mentions that contain area, lubule, crus or nerve.. a more general fix is to ignore regions with digits or roman numbers 
            if ( mentionText.contains( "area" ) || mentionText.contains( "lobule" ) || mentionText.contains( "crus" )
                    || mentionText.contains( "nerve" ) ) continue;

            // iterate though all names and check for off by one string matches
            Set<Resource> neuroterms = new HashSet<Resource>();
            for ( String neuroterm : names.keySet() ) {
                if ( LOOM.compare( neuroterm, mentionText ) ) {
                    neuroterms.addAll( names.get( neuroterm ) );
                }
            }

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

    /**
     * @param args
     */
    public static void main( String[] args ) {
    }

}

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

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.Vocabulary;

import com.hp.hpl.jena.rdf.model.Property;

public class BagOfWordsResolver implements Resolver {
    protected static Log log = LogFactory.getLog( BagOfWordsResolver.class );

    // have a bunch of vocabs - BAMS, Allen, Neuronames

    Set<Set<String>> names;
    // delims from Ngrampipefactory
    public static String delims = "~`!@#$%^&*()_-+={[}]|\\:;\"',<.>?/ \t\n\r";

    public String getName() {
        return "Bag of Words exact Matcher";
    }

    public Set<Set<String>> getNames() {
        return names;
    }

    public BagOfWordsResolver() {

    }

    public BagOfWordsResolver( Set<String> names ) {
        System.out.println( "New matcher size:" + names.size() );
        this.names = makeSetofBags( names );
    }

    public String resolve( String text ) {
        // should return the original string
        if ( names.contains( makeBagOfWords( text ) ) ) {
            return "hit";
        } else
            return null;
    }

    public static Set<String> makeBagOfWords( String phrase ) {
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

    public static Set<Set<String>> makeSetofBags( Set<String> lexicon ) {
        // what if we treat each entry as a bag of words
        // delims taken from ngrampipefactory
        Set<Set<String>> lexiconSets = new HashSet<Set<String>>();
        for ( String name : lexicon ) {
            Set<String> bagOfWords = makeBagOfWords( name );
            if ( lexiconSets.contains( bagOfWords ) ) {
                log.info( name + " is already in under " + bagOfWords.toString() );
            }
            lexiconSets.add( bagOfWords );
        }
        log.info( "Size before:" + lexicon.size() );
        log.info( "Size after:" + lexiconSets.size() );
        return lexiconSets;
    }

    public Property getProperty() {
        return Vocabulary.word_bag_match_ignorecase;
    }

    public boolean matches( String a, String b ) {
        Set<String> aBagOfWords = makeBagOfWords( a );
        Set<String> bBagOfWords = makeBagOfWords( b );
        return aBagOfWords.equals( bBagOfWords );
    }

}

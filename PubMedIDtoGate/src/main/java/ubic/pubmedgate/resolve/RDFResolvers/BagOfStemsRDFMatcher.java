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

import ubic.BAMSandAllen.Vocabulary;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class BagOfStemsRDFMatcher extends BagOfWordsRDFMatcher {
    static Stemmer stemmer;
    static {
        stemmer = new LovinsStemmer();
    }

    public BagOfStemsRDFMatcher( Set<Resource> terms ) {
        super( terms );
    }

    public String getName() {
        return "Bag of Stems exact Matcher";
    }

    public Set<String> makeBagOfWords( String phrase ) {
        phrase = phrase.toLowerCase().trim();
        phrase = stemmer.stemString( phrase );
        StringTokenizer tokens = new StringTokenizer( phrase, delims, false );
        Set<String> bagOfWords = new HashSet<String>();
        while ( tokens.hasMoreTokens() ) {
            bagOfWords.add( tokens.nextToken() );
        }
        bagOfWords.remove( "th" );
        bagOfWords.remove( "of" );
        return bagOfWords;
    }

    public Property getProperty() {
        return Vocabulary.stem_bag_match_ignorecase;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // TODO Auto-generated method stub
        BagOfStemsRDFMatcher test = new BagOfStemsRDFMatcher(new HashSet<Resource>());
        log.info( test.makeBagOfWords( "Anterior hypothalamic dorsal part" ));
        log.info( test.makeBagOfWords( "Anterior hypothalamus dorsal part" ));
        

    }

}

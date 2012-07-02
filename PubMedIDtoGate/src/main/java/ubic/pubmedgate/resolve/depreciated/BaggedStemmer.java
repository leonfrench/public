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

import kea.LovinsStemmer;
import kea.Stemmer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.pubmedgate.NeuroNamesLoader;
import ubic.pubmedgate.NeuroNamesMouseAndRatLoader;

public class BaggedStemmer extends BagOfWordsResolver implements Resolver {
    protected static Log log = LogFactory.getLog( BaggedStemmer.class );

    Stemmer stemmer;

    public BaggedStemmer( Set<String> names ) {
        stemmer = new LovinsStemmer();

        // add stems to names
        int count = 0;
        Set<String> newNames = new HashSet<String>();
        for ( String name : names ) {
            log.info( count++ + " of " + names.size() + " " + name + " -> " + stemmer.stemString( name ) );
            try {
                newNames.add( stemmer.stemString( name ) );
            } catch ( Exception e ) {
                throw new RuntimeException( e );
            }
        }
        this.names = makeSetofBags( newNames );
        log.info( "Original size:" + names.size() + " New Size:" + this.names.size() );
    }

    public String getName() {
        return "Stem Bagged Matcher";
    }

    public static void main( String[] args ) throws Exception {
        NeuroNamesLoader NN = new NeuroNamesLoader();
        NeuroNamesMouseAndRatLoader NNMR = new NeuroNamesMouseAndRatLoader();

        Set<String> NNSet = NN.getRegionsForLexicon();
        NNSet.addAll( NNMR.getRegionsForLexicon() );

        Resolver resolver = new BaggedStemmer( NNSet );
        log.info( resolver.resolve( "fimbria of fornix" ) );

        Stemmer x = new LovinsStemmer();
        x.stem( "testing" );
        log.info( x.stemString( "mesostriatal fibers" ) );
        log.info( x.stemString( "area 1/2 in the opposite hemisphere" ) );
        log.info( x.stemString( "parasympathetic preganglionic column of the lumbosacral spinal cord" ) );
        log.info( x.stemString( "primary olfactory centers" ) );
        log.info( x.stemString( "sensory-motor region" ) );

    }

    public String resolve( String text ) {
        return super.resolve( stemmer.stemString( text ) );
    }

}

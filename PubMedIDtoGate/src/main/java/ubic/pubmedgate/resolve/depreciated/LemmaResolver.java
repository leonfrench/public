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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.pubmedgate.treetagger.RegionLemma;

public class LemmaResolver extends SimpleExactMatcher implements Resolver {
    protected static Log log = LogFactory.getLog( SimpleExactMatcher.class );

    RegionLemma stemmer;

    public LemmaResolver( Set<String> names ) {
        super( names );
        // add stems to names
        stemmer = new RegionLemma();
        int count = 0;
        Set<String> newNames = new HashSet<String>();
        for ( String name : names ) {
            log.info( count++ + " of " + names.size() );
            try {
                newNames.add( stemmer.stem( name ) );
            } catch ( Exception e ) {
                throw new RuntimeException( e );
            }
        }
        this.names.addAll( newNames );
    }

    public String getName() {
        return "Lemma Matcher";
    }

    public static void main( String[] args ) throws Exception {

    }

    public String resolve( String text ) {
        String simpleExactMatch = super.resolve( text );
        if ( simpleExactMatch == null ) {
            try {
                return super.resolve( stemmer.stem( text ) );
            } catch ( Exception e ) {
                throw new RuntimeException( e );
            }
        } else {
            return simpleExactMatch;
        }
    }
}

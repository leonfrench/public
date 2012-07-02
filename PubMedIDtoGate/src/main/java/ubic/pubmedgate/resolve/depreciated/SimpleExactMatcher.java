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

import java.util.Set;

import ubic.BAMSandAllen.Vocabulary;

import com.hp.hpl.jena.rdf.model.Property;

public class SimpleExactMatcher implements Resolver {
    // have a bunch of vocabs - BAMS, Allen, Neuronames

    Set<String> names;

    public SimpleExactMatcher() {
        // non cached version - should be another class?
    }

    public SimpleExactMatcher( Set<String> names ) {
        System.out.println( "New matcher size:" + names.size() );
        this.names = names;
    }

    public String getName() {
        return "Simple Exact Matcher";
    }

    public Property getProperty() {
        return Vocabulary.string_match_ignorecase;
    }

    public boolean matches( String a, String b ) {
//        a = a.trim();
//        b = b.trim();
        return a.equalsIgnoreCase( b );
    }

    public Set<String> getNames() {
        return names;
    }

    public String resolve( String text ) {
        String textClean = text;//.trim();
        for ( String name : names ) {
            String nameClean = name;//.trim();
            if ( nameClean.equalsIgnoreCase( textClean ) ) return name;
        }
        return null;
    }
}

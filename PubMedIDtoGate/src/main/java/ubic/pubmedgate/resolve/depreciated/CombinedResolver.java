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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ubic.BAMSandAllen.Vocabulary;

import com.hp.hpl.jena.rdf.model.Property;

public class CombinedResolver implements Resolver {
    Resolver[] resolvers;

    public CombinedResolver( Resolver... resolvers ) {
        this.resolvers = resolvers;
    }

    public String resolve( String text ) {
        // try them all
        for ( Resolver resolver : resolvers ) {
            String result = resolver.resolve( text );
            if ( result != null ) return result;
        }
        return null;
    }

    public String getName() {
        String result = "Combination of:";
        for ( Resolver resolver : resolvers )
            result += " " + resolver.getName();
        return result;
    }

    public Property getProperty() {
        return null;
    }

    public boolean matches( String a, String b ) {
        throw new RuntimeException( "Not implemented" );
    }
}

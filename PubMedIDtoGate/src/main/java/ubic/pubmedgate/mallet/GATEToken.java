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

package ubic.pubmedgate.mallet;

import gate.Annotation;
import ubic.pubmedgate.ConnectionsDocument;
import cc.mallet.types.Token;

/**
 * Here the Mallet token is used to wrap a GATE token, so we can use all the Mallet pipes on it
 * 
 * The original annotation is stored as a property, this class just adds in some extra methods to make it easy to pull out
 * 
 * @author leon
 *
 */
public class GATEToken extends Token {
    public static final long serialVersionUID = 1l;
    public static final String GATEPROPERTYNAME = "GATEAnnotation";

    public GATEToken( Annotation token, ConnectionsDocument d ) {
        super( d.getAnnotationText( token ) );
        setProperty( GATEPROPERTYNAME, token );
    }

    public Annotation getGATEAnnotation() {
        return ( Annotation ) getProperty( GATEPROPERTYNAME );
    }
    

}

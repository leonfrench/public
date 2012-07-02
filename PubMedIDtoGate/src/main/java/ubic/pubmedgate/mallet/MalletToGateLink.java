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

import java.io.Serializable;
import java.util.Collections;

import ubic.pubmedgate.ConnectionsDocument;
import cc.mallet.types.TokenSequence;

/*
 * Wrapper for a Gate document and a mallet tokensequence.  To be stored in the source of an instance
 */
public class MalletToGateLink implements Serializable {
    // the document
    ConnectionsDocument doc;
    // the token sequence (type is GATEToken)
    TokenSequence tokens;

    public MalletToGateLink( ConnectionsDocument doc, TokenSequence tokens ) {
        this.doc = doc;
        this.tokens = tokens;
    }

    public ConnectionsDocument getDoc() {
        return doc;
    }

    public TokenSequence getTokens() {
        return tokens;
    }

    public void reverseTokens() {
        Collections.reverse( tokens );
    }

    public String toString() {
        String result = doc.getName();
        result += " Tokens:" + tokens.toString();
        return result;
    }
}

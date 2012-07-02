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

import java.util.List;

import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.editors.AbbreviationLoader;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.TokenSequence;

/*
 * how to deal with abbreviations?
 * 
 */
public class Sentence2TokenSequence extends Pipe {
    boolean skipAbbrev;

    public static final long serialVersionUID = 1l;
    String annotationSet;

    public Sentence2TokenSequence( boolean skipAbbrev, String annotationSet ) {
        // super( new Alphabet(), new LabelAlphabet() );
        this.annotationSet = annotationSet;
        this.skipAbbrev = skipAbbrev;
    }

    public Instance pipe( Instance carrier ) {

        // input data should be a GATE annotation
        // CHECK this?
        Annotation sentence = ( Annotation ) carrier.getData();
        ConnectionsDocument doc = ( ConnectionsDocument ) carrier.getSource();
        AbbreviationLoader abbrevInterface = null;
        if ( skipAbbrev ) {
            abbrevInterface = new AbbreviationLoader( doc );
        }

        // convert the sentence into sorted / ordered list of tokens
        List<Annotation> tokens = doc.getTokens( sentence, annotationSet );

        TokenSequence output = new TokenSequence();

        boolean firstToken = true;
        for ( Annotation GATEtoken : tokens ) {
            // don't use the long form (looks like the original)
            // if ( skipAbbrev && abbrevInterface.hasLongFormOverlap( GATEtoken ) ) {

            //bit of a hack, removes PMID from the front of a sentence, better place to fix is in GeniaRunner, for GATETokens
            // not a problem for GATE and TreeTagger sets
            if ( firstToken && doc.getPMID() != null && doc.getAnnotationText( GATEtoken ).equals( doc.getPMID() ) ) {
                continue;
            }

            // don't use the braketed part
            if ( skipAbbrev && abbrevInterface.hasShortFormOverlap( GATEtoken ) ) {
                // this token does not make it to the output
                continue;
            }
            // somehow convert the token string, token feature, into a Feature vector
            // get the label from where???
            GATEToken tokenInMallet = new GATEToken( GATEtoken, doc );

            output.add( tokenInMallet );
            firstToken = false;
        }

        // set the source so it knows about the tokens (it's in two places because it's later erased by
        // Tokenseeq2Vectorseq
        MalletToGateLink newSource = new MalletToGateLink( ( ConnectionsDocument ) carrier.getSource(), output );

        // set the instance data to the output (collection of GATETokens)
        Instance result = new Instance( output, carrier.getTarget(), carrier.getName(), newSource );

        return result;
    }

    public static void main( String argsp[] ) {

    }

}

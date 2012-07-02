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

package ubic.pubmedgate.mallet.features;

import gate.Annotation;
import gate.FeatureMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.pubmedgate.mallet.GATEToken;
import ubic.pubmedgate.ner.TokenTargetLabeller;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class TargetFromGATE extends Pipe {
    protected static Log log = LogFactory.getLog( TargetFromGATE.class );
    String targetLabelFeature;
    // what it is in GATE
    public static final String GATE_SIMPLE_TARGET_FEATURE = "target";
    public static final String GATE_BIO_TARGET_FEATURE = "BIOTarget";

    public static final long serialVersionUID = 1l;

    public TargetFromGATE( String labelFeature ) {
        super( new Alphabet(), new LabelAlphabet() );
        this.targetLabelFeature = labelFeature;
    }

    public TargetFromGATE() {
        this( GATE_SIMPLE_TARGET_FEATURE );
    }

    public Instance pipe( Instance carrier ) {
        LabelAlphabet labels;
        LabelSequence target = null;
        boolean nullLabel = false;

        TokenSequence tokens = ( TokenSequence ) carrier.getData();

        labels = ( LabelAlphabet ) getTargetAlphabet();
        // start with a blank labelsequence
        target = new LabelSequence( labels, tokens.size() );

        for ( Token tokenM : tokens ) {
            GATEToken token = ( GATEToken ) tokenM;
            Annotation tokenAnnotation = token.getGATEAnnotation();
            FeatureMap features = tokenAnnotation.getFeatures();

            // get the targets value and add it
            Object feature = features.get( targetLabelFeature );
            if ( feature != null ) {
                String GATEFeatureValue = feature.toString();
                target.add( GATEFeatureValue );
            } else {
                // not realy a good thing todo here, but if it's null then add outside target
                nullLabel = true;
                target.add( TokenTargetLabeller.OUTSIDE_TARGET );
            }
        }

        if ( nullLabel ) log.info( "Warning, missing target labels on " + carrier.getName() );
        // set the target sequences
        carrier.setTarget( target );
        return carrier;
    }
}

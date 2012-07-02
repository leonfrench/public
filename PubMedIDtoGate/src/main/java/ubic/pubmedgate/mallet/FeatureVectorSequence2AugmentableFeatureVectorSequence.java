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

import java.util.Iterator;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.ArrayListSequence;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.Instance;

public class FeatureVectorSequence2AugmentableFeatureVectorSequence extends Pipe {
    boolean binary;

    public FeatureVectorSequence2AugmentableFeatureVectorSequence (boolean binary)
    {
        this.binary = binary;
    }

    public FeatureVectorSequence2AugmentableFeatureVectorSequence ()
    {
        this (false);
    }
    
    public Instance pipe (Instance carrier)
    {
        // carrier.setData(new AugmentableFeatureVector ((FeatureVectorSequence)carrier.getData(), binary));
        FeatureVectorSequence data = (FeatureVectorSequence)carrier.getData();
        ArrayListSequence<FeatureVector> newData = new ArrayListSequence<FeatureVector>();
        Iterator<FeatureVector> i = data.iterator();
        while(i.hasNext()) {
            newData.add(new AugmentableFeatureVector(i.next()));
        }
        carrier.setData(newData);
        return carrier;
    }
 }



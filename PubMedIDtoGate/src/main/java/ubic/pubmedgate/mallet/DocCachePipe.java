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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.pubmedgate.ConnectionsDocument;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.Instance;
import cc.mallet.types.SingleInstanceIterator;

public class DocCachePipe extends Pipe implements Serializable {
    protected static Log log = LogFactory.getLog( DocCachePipe.class );

    SerialPipes pipes;
    Map<String, List<Instance>> cache;

    public DocCachePipe( SerialPipes pipe ) {
        this.pipes = pipe;
        cache = new HashMap<String, List<Instance>>();
    }

    public Iterator<Instance> newIteratorFrom( Iterator<Instance> source ) {
        List<Instance> result = new LinkedList<Instance>();
        for ( ; source.hasNext(); ) {
            Instance i = source.next();
            ConnectionsDocument doc = ( ConnectionsDocument ) i.getData();
            //log.info( "checking cache " + doc.getName() );
            
            List<Instance> cached = cache.get( doc.getName() );
            
            
            // if it's not cached then compute the pipe on it and store it
            if ( cached == null ) {
                Iterator<Instance> output = pipes.newIteratorFrom( new SingleInstanceIterator( i ) );
                cached = new LinkedList<Instance>();
                for ( ; output.hasNext(); ) {
                    cached.add( output.next() );
                }
                cache.put( doc.getName(), cached );
            }
            result.addAll( cached );
        }
        return result.iterator();
    }

    public void setTargetProcessing( boolean lookForAndProcessTarget ) {
        pipes.setTargetProcessing( lookForAndProcessTarget );
    }

    public int size() {
        return pipes.size();
    }

    public int cacheSize() {
        return cache.size();
    }

    public Pipe getPipe( int index ) {
        return pipes.getPipe( index );
    }

    /** Allows access to the underlying collection of Pipes. Use with caution. */
    public ArrayList<Pipe> pipes() {
        return pipes.pipes();
    }

    public String toString() {
        return pipes.toString();
    }

    private static final long serialVersionUID = 1;

}

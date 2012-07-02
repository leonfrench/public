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

import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class MalletLogFilter implements Filter {

    public boolean isLoggable( LogRecord record ) {
        // TODO Auto-generated method stub
        String message = record.getMessage();
        // if ( message.startsWith( "CRF about to train with" ) ) return false;
        if ( message.startsWith( "CRF finished one iteration of maximizer" ) ) return false;
        if ( message.startsWith( "getValue() (" ) ) return false;
        // from augmentable features
        if ( message.startsWith( "Adding error" ) ) return false;
        return true;
    }
}

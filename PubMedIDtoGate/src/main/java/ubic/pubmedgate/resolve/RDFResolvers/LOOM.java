package ubic.pubmedgate.resolve.RDFResolvers;

/*
 * Simple matching mapper algorithm from:
 *               Creating Mappings For Ontologies in Biomedicine:
 Simple Methods Work
 Amir Ghazvinian, Natalya F. Noy, PhD, Mark A. Musen, MD, PhD
 Stanford Center for Biomedical Informatics Research, Stanford University, Stanford, CA
 
 "Our string-comparison function first
 removes all delimiters from both strings (e.g., spaces,
 underscores, parentheses, etc.). It then uses an
 approximate matching technique to compare the
 strings, allowing for a mismatch of at most one
 character in strings with length greater than four and
 no mismatches for shorter strings.
 "
 
 
 */

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LOOM {

    protected static Log log = LogFactory.getLog( LOOM.class );

    public static final int EQUAL = 0;
    public static final int APPROXIMATE_MATCH = 40;
    private static final char[] DELIMITERS = { '_', '-', ' ', '(', ')', '.', '/' };
    private static final String[] COMMON_WORDS = { "of", "the", "and", "to", "in", "as", "or" };
    private static final char[] DIGITS = { '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' };
    private static final char[] NUMERALS = { 'i', 'v', 'x' };

    private static double THRESHOLD = 0.95D;

    private static int TOO_SMALL = 4;

    private static int TYPO_LENGTH = 1;

    private static double WORD_MATCH_THRESHOLD = 0.7D;

    private static double WORD_MATCH_MIN_SIZE = 3.0D;

    public static int compareNamesForSorting( String name1, String name2 ) {
        return compareNames( name1, name2, false, false );
    }

    public static int compareNamesWithExactMatch( String name1, String name2 ) {
        return compareNames( name1, name2, true, true );
    }

    public static int compareNames( String name1, String name2 ) {
        return compareNames( name1, name2, true, false );
    }

    public static int compareNames( String name1, String name2, boolean convertStrs, boolean trueComparison ) {
        String n1 = ( convertStrs ) ? convertString( name1 ) : name1;
        String n2 = ( convertStrs ) ? convertString( name2 ) : name2;

        int comparisonResult = n1.compareTo( n2 );

        if ( comparisonResult == 40 ) ++comparisonResult;

        int result;
        if ( comparisonResult == 0 ) {
            result = 0;
        } else {
            if ( ( !trueComparison ) && ( closeEnough( n1, n2 ) ) ) {
                if ( ( numberCheck( name1, name2 ) ) && ( numberCheck( name2, name1 ) ) )
                    result = 40;
                else
                    result = comparisonResult;
            } else {
                result = comparisonResult;
            }
        }
        return result;
    }

    private static String convertString( String str ) {
        String result = str.toLowerCase();
        for ( int i = 0; i < DELIMITERS.length; ++i )
            result = removeCharacter( result, DELIMITERS[i] );
        return result;
    }

    private static String removeCharacter( String s, char c ) {
        String result = new String();
        int currentIndex = 0;
        int searchIndex;
        do {
            searchIndex = s.indexOf( c, currentIndex );
            if ( searchIndex != -1 ) {
                result = result.concat( s.substring( currentIndex, searchIndex ) );
                currentIndex = searchIndex + 1;
            } else {
                result = result.concat( s.substring( currentIndex ) );
            }
        } while ( searchIndex != -1 );

        return result;
    }

    public static boolean closeEnough( String str1, String str2 ) {
        if ( str1.equals( str2 ) ) {
            return true;
        }
        String s1 = convertString( str1 );
        String s2 = convertString( str2 );

        boolean result = compare( s1, s2 );

        return result;
    }

    public static boolean compare( String s1, String s2 ) {
        // round 95% of the length
        int endIndex1 = ( int ) Math.round( s1.length() * THRESHOLD );
        int endIndex2 = ( int ) Math.round( s2.length() * THRESHOLD );

        // not sure how these indexes can be less than zero
        if ( ( endIndex1 < 0 ) && ( endIndex1 < s1.length() ) ) {
            return false;
        }

        if ( ( endIndex2 < 0 ) && ( endIndex1 < s2.length() ) ) {
            return false;
        }

        // take the 95% of the string
        String part1 = s1.substring( 0, endIndex1 );
        String part2 = s2.substring( 0, endIndex2 );

//        log.info( part1 );
//        log.info( part2 );
        if ( part1.length() <= TOO_SMALL ) {
            return false;
        }
        if ( part2.length() <= TOO_SMALL ) {
            return false;
        }

        // if one is inside another then return a match
        if ( ( s2.indexOf( part1 ) != -1 ) && ( s1.length() / s2.length() > THRESHOLD ) ) {
            return true;
        }
        if ( ( s1.indexOf( part2 ) != -1 ) && ( s2.length() / s1.length() > THRESHOLD ) ) {
            return true;
        }

        // if the difference in lengths is greater than four then it's not a match??
//        log.info( "TOO small difference check" + Math.abs( s1.length() - s2.length() ) );
        if ( Math.abs( s1.length() - s2.length() ) > TOO_SMALL ) {
//            System.out.println( "TOO small difference check" + Math.abs( s1.length() - s2.length() ) );
            return false;
        }

        // f is when s1 stops being equal to s2 character
        int f = 0;
        for ( f = 0; ( f < s1.length() ) && ( f < s2.length() ); ++f ) {
            if ( s1.charAt( f ) != s2.charAt( f ) ) {
                break;
            }

        }
        // b is when s1 stops being equal to s2 character but from the other direction
        int b = 1;
        for ( b = 1; ( b < s1.length() ) && ( b < s2.length() ); ++b ) {
            if ( s1.charAt( s1.length() - b ) != s2.charAt( s2.length() - b ) ) break;
        }
        --b;
        // log.info( "f, from start = " + f );
        // log.info( "b, from end = " + b );
        // log.info( "b+f = " + ( b + f ) );
        // log.info( s1.length() );
        // log.info( "Math.abs( s1.length() - b - f ) = " + ( Math.abs( s1.length() - b - f ) ) );

        return ( Math.abs( s1.length() - b - f ) <= TYPO_LENGTH ) && ( Math.abs( s2.length() - b - f ) <= TYPO_LENGTH );
    }

    public static boolean wordOrderMatch( String s1, String s2 ) {
        ArrayList words1 = parse( s1 );
        ArrayList words2 = parse( s2 );
        boolean match = wordOrderCheck( words1, words2 );
        if ( !match ) {
            match = wordOrderCheck( words2, words1 );
        }
        return match;
    }

    public static boolean wordOrderCheck( ArrayList<String> words1, ArrayList<String> words2 ) {
        int size1 = words1.size();
        int size2 = words2.size();
        int matchCount = 0;
        for ( int i = 0; i < words1.size(); ++i ) {
            String toTry = ( String ) words1.get( i );
            boolean matchFound = false;
            if ( words2.contains( toTry ) )
                matchFound = true;
            else {
                for ( int k = 0; k < words2.size(); ++k ) {
                    if ( closeEnough( ( String ) words1.get( i ), ( String ) words2.get( k ) ) ) {
                        matchFound = true;
                        break;
                    }
                }
            }
            if ( matchFound )
                ++matchCount;
            else {
                for ( int j = 0; j < COMMON_WORDS.length; ++j ) {
                    if ( toTry.equals( COMMON_WORDS[j] ) ) {
                        ++matchCount;
                        ++size2;
                        break;
                    }
                }
            }
        }
        int sizeDiff = Math.abs( size1 - size2 );
        boolean bigEnough = ( words1.size() >= WORD_MATCH_MIN_SIZE ) && ( words2.size() >= WORD_MATCH_MIN_SIZE );
        boolean meetsThreshold = words1.size() / words2.size() >= WORD_MATCH_THRESHOLD;
        return ( matchCount == words1.size() ) && ( matchCount > 0 ) && ( sizeDiff == 0 );
    }

    public static ArrayList<String> parseWords( ArrayList<String> init, char delimiter ) {
        ArrayList words = new ArrayList();
        for ( int i = 0; i < init.size(); ++i ) {
            String s1 = ( String ) init.get( i );
            while ( s1.indexOf( delimiter ) != -1 ) {
                int parseIndex = s1.indexOf( delimiter );
                String toAdd = s1.substring( 0, parseIndex );
                if ( !toAdd.equals( "" ) ) {
                    words.add( toAdd.toLowerCase() );
                }
                s1 = s1.substring( parseIndex + 1 );
            }
            if ( !s1.equals( "" ) ) {
                words.add( s1.toLowerCase() );
            }
        }
        return words;
    }

    public static ArrayList<String> parse( String s1 ) {
        ArrayList a = new ArrayList();
        a.add( s1 );
        for ( int i = 0; i < DELIMITERS.length; ++i ) {
            a = parseWords( a, DELIMITERS[i] );
        }
        return a;
    }

    public static boolean numberCheck( String s1, String s2 ) {
        return ( digitCheck( s1, s2 ) ) && ( numeralCheck( s1, s2 ) );
    }

    public static boolean digitCheck( String s1, String s2 ) {
        for ( int i = 0; i < DIGITS.length; ++i ) {
            int numIndex = s1.indexOf( DIGITS[i] );
            if ( numIndex != -1 ) {
                String num = String.valueOf( DIGITS[i] );
                for ( int j = 0; j < DIGITS.length; ++j ) {
                    if ( ( numIndex >= s1.length() - 1 ) || ( s1.charAt( numIndex + 1 ) != DIGITS[j] ) ) continue;
                    num = num.concat( String.valueOf( DIGITS[j] ) );
                    ++numIndex;
                    j = -1;
                }

                if ( s2.indexOf( num ) == -1 ) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean numeralCheck( String s1, String s2 ) {
        ArrayList words1 = parse( s1 );
        ArrayList words2 = parse( s2 );
        for ( int i = 0; i < words1.size(); ++i ) {
            if ( ( isNumeral( ( String ) words1.get( i ) ) ) && ( !words2.contains( words1.get( i ) ) ) ) {
                return false;
            }
        }

        return true;
    }

    public static boolean isNumeral( String s ) {
        for ( int i = 0; i < NUMERALS.length; ++i ) {
            s = removeCharacter( s, NUMERALS[i] );
        }
        return s.length() == 0;
    }

    public static void main( String args[] ) {
        System.out.println( compare( "leonfrench", "leonrench" ) );
    }
}

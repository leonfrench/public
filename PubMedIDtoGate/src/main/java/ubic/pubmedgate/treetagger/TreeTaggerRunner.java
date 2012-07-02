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

package ubic.pubmedgate.treetagger;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.FeatureMap;
import gate.creole.ANNIEConstants;
import gate.util.OffsetComparator;
import gate.util.SimpleFeatureMapImpl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.editors.AbbreviationLoader;
import ubic.pubmedgate.mallet.GateReseter;

/**
 * This runs the tree tagger software with parameters from the Genia corpus.
 * 
 * @author leon
 */
public class TreeTaggerRunner {

	// make sure that GATE tokens are not duplicated - this often occurs,
	// overlapping tokens are created if the GATE
	// tokenizer is ran twice

	protected static Log log = LogFactory.getLog(TreeTaggerRunner.class);

	String annotationSetName;
	String checkText;
	boolean GATETokens;

	public TreeTaggerRunner(String annotationSetName, boolean GATETokens) {
		this.annotationSetName = annotationSetName;
		this.GATETokens = GATETokens;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String annotationSet = "TreeTagger";
		// set GATE to use gate tokens instead of treetaggers tokenization,
		// untested
		boolean GATETokens = true;
		if (GATETokens) {
			annotationSet += "GATETokens";
		}

		StopWatch watch = new org.apache.commons.lang.time.StopWatch();
		watch.start();
		GateInterface p2g = new GateInterface();

		// Corpus corp = p2g.getCorp();
		// Corpus corp = p2g.getTrainingCorp();
		// Corpus corp = p2g.getNoAbbrevCorp();
		Corpus corp = p2g.getUnseenCorp();
		TreeTaggerRunner runner = new TreeTaggerRunner(annotationSet,
				GATETokens);

		// code to test a single document
		// Document test = ( Document ) p2g.getUnseenCorp().get( 17761 );
		// runner.runTreeTagger( new ConnectionsDocument( test ) );
		// System.exit( 1 );

		GateReseter reset = new GateReseter(GateInterface.getDocuments(corp),
				annotationSet);
		reset.reset();
		log.info("Done reseting");

		// 128 had a problem..
		// runner.runTreeTagger( new ConnectionsDocument( ( Document )
		// p2g.getTrainingCorp().get( 128 ) ) );
		// System.exit( 1 );

		System.out.println("Time:" + watch.toString());
		int i = 0;
		int errors = 0;
		for (ConnectionsDocument doc : GateInterface.getDocuments(corp)) {
			log.info(doc.getName());
			try {
				runner.runTreeTagger(doc);
				doc.sync();
			} catch (Exception e) {
				e.printStackTrace();
				errors++;
			}
			log.info("i:" + i++ + " Time:" + watch.toString() + " errors:"
					+ errors);
		}

	}

	private File getFile(ConnectionsDocument document) throws Exception {
		// tokenize
		File gateTextFile = File.createTempFile("treetagger", ".txt");
		// gateTextFile.deleteOnExit();

		FileOutputStream fos = new FileOutputStream(gateTextFile);
		OutputStreamWriter osw = new OutputStreamWriter(fos);
		BufferedWriter bw = new BufferedWriter(osw);

		// get the ArticleTilte and AbstractText
		Annotation title = document.getTitle();
		String titleString = document.getAnnotationText(title);

		Annotation abstractText = document.getAbstract();
		String abstractString = document.getAnnotationText(abstractText);

		// write it
		bw.write(titleString);
		bw.newLine();
		bw.write(abstractString);

		log.info("Wrote:" + abstractString);
		// bad scope - this is per document
		checkText = titleString + System.getProperty("line.separator")
				+ abstractString;
		// this is a fix for abstract PMID: 21294731, it uses a EM space instead
		// of a space!
		checkText = checkText.replaceAll("\u2003", " ");
		checkText = checkText.replaceAll("\u200A", " ");

		log.info("Title:" + titleString);
		log.info("abstractString:" + abstractString);

		bw.flush();
		bw.close();
		return gateTextFile;
	}

	public static String[] getTreeTaggerCommand(File inputFile) {
		// /home/leon/Desktop/WhiteTextTools/Treetagger/bin/tree-tagger -token
		// -lemma -sgml -pt-with-lemma -quiet
		// /home/leon/Desktop/WhiteTextTools/Treetagger/lib/GeniaParams.par
		String[] command = new String[8];
		command[0] = Config.config.getString("whitetext.treetagger.location");
		command[1] = "-token";
		command[2] = "-lemma";
		command[3] = "-sgml";
		command[4] = "-pt-with-lemma";
		command[5] = "-quiet";
		// param file
		command[6] = Config.config.getString("whitetext.GENIA.params");
		// input file
		command[7] = inputFile.getAbsolutePath();
		return command;
	}

	public static String[] getTokenizerCommand(File inputFile) {
		String[] command = new String[4];
		// command[0] = "/usr/bin/perl";
		command[0] = Config.config.getString("whitetext.treetagger.tokenizer"); // the
																				// perl
																				// script
																				// -
																				// tokenize.pl
		command[1] = "-w"; // replace white space with SGML
		command[2] = "-e"; // english text
		command[3] = inputFile.getAbsolutePath(); // input file
		return command;
	}

	/**
	 * Based on TreeTagger.java in the GATE project
	 */
	public void runTreeTagger(ConnectionsDocument document) throws Exception {
		// log.info( "Inputfile:" + inputFile.getAbsolutePath() );

		String line, word, category, lemma;
		AnnotationSet aSet;

		FeatureMap braketFeatures = new SimpleFeatureMapImpl();
		braketFeatures.put("string", ")");
		braketFeatures.put("lemma", ")");
		braketFeatures.put("category", ")");
		braketFeatures.put("length", "1");

		// remove the old annotations
		document.removeAnnotationSet(annotationSetName);

		aSet = document.getAnnotations(annotationSetName);

		// run tokenizer and save output
		File inputFile;
		if (!GATETokens) {
			inputFile = getFile(document);
			inputFile = tokenize(inputFile);
		} else {
			inputFile = gateTokenize(document);
		}
		log.info("Inputfile after tokenization:" + inputFile.getAbsolutePath());

		// run TreeTagger and save output
		ArrayList<String> lines = runTreeTagger(inputFile);

		// take one line at a time
		// check its length and the string feature and go on addition
		// features

		long position;
		if (!GATETokens) {
			position = document.getTitle().getStartNode().getOffset();
		} else {
			position = 0;
		}
		long sentenceStart = position;

		String text = new String();

		for (int i = 0; i < lines.size(); i++) {
			line = (String) lines.get(i);
			// System.out.println( line );
			StringTokenizer st = new StringTokenizer(line);

			category = null;
			lemma = null;
			word = st.nextToken();
			if (word.equals("<internal_BL>")) {
				word = " ";
			} else if (word.equals("<internal_NL>")) {
				word = System.getProperty("line.separator");
			} else {
				// if we have not sentence start, then set one
				if (sentenceStart == -1) {
					sentenceStart = position;
				}
				if (word.startsWith("<"))
					log.info(word);

				if (st.hasMoreTokens())
					category = st.nextToken();
				if (category != null && st.hasMoreTokens())
					lemma = st.nextToken();

				FeatureMap features = new SimpleFeatureMapImpl();
				features.put("string", word);
				features.put("length", word.length());

				// finally add features on the top of tokens
				if (lemma != null)
					features.put("lemma", lemma);
				if (category != null)
					features.put("category", category);

				long start = position;
				long end = position + word.length();

				AbbreviationLoader abbrev = new AbbreviationLoader(document);
				// so here we split into two, if we have an abbreviation we gota
				// make three tokens instead of one
				if (!GATETokens && abbrev.hasShortFormOverlap(start, end)
						&& !word.equals(")") && word.contains("(")) {
					// turn it into three tokens
					AnnotationSet shortAbbrevs = abbrev.getShortForms(start,
							end);
					Annotation abbrevAnnotation = shortAbbrevs.iterator()
							.next();
					// start to
					long abbrevStart = abbrevAnnotation.getStartNode()
							.getOffset();
					String endString = word
							.substring((int) (abbrevStart + 1 - start));
					// log.info( endString );
					// log.info( word );
					// log.info( start );
					// log.info( abbrevStart );
					aSet.add(start, abbrevStart,
							ANNIEConstants.TOKEN_ANNOTATION_TYPE, features);
					// braket
					aSet.add(abbrevStart, abbrevStart + 1,
							ANNIEConstants.TOKEN_ANNOTATION_TYPE,
							braketFeatures);
					// the short form
					FeatureMap endFeatures = new SimpleFeatureMapImpl();
					endFeatures.put("string", endString);
					endFeatures.put("lemma", endString);
					endFeatures.put("category", "ABBREV");
					endFeatures.put("length", endString.length());
					aSet.add(abbrevStart + 1, end,
							ANNIEConstants.TOKEN_ANNOTATION_TYPE, endFeatures);

				} else {
					// log.info( "start=" + start + "end=" + end + " " + word );
					aSet.add(start, end, ANNIEConstants.TOKEN_ANNOTATION_TYPE,
							features);
				}

				if (category.equals("SENT")) {
					aSet.add(sentenceStart, end,
							ANNIEConstants.SENTENCE_ANNOTATION_TYPE, features);
					sentenceStart = -1;
				}
			}
			text += word;
			position += word.length();
		}

		if (!text.toString().equals(checkText)) {
			log.info("Text doesnt match! something was changed in the processing, check for unicode characters");
			log.info("Original|" + checkText + "|");
			log.info("FromFile|" + text.toString() + "|");
			System.out.println("Original Size:" + checkText.length());
			System.out.println("FromFile Size:" + text.toString().length());

			for (int cPos = 0; cPos < text.length(); cPos++) {
				char file = text.charAt(cPos);
				char orig = checkText.charAt(cPos);
				if (file != orig) {
					System.out.println("Character:" + cPos + ":file:orig");
					System.out.println("Character:" + cPos + ":" + file + ":"
							+ orig);
					System.out.println("Character:" + cPos + ":" + (int) file
							+ ":" + (int) orig);
				}
			}

			System.exit(1);
		}
	}

	public static ArrayList<String> runTreeTagger(File inputFile)
			throws IOException {
		String line;
		String[] command = getTreeTaggerCommand(inputFile);
		// log.info( Arrays.asList( command ) );
		Process p = Runtime.getRuntime().exec(command);

		// get tree tagger output (gate input)
		BufferedReader input = new BufferedReader(new InputStreamReader(
				p.getInputStream()));

		ArrayList<String> lines = new ArrayList<String>();
		while ((line = input.readLine()) != null) {
			lines.add(line);
		}
		input.close();
		p.destroy();
		return lines;
	}

	private File gateTokenize(ConnectionsDocument document) throws IOException,
			FileNotFoundException {
		checkText = document.getContent().toString();
		checkText = checkText.replaceAll("\u2003", " ");
		checkText = checkText.replaceAll("\u200A", " ");

		File temp = File.createTempFile("GATEtokens", ".txt");

		FileOutputStream fos = new FileOutputStream(temp);
		OutputStreamWriter osw = new OutputStreamWriter(fos);
		BufferedWriter bw = new BufferedWriter(osw);

		AnnotationSet tokens = document
				.getAnnotations(ConnectionsDocument.GATETOKENS);
		log.info("Token size:" + tokens.size());
		if (tokens.size() == 0) {
			log.info("Warning, no tokens in the GATETokens annotation set");
		}

		Set<String> types = new HashSet<String>();
		types.add("Token");
		types.add("SpaceToken");
		tokens = tokens.get(types);
		// sort
		List<Annotation> tokensSorted = new ArrayList<Annotation>(tokens);
		Collections.sort(tokensSorted, new OffsetComparator());
		log.info("Tokens:" + tokensSorted.size());
		for (Annotation a : tokensSorted) {
			// log.info( a.getType() );
			String line = "";
			if (a.getType().equals("Token")) {
				line = document.getAnnotationText(a);
			}
			if (a.getType().equals("SpaceToken")) {
				if (a.getFeatures().get("kind").equals("control")) {
					line = "<internal_NL>";
				} else {
					line = "<internal_BL>";
				}
			}
			bw.write(line);
			bw.newLine();
		}
		bw.close();
		fos.close();
		return temp;
	}

	public static File tokenize(File inputFile) throws IOException,
			FileNotFoundException {
		File temp = File.createTempFile("tokenizer", ".txt");
		// temp.deleteOnExit();
		String[] command = getTokenizerCommand(inputFile);
		// log.info( Arrays.asList( command ) );
		Process p = null;
		try {
			p = Runtime.getRuntime().exec(command);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Trouble executing tree tagger");
			log.error(Arrays.asList(command));
			log.error("Please set whitetext.treetagger.tokenizer");
			System.exit(1);
		}
		InputStream is = p.getInputStream();
		// is = p.getErrorStream();
		OutputStream os = new FileOutputStream(temp);
		int c;
		while ((c = is.read()) != -1) {
			os.write(c);
		}
		os.close();
		return temp;
	}
}

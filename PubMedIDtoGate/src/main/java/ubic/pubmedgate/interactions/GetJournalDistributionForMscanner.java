package ubic.pubmedgate.interactions;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.util.FileTools;
import ubic.gemma.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.model.common.description.BibliographicReference;

public class GetJournalDistributionForMscanner {
	protected static Log log = LogFactory.getLog(GetJournalDistributionForMscanner.class);
	static PubMedSearch pSearch = new PubMedSearch();

	public GetJournalDistributionForMscanner(String filename) throws Exception {
		List<String> rdfText = FileTools.getLines(filename);
		Pattern p = Pattern.compile("pubmed:([^\"]*)");
		log.info("Done getting lines");
		Set<String> pmidsInWebsite = new HashSet<String>();
		for (String line : rdfText) {
			if (line.contains("http://bio2rdf.org/pubmed:")) {
				Matcher m = p.matcher(line);
				m.find();
				// if (m.group(1).startsWith("8")) {
				pmidsInWebsite.add(m.group(1));
				// }

			}
		}
		log.info("PMIDS in website rdf:" + pmidsInWebsite.size());

		// Set<String> mscanner1PMIDs =
		// getMscannerPMIDs("/Users/lfrench/Desktop/results/WhiteTextJournalDistribution/WhiteTextUnseenMScan1.xml");
		// log.info("PMIDS in mscanner1:" + mscanner1PMIDs.size());
		// Set<String> mscannerPMIDs2 =
		// getMscannerPMIDs("/Users/lfrench/Desktop/results/WhiteTextJournalDistribution/WhiteTextUnseenMScan2.xml");
		// log.info("PMIDS in mscanner2:" + mscannerPMIDs2.size());

		// log.info("Mscan1 intersect website:" +
		// Util.intersectSize(mscanner1PMIDs, pmidsInWebsite));
		// log.info("Mscan2 intersect website:" +
		// Util.intersectSize(mscannerPMIDs2, pmidsInWebsite));

		// so all PMID's with connections are loaded.
		// now get journal distribution
		CountingMap<String> journals = new CountingMap<String>();
		CountingMap<Integer> years = new CountingMap<Integer>();

		List<String> pmidsInWebsiteList = new ArrayList<String>(pmidsInWebsite);

		for (int i = 0; i < pmidsInWebsiteList.size() / 10; i += 1) {
			try {
				List<String> subList = pmidsInWebsiteList.subList(i * 10, i * 10 + 10);
				Collection<BibliographicReference> refs = pSearch.searchAndRetrieveIdByHTTP(subList);
				for (BibliographicReference ref : refs) {
					journals.increment(ref.getPublication());
					years.increment(ref.getPublicationDate().getYear());
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Error on PMIDs=" + pmidsInWebsite.size() + " - " + e.getMessage());
			}
		}

		String yearString = "Year\tCount\n";
		String journalString = "Journal\tCount\n";
		for (String key : journals.sortedKeyList()) {
			journalString += key + "\t" + journals.get(key) + "\n";
		}

		for (Integer key : years.sortedKeyList()) {
			yearString += key + "\t" + years.get(key) + "\n";
		}
		FileTools.stringToFile(journalString,
				new File("/Users/lfrench/Desktop/results/WhiteTextJournalDistribution/journalDistroLoad" + System.currentTimeMillis() + ".txt"));
		FileTools.stringToFile(yearString, new File("/Users/lfrench/Desktop/results/WhiteTextJournalDistribution/YearDistroLoad" + System.currentTimeMillis()
				+ ".txt"));

	}

	// http://bio2rdf.org/pubmed:

	public Set<String> getMscannerPMIDs(String filename) throws Exception {
		log.info("Reading XML");
		SAXReader saxReader = new SAXReader();
		Document document = saxReader.read(filename);

		String path = "//corpus/document/sentence/pair";
		List list = document.selectNodes(path);
		Iterator iter = list.iterator();
		int pairCount = 0;
		Set<String> pmids = new HashSet<String>();
		while (iter.hasNext()) {
			Element pair = (Element) iter.next();
			pairCount++;
			String pairID = pair.attributeValue("id");

			// get pairs sentence
			Element sentence = pair.getParent();

			// get pair's PMID
			Element abstractElement = sentence.getParent();
			String PMID = abstractElement.attributeValue("origID");
			pmids.add(PMID);
			// log.info("|" + PMID + "|");

			// PMIDs.add( PMID );
		}
		return pmids;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		GetJournalDistributionForMscanner x = new GetJournalDistributionForMscanner(
				"/Users/lfrench/Desktop/results/WhiteTextJournalDistribution/WhiteTextWeb.model.plus.mscanner.rdf");

	}

}

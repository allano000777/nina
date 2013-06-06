package edu.nd.nina.io;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import edu.jhu.nlp.wikipedia.PageCallbackHandler;
import edu.jhu.nlp.wikipedia.WikiPage;
import edu.jhu.nlp.wikipedia.WikiXMLParser;
import edu.jhu.nlp.wikipedia.WikiXMLParserFactory;

public class WikidumpToHbase {

	public static void main(String[] args) {
		Configuration config = HBaseConfiguration.create();
		config.set("hbase.zookeeper.quorum", "dmserv3.cs.illinois.edu");

		try {
			final HTable table = new HTable(config, "wikipedia");

			WikiXMLParser wxsp = WikiXMLParserFactory.getSAXParser("");

			wxsp.setPageCallback(new PageCallbackHandler() {
				int i = 0;

				public void process(WikiPage page) {

					String title = page.getTitle();
					title = title.toLowerCase().trim();

					System.out.println(i + ": " + title);
					Put p = new Put(Bytes.toBytes(title));
					p.add(Bytes.toBytes("p"), Bytes.toBytes("t"),
							Bytes.toBytes(title));
					p.add(Bytes.toBytes("p"), Bytes.toBytes("id"),
							Bytes.toBytes(page.getID()));
					p.add(Bytes.toBytes("p"), Bytes.toBytes("text"),
							Bytes.toBytes(page.getText()));
					p.add(Bytes.toBytes("p"), Bytes.toBytes("wt"),
							Bytes.toBytes(page.getWikiText()));
					p.add(Bytes.toBytes("p"), Bytes.toBytes("isDis"),
							Bytes.toBytes(page.isDisambiguationPage()));
					p.add(Bytes.toBytes("p"), Bytes.toBytes("isRed"),
							Bytes.toBytes(page.isRedirect()));
					p.add(Bytes.toBytes("p"), Bytes.toBytes("isSpec"),
							Bytes.toBytes(page.isSpecialPage()));
					p.add(Bytes.toBytes("p"), Bytes.toBytes("isStub"),
							Bytes.toBytes(page.isStub()));

					for (String s : page.getCategories()) {
						s = s.toLowerCase().trim();
						p.add(Bytes.toBytes("p"), Bytes.toBytes("cat"),
								Bytes.toBytes(s));
					}

					for (String s : page.getLinks()) {
						s = s.toLowerCase().trim();
						p.add(Bytes.toBytes("p"), Bytes.toBytes("cat"),
								Bytes.toBytes(s));
					}

					try {
						table.put(p);
					} catch (IOException e) {
						e.printStackTrace();
					}

					// graph.addVertex(ins);
					i++;

				}
			});

			wxsp.parse();
			table.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
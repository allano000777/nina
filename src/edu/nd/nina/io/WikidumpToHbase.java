package edu.nd.nina.io;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
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
			createTable(config, "wikipedia", new String[] {"p", "c", "ol"});
			final HTable table = new HTable(config, "wikipedia");
			table.setAutoFlush(false);
			WikiXMLParser wxsp = WikiXMLParserFactory.getSAXParser("C:\\Users\\weninger\\Downloads\\enwiki-latest-pages-articles.xml.bz2");

			wxsp.setPageCallback(new PageCallbackHandler() {
				int i = 0;

				public void process(WikiPage page) {

					String title = page.getTitle();
					title = title.trim();

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
						s = s.trim();
						p.add(Bytes.toBytes("c"), Bytes.toBytes(s),
								Bytes.toBytes(s));
					}

					for (String s : page.getLinks()) {
						s = s.trim();
						p.add(Bytes.toBytes("ol"), Bytes.toBytes(s),
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
	
	/**
     * Create a table
     */
    public static void createTable(Configuration conf, String tableName, String[] familys)
            throws Exception {
        HBaseAdmin admin = new HBaseAdmin(conf);
        if (admin.tableExists(tableName)) {
            System.out.println("table already exists!");
        } else {
            HTableDescriptor tableDesc = new HTableDescriptor(tableName);
            for (int i = 0; i < familys.length; i++) {
                tableDesc.addFamily(new HColumnDescriptor(familys[i]));
            }
            admin.createTable(tableDesc);
            System.out.println("create table " + tableName + " ok.");
        }
        admin.close();
    }

}
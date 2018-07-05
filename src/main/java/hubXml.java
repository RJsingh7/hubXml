import org.jdom2.CDATA;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

public class hubXml {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36";
    public static final String ROOT_URL = "https://www.awesomeblog.com";
    public static final String[] POSTS = {"https://www.awesomeblog.com/awesome-post-1", "https://www.awesomeblog.com/awesome-post-2", "https://www.awesomeblog.com/awesome-post-3"};
    // TODO FIGURED OUT HOW TO FIND POST URLS

    public static void main(String[] args) {

        // XML Setup
        Namespace ce = Namespace.getNamespace("content", "http://purl.org/rss/1.0/modules/content/");
        Namespace ee = Namespace.getNamespace("excerpt", "http://wordpress.org/export/1.2/excerpt/");
        Namespace wp = Namespace.getNamespace("wp", "http://wordpress.org/export/1.2/");
        Namespace dc = Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/");
        ArrayList<String> authorList = new ArrayList<String>();
        List items = new ArrayList();

        Document document = new Document();
        Element rss = new Element("rss");
        Element channel = new Element("channel");
        rss.addContent(channel);
        rss.addNamespaceDeclaration(ce);
        rss.addNamespaceDeclaration(ee);
        rss.addNamespaceDeclaration(wp);
        rss.addNamespaceDeclaration(dc);
        Element rootLink = new Element("link").setText(ROOT_URL);
        channel.addContent(rootLink);

        for(int i=0; i< POSTS.length; i++) {

            try {
                // Fetch the page
                org.jsoup.nodes.Document doc = Jsoup.connect(POSTS[i]).userAgent(USER_AGENT).get();

                // Soups you likely do not need to touch
                String title = doc.title();
                String metaD = doc.select("meta[name=description]").get(0).attr("content");

                // Soups you likely need to touch
                String author = doc.select("a[rel=author]").get(0).text();
                Elements tags = doc.select("a[rel=category tag]");
                String postBody = doc.select(".entry-content").get(0).toString();
                // TODO FIGURE OUT HOW TO GRAB PUBLISH DATE WELL

                // Build XML item
                // Build <item>
                Element item = new Element("item");

                // Build <title>
                item.addContent(new Element("title").setText(title));

                // Build <link>
                item.addContent(new Element("link").setText(POSTS[i]));

                // Build <pubDate>
                item.addContent(new Element("pubDate").setText("Wed, 25 Apr 2018 13:19:35 +0000"));

                // Build <wp:postIid>
                Element wpPostId = new Element("post_id", wp);
                wpPostId.setText(String.valueOf(i + 1));
                wpPostId.removeAttribute("wp");
                item.addContent(wpPostId);

                // Build <wp:status>
                Element wpStatus = new Element("status", wp);
                wpStatus.setText("publish");
                item.addContent(wpStatus);

                // Build <wp:post_type>
                Element wpPostType = new Element("post_type", wp);
                wpPostType.setText("post");
                item.addContent(wpPostType);

                // Build <excerpt:encoded>
                Element excerptEncoded = new Element("encoded", ee);
                CDATA excerptEncodedCdata = new CDATA(metaD);
                excerptEncoded.setContent(excerptEncodedCdata);
                item.addContent(excerptEncoded);

                // Build <dc:creator>
                Element dcCreator = new Element("creator", dc);
                dcCreator.setText(author);
                item.addContent(dcCreator);
                // Build <wp:author>
                if (!authorList.contains(author)) {
                    authorList.add(author);
                    Element wpAuthor = new Element("author", wp);
                    channel.addContent(wpAuthor);
                    CDATA wpAuthorDisplayNameCdata = new CDATA(author);
                    CDATA wpAuthorloginCdata = new CDATA(author);
                    Element wpAuthorDisplayName = new Element("author_display_name", wp).addContent(wpAuthorDisplayNameCdata);
                    Element wpAuthorlogin = new Element("author_login", wp).addContent(wpAuthorloginCdata);
                    wpAuthor.addContent(wpAuthorDisplayName);
                    wpAuthor.addContent(wpAuthorlogin);
                }

                // Build <category>(s)
                for (org.jsoup.nodes.Element tag : tags) {
                    Element category = new Element("category").setText(tag.ownText());
                    category.setAttribute("domain", "category");
                    category.setAttribute("nicename", tag.ownText().replace(" ", "-"));
                    item.addContent(category);
                }

                // Build <content:encoded>
                Element contentEncoded = new Element("encoded", ce);
                CDATA contentEncodedCdata = new CDATA(postBody);
                contentEncoded.setContent(contentEncodedCdata);
                item.addContent(contentEncoded);

                // Add Built <item> to list items
                items.add(item);


            } catch(Exception e) {
                System.out.println("ERROR I just spent a day at the beach mate... " + e.getMessage());
                e.printStackTrace();
            }

        }

        // Finally add <item>(s) to <channel> to ensure <wp:authors> are on top
        channel.addContent(items);
        document.setContent(rss);

        try {
            FileWriter writer = new FileWriter("blog.xml");
            XMLOutputter outputter = new XMLOutputter();
            outputter.setFormat(Format.getPrettyFormat());
            outputter.output(document, writer);
            outputter.output(document, System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}

// DESIRED XML OUTPUT
//<?xml version='1.0' encoding='UTF-8'?>
//<rss>
//  <channel>
//    <link>https://www.awesomeblog.com</link>
//    <wp:author>
//      <wp:author_display_name><![CDATA[author]]></wp:author_display_name>
//      <wp:author_login><![CDATA[author]]></wp:author_login>
//    </wp:author>
//    <item>
//      <title>Post Title</title>
//      <pubDate>Wed, 25 Apr 2018 13:19:35 +0000</pubDate>
//      <link>https://www.awesomeblog.com/awesome-post</link>
//      <wp:post_id>1</wp:post_id>
//      <wp:status>publish</wp:status>
//      <wp:post_type>post</wp:post_type>
//      <dc:creator>Author</dc:creator>
//      <category domain="category" nicename="This-is-a-tag"><![CDATA[This is a tag]]></category>
//      <excerpt:encoded><![CDATA[This is the meta description of my awesome post!]]></excerpt:encoded>
//      <content:encoded><![CDATA[<div>This is the post body</div>]]></content:encoded>
//    </item>
//    ...
//  </channel>
//</rss>
package cn.edu.zju.test;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by gzx-zju on 2017/11/21.
 * test dom4j
 */
public class TestDom4j {
    public static Document createDocument(){
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("root");

        root.addElement("author")
                .addAttribute("name", "James")
                .addAttribute("location", "UK")
                .addText("James");

        root.addElement("author")
                .addAttribute("name", "Bill")
                .addAttribute("location", "US")
                .addText("Bill");

        return document;
    }

    public static void main(String[] args) throws IOException {
        Document document = createDocument();
        OutputFormat format = new OutputFormat();
        format.setIndent(true);
        format.setIndent("    ");
        format.setEncoding("UTF-8");
        format.setNewlines(true);
        FileWriter out = new FileWriter("foo.xml");
        XMLWriter writer = new XMLWriter(out, format);
        writer.write(document);
        writer.close();
    }
}

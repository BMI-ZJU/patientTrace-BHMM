package cn.edu.zju.util;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

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

    public static void writeDocument() throws IOException {
        Document document = createDocument();
        OutputFormat format = new OutputFormat();
        format.setIndent(true);
        format.setIndent("    ");
        format.setEncoding("UTF-8");
        format.setNewlines(true);

        FileWriter out = new FileWriter("resources/foo.xml");
        XMLWriter writer = new XMLWriter(out, format);
        writer.write(document);
        writer.close();
    }

    public static void parseXML() throws DocumentException {
        String rootPath = "resources/patientTrace/";
        File root = new File(rootPath);

        File[] files = root.listFiles();

        assert files != null;
        File file = files[0];
        String filename = file.getPath();
        System.out.println(filename);
        SAXReader reader = new SAXReader();
        Document document = reader.read(filename);
        Element rootElement = document.getRootElement();
        System.out.println(rootElement.getName());

        List<Element> orders = rootElement.element("orders").elements();
        for (Element order : orders) {
            if( order.attributeValue("dosage") != null) {
                System.out.println(order.attribute("dosage").getValue());
            }
        }
    }

    public static void main(String[] args) throws IOException, DocumentException {
        String t = "1/日";
        double f = Double.parseDouble(t.substring(0, t.length()-2));
        System.out.println(f);
    }
}

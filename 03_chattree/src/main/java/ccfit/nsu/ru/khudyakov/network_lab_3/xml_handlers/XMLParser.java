package ccfit.nsu.ru.khudyakov.network_lab_3.xml_handlers;

import ccfit.nsu.ru.khudyakov.network_lab_3.messages.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;

public class XMLParser {
    private static XMLParser xmlParser;
    private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    private XMLParser() {
    }

    public static XMLParser getInstance() {
        if (xmlParser == null) {
            xmlParser = new XMLParser();
        }
        return xmlParser;
    }

    public Message parseMessage(byte[] xml, int length) {
        Message message = null;

        try {
            Document xmlInputMessage = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml, 0, length));
            String tag = xmlInputMessage.getDocumentElement().getTagName();
            if (tag.equals("message")) {
                String messageID = xmlInputMessage.getElementsByTagName("messageID").item(0).getTextContent();
                MessageType type = MessageType.get(xmlInputMessage.getDocumentElement().getFirstChild().getTextContent());

                switch (type) {
                    case CONNECT:
                        message = new ConnectMessage(messageID);
                        break;

                    case CONFIRM:
                        message = new ConfirmMessage(messageID);
                        break;

                    case TEXT:
                        String name = xmlInputMessage.getElementsByTagName("name").item(0).getTextContent();
                        String content = xmlInputMessage.getElementsByTagName("content").item(0).getTextContent();
                        message = new TextMessage(content, messageID, name);
                        break;

                    case ALIVE:
                        message = new AliveMessage(messageID);
                        break;

                    case ALTERNATE:
                        InetAddress ip = InetAddress.getByName(xmlInputMessage.getElementsByTagName("alternateIp").item(0).getTextContent());
                        int port =Integer.valueOf(xmlInputMessage.getElementsByTagName("alternatePort").item(0).getTextContent());

                        message = new AlternateMessage(messageID, ip, port);
                        break;
                }
            }
        } catch (SAXException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
        }

        return message;
    }
}

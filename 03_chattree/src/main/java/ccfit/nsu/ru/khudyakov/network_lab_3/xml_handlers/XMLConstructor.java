package ccfit.nsu.ru.khudyakov.network_lab_3.xml_handlers;


import ccfit.nsu.ru.khudyakov.network_lab_3.messages.MessageType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;

public class XMLConstructor {
    private static DocumentBuilder documentBuilder;
    private static Transformer transformer;

    static {
        try {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            transformer = TransformerFactory.newInstance().newTransformer();
        } catch (ParserConfigurationException | TransformerConfigurationException e) {
            e.printStackTrace();
        }
    }

    private static XMLConstructor xmlConstructor;

    private XMLConstructor() {
    }

    public static XMLConstructor getInstance() {
        if (xmlConstructor == null) {
            xmlConstructor = new XMLConstructor();
        }
        return xmlConstructor;
    }

    private Document createMsg(MessageType messageType, String msgID) {
        Document document = documentBuilder.newDocument();
        Element message = document.createElement("message");
        document.appendChild(message);

        Element type = document.createElement("type");
        type.setTextContent(messageType.getType());
        message.appendChild(type);

        Element messageID = document.createElement("messageID");
        messageID.setTextContent(msgID);
        message.appendChild(messageID);

        return document;
    }

    private byte[] documentToByteArray(Document document) {
        DOMSource source = new DOMSource(document);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(bos);

        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    public byte[] createAlternateMsg(String messageID, String alternateIp, int alternatePort) {
        Document document = createMsg(MessageType.ALTERNATE, messageID);
        Element message = document.getDocumentElement();

        Element elAlternatePort = document.createElement("alternatePort");
        elAlternatePort.setTextContent(String.valueOf(alternatePort));
        message.appendChild(elAlternatePort);

        Element elAlternateIp = document.createElement("alternateIp");
        elAlternateIp.setTextContent(alternateIp);
        message.appendChild(elAlternateIp);

        return documentToByteArray(document);
    }

    public byte[] createTextMsg(String messageID, String messageText, String name) {
        Document document = createMsg(MessageType.TEXT, messageID);
        Element message = document.getDocumentElement();

        Element elName = document.createElement("name");
        elName.setTextContent(name);
        message.appendChild(elName);

        Element elContent = document.createElement("content");
        elContent.setTextContent(messageText);
        message.appendChild(elContent);

        return documentToByteArray(document);
    }

    public byte[] createConfirmDeliveryMsg(String messageID) {
        return documentToByteArray(createMsg(MessageType.CONFIRM, messageID));
    }

    public byte[] createConnectMsg(String messageID) {
        return documentToByteArray(createMsg(MessageType.CONNECT, messageID));
    }

    public byte[] createAliveMsg(String messageID) {
        return documentToByteArray(createMsg(MessageType.ALIVE, messageID));
    }

}

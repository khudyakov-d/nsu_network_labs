package ccfit.nsu.ru.khudyakov.network_lab_3.messages;

public class ConnectMessage extends Message{

    public ConnectMessage(String messageID) {
        super(MessageType.CONNECT, messageID);
    }
}

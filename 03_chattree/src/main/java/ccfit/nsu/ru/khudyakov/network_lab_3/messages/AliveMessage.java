package ccfit.nsu.ru.khudyakov.network_lab_3.messages;

public class AliveMessage extends Message {
    public AliveMessage(String messageID) {
        super(MessageType.ALIVE, messageID);
    }

}

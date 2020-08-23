package ccfit.nsu.ru.khudyakov.network_lab_3.messages;

public abstract class Message {
    private MessageType type;
    private String messageID;

    public Message(MessageType type, String messageID) {
        this.type = type;
        this.messageID = messageID;
    }

    public MessageType getType() {
        return type;
    }

    public String getMessageID() {
        return messageID;
    }
}

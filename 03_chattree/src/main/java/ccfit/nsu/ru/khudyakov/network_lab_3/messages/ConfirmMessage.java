package ccfit.nsu.ru.khudyakov.network_lab_3.messages;

public class ConfirmMessage extends Message {

    public ConfirmMessage(String messageID) {
        super(MessageType.CONFIRM, messageID);
    }

}

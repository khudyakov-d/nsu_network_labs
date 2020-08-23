package ru.nsu.ccfit.khudyakov.network_lab_5;

import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SocksProxy {
    private final static Logger LOGGER = Logger.getLogger(SocksProxy.class.getName());
    private final int BUF_SIZE = 1024 * 8;

    private int lastID = 0;

    private Selector selector;

    private SelectionKey dnsKey;
    private Map<Integer, SelectionKey> dnsKeys = new HashMap<>();

    private byte[] successfulAnswer = new byte[]{0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private byte[] unsuccessfulAnswer = new byte[]{0x05, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private final byte[] METHOD = new byte[]{0x05, 0x00};

    public SocksProxy(int port) {
        try {
            LOGGER.setLevel(Level.OFF);

            selector = SelectorProvider.provider().openSelector();

            ServerSocketChannel mainServerChannel = ServerSocketChannel.open();
            mainServerChannel.configureBlocking(false);
            mainServerChannel.socket().bind(new InetSocketAddress(port));
            mainServerChannel.register(selector, mainServerChannel.validOps());

            int UDP_PORT = 4338;
            DatagramChannel dnsChannel = DatagramChannel.open();
            dnsChannel.configureBlocking(false);
            dnsChannel.socket().bind(new InetSocketAddress(UDP_PORT));
            dnsKey = dnsChannel.register(selector, SelectionKey.OP_READ);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startProxying() {
        try {
            while (selector.select() > -1) {
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isValid()) {
                        try {
                            if (key.isAcceptable()) {
                                acceptQuery(key);
                            } else if (key.isConnectable()) {
                                completeConnection(key);
                            } else if (key.isReadable()) {
                                if (key.equals(dnsKey)) {
                                    getPeerIpAddress(key);
                                } else {
                                    read(key);
                                }
                            } else if (key.isWritable()) {
                                write(key);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void acceptQuery(SelectionKey key) throws IOException {
        SocketChannel newAcceptingChannel = ((ServerSocketChannel) key.channel()).accept();

        LOGGER.info("accept: " + " " + newAcceptingChannel.getRemoteAddress());

        newAcceptingChannel.configureBlocking(false);
        newAcceptingChannel.register(key.selector(), SelectionKey.OP_READ);
    }

    private void completeConnection(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        Attachment attachment = (Attachment) key.attachment();

        try {
            while (channel.isConnectionPending()) {
                LOGGER.info("complete connection: " + " " + channel.getRemoteAddress());
                channel.finishConnect();

                attachment.setInputData(ByteBuffer.allocate(BUF_SIZE));
                attachment.getInputData().put(successfulAnswer).flip();
                attachment.getPeerKey().interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                key.interestOps(0);

                attachment.setOutputData(((Attachment) attachment.getPeerKey().attachment()).getInputData());
                ((Attachment) attachment.getPeerKey().attachment()).setOutputData(attachment.getInputData());

                attachment.setState(SocksState.PASSING_DATA);
            }
        } catch (IOException e) {
            prepareAnswer(key, unsuccessfulAnswer);
            closeKey(key);
            e.printStackTrace();
        }
    }

    private void prepareAnswer(SelectionKey key, byte[] answer) {
        Attachment attachment = (Attachment) key.attachment();
        attachment.getPeerKey().interestOps(attachment.getPeerKey().interestOps() | SelectionKey.OP_WRITE);

        Attachment peerAttachment = (Attachment) attachment.getPeerKey().attachment();
        peerAttachment.setOutputData(ByteBuffer.wrap(answer, 0, answer.length));
    }


    private void getPeerIpAddress(SelectionKey key) throws IOException {

        DatagramChannel channel = ((DatagramChannel) key.channel());
        ByteBuffer byteBuffer = (ByteBuffer.allocate(BUF_SIZE));
        channel.receive(byteBuffer);
        byteBuffer.flip();

        Message response = new Message(byteBuffer);

        if (dnsKeys.containsKey(response.getHeader().getID())) {
            Record[] records = response.getSectionArray(Section.ANSWER);
            SelectionKey regKey = dnsKeys.get(response.getHeader().getID());

            LOGGER.info(" get peer ip: " + " " + ((SocketChannel) regKey.channel()).getRemoteAddress());

            if (records.length >= 1) {
                boolean flag = false;

                for (Record record : records) {
                    if (record.getType() == 1) {
                        handleIpV4(record.rdataToWireCanonical(), regKey);
                        flag = true;
                        break;
                    }
                }

                if (!flag) {
                    throw new IllegalStateException("No answer for dns query");
                }
            } else {
                throw new IllegalStateException("No answer for dns query");
            }
        } else {
            throw new IllegalStateException("Unknown dns response id");
        }
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = ((SocketChannel) key.channel());
        Attachment attachment = (Attachment) key.attachment();

        if (attachment == null) {
            key.attach(attachment = new Attachment());
            attachment.setInputData(ByteBuffer.allocate(BUF_SIZE));
        }

        try {
            if (attachment.isOutputShutdown()) {
                channel.shutdownInput();
                key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
            } else if (channel.read(attachment.getInputData()) == -1) {
                closeInput(key);
            } else {
                switch (attachment.getState()) {
                    case METHOD_SENDING:
                        LOGGER.info("read method: " + " " + channel.getRemoteAddress());

                        defineMethod(key);
                        break;
                    case ANSWER_SENDING:
                        LOGGER.info("read answer: " + " " + channel.getRemoteAddress());

                        defineAnswer(key);
                        break;
                    case PASSING_DATA:
                        LOGGER.info("read data: " + " " + channel.getRemoteAddress());

                        key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
                        attachment.getPeerKey().interestOps(attachment.getPeerKey().interestOps() | SelectionKey.OP_WRITE);

                        attachment.getInputData().flip();
                        break;
                }
            }
        } catch (IOException e) {
            closeInput(key);
            e.printStackTrace();
        }
    }

    private void closeInput(SelectionKey key) throws IOException {
        SocketChannel channel = ((SocketChannel) key.channel());
        Attachment attachment = (Attachment) key.attachment();

        LOGGER.info("input shutdown: " + " " + channel.getRemoteAddress());
        key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
        channel.shutdownInput();
        attachment.getInputData().flip();

        if (attachment.getPeerKey() != null) {
            ((Attachment) attachment.getPeerKey().attachment()).setInputShutdown(true);

            if (attachment.getInputData().remaining() > 0) {
                attachment.getPeerKey().interestOps(attachment.getPeerKey().interestOps() | SelectionKey.OP_WRITE);
            } else {
                ((SocketChannel) attachment.getPeerKey().channel()).shutdownOutput();
                attachment.getPeerKey().interestOps(attachment.getPeerKey().interestOps() & ~SelectionKey.OP_WRITE);
            }
        }
    }


    private void defineMethod(SelectionKey key) throws IOException {
        Attachment attachment = (Attachment) key.attachment();
        byte[] ar = attachment.getInputData().array();

        if (ar[0] != 0x5) {
            closeKey(key);
            throw new IllegalStateException("Bad Request: not correct version of protocol");
        } else {
            attachment.setOutputData((ByteBuffer) ByteBuffer.allocate(METHOD.length).put(METHOD).flip());
            key.interestOps(key.interestOps() & ~SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            attachment.getInputData().clear();
        }
    }

    private void defineAnswer(SelectionKey key) throws IOException {
        Attachment attachment = (Attachment) key.attachment();
        byte[] ar = attachment.getInputData().array();

        if (ar[0] != 0x5) {
            closeKey(key);
            throw new IllegalStateException("Bad Request: not correct version of protocol");
        } else if (ar[1] != 0x1) {
            closeKey(key);
            throw new IllegalStateException("Bad Request: this type of command not allowed");
        } else if (ar[3] == 0x4) {
            closeKey(key);
            throw new IllegalStateException("Bad Request: ipv6 not allowed");
        } else {
            byte[] addr;

            if (ar[3] == 0x1) {
                addr = new byte[]{ar[4], ar[5], ar[6], ar[7]};
                attachment.setPeerPort((((0xFF & ar[8]) << 8) + (0xFF & ar[9])));
                handleIpV4(addr, key);
            } else if (ar[3] == 0x3) {
                byte length = (byte) (0xFF & ar[4]);
                addr = new byte[length];
                System.arraycopy(ar, 5, addr, 0, length);

                attachment.setPeerPort((((0xFF & ar[5 + length]) << 8) + (0xFF & ar[6 + length])));
                sendDnsQuestion(addr, key);
            } else {
                closeKey(key);
                throw new IllegalStateException("Bad Request: Unknown error");
            }
        }

        key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
    }

    private void handleIpV4(byte[] addr, SelectionKey key) throws IOException {
        SocketChannel peer = SocketChannel.open();
        peer.configureBlocking(false);
        peer.connect(new InetSocketAddress(InetAddress.getByAddress(addr), ((Attachment) key.attachment()).getPeerPort()));

        SelectionKey peerKey = peer.register(key.selector(), SelectionKey.OP_CONNECT);
        ((Attachment) key.attachment()).setPeerKey(peerKey);
        ((Attachment) key.attachment()).getInputData().clear();

        Attachment peerAttachment = new Attachment();
        peerAttachment.setPeerKey(key);
        peerKey.attach(peerAttachment);
    }

    private void sendDnsQuestion(byte[] addr, SelectionKey key) throws IOException {
        Attachment attachment = (Attachment) key.attachment();

        LOGGER.info("send dns question: " + " " + ((SocketChannel) key.channel()).getRemoteAddress());

        Message message = new Message();
        message.addRecord(Record.newRecord(new Name((new String(addr)) + "."), Type.A, DClass.IN), Section.QUESTION);

        Header header = message.getHeader();
        header.setOpcode(Opcode.QUERY);
        header.setID(lastID);
        header.setFlag(Flags.RD);

        byte[] wire = message.toWire();

        String[] dnsServers = ResolverConfig.getCurrentConfig().servers();
        InetAddress socketAddress = InetAddress.getByName(dnsServers[0]);

        dnsKeys.put(lastID++, key);
        ((DatagramChannel) dnsKey.channel()).send(ByteBuffer.wrap(wire, 0, wire.length), new InetSocketAddress(socketAddress, 53));
        attachment.getInputData().clear();
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel channel = ((SocketChannel) key.channel());
        Attachment attachment = ((Attachment) key.attachment());

        try {
            if (channel.write(attachment.getOutputData()) == -1) {
                closeOutput(key);
            } else {
                switch (attachment.getState()) {
                    case METHOD_SENDING:
                        LOGGER.info("write method: " + " " + channel.getRemoteAddress());

                        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE | SelectionKey.OP_READ);
                        attachment.setState(SocksState.ANSWER_SENDING);
                        break;
                    case ANSWER_SENDING:
                        LOGGER.info("write answer: " + " " + channel.getRemoteAddress());

                        attachment.setState(SocksState.PASSING_DATA);
                        break;
                    case PASSING_DATA:
                        if (attachment.isInputShutdown()) {
                            if (attachment.getOutputData().remaining() == 0) {
                                channel.shutdownOutput();
                                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);

                                LOGGER.info("output shutdown: " + " " + channel.getRemoteAddress());
                            }
                        } else {
                            LOGGER.info("write data: " + " " + channel.getRemoteAddress());

                            attachment.getOutputData().compact();
                            attachment.getPeerKey().interestOps(attachment.getPeerKey().interestOps() | SelectionKey.OP_READ);
                            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                        }
                        break;
                }
            }
        } catch (IOException e) {
            closeOutput(key);
            e.printStackTrace();
        }
    }

    private void closeOutput(SelectionKey key) throws IOException {
        SocketChannel channel = ((SocketChannel) key.channel());
        Attachment attachment = ((Attachment) key.attachment());

        channel.shutdownOutput();
        ((Attachment) attachment.getPeerKey().attachment()).setOutputShutdown(true);
        if (!attachment.isInputShutdown()) {
            attachment.getPeerKey().interestOps(attachment.getPeerKey().interestOps() | SelectionKey.OP_READ);
        }
        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
    }

    private void closeKey(SelectionKey key) throws IOException {
        key.cancel();
        key.channel().close();
    }

    public static void main(String[] args) {
        SocksProxy socksProxy = new SocksProxy(1080);
        socksProxy.startProxying();
    }

}
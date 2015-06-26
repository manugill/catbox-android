package com.flooat.catbox.models;

public class Message {

    public static final int TYPE_MESSAGE = 0;
    public static final int TYPE_LOG = 1;
    public static final int TYPE_ACTION = 2;

    private int type;
    private String message;
    private String name;

    public static class Builder {
        private final int type;
        private String name;
        private String message;

        public Builder(int sType) {
            type = sType;
        }

        public Builder name(String sName) {
            name = sName;
            return this;
        }

        public Builder message(String sMessage) {
            message = sMessage;
            return this;
        }

        public Message build() {
            Message messageObject = new Message();
            messageObject.type = type;
            messageObject.name = name;
            messageObject.message = message;

            return messageObject;
        }
    }

    public int getType() {
        return type;
    };
    public String getName() {
        return name;
    };
    public String getMessage() {
        return message;
    };

}

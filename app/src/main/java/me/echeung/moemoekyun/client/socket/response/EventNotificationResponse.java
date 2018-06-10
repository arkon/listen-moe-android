package me.echeung.moemoekyun.client.socket.response;

import lombok.Getter;

@Getter
public class EventNotificationResponse extends NotificationResponse {
    public static final String TYPE = "EVENT";

    private Details d;

    @Getter
    public static class Details extends NotificationResponse.Details {
        private Event event;
    }

    @Getter
    public static class Event {
        private int id;
        private String image;
        private String name;
        private String slug;
    }
}

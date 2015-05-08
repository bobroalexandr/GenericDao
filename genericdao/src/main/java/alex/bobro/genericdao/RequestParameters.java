package alex.bobro.genericdao;


public class RequestParameters {

    public enum NotificationMode {
        FOR_EACH, AFTER_ALL;
    }

    public enum RequestMode {
        FULL, JUST_NESTED, JUST_PARENT, PARENT_WITH_MANY_TO_ONE
    }

    private RequestMode requestMode;
    private NotificationMode notificationMode;

    private RequestParameters(Builder builder) {
        this.requestMode = builder.requestMode;
        this.notificationMode = builder.notificationMode;
    }

    public RequestMode getRequestMode() {
        return requestMode;
    }

    public NotificationMode getNotificationMode() {
        return notificationMode;
    }

    public static class Builder {

        private RequestMode requestMode;
        private NotificationMode notificationMode;

        public Builder() {
            requestMode = RequestMode.FULL;
            notificationMode = NotificationMode.FOR_EACH;
        }

        public RequestParameters build() {
            return new RequestParameters(this);
        }

        public Builder withRequestMode(RequestMode requestMode) {
            this.requestMode = requestMode;
            return this;
        }

        public Builder withNotificationMode(NotificationMode notificationMode) {
            this.notificationMode = notificationMode;
            return this;
        }
    }
}

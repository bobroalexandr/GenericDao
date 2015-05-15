package alex.bobro.genericdao;


public class RequestParameters {

    public enum NotificationMode {
        FOR_EACH, AFTER_ALL;
    }

    public enum RequestMode {
        FULL, JUST_NESTED, JUST_PARENT
    }

    private RequestMode requestMode;
    private NotificationMode notificationMode;
    private boolean isManyToOneNestedAffected;

    private RequestParameters(Builder builder) {
        this.requestMode = builder.requestMode;
        this.notificationMode = builder.notificationMode;
        this.isManyToOneNestedAffected = builder.isManyToOneNestedAffected;
    }

    public RequestMode getRequestMode() {
        return requestMode;
    }

    public NotificationMode getNotificationMode() {
        return notificationMode;
    }

    public boolean isManyToOneNestedAffected() {
        return isManyToOneNestedAffected;
    }

    public static class Builder {

        private RequestMode requestMode;
        private NotificationMode notificationMode;
        private boolean isManyToOneNestedAffected;

        public Builder() {
            requestMode = RequestMode.FULL;
            notificationMode = NotificationMode.FOR_EACH;
            isManyToOneNestedAffected = true;
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

        public Builder withIsManyToOneNestedAffected(boolean isManyToOneNestedAffected) {
            this.isManyToOneNestedAffected = isManyToOneNestedAffected;
            return this;
        }
    }
}

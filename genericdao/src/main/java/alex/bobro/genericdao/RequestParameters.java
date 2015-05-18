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
    private boolean isManyToOneGotWithParent;

    private RequestParameters(Builder builder) {
        this.requestMode = builder.requestMode;
        this.notificationMode = builder.notificationMode;
        this.isManyToOneGotWithParent = builder.isManyToOneGotWithParent;
    }

    public RequestMode getRequestMode() {
        return requestMode;
    }

    public NotificationMode getNotificationMode() {
        return notificationMode;
    }

    public boolean isManyToOneGotWithParent() {
        return isManyToOneGotWithParent;
    }

    public static class Builder {

        private RequestMode requestMode;
        private NotificationMode notificationMode;
        private boolean isManyToOneGotWithParent;

        public Builder() {
            requestMode = RequestMode.FULL;
            notificationMode = NotificationMode.FOR_EACH;
            isManyToOneGotWithParent = true;
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

        public Builder withIsManyToOneGotWithParent(boolean isManyToOneGotWithParent) {
            this.isManyToOneGotWithParent = isManyToOneGotWithParent;
            return this;
        }
    }
}

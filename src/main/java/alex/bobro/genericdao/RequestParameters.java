package alex.bobro.genericdao;


import java.util.HashMap;
import java.util.Map;

public class RequestParameters {

    enum NotificationMode {
        FOR_EACH, AFTER_ALL;
    }

    private boolean isDeep;
    private NotificationMode notificationMode;

    private RequestParameters(Builder builder) {
        this.isDeep = builder.isDeep;
        this.notificationMode = builder.notificationMode;
    }

    public boolean isDeep() {
        return isDeep;
    }

    public NotificationMode getNotificationMode() {
        return notificationMode;
    }

    public static class Builder {

        private boolean isDeep;
        private NotificationMode notificationMode;

        public Builder() {
            isDeep = true;
            notificationMode = NotificationMode.FOR_EACH;
        }

        public RequestParameters build() {
            return new RequestParameters(this);
        }

        public Builder withIsDeep(boolean isDeep) {
            this.isDeep = isDeep;
            return this;
        }

        public Builder withNotificationMode(NotificationMode notificationMode) {
            this.notificationMode = notificationMode;
            return this;
        }
    }
}

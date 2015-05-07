package alex.bobro.genericdao;


public class RequestParameters {

    public enum NotificationMode {
        FOR_EACH, AFTER_ALL;
    }

    public enum SavingMode {
        FULL, JUST_NESTED, JUST_PARENT;
    }

    private SavingMode savingMode;
    private NotificationMode notificationMode;

    private RequestParameters(Builder builder) {
        this.savingMode = builder.savingMode;
        this.notificationMode = builder.notificationMode;
    }

    public SavingMode getSavingMode() {
        return savingMode;
    }

    public NotificationMode getNotificationMode() {
        return notificationMode;
    }

    public static class Builder {

        private SavingMode savingMode;
        private NotificationMode notificationMode;

        public Builder() {
            savingMode = SavingMode.FULL;
            notificationMode = NotificationMode.FOR_EACH;
        }

        public RequestParameters build() {
            return new RequestParameters(this);
        }

        public Builder withSavingMode(SavingMode savingMode) {
            this.savingMode = savingMode;
            return this;
        }

        public Builder withNotificationMode(NotificationMode notificationMode) {
            this.notificationMode = notificationMode;
            return this;
        }
    }
}

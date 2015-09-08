package alex.bobro.genericdao;


import java.util.HashMap;
import java.util.Map;

public class QueryParameters {

    private Map<String, String> parameters;

    private QueryParameters(Builder builder) {
        this.parameters = builder.parameters;
    }

    Map<String, String> getParameters() {
        return parameters;
    }

    public void addParameter(String key, String value) {
        parameters.put(key, value);
    }

    public String getParameter(String key) {
        return parameters.get(key);
    }

    public static class Builder {
        private Map<String, String> parameters;

        public Builder() {
            parameters = new HashMap<>();
        }

        public Builder(Map<String, String> parameters) {
            this.parameters = new HashMap<>(parameters);
        }

        public QueryParameters build() {
            return new QueryParameters(this);
        }

        public Builder addParameter(String key, String value) {
            parameters.put(key, value);
            return this;
        }
    }
}

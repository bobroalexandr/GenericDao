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

    public static class Builder {
        private Map<String, String> parameters;

        public Builder() {
            parameters = new HashMap<>();
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

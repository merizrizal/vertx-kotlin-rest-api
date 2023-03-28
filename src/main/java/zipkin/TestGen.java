package zipkin;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true, publicConverter = false)
public class TestGen {
    private String serviceName = "";
    private String heyName = "";
    private String className = "";

    public TestGen(JsonObject json) {
        TestGenConverter.fromJson(json, this);
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getHeyName() {
        return heyName;
    }

    public void setHeyName(String heyName) {
        this.heyName = heyName;
    }
}

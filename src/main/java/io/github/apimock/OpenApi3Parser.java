package io.github.apimock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.model.AuthOption;
import org.openapi4j.core.model.v3.OAI3Context;
import org.openapi4j.core.util.TreeUtil;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.validation.v3.OpenApi3Validator;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

public class OpenApi3Parser extends org.openapi4j.parser.OpenApi3Parser {
    private static final String INVALID_SPEC = "Failed to load spec at '%s'";

    public OpenApi3 parse(URL... urls) throws ResolutionException, ValidationException {
        return this.parse(urls, null, false);
    }

    public OpenApi3 parse(URL[] urls, List<AuthOption> authOptions, boolean validate) throws ResolutionException, ValidationException {
        if(urls.length == 1) {
            return parse(urls[0], authOptions, false);
        }
        JsonNode baseDocument = null;
        for (URL url : urls) {
            try {
                OAI3Context context = new OAI3Context(url);
                if(baseDocument == null) {
                    baseDocument = context.getBaseDocument();
                } else {
                    baseDocument = merge(baseDocument, context.getBaseDocument());
                }
            } catch (IllegalArgumentException e) {
                throw new ResolutionException(String.format(INVALID_SPEC, url.toString()), e);
            }
        }

        URL url = urls[0];
        OpenApi3 api;

        try {
            OAI3Context context = new OAI3Context(url);
            api = TreeUtil.json.convertValue(baseDocument, OpenApi3.class);
            api.setContext(context);
        } catch (IllegalArgumentException e) {
            throw new ResolutionException(String.format(INVALID_SPEC, url.toString()), e);
        }

        if (validate) {
            OpenApi3Validator.instance().validate(api);
        }

        return api;
    }

    protected JsonNode merge(JsonNode mainNode, JsonNode updateNode) {

        Iterator<String> fieldNames = updateNode.fieldNames();
        while (fieldNames.hasNext()) {

            String fieldName = fieldNames.next();
            JsonNode jsonNode = mainNode.get(fieldName);
            // if field exists and is an embedded object
            if (jsonNode != null && jsonNode.isObject()) {
                merge(jsonNode, updateNode.get(fieldName));
            }
            else {
                if (mainNode instanceof ObjectNode) {
                    // Overwrite field
                    JsonNode value = updateNode.get(fieldName);
                    ((ObjectNode) mainNode).put(fieldName, value);
                }
            }

        }

        return mainNode;
    }
}
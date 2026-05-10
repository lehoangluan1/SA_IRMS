package SA.irms.reporting.infrastructure.persistence;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import SA.irms.reporting.application.view.ReportValueType;
import SA.irms.reporting.application.view.dynamic.DynamicReportCell;
import SA.irms.reporting.application.view.dynamic.DynamicReportColumn;
import SA.irms.reporting.application.view.dynamic.DynamicReportRow;
import SA.irms.reporting.application.view.dynamic.DynamicReportView;
import SA.irms.reporting.application.view.dynamic.ReportSnapshotPayload;

final class ReportSnapshotPayloadMapper {
    private final ObjectMapper objectMapper;

    ReportSnapshotPayloadMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ReportSnapshotPayload snapshotPayload(String reportCode, String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return ReportSnapshotPayload.empty(reportCode, prettyReportName(reportCode));
        }
        try {
            JsonNode payload = objectMapper.readTree(payloadJson);
            if (payload.has("reportView")) {
                return objectMapper.treeToValue(payload, ReportSnapshotPayload.class);
            }
            return legacyPayload(reportCode, payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored reporting snapshot payload could not be read.", exception);
        }
    }

    String eventPayloadToJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Reporting payload could not be serialized.", exception);
        }
    }

    String snapshotToJson(ReportSnapshotPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload == null
                    ? ReportSnapshotPayload.empty("dynamic-report", "Dynamic Report")
                    : payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Reporting payload could not be serialized.", exception);
        }
    }

    private ReportSnapshotPayload legacyPayload(String reportCode, JsonNode payload) {
        String sourceEventId = payload.path("sourceEventId").isMissingNode() || payload.path("sourceEventId").isNull()
                ? null
                : payload.path("sourceEventId").asText();

        JsonNode metricsNode = payload.has("metrics") ? payload.get("metrics") : payload;
        if (sourceEventId == null && metricsNode != null && metricsNode.has("outboxEventId")) {
            sourceEventId = metricsNode.path("outboxEventId").asText(null);
        }

        DynamicReportView reportView;
        if (metricsNode != null && metricsNode.isObject() && !payload.has("rows")) {
            List<DynamicReportColumn> columns = List.of(
                    new DynamicReportColumn("metricKey", "Metric Key", ReportValueType.TEXT, 0),
                    new DynamicReportColumn("metricLabel", "Metric Label", ReportValueType.TEXT, 1),
                    new DynamicReportColumn("metricValue", "Metric Value", ReportValueType.TEXT, 2)
            );
            List<DynamicReportRow> rows = new java.util.ArrayList<>();
            metricsNode.fields().forEachRemaining(entry -> rows.add(new DynamicReportRow(List.of(
                    new DynamicReportCell("metricKey", entry.getKey(), ReportValueType.TEXT, entry.getKey()),
                    new DynamicReportCell("metricLabel", prettyMetricLabel(entry.getKey()), ReportValueType.TEXT, prettyMetricLabel(entry.getKey())),
                    new DynamicReportCell("metricValue", display(entry.getValue()), inferType(entry.getValue()), scalarValue(entry.getValue()))
            ))));
            reportView = new DynamicReportView(reportCode, prettyReportName(reportCode), columns, rows);
        } else {
            ArrayNode rowsNode = payload.has("rows") && payload.get("rows").isArray() ? (ArrayNode) payload.get("rows") : objectMapper.createArrayNode();
            List<DynamicReportRow> rows = new java.util.ArrayList<>();
            java.util.LinkedHashSet<String> columnKeys = new java.util.LinkedHashSet<>();
            for (JsonNode rowNode : rowsNode) {
                List<DynamicReportCell> cells = new java.util.ArrayList<>();
                rowNode.fields().forEachRemaining(entry -> {
                    columnKeys.add(entry.getKey());
                    cells.add(new DynamicReportCell(entry.getKey(), display(entry.getValue()), inferType(entry.getValue()), scalarValue(entry.getValue())));
                });
                rows.add(new DynamicReportRow(cells));
            }
            List<DynamicReportColumn> columns = new java.util.ArrayList<>();
            int order = 0;
            for (String key : columnKeys) {
                columns.add(new DynamicReportColumn(key, prettyMetricLabel(key), ReportValueType.TEXT, order++));
            }
            reportView = new DynamicReportView(reportCode, prettyReportName(reportCode), columns, rows);
        }
        return new ReportSnapshotPayload(sourceEventId, reportView);
    }

    private Object scalarValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isIntegralNumber()) {
            return value.longValue();
        }
        if (value.isFloatingPointNumber() || value.isBigDecimal()) {
            return value.decimalValue();
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isTextual()) {
            return value.textValue();
        }
        return value;
    }

    private ReportValueType inferType(JsonNode value) {
        if (value == null || value.isNull() || value.isTextual()) {
            return ReportValueType.TEXT;
        }
        if (value.isIntegralNumber()) {
            return ReportValueType.INTEGER;
        }
        if (value.isFloatingPointNumber() || value.isBigDecimal()) {
            return ReportValueType.DECIMAL;
        }
        if (value.isBoolean()) {
            return ReportValueType.BOOLEAN;
        }
        return ReportValueType.JSON;
    }

    private String display(JsonNode value) {
        if (value == null || value.isNull()) {
            return "";
        }
        return value.isValueNode() ? value.asText() : value.toString();
    }

    private String prettyMetricLabel(String key) {
        if (key == null || key.isBlank()) {
            return "Metric";
        }
        StringBuilder builder = new StringBuilder();
        char[] chars = key.replace('-', ' ').replace('_', ' ').toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char current = chars[i];
            if (i > 0 && Character.isUpperCase(current) && Character.isLowerCase(chars[i - 1])) {
                builder.append(' ');
            }
            builder.append(current);
        }
        String value = builder.toString().trim().replaceAll("\\s+", " ");
        return value.isEmpty() ? "Metric" : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String prettyReportName(String reportCode) {
        String label = prettyMetricLabel(reportCode);
        return label.endsWith("Report") ? label : label + " Report";
    }
}

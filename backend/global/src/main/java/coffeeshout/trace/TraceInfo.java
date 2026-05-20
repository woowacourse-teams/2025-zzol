package coffeeshout.trace;

public record TraceInfo(String traceId, String spanId) {

    public boolean traceable() {
        return !traceId.isBlank() && !spanId.isBlank();
    }
}

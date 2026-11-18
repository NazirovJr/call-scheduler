package com.callscheduler.meeting;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

// По ТЗ время приходит в ISO 8601, а стандартный десериализатор принимает ещё и секунды эпохи
@JacksonComponent
class IsoInstantDeserializer extends ValueDeserializer<Instant> {

    @Override
    public Instant deserialize(JsonParser parser, DeserializationContext context) {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            return (Instant) context.handleUnexpectedToken(Instant.class, parser);
        }
        try {
            return Instant.parse(parser.getString());
        } catch (DateTimeParseException e) {
            return (Instant) context.handleWeirdStringValue(Instant.class, parser.getString(), "ожидается ISO 8601");
        }
    }
}

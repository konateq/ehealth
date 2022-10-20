package eu.europa.ec.sante.ehdsi.openncp.gateway.module.eadc.persistence.model;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Converter
public class EadcDbTimeConverter implements AttributeConverter<Instant, String> {

    private static final Logger logger = LoggerFactory.getLogger(EadcDbTimeConverter.class);

    private  final String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";
    private final ZoneId zoneId =  ZoneOffset.UTC; //ZoneId.systemDefault()

    @Override
    public String convertToDatabaseColumn(Instant instant) {
        throw new RuntimeException("No data should be right in the eadc db");
        // DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT).withZone(zoneId);
        // return formatter.format(instant);
    }

    @Override
    public Instant convertToEntityAttribute(String s) {
        if(StringUtils.isBlank(s)) {
            logger.warn("date field is not populated!");
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        try {
            ZonedDateTime timed = ZonedDateTime.parse(s, formatter);
            timed.format(formatter);
            return Instant.from(timed);
        } catch (RuntimeException ex) {
            logger.warn(ex.getMessage());
            return null;
        }
    }
}

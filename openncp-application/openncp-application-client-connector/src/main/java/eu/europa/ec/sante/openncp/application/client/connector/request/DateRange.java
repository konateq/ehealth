package eu.europa.ec.sante.openncp.application.client.connector.request;

import eu.europa.ec.sante.openncp.common.immutables.Domain;

import java.time.LocalDate;
import java.util.Optional;

@Domain
public interface DateRange {
    Optional<LocalDate> getFrom();

    Optional<LocalDate> getTo();
}

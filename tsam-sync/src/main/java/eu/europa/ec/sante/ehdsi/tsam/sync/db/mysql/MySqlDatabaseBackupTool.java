package eu.europa.ec.sante.ehdsi.tsam.sync.db.mysql;

import eu.europa.ec.sante.ehdsi.tsam.sync.db.DatabaseBackupTool;
import eu.europa.ec.sante.ehdsi.tsam.sync.db.DatabaseToolException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Profile("mysql")
public class MySqlDatabaseBackupTool implements DatabaseBackupTool {

    @Value("${openncp.ltrdb.host}")
    private String host;

    @Value("${openncp.ltrdb.port}")
    private String port;

    @Value("${openncp.ltrdb.username}")
    private String username;

    @Value("${openncp.ltrdb.password}")
    private String password;

    @Value("${openncp.ltrdb.database-name}")
    private String database;

    @Override
    public boolean backupDatabase() {

        String date = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
        date = StringUtils.replace(date, ":", "-");
        String output = database + "." + date + ".sql";

        StringBuilder command =
                new StringBuilder("mysqldump --user=" + username + " --password=" + password + " --result-file=" + output);

        if (StringUtils.isNotBlank(host)) {
            command
                    .append(" --host=")
                    .append(host);
        }
        if (StringUtils.isNotBlank(port)) {
            command
                    .append(" --port=")
                    .append(port);
        }
        command.append(" ").append(database);

        try {
            Process process = Runtime.getRuntime().exec(command.toString());
            int exitValue = process.waitFor();
            return exitValue == 0;
        } catch (IOException e) {
            throw new DatabaseToolException("Mysqldump command error", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DatabaseToolException("Database backup process has been interrupted", e);
        }
    }
}

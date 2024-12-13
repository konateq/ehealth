package eu.europa.ec.sante.openncp.api.common;


import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.io.PrintWriter;

public class StreamingResponseBodyUtils {

    public static final int DEFAULT_BUFFER_SIZE = 8192; //8KB

    /**
     * Creates a StreamingResponseBody for streaming binary data from an InputStream with a default buffer size.
     *
     * @param inputStream the InputStream to read the data from
     * @return a StreamingResponseBody that streams data from the InputStream
     */
    public static StreamingResponseBody createFromInputStream(final InputStream inputStream) {
        return createFromInputStream(inputStream, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Creates a StreamingResponseBody for streaming binary data from an InputStream.
     *
     * @param inputStream the InputStream to read the data from
     * @param bufferSize  the buffer size
     * @return a StreamingResponseBody that streams data from the InputStream
     */
    public static StreamingResponseBody createFromInputStream(final InputStream inputStream, final int bufferSize) {
        return outputStream -> {
            try (inputStream) {
                final byte[] buffer = new byte[bufferSize];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        };
    }

    /**
     * Creates a StreamingResponseBody for streaming an error message with a stack trace.
     *
     * @param errorMessage the error message to stream
     * @param exception    the exception whose stack trace will be streamed
     * @return a StreamingResponseBody that streams the error message and stack trace
     */
    public static StreamingResponseBody createErrorResponse(final String errorMessage, final Exception exception) {
        return outputStream -> {
            try (final PrintWriter writer = new PrintWriter(outputStream)) {
                writer.println(errorMessage);
                if (exception != null) {
                    writer.println("Message: " + exception.getMessage());
                    writer.println("Stack Trace:");
                    exception.printStackTrace(writer);
                }
                writer.flush();
            }
        };
    }
}

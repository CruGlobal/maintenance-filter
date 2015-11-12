package org.ccci.maintenance;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaintenanceControlClient
{

    private final Logger log = Logger.getLogger(getClass());
    
    public class Failure
    {
        public Failure(String message, IOException exception)
        {
            this.exception = exception;
            this.message = message;
        }
        
        public final IOException exception;
        public final String message;
    }

    private final HttpClient httpClient = new DefaultHttpClient();
    private final DateTimeFormatter formatter = WindowControlApi.dateTimeFormatter();
    
    public Map<URI, Failure> createOrUpdateWindow(List<URI> servers, MaintenanceWindow window, String key)
    {
        Map<URI, Failure> failures = new HashMap<URI, Failure>(); 
        for (URI server : servers)
        {
            HttpPost httpPost = buildPost(server, window, key);
            HttpResponse response;
            try
            {
                response = httpClient.execute(httpPost);
            }
            catch (IOException e)
            {
                handleFailure(server, e, window, failures);
                continue;
            }
            HttpEntity entity = response.getEntity();
            if (entity == null)
            {
                handleFailure(server, "[no entity body given]", window, failures);
                continue;
            }
            String body;
            try
            {
                body = EntityUtils.toString(entity, WindowControlApi.REQUEST_CHARACTER_ENCODING);
            }
            catch (IOException e)
            {
                handleFailure(server, e, window, failures);
                continue;
            }
            if (response.getStatusLine().getStatusCode() >= 300)
            {
                handleFailure(server, response.getStatusLine(), body, window, failures);
                continue;
            }
            if (!body.equals(WindowControlApi.WINDOW_SUCCESSFULLY_UPDATED_RESPONSE))
            {
                handleFailure(server, body, window, failures);
                continue;
            }
            log.info("successfully updated window on " + server);
        }
        if (failures.isEmpty())
        {
            printConfirmationOfWindowBoundaries(window);
        }
        return failures;
    }

    /** this helps the user verify that he got the timestamps correct in his config file*/
    private void printConfirmationOfWindowBoundaries(MaintenanceWindow window)
    {
        DateTime now = new DateTime();
        String beginPhrase = getRelativeTimePhrase(
            now,
            window.getBeginAt(),
            "began",
            "will begin");
        String endPhrase = getRelativeTimePhrase(
            now,
            window.getEndAt(),
            "ended",
            "will end");

        log.info("window " + beginPhrase + " and " + endPhrase);
    }

    private String getRelativeTimePhrase(
        DateTime now,
        DateTime begin,
        String pastVerb,
        String futureVerb)
    {
        PeriodType type = PeriodType.standard().withMillisRemoved();
        Period period;
        String verb;
        if (now.isBefore(begin))
        {
            period = new Period(now, begin, type);
            verb = futureVerb + " in %s";
        }
        else
        {
            period = new Period(begin, now, type);
            verb = pastVerb + " %s ago";
        }
        PeriodFormatter periodFormatter = PeriodFormat.getDefault();
        return String.format(verb, periodFormatter.print(period));
    }

    private void handleFailure(URI server, 
                               StatusLine statusLine, 
                               String body, 
                               MaintenanceWindow window,
                               Map<URI, Failure> failures)
    {
        failures.put(server, new Failure(
            String.format(
                "Failure sending window %s to %s: %n" +
                "    http status code : %s (%s)%n" +
                "    body: %s", 
                window, 
                server,
                statusLine.getStatusCode(),
                statusLine.getReasonPhrase(),
                body), 
            null));    
    }

    private void handleFailure(URI server, String response, MaintenanceWindow window, Map<URI, Failure> failures)
    {
        failures.put(server, new Failure(
            String.format(
                "Unexpected response while sending window %s to %s: %s", 
                window, 
                server,
                response), 
            null));
    }

    private void handleFailure(URI server, IOException e, MaintenanceWindow window, Map<URI, Failure> failures)
    {
        failures.put(server, new Failure(
            String.format(
                "IOException while sending window %s to %s", 
                window, 
                server),
            e));
    }

    private HttpPost buildPost(URI server, MaintenanceWindow window, String key)
    {
        HttpPost httpPost = new HttpPost(server + "/" + WindowControlApi.CREATE_OR_UPDATE_PATH);
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair(WindowControlApi.ID_PARAMETER, window.getId()));
        parameters.add(new BasicNameValuePair(WindowControlApi.SHORT_MESSAGE_PARAMETER, window.getShortMessage()));
        parameters.add(new BasicNameValuePair(WindowControlApi.LONG_MESSAGE_PARAMETER, window.getLongMessage()));
        parameters.add(new BasicNameValuePair(WindowControlApi.BEGIN_AT_PARAMETER, formatter.print(window.getBeginAt())));
        parameters.add(new BasicNameValuePair(WindowControlApi.END_AT_PARAMETER, formatter.print(window.getEndAt())));
        
        parameters.add(new BasicNameValuePair(WindowControlApi.BYPASS_REQUEST_PARAMETER, "true"));

        UrlEncodedFormEntity entity;
        try
        {
            entity = new UrlEncodedFormEntity(parameters, WindowControlApi.REQUEST_CHARACTER_ENCODING);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new AssertionError(WindowControlApi.REQUEST_CHARACTER_ENCODING + " should be present on any compliant JVM");
        }
        httpPost.setEntity(entity);

        httpPost.setHeader(new BasicHeader("Authorization", "Key " + key));
        return httpPost;
    }
    
    public void shutdown()
    {
        httpClient.getConnectionManager().shutdown();
    }
    
}

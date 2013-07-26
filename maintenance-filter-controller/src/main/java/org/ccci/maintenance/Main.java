package org.ccci.maintenance;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ccci.maintenance.MaintenanceControlClient.Failure;
import org.ccci.maintenance.util.ProgramFailureException;

public class Main
{

    private MaintenanceControlClient client;
    private String configurationFileName = "maintenanceWindowUpdate.yml";
    
    public static void main(String[] args)
    {
        int exitCode;
        try
        {
            Main main = new Main();
            main.boot(args);
            try
            {
                main.run();
            }
            finally
            {
                main.shutdown();
            }
            exitCode = 0;
        }
        catch (ProgramFailureException e)
        {
            System.err.println(e.getLocalizedMessage());
            exitCode = e.getExitCode();
        }
        catch (RuntimeException e)
        {
            System.err.println("Internal programming error:");
            e.printStackTrace();
            exitCode = 1;
        }
        System.exit(exitCode);
    }
    
    void boot(String[] args)
    {
        client = new MaintenanceControlClient();
        
        //simple for now; may be later use jcommander
        if (args.length > 0)
        {
            if (args.length != 2)
                throw new ProgramFailureException("invalid arguments; only accepted argument is '--configurationFile configurationFileName'");
            if (args[0].equals("--configurationFile"))
            {
                configurationFileName = args[1];
            }
            else
            {
                throw new ProgramFailureException("invalid arguments; only accepted argument is '--configurationFile configurationFileName'");
            }
        }
    }
    
    void shutdown()
    {
        client.shutdown();
    }
    
    private void run()
    {
        ConfigFileReader reader = new ConfigFileReader();
        MaintenanceWindowUpdate windowUpdate = reader.readConfigFile(configurationFileName);
        List<URI> servers = buildServerControlUris(windowUpdate);
        String key = getKey(windowUpdate);
        Map<URI, Failure> failures = client.createOrUpdateWindow(servers, windowUpdate.getWindow(), key);
        handleOutcome(servers, failures);
    }

    private String getKey(MaintenanceWindowUpdate windowUpdate) {
        String key = windowUpdate.getKey();
        if (key == null)
            throw new ProgramFailureException("configuration contains no key");
        return key;
    }

    private List<URI> buildServerControlUris(MaintenanceWindowUpdate windowUpdate)
    {
        List<URI> servers;
        try
        {
            servers = new ArrayList<URI>();
            for (String serverControlUrl : windowUpdate.getServerControlUrls())
            {
                servers.add(new URI(serverControlUrl));
            }
        }
        catch (URISyntaxException e)
        {
            throw new ProgramFailureException("configuration contains bad server control url: " + e.getMessage(), e);
        }
        return servers;
    }

    private void handleOutcome(List<URI> servers, Map<URI, Failure> failures)
    {
        for (Map.Entry<URI, Failure> failureEntry : failures.entrySet())
        {
            Failure failure = failureEntry.getValue();
            System.err.println(failure.message);
            if (failure.exception != null)
            {
                System.err.print("  ");
                System.err.println(failure.exception.toString());
            }
        }
        boolean allFailed = failures.size() == servers.size();
        if (allFailed)
        {
            throw new ProgramFailureException("All " + failures.size() + " updates failed");
        }
        else if (failures.isEmpty())
        {
            System.out.println("All " + servers.size() + " updates succeeded");
        }
        else
        {
            System.out.println(failures.size() +" updates failed, " + (servers.size() - failures.size()) + " updates succeeded");
        }
    }

}
